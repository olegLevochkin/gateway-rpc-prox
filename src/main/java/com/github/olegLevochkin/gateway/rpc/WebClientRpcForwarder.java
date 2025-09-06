package com.github.olegLevochkin.gateway.rpc;

import com.github.olegLevochkin.gateway.config.AppConfig;
import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import lombok.extern.slf4j.Slf4j;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

@Slf4j
public record WebClientRpcForwarder(WebClient client, String targetUrl, CircuitBreaker circuitBreaker)
        implements AutoCloseable {

    private static final int DEFAULT_MAX_POOL_SIZE = 200;
    private static final boolean TRUST_ALL_CERTIFICATES = true;
    private static final boolean VERIFY_HOSTNAME = false;
    private static final String CIRCUIT_BREAKER_NAME = "upstream-rpc";

    public static WebClientRpcForwarder of(Vertx vertx, AppConfig config) {
        WebClientOptions options = new WebClientOptions()
                .setKeepAlive(true)
                .setTrustAll(TRUST_ALL_CERTIFICATES)
                .setVerifyHost(VERIFY_HOSTNAME)
                .setMaxPoolSize(DEFAULT_MAX_POOL_SIZE);

        WebClient webClient = WebClient.create(vertx, options);

        CircuitBreaker breaker = null;
        if (config.circuitBreakerEnabled()) {
            breaker = CircuitBreaker.create(CIRCUIT_BREAKER_NAME, vertx, new CircuitBreakerOptions()
                    .setMaxFailures(config.circuitBreakerFailuresThreshold())
                    .setResetTimeout(config.circuitBreakerResetTimeoutMs())
                    .setTimeout(config.requestTimeoutMs())
                    .setFallbackOnFailure(false));
        }

        return new WebClientRpcForwarder(webClient, config.targetRpcUrl(), breaker);
    }

    public Future<Buffer> forward(Buffer jsonBody, int timeoutMillis) {
        if (circuitBreaker == null) {
            return doRequest(jsonBody, timeoutMillis)
                    .onFailure(this::logUpstreamFailure);
        }

        return circuitBreaker.<Buffer>execute(promise ->
                doRequest(jsonBody, timeoutMillis)
                        .onSuccess(promise::complete)
                        .onFailure(promise::fail)
        ).onFailure(this::logUpstreamFailure);
    }

    private Future<Buffer> doRequest(Buffer jsonBody, int timeoutMillis) {
        return client.postAbs(targetUrl)
                .putHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())
                .timeout(timeoutMillis)
                .sendBuffer(jsonBody)
                .map(HttpResponse::bodyAsBuffer);
    }

    private void logUpstreamFailure(Throwable error) {
        log.warn("Upstream call failed: {}", error.toString());
    }

    @Override
    public void close() {
        try {
            client.close();
        } catch (Exception e) {
            log.warn("WebClient close failed", e);
        }
    }
}
