package com.api_gateway.backend.filter;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Component
public class SecurityAuthFilterConfiguration {

    private final List<String> permittedUrls = List.of("/oauth2", "/login");
}
