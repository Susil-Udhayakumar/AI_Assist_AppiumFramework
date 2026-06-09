package io.framework.api;

/** Sends an {@link ApiRequest} and returns an {@link ApiResponse}. Abstracted so ApiClient
 *  request-building and response handling are unit-tested without real network I/O. */
@FunctionalInterface
public interface HttpTransport {
    ApiResponse send(ApiRequest request);
}
