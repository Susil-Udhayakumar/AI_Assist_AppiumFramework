package io.framework.api;

import io.framework.core.exception.FrameworkException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Default {@link HttpTransport} over the JDK HttpClient. Not unit-tested (network); the
 *  ApiClient logic is tested with a fake transport. */
public final class JdkHttpTransport implements HttpTransport {

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    @Override
    public ApiResponse send(ApiRequest request) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(request.url()))
                    .timeout(Duration.ofSeconds(30));
            request.headers().forEach(builder::header);
            HttpRequest.BodyPublisher body = request.body() == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(request.body());
            builder.method(request.method().name(), body);

            HttpResponse<String> response = http.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());

            Map<String, String> headers = new LinkedHashMap<>();
            response.headers().map().forEach((k, v) -> headers.put(k, String.join(",", v)));
            return new ApiResponse(response.statusCode(), response.body(), headers);
        } catch (IOException e) {
            throw new FrameworkException("API request failed: " + request.method() + " " + request.url(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FrameworkException("API request interrupted: " + request.url(), e);
        }
    }
}
