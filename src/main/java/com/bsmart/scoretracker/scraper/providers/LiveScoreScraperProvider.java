package com.bsmart.scoretracker.scraper.providers;

import com.bsmart.scoretracker.dto.MatchSnapshot;
import com.bsmart.scoretracker.exception.ScraperException;
import com.bsmart.scoretracker.model.enums.ProviderType;
import com.bsmart.scoretracker.scraper.MatchScraperProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.inject.Provider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;

@RequiredArgsConstructor
@Slf4j
@Profile("selenium")
@Component
public class LiveScoreScraperProvider implements MatchScraperProvider {

    private final Provider<WebDriver> webDriverProvider;

    @Override
    public ProviderType supports() {
        return ProviderType.LIVE_SCORE;
    }

    @Override
    @CircuitBreaker(name = "liveScoreScraper", fallbackMethod = "fallbackFetch")
    @RateLimiter(name = "liveScoreScraper")
    public MatchSnapshot fetch(String url) {
        log.info("Scraping LiveScore: {}", url);
        WebDriver webDriver = webDriverProvider.get();

        try {
            webDriver.get(url);
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(15));

            // Wait for page body to load
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            // Additional wait for JavaScript to render
            Thread.sleep(3000);

            // Get page source to extract JSON data
            String pageSource = webDriver.getPageSource();

            // Extract data from __NEXT_DATA__ JSON
            String minute = extractMinuteFromPage(pageSource);
            String status = extractStatusFromPage(pageSource, minute);
            Integer homeScore = extractHomeScoreFromPage(pageSource);
            Integer awayScore = extractAwayScoreFromPage(pageSource);
            Integer penaltyHomeScore = extractPenaltyHomeScoreFromPage(pageSource);
            Integer penaltyAwayScore = extractPenaltyAwayScoreFromPage(pageSource);

            if (penaltyHomeScore != null && penaltyAwayScore != null) {
                log.info("LiveScore scrape result - Status: {}, Score: {}-{}, Penalties: {}-{}, Minute: {}",
                    status, homeScore, awayScore, penaltyHomeScore, penaltyAwayScore, minute);
            } else {
                log.info("LiveScore scrape result - Status: {}, Score: {}-{}, Minute: {}",
                    status, homeScore, awayScore, minute);
            }

            return MatchSnapshot.builder()
                .status(status)
                .home(homeScore)
                .away(awayScore)
                .minute(minute)
                .rawStatus(status)
                .found(true)
                .penaltyHome(penaltyHomeScore)
                .penaltyAway(penaltyAwayScore)
                .build();

        } catch (Exception e) {
            log.error("Failed to scrape LiveScore: {}", e.getMessage(), e);
            throw new ScraperException("LiveScore scraping failed: " + e.getMessage(), e);
        } finally {
            if (webDriver != null) {
                webDriver.quit();
            }
        }
    }

    private String extractStatusFromPage(String pageSource, String minute) {
        try {
            // CRITICAL: Check minute FIRST (more reliable than eventStatus)
            // LiveScore sometimes shows "EventScheduled" even when match is live!
            if (minute != null && !minute.isEmpty()) {
                String minuteLower = minute.toLowerCase();

                // Check for half-time (more specific, should come before full time)
                if (minuteLower.equals("halftime") || minuteLower.equals("ht") ||
                    minuteLower.contains("half-time") || minuteLower.contains("mi-temps")) {
                    log.debug("Match is PAUSED based on minute: {}", minute);
                    return "HT";
                }

                // Check for penalty shootout (still LIVE/IN_PLAY)
                if (minuteLower.equals("pen") || minuteLower.contains("penalties")) {
                    log.debug("Match is IN PENALTY SHOOTOUT (still LIVE) based on minute: {}", minute);
                    return "LIVE";
                }

                // Check for full time
                if (minuteLower.equals("aet") || minuteLower.contains("after extra time")) {
                    log.debug("Match is FINISHED (after extra time) based on minute: {}", minute);
                    return "FT";
                }

                if (minuteLower.equals("ap") || minuteLower.contains("after penalties")) {
                    log.debug("Match is FINISHED (after penalties) based on minute: {}", minute);
                    return "FT";
                }

                if (minuteLower.equals("et") || minuteLower.contains("extra time")) {
                    log.debug("Match is IN EXTRA TIME (still LIVE) based on minute: {}", minute);
                    return "LIVE";
                }

                if (minuteLower.contains("full time") || minuteLower.contains("ft") ||
                    minuteLower.contains("finished")) {
                    log.debug("Match is FINISHED based on minute: {}", minute);
                    return "FT";
                }

                // Check if it's a numeric minute (means match is LIVE)
                if (minuteLower.matches(".*\\d+'.*")) {
                    log.debug("Match is LIVE based on minute: {}", minute);
                    return "LIVE";
                }
            }

            // Extract eventStatus from JSON as FALLBACK only
            String pattern = "\"eventStatus\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = compiledPattern.matcher(pageSource);

            if (matcher.find()) {
                String eventStatus = matcher.group(1);
                log.debug("Extracted eventStatus: {}", eventStatus);

                // Map LiveScore status to our status
                if (eventStatus.equalsIgnoreCase("LIVE") || eventStatus.equalsIgnoreCase("IN_PROGRESS")) {
                    return "LIVE";
                } else if (eventStatus.equalsIgnoreCase("FINISHED") || eventStatus.equalsIgnoreCase("FT")) {
                    return "FT";
                } else if (eventStatus.equalsIgnoreCase("HALFTIME") || eventStatus.equalsIgnoreCase("HT")) {
                    return "HT";
                } else if (eventStatus.equalsIgnoreCase("SCHEDULED") ||
                           eventStatus.equalsIgnoreCase("NOT_STARTED") ||
                           eventStatus.equalsIgnoreCase("EventScheduled")) {
                    return "SCHEDULED";
                }

                // Unknown eventStatus - return as-is for debugging
                log.warn("Unknown eventStatus: {}", eventStatus);
                return eventStatus;
            }

            log.warn("Could not extract status from LiveScore");
            return "SCHEDULED";

        } catch (Exception e) {
            log.warn("Error extracting status: {}", e.getMessage());
            return "SCHEDULED";
        }
    }

    private Integer extractHomeScoreFromPage(String pageSource) {
        try {
            // Extract homeTeamScore from JSON: "homeTeamScore":"1"
            String pattern = "\"homeTeamScore\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = compiledPattern.matcher(pageSource);

            if (matcher.find()) {
                String score = matcher.group(1);
                if (!score.equals("-") && !score.isEmpty()) {
                    try {
                        return Integer.parseInt(score);
                    } catch (NumberFormatException e) {
                        log.debug("Home score '{}' is not a number", score);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting home score: {}", e.getMessage());
        }
        return null;
    }

    private Integer extractAwayScoreFromPage(String pageSource) {
        try {
            // Extract awayTeamScore from JSON: "awayTeamScore":"0"
            String pattern = "\"awayTeamScore\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = compiledPattern.matcher(pageSource);

            if (matcher.find()) {
                String score = matcher.group(1);
                if (!score.equals("-") && !score.isEmpty()) {
                    try {
                        return Integer.parseInt(score);
                    } catch (NumberFormatException e) {
                        log.debug("Away score '{}' is not a number", score);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting away score: {}", e.getMessage());
        }
        return null;
    }

    private String extractMinuteFromPage(String pageSource) {
        try {
            // Extract status (which contains minute) from JSON: "status":"50'"
            String pattern = "\"status\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = compiledPattern.matcher(pageSource);

            if (matcher.find()) {
                String minute = matcher.group(1);
                if (!minute.isEmpty() && !minute.equalsIgnoreCase("null")) {
                    log.debug("Extracted minute: {}", minute);
                    return minute;
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract minute: {}", e.getMessage());
        }
        return null;
    }

    private Integer extractPenaltyHomeScoreFromPage(String pageSource) {
        try {
            // Extract penaltyHomeScore from JSON: "penaltyHomeScore":"0"
            String pattern = "\"penaltyHomeScore\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = compiledPattern.matcher(pageSource);

            if (matcher.find()) {
                String score = matcher.group(1);
                if (!score.equals("-") && !score.isEmpty()) {
                    try {
                        return Integer.parseInt(score);
                    } catch (NumberFormatException e) {
                        log.debug("Penalty home score '{}' is not a number", score);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error extracting penalty home score: {}", e.getMessage());
        }
        return null;
    }

    private Integer extractPenaltyAwayScoreFromPage(String pageSource) {
        try {
            // Extract penaltyAwayScore from JSON: "penaltyAwayScore":"1"
            String pattern = "\"penaltyAwayScore\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = compiledPattern.matcher(pageSource);

            if (matcher.find()) {
                String score = matcher.group(1);
                if (!score.equals("-") && !score.isEmpty()) {
                    try {
                        return Integer.parseInt(score);
                    } catch (NumberFormatException e) {
                        log.debug("Penalty away score '{}' is not a number", score);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error extracting penalty away score: {}", e.getMessage());
        }
        return null;
    }

    private MatchSnapshot fallbackFetch(String url, Exception e) {
        log.error("Circuit breaker fallback for LiveScore: {}", url, e);
        return MatchSnapshot.builder()
            .found(false)
            .build();
    }
}
