package io.framework.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    private ApiResponse response(int status, String body) {
        return new ApiResponse(status, body, Map.of());
    }

    @Test
    void detects2xx() {
        assertThat(response(200, "").is2xx()).isTrue();
        assertThat(response(204, "").is2xx()).isTrue();
        assertThat(response(404, "").is2xx()).isFalse();
        assertThat(response(500, "").is2xx()).isFalse();
    }

    @Test
    void extractsTopLevelJsonField() {
        var r = response(200, "{\"id\": 42, \"name\": \"alice\"}");
        assertThat(r.jsonField("id")).contains(42);
        assertThat(r.jsonField("name")).contains("alice");
    }

    @Test
    void extractsNestedJsonField() {
        var r = response(200, "{\"user\": {\"name\": \"bob\", \"role\": \"admin\"}}");
        assertThat(r.jsonField("user.role")).contains("admin");
    }

    @Test
    void emptyWhenFieldMissing() {
        var r = response(200, "{\"id\": 1}");
        assertThat(r.jsonField("missing")).isEmpty();
        assertThat(r.jsonField("id.deep")).isEmpty();
    }
}
