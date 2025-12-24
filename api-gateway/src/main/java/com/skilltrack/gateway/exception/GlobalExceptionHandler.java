package com.skilltrack.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

/**
 * Global error handler for Spring Cloud Gateway (WebFlux).
 *
 * Handles:
 * - Downstream connectivity failures (mapped to 503)
 * - Any uncaught exceptions (mapped to 500)
 *
 * Note: HTTP errors returned by downstream services are typically passed through
 * as-is by the gateway and won't reach this handler.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Unexpected error";

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : message;
        } else if (hasCause(ex, ConnectException.class)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Downstream service unavailable";
        }

        if (status.is5xxServerError()) {
            log.warn("Gateway error: path={} status={} message={}", exchange.getRequest().getURI().getPath(), status.value(), ex.getMessage());
        }

        return write(exchange, status, message);
    }

    private Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getURI().getPath();
        String body = "{\"timestamp\":\"" + OffsetDateTime.now() + "\"," +
                "\"status\":" + status.value() + "," +
                "\"message\":\"" + escapeJson(message) + "\"," +
                "\"path\":\"" + escapeJson(path) + "\"," +
                "\"errors\":[]}";

        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private static boolean hasCause(Throwable ex, Class<? extends Throwable> type) {
        Throwable cur = ex;
        while (cur != null) {
            if (type.isInstance(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
