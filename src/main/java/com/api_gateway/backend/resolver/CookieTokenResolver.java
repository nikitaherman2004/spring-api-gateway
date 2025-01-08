package com.api_gateway.backend.resolver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Optional;

@Component
public class CookieTokenResolver implements TokenResolver {

    @Value("${cookie.auth}")
    private String authCookieName;

    public Optional<String> resolveAccessToken(ServerHttpRequest request) {
        MultiValueMap<String, HttpCookie> cookieMap = request.getCookies();
        List<HttpCookie> cookies = cookieMap.get(authCookieName);

        if (cookies == null || cookies.isEmpty()) {
            return Optional.empty();
        }

        HttpCookie cookie = cookies.get(0);

        return Optional.of(cookie.getValue());
    }
}
