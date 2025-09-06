package com.github.olegLevochkin.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.olegLevochkin.gateway.metrics.MetricsNames.JSONRPC_CALLS_TOTAL;
import static com.github.olegLevochkin.gateway.metrics.MetricsNames.TAG_METHOD;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

public record JsonRpcMetricsHandler(MeterRegistry meterRegistry) implements Handler<RoutingContext> {

    private static final String FIELD_CALLS = "calls";

    @Override
    public void handle(RoutingContext context) {
        Map<String, Long> countsByMethod = collectCountsByMethod();
        JsonObject payload = new JsonObject().put(FIELD_CALLS, countsByMethod);

        context.response()
                .putHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())
                .end(payload.encode());
    }

    private Map<String, Long> collectCountsByMethod() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Counter counter : meterRegistry.find(JSONRPC_CALLS_TOTAL).counters()) {
            String method = counter.getId().getTag(TAG_METHOD);
            if (method != null && !method.isBlank()) {
                result.put(method, (long) counter.count());
            }
        }
        return result;
    }
}
