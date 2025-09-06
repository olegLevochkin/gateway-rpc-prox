package com.github.olegLevochkin.gateway.http;

import com.github.olegLevochkin.gateway.config.AppConfig;
import com.github.olegLevochkin.gateway.rpc.WebClientRpcForwarder;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.ext.web.Router;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public final class HttpServerFactory {

    private static final String BIND_HOST = "0.0.0.0";
    private static final String SCHEME_HTTP = "http";
    private static final String SCHEME_HTTPS = "https";

    private final Vertx vertx;
    private final AppConfig config;

    private HttpServer server;
    private WebClientRpcForwarder forwarder;

    public void start(int port) {
        if (server != null) {
            throw new IllegalStateException("HTTP server is already started");
        }

        HttpServerOptions options = buildServerOptions(port);

        forwarder = WebClientRpcForwarder.of(vertx, config);
        Router router = RouterFactory.create(vertx, config, forwarder);

        server = vertx.createHttpServer(options).requestHandler(router);
        server.listen()
                .onSuccess(s -> log.info("HTTP server started on {}://localhost:{}",
                        config.tlsEnabled() ? SCHEME_HTTPS : SCHEME_HTTP, port))
                .onFailure(err -> log.error("Failed to start HTTP server on port {}", port, err));
    }

    public void stop() {
        try {
            if (server != null) {
                server.close();
                server = null;
            }
        } finally {
            if (forwarder != null) {
                forwarder.close();
                forwarder = null;
            }
        }
        log.info("HTTP server stopped");
    }

    private HttpServerOptions buildServerOptions(int port) {
        HttpServerOptions options = new HttpServerOptions()
                .setHost(BIND_HOST)
                .setPort(port);

        if (config.tlsEnabled()) {
            options.setSsl(true)
                    .setKeyCertOptions(new PfxOptions()
                            .setPath(config.pkcs12Path())
                            .setPassword(config.pkcs12Password()));
        }
        return options;
    }
}
