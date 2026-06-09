package io.framework.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiClientTest {

    /** Fake transport that records the request and returns a canned response. */
    private static final class Capturing implements HttpTransport {
        ApiRequest last;
        public ApiResponse send(ApiRequest request) {
            this.last = request;
            return new ApiResponse(200, "{\"ok\":true}", Map.of());
        }
    }

    @Test
    void getJoinsBaseUrlAndPath() {
        var transport = new Capturing();
        new ApiClient("https://api.example.com/", transport).get("/users/1");

        assertThat(transport.last.method()).isEqualTo(HttpMethod.GET);
        assertThat(transport.last.url()).isEqualTo("https://api.example.com/users/1");
        assertThat(transport.last.body()).isNull();
    }

    @Test
    void postCarriesBody() {
        var transport = new Capturing();
        new ApiClient("https://api.example.com", transport).post("orders", "{\"qty\":2}");

        assertThat(transport.last.method()).isEqualTo(HttpMethod.POST);
        assertThat(transport.last.url()).isEqualTo("https://api.example.com/orders");
        assertThat(transport.last.body()).isEqualTo("{\"qty\":2}");
    }

    @Test
    void bearerAuthAddsHeader() {
        var transport = new Capturing();
        new ApiClient("https://api.example.com", transport).withBearer("tok123").get("/me");

        assertThat(transport.last.headers()).containsEntry("Authorization", "Bearer tok123");
    }

    @Test
    void absolutePathOverridesBaseUrl() {
        assertThat(ApiClient.join("https://api.example.com", "https://other.com/x"))
                .isEqualTo("https://other.com/x");
    }

    @Test
    void responseFieldsAreUsable() {
        var transport = new Capturing();
        ApiResponse r = new ApiClient("https://api.example.com", transport).get("/ping");
        assertThat(r.is2xx()).isTrue();
        assertThat(r.jsonField("ok")).contains(true);
    }
}
