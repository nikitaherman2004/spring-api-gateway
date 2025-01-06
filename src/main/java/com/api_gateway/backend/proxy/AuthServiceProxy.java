package com.api_gateway.backend.proxy;

import com.api_gateway.backend.dto.SubjectRoleDto;
import com.api_gateway.backend.exception.ApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.function.Supplier;

@Slf4j
@Service
public class AuthServiceProxy {

    private static final String BEARER = "Bearer";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.cloud.gateway.uris.auth_service_uri}")
    private String authServiceUri;

    public SubjectRoleDto getCurrentAuthenticatedUser(String accessToken) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(BEARER, accessToken);

        RequestEntity<Void> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, createCurrentUserUri());

        ResponseEntity<SubjectRoleDto> responseEntity = handleHttpClientException(() -> restTemplate.exchange(
                requestEntity, SubjectRoleDto.class
        ));

        return responseEntity.getBody();
    }

    private ResponseEntity<SubjectRoleDto> handleHttpClientException(Supplier<ResponseEntity<SubjectRoleDto>> fetcher) {
        try {
            return fetcher.get();
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            log.warn(
                    "The authorization service threw an authorization error with the message {}",
                    exception.getMessage()
            );
            throw new ApplicationException(exception.getMessage(), HttpStatus.UNAUTHORIZED);
        } catch (RestClientException exception) {
            log.warn(
                    "When sending a request to user/current, " +
                            "the authorization service returned an error with the message {}",
                    exception.getMessage()
            );

            throw new ApplicationException(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private URI createCurrentUserUri() {
        String currentUserUri = authServiceUri + "/oauth2/current";

        return URI.create(currentUserUri);
    }
}
