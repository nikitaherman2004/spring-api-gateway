package com.api_gateway.backend.filter;

import com.api_gateway.backend.dto.SubjectRoleDto;
import com.api_gateway.backend.exception.ApplicationException;
import com.api_gateway.backend.proxy.AuthServiceProxy;
import com.api_gateway.backend.resolver.CompositeTokenResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GlobalSecurityAuthFilter implements GlobalFilter, Ordered {

    private static final String BEARER = "Bearer";

    @Value("${attributes.sub}")
    private String subAttribute;

    @Value("${attributes.role}")
    private String roleAttribute;

    private final AuthServiceProxy authServiceProxy;

    private final CompositeTokenResolver compositeTokenResolver;

    private final SecurityAuthFilterConfiguration filterConfiguration;

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange, GatewayFilterChain chain) {
        ServerHttpRequest request = serverWebExchange.getRequest();

        if (allowRequest(request)) {
            return chain.filter(serverWebExchange);
        }

        return processFilter(request, serverWebExchange, chain);
    }

    private Mono<Void> processFilter(
            ServerHttpRequest request, ServerWebExchange serverWebExchange, GatewayFilterChain chain
    ) {
        try {
            Optional<String> optionalToken = compositeTokenResolver.resolveAccessToken(request);

            if (optionalToken.isEmpty()) {
                return setResponseCompleteWithStatusCode(serverWebExchange, HttpStatus.UNAUTHORIZED);
            }

            String accessTokenValue = optionalToken.get();

            SubjectRoleDto currentUserInfo = authServiceProxy.getCurrentAuthenticatedUser(accessTokenValue);

            ServerWebExchange mutatedExchange = attachAuthHeadersAndMutateExchange(
                    request, currentUserInfo, accessTokenValue, serverWebExchange
            );

            return chain.filter(mutatedExchange);
        } catch (ApplicationException exception) {
            return setResponseCompleteWithStatusCode(serverWebExchange, exception.getStatus());
        }
    }

    /**
     * This method returns true if at least one request from the list of allowed http requests matches
     */
    private Boolean allowRequest(ServerHttpRequest request) {
        String requestPath = request.getPath().toString();

        List<String> permittedUrls = filterConfiguration.getPermittedUrls();

        return permittedUrls.stream()
                .anyMatch(requestPath::startsWith);
    }

    private ServerWebExchange attachAuthHeadersAndMutateExchange(
            ServerHttpRequest request, SubjectRoleDto currentUserInfo,
            String accessTokenValue, ServerWebExchange serverWebExchange
    ) {
        ServerHttpRequest authenticatedRequest = request.mutate()
                .header(roleAttribute, currentUserInfo.getRole())
                .header(subAttribute, currentUserInfo.getSub())
                .header(BEARER, accessTokenValue)
                .build();

        return serverWebExchange.mutate()
                .request(authenticatedRequest)
                .build();
    }

    private Mono<Void> setResponseCompleteWithStatusCode(ServerWebExchange serverWebExchange, HttpStatus httpStatus) {
        ServerHttpResponse response = serverWebExchange.getResponse();
        response.setStatusCode(httpStatus);

        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
