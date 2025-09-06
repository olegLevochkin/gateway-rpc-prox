package com.github.olegLevochkin.gateway;

import com.github.olegLevochkin.gateway.config.AppConfig;
import com.github.olegLevochkin.gateway.config.ConfigLoader;
import com.github.olegLevochkin.gateway.http.HttpServerFactory;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GatewayRpcProxyApplication {

    public static void main(String[] args) {
        AppConfig config = ConfigLoader.load();

        Vertx vertx = createVertxWithMetrics();
        HttpServerFactory httpServerFactory = new HttpServerFactory(vertx, config);

        addShutdownHook(httpServerFactory, vertx);

        httpServerFactory.start(config.httpPort());
        log.info("Starting Gateway RPC Proxy on port {} -> {} (timeout={}ms, maxBody={})",
                config.httpPort(), config.targetRpcUrl(), config.requestTimeoutMs(), config.maxBodyBytes());
    }

    private static Vertx createVertxWithMetrics() {
        VertxOptions options = new VertxOptions()
                .setMetricsOptions(new MicrometerMetricsOptions()
                        .setEnabled(true)
                        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true)));
        return Vertx.vertx(options);
    }

    private static void addShutdownHook(HttpServerFactory httpServerFactory, Vertx vertx) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            try {
                httpServerFactory.stop();
            } catch (Exception e) {
                log.warn("HTTP server stop failed", e);
            }
            try {
                vertx.close();
            } catch (Exception e) {
                log.warn("Vert.x close failed", e);
            }
        }, "shutdown-hook"));
    }
}
