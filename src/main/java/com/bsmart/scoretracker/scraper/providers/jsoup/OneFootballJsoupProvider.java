package com.bsmart.scoretracker.scraper.providers.jsoup;

import com.bsmart.scoretracker.dto.MatchSnapshot;
import com.bsmart.scoretracker.model.enums.ProviderType;
import com.bsmart.scoretracker.scraper.MatchScraperProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Scraper OneFootball utilisant Jsoup (léger, sans Chrome)
 */
@Component
@Profile("jsoup")
@Slf4j
public class OneFootballJsoupProvider implements MatchScraperProvider {

    private static final String CSS_SCORE = ".title-2-bold.MatchScore_numeric__ke8YT";
    private static final String CSS_STATUS = "div.matchHeader__status"; // À ajuster selon la page réelle

    @Override
    public ProviderType supports() {
        return ProviderType.ONE_FOOTBALL;
    }

    @Override
    @CircuitBreaker(name = "oneFootballScraper", fallbackMethod = "fallback")
    @RateLimiter(name = "oneFootballScraper")
    public MatchSnapshot fetch(String url) {
        log.info("📳 OneFootballJsoupProvider - Fetching score from: {}", url);

        try {
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

            // Sélectionner l'élément contenant le score
            Element scoreElement = doc.select(CSS_SCORE).first();

            if (scoreElement == null) {
                // Match pas encore commencé
                log.info("Match not started yet (no score element found)");
                return MatchSnapshot.builder()
                    .found(true)
                    .rawStatus("SCHEDULED")
                    .home(null)
                    .away(null)
                    .minute(null)
                    .build();
            }

            // Récupérer le texte du score
            String scoreText = scoreElement.text();
            log.info("Score found: {}", scoreText);

            // Parser le score (format "2:1" ou "2 : 1")
            String[] scoreParts = scoreText.split(":");
            Integer homeScore = null;
            Integer awayScore = null;

            if (scoreParts.length == 2) {
                try {
                    homeScore = Integer.parseInt(scoreParts[0].trim());
                    awayScore = Integer.parseInt(scoreParts[1].trim());
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse score: {}", scoreText);
                }
            }

            // Récupérer le statut
            Element statusElement = doc.select(CSS_STATUS).first();
            String rawStatus = statusElement != null ? statusElement.text() : "";
            log.info("Status found: {}", rawStatus);

            // Détecter la minute si en direct
            String minute = null;
            if (rawStatus.contains("'")) {
                try {
                    minute = rawStatus.replaceAll("[^0-9]", "");
                } catch (Exception ignored) {
                }
            }

            // Vérifier Half Time
            boolean isHalfTime = false;
            if (doc.select(CSS_STATUS).size() > 1) {
                Element secondStatus = doc.select(CSS_STATUS).get(1);
                isHalfTime = secondStatus != null && "Half time".equals(secondStatus.text());
            }

            if (isHalfTime) {
                rawStatus = "PAUSED";
            } else if (rawStatus.contains("Live") || rawStatus.contains("'")) {
                rawStatus = "IN_PLAY";
            } else if (rawStatus.contains("Finished") || rawStatus.contains("FT")) {
                rawStatus = "FINISHED";
            }

            return MatchSnapshot.builder()
                .found(true)
                .home(homeScore)
                .away(awayScore)
                .rawStatus(rawStatus)
                .minute(minute)
                .build();

        } catch (IOException e) {
            log.error("Error fetching from OneFootball: {}", e.getMessage(), e);
            return MatchSnapshot.builder()
                .found(false)
                .build();
        }
    }

    /**
     * Fallback en cas d'échec du circuit breaker
     */
    public MatchSnapshot fallback(String url, Exception e) {
        log.warn("OneFootball scraper fallback triggered for URL: {} - {}", url, e.getMessage());
        return MatchSnapshot.builder()
            .found(false)
            .build();
    }
}
