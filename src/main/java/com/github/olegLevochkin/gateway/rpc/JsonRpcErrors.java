package com.github.olegLevochkin.gateway.rpc;

import io.vertx.core.json.JsonObject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonRpcErrors {

    private static final String FIELD_JSONRPC = "jsonrpc";
    private static final String FIELD_ERROR = "error";
    private static final String FIELD_CODE = "code";
    private static final String FIELD_MESSAGE = "message";
    private static final String FIELD_ID = "id";

    private static final String JSONRPC_VERSION_2_0 = "2.0";

    private static final String MSG_INVALID_REQUEST = "Invalid Request";
    private static final String MSG_UPSTREAM_UNAVAILABLE = "Upstream unavailable";

    public static final int CODE_INVALID_REQUEST = -32600;
    public static final int CODE_UPSTREAM_UNAVAILABLE = -32000;

    public static JsonObject invalidRequest(Object id) {
        return error(CODE_INVALID_REQUEST, MSG_INVALID_REQUEST, id);
    }

    public static JsonObject upstreamUnavailable(Object id) {
        return error(CODE_UPSTREAM_UNAVAILABLE, MSG_UPSTREAM_UNAVAILABLE, id);
    }

    private static JsonObject error(int code, String message, Object id) {
        JsonObject payload = new JsonObject()
                .put(FIELD_JSONRPC, JSONRPC_VERSION_2_0)
                .put(FIELD_ERROR, new JsonObject()
                        .put(FIELD_CODE, code)
                        .put(FIELD_MESSAGE, message));

        if (id == null) {
            payload.putNull(FIELD_ID);
        } else {
            payload.put(FIELD_ID, id);
        }
        return payload;
    }
}
