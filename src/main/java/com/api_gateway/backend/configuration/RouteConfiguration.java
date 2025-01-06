package com.api_gateway.backend.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfiguration {

    @Value("${spring.cloud.gateway.routes.auth_service_route}")
    private String authServiceRoute;

    @Value("${spring.cloud.gateway.routes.project_service_route}")
    private String projectServiceRoute;

    @Value("${spring.cloud.gateway.uris.auth_service_uri}")
    private String authServiceUri;

    @Value("${spring.cloud.gateway.uris.project_service_uri}")
    private String projectServiceUri;

    @Bean
    public RouteLocator configureRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(authServiceRoute, (configuration) -> configuration
                        .path("/oauth2/**", "/login/**", "/user/**", "/user-details/**")
                        .uri(authServiceUri))
                .route(projectServiceRoute, (configuration) -> configuration
                        .path("/access/**", "/project/**")
                        .uri(projectServiceUri))
                .build();
    }
}