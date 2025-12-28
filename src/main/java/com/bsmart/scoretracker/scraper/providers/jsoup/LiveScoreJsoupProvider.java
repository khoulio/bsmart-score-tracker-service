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
 * Scraper LiveScore utilisant Jsoup (léger, sans Chrome)
 */
@Component
@Profile("jsoup")
@Slf4j
public class LiveScoreJsoupProvider implements MatchScraperProvider {

    private static final String CSS_SCORE = "#score-or-time";
    private static final String CSS_STATUS = "#SEV__status";

    @Override
    public ProviderType supports() {
        return ProviderType.LIVE_SCORE;
    }

    @Override
    @CircuitBreaker(name = "liveScoreScraper", fallbackMethod = "fallback")
    @RateLimiter(name = "liveScoreScraper")
    public MatchSnapshot fetch(String url) {
        log.info("📳 LiveScoreJsoupProvider - Fetching score from: {}", url);

        try {
            Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get();

            // Sélectionner l'élément contenant le score
            Element scoreElement = doc.select(CSS_SCORE).first();
            Element statusElement = doc.select(CSS_STATUS).first();

            if (scoreElement == null) {
                log.info("Match not started yet (no score element found)");
                return MatchSnapshot.builder()
                    .found(true)
                    .rawStatus("SCHEDULED")
                    .home(null)
                    .away(null)
                    .minute(null)
                    .build();
            }

            // Récupérer le texte du score et du statut
            String scoreText = scoreElement.text();
            String statusText = statusElement != null ? statusElement.text() : "";

            log.info("Score found: {}", scoreText);
            log.info("Status found: {}", statusText);

            // Parser le score (format "2-1" ou "2 - 1")
            String[] scoreParts = scoreText.split("-");
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

            // Détecter la minute si en direct
            String minute = null;
            if (statusText.contains("'")) {
                try {
                    minute = statusText.replaceAll("[^0-9]", "");
                } catch (Exception ignored) {
                }
            }

            // Normaliser le statut
            String rawStatus;
            if ("Half Time".equals(statusText)) {
                rawStatus = "PAUSED";
            } else if (statusText.contains("Pen") || statusText.equals("Pen")) {
                rawStatus = "IN_PLAY";
            } else if ("Full Time".equals(statusText) || "FT".equals(statusText)) {
                rawStatus = "FINISHED";
            } else if (statusText.contains("'")) {
                rawStatus = "IN_PLAY";
            } else {
                rawStatus = statusText;
            }

            // Note: Jsoup provider cannot extract penalty scores from HTML
            // Only the Selenium provider can extract them from __NEXT_DATA__ JSON
            return MatchSnapshot.builder()
                .found(true)
                .home(homeScore)
                .away(awayScore)
                .rawStatus(rawStatus)
                .minute(minute)
                .penaltyHome(null)
                .penaltyAway(null)
                .build();

        } catch (IOException e) {
            log.error("Error fetching from LiveScore: {}", e.getMessage(), e);
            return MatchSnapshot.builder()
                .found(false)
                .build();
        }
    }

    /**
     * Fallback en cas d'échec du circuit breaker
     */
    public MatchSnapshot fallback(String url, Exception e) {
        log.warn("LiveScore scraper fallback triggered for URL: {} - {}", url, e.getMessage());
        return MatchSnapshot.builder()
            .found(false)
            .build();
    }
}
