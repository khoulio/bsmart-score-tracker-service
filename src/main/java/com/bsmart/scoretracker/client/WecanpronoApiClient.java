package com.bsmart.scoretracker.client;

import com.bsmart.scoretracker.dto.external.ExternalCompetitionDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * Client pour communiquer avec l'API WECANPRONO-SERVICE via la découverte de service Eureka.
 * Utilise un RestTemplate avec @LoadBalanced pour résoudre automatiquement l'adresse du service.
 */
@Component
@Slf4j
public class WecanpronoApiClient {

    private final RestTemplate restTemplate;
    private final String serviceName;

    public WecanpronoApiClient(
            RestTemplate restTemplate,
            @Value("${wecanprono.service-name:wecanprono-service}") String serviceName) {
        this.restTemplate = restTemplate;
        this.serviceName = serviceName;
    }

    /**
     * Récupère toutes les compétitions depuis WECANPRONO-SERVICE.
     * Utilise la découverte de service Eureka pour résoudre l'adresse.
     */
    public List<ExternalCompetitionDTO> fetchCompetitions() {
        String url = "http://" + serviceName + "/api/competitions";

        log.info("Fetching competitions from service: {} (URL: {})", serviceName, url);

        try {
            ResponseEntity<List<ExternalCompetitionDTO>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ExternalCompetitionDTO>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully fetched {} competitions from {}", response.getBody().size(), serviceName);
                return response.getBody();
            } else {
                log.warn("Received non-2xx status from {}: {}", serviceName, response.getStatusCode());
                return Collections.emptyList();
            }

        } catch (RestClientException e) {
            log.error("Error fetching competitions from {}: {}", serviceName, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Récupère une compétition spécifique par son ID externe.
     * Utilise la découverte de service Eureka pour résoudre l'adresse.
     */
    public ExternalCompetitionDTO fetchCompetitionById(Long externalId) {
        String url = "http://" + serviceName + "/api/competitions/" + externalId;

        log.info("Fetching competition {} from service: {} (URL: {})", externalId, serviceName, url);

        try {
            ResponseEntity<ExternalCompetitionDTO> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                ExternalCompetitionDTO.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully fetched competition {} from {}", externalId, serviceName);
                return response.getBody();
            } else {
                log.warn("Received non-2xx status from {}: {}", serviceName, response.getStatusCode());
                return null;
            }

        } catch (RestClientException e) {
            log.error("Error fetching competition {} from {}: {}",
                externalId, serviceName, e.getMessage(), e);
            return null;
        }
    }
}
