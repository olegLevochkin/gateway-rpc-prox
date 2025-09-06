package com.github.olegLevochkin.gateway.rpc;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import static com.github.olegLevochkin.gateway.metrics.MetricsNames.JSONRPC_CALLS_TOTAL;
import static com.github.olegLevochkin.gateway.metrics.MetricsNames.TAG_METHOD;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_GATEWAY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;

public record RpcHandler(
        WebClientRpcForwarder forwarder,
        MeterRegistry meterRegistry,
        int timeoutMs
) implements Handler<RoutingContext> {

    private static final String FIELD_METHOD = "method";

    @Override
    public void handle(RoutingContext context) {
        Buffer body = context.body() != null ? context.body().buffer() : null;
        if (body == null || body.length() == 0) {
            respondJson(context, BAD_REQUEST.code(), JsonRpcErrors.invalidRequest(null));
            return;
        }

        String source = body.toString();
        boolean valid = JsonRpcValidator.forEachValid(source, obj -> trackMethod(obj.getString(FIELD_METHOD)));
        if (!valid) {
            respondJson(context, BAD_REQUEST.code(), JsonRpcErrors.invalidRequest(null));
            return;
        }

        forwarder.forward(body, timeoutMs)
                .onSuccess(buf -> respondOk(context, buf))
                .onFailure(err -> respondJson(context, BAD_GATEWAY.code(), JsonRpcErrors.upstreamUnavailable(null)));
    }

    private void trackMethod(String method) {
        if (meterRegistry == null || method == null || method.isBlank()) return;
        meterRegistry.counter(JSONRPC_CALLS_TOTAL, TAG_METHOD, method).increment();
    }

    private void respondOk(RoutingContext context, Buffer payload) {
        context.response()
                .putHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())
                .end(payload);
    }

    private void respondJson(RoutingContext context, int statusCode, JsonObject payload) {
        context.response()
                .setStatusCode(statusCode)
                .putHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())
                .end(payload.encode());
    }
}
