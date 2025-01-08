package com.api_gateway.backend.resolver;

import lombok.RequiredArgsConstructor;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CompositeTokenResolver implements TokenResolver {

    private final List<TokenResolver> tokenResolvers;

    public Optional<String> resolveAccessToken(ServerHttpRequest request) {
        for (TokenResolver resolver : tokenResolvers) {
            Optional<String> optionalToken = resolver.resolveAccessToken(request);

            if (optionalToken.isPresent()) {
                return optionalToken;
            }
        }

        return Optional.empty();
    }
}
