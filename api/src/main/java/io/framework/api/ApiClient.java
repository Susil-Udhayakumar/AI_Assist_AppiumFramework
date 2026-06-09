package io.framework.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small REST client over a {@link HttpTransport}. Immutable: {@code withHeader}/{@code withBearer}
 * return a new client so per-request auth/headers compose cleanly. Usable for standalone API
 * suites or mid-flow inside a mobile test (seed data, read an OTP, verify backend state).
 */
public final class ApiClient {

    private final String baseUrl;
    private final HttpTransport transport;
    private final Map<String, String> defaultHeaders;

    public ApiClient(String baseUrl, HttpTransport transport) {
        this(baseUrl, transport, Map.of());
    }

    private ApiClient(String baseUrl, HttpTransport transport, Map<String, String> defaultHeaders) {
        this.baseUrl = baseUrl;
        this.transport = transport;
        this.defaultHeaders = Map.copyOf(defaultHeaders);
    }

    public ApiClient withHeader(String name, String value) {
        Map<String, String> merged = new LinkedHashMap<>(defaultHeaders);
        merged.put(name, value);
        return new ApiClient(baseUrl, transport, merged);
    }

    public ApiClient withBearer(String token) {
        return withHeader("Authorization", "Bearer " + token);
    }

    public ApiResponse get(String path) {
        return send(HttpMethod.GET, path, null);
    }

    public ApiResponse post(String path, String body) {
        return send(HttpMethod.POST, path, body);
    }

    public ApiResponse put(String path, String body) {
        return send(HttpMethod.PUT, path, body);
    }

    public ApiResponse patch(String path, String body) {
        return send(HttpMethod.PATCH, path, body);
    }

    public ApiResponse delete(String path) {
        return send(HttpMethod.DELETE, path, null);
    }

    private ApiResponse send(HttpMethod method, String path, String body) {
        return transport.send(new ApiRequest(method, join(baseUrl, path), defaultHeaders, body));
    }

    static String join(String base, String path) {
        if (path == null || path.isEmpty()) {
            return base;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String b = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String p = path.startsWith("/") ? path : "/" + path;
        return b + p;
    }
}
