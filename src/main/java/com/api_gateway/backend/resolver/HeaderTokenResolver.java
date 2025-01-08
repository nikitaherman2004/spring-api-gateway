package com.api_gateway.backend.resolver;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class HeaderTokenResolver implements TokenResolver {

    private static final String AUTHORIZATION = "Authorization";

    public Optional<String> resolveAccessToken(ServerHttpRequest request) {
        HttpHeaders requestHeaders = request.getHeaders();

        List<String> values = requestHeaders.get(AUTHORIZATION);

        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        String accessToken = values.get(0);

        return Optional.of(accessToken);
    }
}
