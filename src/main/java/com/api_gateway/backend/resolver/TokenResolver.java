package com.api_gateway.backend.resolver;

import org.springframework.http.server.reactive.ServerHttpRequest;

import java.util.Optional;

public interface TokenResolver {

    Optional<String> resolveAccessToken(ServerHttpRequest request);
}
