package com.bsmart.scoretracker.client;

import com.bsmart.scoretracker.dto.external.ExternalPhaseDTO;
import lombok.RequiredArgsConstructor;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class WecanpronoMatchApiClient {

    private final RestTemplate restTemplate;

    @Value("${gateway.url:http://localhost:8222}")
    private String gatewayUrl;

    private static final String SERVICE_NAME = "WECANPRONO-SERVICE";
    private static final Long DEFAULT_USER_ID = 1L;
    private static final boolean DEFAULT_IS_MY = false;

    /**
     * Récupère toutes les phases avec leurs matches pour une compétition donnée
     *
     * @param competitionId ID de la compétition externe
     * @return Liste des phases avec leurs rencontres
     */
    public List<ExternalPhaseDTO> fetchPhasesWithMatches(Long competitionId) {
        try {
            String url = String.format("%s/%s/api/rencontres/all?competitionId=%d&userId=%d&isMy=%b",
                    gatewayUrl, SERVICE_NAME, competitionId, DEFAULT_USER_ID, DEFAULT_IS_MY);

            log.info("Fetching phases and matches from external API: {}", url);

            ResponseEntity<List<ExternalPhaseDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ExternalPhaseDTO>>() {}
            );

            List<ExternalPhaseDTO> phases = response.getBody();
            log.info("Successfully fetched {} phases from external API", phases != null ? phases.size() : 0);

            return phases != null ? phases : Collections.emptyList();

        } catch (RestClientException e) {
            log.error("Error fetching phases and matches from external API for competitionId={}: {}",
                    competitionId, e.getMessage());
            return Collections.emptyList();
        }
    }


}
