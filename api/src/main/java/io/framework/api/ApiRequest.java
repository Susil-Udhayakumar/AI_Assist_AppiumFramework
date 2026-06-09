package io.framework.api;

import java.util.Map;

/** An immutable HTTP request. body is null for bodyless methods. */
public record ApiRequest(HttpMethod method, String url, Map<String, String> headers, String body) {

    public ApiRequest {
        headers = Map.copyOf(headers);
    }
}
