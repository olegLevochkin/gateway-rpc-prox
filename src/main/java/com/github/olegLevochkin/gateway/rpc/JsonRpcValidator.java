package com.github.olegLevochkin.gateway.rpc;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonRpcValidator {

    private static final String FIELD_JSONRPC = "jsonrpc";
    private static final String FIELD_METHOD = "method";
    private static final String JSONRPC_2_0 = "2.0";
    private static final char ARRAY_START = '[';

    public static boolean forEachValid(String source, Consumer<JsonObject> consumer) {
        try {
            String trimmed = source == null ? "" : source.stripLeading();
            if (trimmed.isEmpty()) return false;

            if (trimmed.charAt(0) == ARRAY_START) {
                JsonArray array = new JsonArray(source);
                if (array.isEmpty()) return false;
                for (int i = 0; i < array.size(); i++) {
                    Object element = array.getValue(i);
                    if (!(element instanceof JsonObject obj) || isInvalidObject(obj)) {
                        return false;
                    }
                    consumer.accept(obj);
                }
                return true;
            }

            JsonObject object = new JsonObject(source);
            if (isInvalidObject(object)) return false;
            consumer.accept(object);
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }

    private static boolean isInvalidObject(JsonObject object) {
        if (!JSONRPC_2_0.equals(object.getString(FIELD_JSONRPC))) return true;
        String method = object.getString(FIELD_METHOD);
        return method == null || method.isBlank();
    }
}
