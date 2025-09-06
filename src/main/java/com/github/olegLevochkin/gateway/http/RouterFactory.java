package com.github.olegLevochkin.gateway.http;

import com.github.olegLevochkin.gateway.config.AppConfig;
import com.github.olegLevochkin.gateway.metrics.JsonRpcMetricsHandler;
import com.github.olegLevochkin.gateway.rpc.RpcHandler;
import com.github.olegLevochkin.gateway.rpc.WebClientRpcForwarder;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.micrometer.PrometheusScrapingHandler;
import io.vertx.micrometer.backends.BackendRegistries;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class RouterFactory {

    private static final String PATH_RPC = "/rpc";
    private static final String PATH_METRICS = "/metrics";
    private static final String PATH_PROMETHEUS = "/prometheus";
    private static final String PATH_HEALTH = "/health";

    private static final String CONTEXT_REQUEST_ID = "reqId";
    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEALTH_OK = "OK";

    public static Router create(Vertx vertx,
                                AppConfig config,
                                WebClientRpcForwarder forwarder) {
        final Router router = Router.router(vertx);

        router.route().handler(createBodyHandler(config));
        router.route().handler(new AccessLogHandler());
        router.route().handler(RouterFactory::ensureRequestId);

        final MeterRegistry meterRegistry = BackendRegistries.getDefaultNow();

        router.post(PATH_RPC).handler(new RpcHandler(forwarder, meterRegistry, config.requestTimeoutMs()));
        router.get(PATH_METRICS).handler(new JsonRpcMetricsHandler(meterRegistry));
        router.get(PATH_PROMETHEUS).handler(PrometheusScrapingHandler.create());
        router.get(PATH_HEALTH).handler(ctx -> ctx.response().end(HEALTH_OK));

        return router;
    }

    private static BodyHandler createBodyHandler(AppConfig config) {
        BodyHandler handler = BodyHandler.create();
        handler.setHandleFileUploads(false);
        handler.setBodyLimit(config.maxBodyBytes());
        return handler;
    }

    private static void ensureRequestId(RoutingContext context) {
        String requestId = context.request().getHeader(HEADER_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        context.put(CONTEXT_REQUEST_ID, requestId);
        context.response().putHeader(HEADER_REQUEST_ID, requestId);
        context.next();
    }
}
