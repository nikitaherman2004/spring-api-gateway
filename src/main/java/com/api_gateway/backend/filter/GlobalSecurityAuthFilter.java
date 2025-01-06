package com.api_gateway.backend.filter;

import com.api_gateway.backend.dto.SubjectRoleDto;
import com.api_gateway.backend.exception.ApplicationException;
import com.api_gateway.backend.proxy.AuthServiceProxy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GlobalSecurityAuthFilter implements GlobalFilter, Ordered {

    @Value("${attributes.sub}")
    private String subAttribute;

    @Value("${attributes.role}")
    private String roleAttribute;

    @Value("${cookie.auth}")
    private String authCookieName;

    private final AuthServiceProxy authServiceProxy;

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
            String accessTokenValue = resolveAccessToken(request);
            SubjectRoleDto currentUserInfo = authServiceProxy.getCurrentAuthenticatedUser(accessTokenValue);

            ServerWebExchange mutatedExchange = attachAuthHeadersAndMutateExchange(
                    request, currentUserInfo, serverWebExchange
            );

            return chain.filter(mutatedExchange);
        } catch (ApplicationException exception) {
            return setResponseCompleteWithStatusCode(serverWebExchange, exception.getStatus());
        }
    }

    private Boolean allowRequest(ServerHttpRequest request) {
        String requestPath = request.getPath().toString();

        List<String> permittedUrls = filterConfiguration.getPermittedUrls();

        return permittedUrls.stream()
                .anyMatch(requestPath::startsWith);
    }

    private ServerWebExchange attachAuthHeadersAndMutateExchange(
            ServerHttpRequest request, SubjectRoleDto currentUserInfo, ServerWebExchange serverWebExchange
    ) {
        ServerHttpRequest authenticatedRequest = request.mutate()
                .header(roleAttribute, currentUserInfo.getRole())
                .header(subAttribute, currentUserInfo.getSub())
                .build();

        return serverWebExchange.mutate()
                .request(authenticatedRequest)
                .build();
    }

    private String resolveAccessToken(ServerHttpRequest request) {
        MultiValueMap<String, HttpCookie> cookieMap = request.getCookies();
        List<HttpCookie> cookies = cookieMap.get(authCookieName);

        if (cookies == null || cookies.isEmpty()) {
            throw new ApplicationException("Cookie is empty or null", HttpStatus.UNAUTHORIZED);
        }

        HttpCookie cookie = cookies.get(0);

        return cookie.getValue();
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
