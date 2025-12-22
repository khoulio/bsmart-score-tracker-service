package com.bsmart.scoretracker.scraper.providers;

import com.bsmart.scoretracker.dto.MatchSnapshot;
import com.bsmart.scoretracker.exception.ScraperException;
import com.bsmart.scoretracker.model.enums.ProviderType;
import com.bsmart.scoretracker.scraper.MatchScraperProvider;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "selenium.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class OneFootballScraperProvider implements MatchScraperProvider {

    private final WebDriver webDriver;

    @Override
    public ProviderType supports() {
        return ProviderType.ONE_FOOTBALL;
    }

    @Override
    @CircuitBreaker(name = "oneFootballScraper", fallbackMethod = "fallbackFetch")
    @RateLimiter(name = "oneFootballScraper")
    public MatchSnapshot fetch(String url) {
        log.info("Scraping OneFootball: {}", url);

        try {
            webDriver.get(url);
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(15));

            // Wait for page body to load
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            // Additional wait for JavaScript to render
            Thread.sleep(3000);

            // Try to extract from JSON data in page source
            String pageSource = webDriver.getPageSource();

            // Debug: Log a snippet of the page source to understand the structure
            if (log.isDebugEnabled()) {
                int matchScoreIndex = pageSource.indexOf("matchScore");
                if (matchScoreIndex > 0) {
                    int start = Math.max(0, matchScoreIndex - 100);
                    int end = Math.min(pageSource.length(), matchScoreIndex + 2000);
                    log.debug("Page source snippet around 'matchScore': {}",
                        pageSource.substring(start, end));
                }

                // Also log homeTeam and awayTeam sections separately
                int homeIndex = pageSource.indexOf("\"homeTeam\"");
                if (homeIndex > 0) {
                    int homeEnd = Math.min(pageSource.length(), homeIndex + 200);
                    log.debug("homeTeam section: {}", pageSource.substring(homeIndex, homeEnd));
                }

                int awayIndex = pageSource.indexOf("\"awayTeam\"");
                if (awayIndex > 0) {
                    int awayEnd = Math.min(pageSource.length(), awayIndex + 200);
                    log.debug("awayTeam section: {}", pageSource.substring(awayIndex, awayEnd));
                }
            }

            // Extract minute FIRST - this is key to determine if match is live
            String minute = extractMinuteFromPage(wait, pageSource);

            // Extract scores - NOTE: OneFootball seems to have them inverted!
            Integer homeScoreRaw = extractHomeScoreFromPage(wait, pageSource);
            Integer awayScoreRaw = extractAwayScoreFromPage(wait, pageSource);

            // INVERT scores because OneFootball has them backwards
            Integer homeScore = awayScoreRaw;  // Home = Away from JSON
            Integer awayScore = homeScoreRaw;  // Away = Home from JSON

            log.debug("Extracted raw: home={}, away={} | Corrected: home={}, away={}",
                homeScoreRaw, awayScoreRaw, homeScore, awayScore);

            // Extract status - pass minute to help determine correct status
            String status = extractStatusFromPage(wait, pageSource, minute);

            log.info("OneFootball scrape result - Status: {}, Score: {}-{}, Minute: {}",
                status, homeScore, awayScore, minute);

            return MatchSnapshot.builder()
                .status(status)
                .home(homeScore)
                .away(awayScore)
                .minute(minute)
                .rawStatus(status)
                .found(true)
                .build();

        } catch (Exception e) {
            log.error("Failed to scrape OneFootball: {}", e.getMessage(), e);
            throw new ScraperException("OneFootball scraping failed: " + e.getMessage(), e);
        }
    }

    private String extractStatusFromPage(WebDriverWait wait, String pageSource, String minute) {
        // IMPORTANT: If we have a valid minute, the match is IN PROGRESS!
        if (minute != null && !minute.isEmpty()) {
            String minuteLower = minute.toLowerCase();

            // Check for FINISHED/Full time FIRST
            if (minuteLower.contains("full time") || minuteLower.contains("fulltime") ||
                minuteLower.contains("ft") || minuteLower.contains("terminé") ||
                minuteLower.contains("finished")) {
                log.debug("Match is FINISHED based on minute: {}", minute);
                return "FT";
            }

            // Check for half-time
            if (minuteLower.contains("half") || minuteLower.contains("ht") ||
                minuteLower.contains("mi-temps") || minuteLower.equals("45'")) {
                log.debug("Match is HALF_TIME based on minute: {}", minute);
                return "HT";
            }

            // Try to parse as number for IN_PLAY detection
            String minuteNum = minute.replaceAll("[^0-9]", ""); // Remove ' or other chars

            if (!minuteNum.isEmpty()) {
                try {
                    int min = Integer.parseInt(minuteNum);

                    // Any minute from 0 to 120 means match is live
                    if (min >= 0 && min <= 120) {
                        log.debug("Match is LIVE based on minute: {}", minute);
                        return "LIVE";
                    }
                } catch (NumberFormatException e) {
                    log.debug("Could not parse minute as number: {}", minute);
                }
            }

            // If we have a minute but couldn't parse it, default to SCHEDULED
            log.warn("Unknown minute format '{}' - defaulting to SCHEDULED", minute);
            return "SCHEDULED";
        }

        // If no minute, check for explicit status indicators in JSON
        // Be VERY specific - look for exact JSON patterns
        if (pageSource.contains("\"matchScore\"")) {
            // Look for explicit LIVE indicators
            if (pageSource.contains("\"liveBadge\":\"LIVE\"") ||
                pageSource.contains("\"liveBadge\":\"En direct\"") ||
                pageSource.contains("\"status\":\"LIVE\"") ||
                pageSource.contains("\"status\":\"IN_PLAY\"")) {
                return "LIVE";
            }

            // Look for HalfTime - be specific in JSON
            if (pageSource.contains("\"status\":\"HALF_TIME\"") ||
                pageSource.contains("\"status\":\"HT\"") ||
                pageSource.contains("\"timePeriod\":\"HT\"")) {
                return "HT";
            }

            // Look for FullTime - be specific in JSON, not just anywhere in HTML
            if (pageSource.contains("\"status\":\"FINISHED\"") ||
                pageSource.contains("\"status\":\"FULL_TIME\"") ||
                pageSource.contains("\"status\":\"FT\"") ||
                (pageSource.contains("\"timePeriod\":\"FT\"") && !pageSource.contains("\"timePeriod\":\"\""))) {
                return "FT";
            }

            // PreMatch/Scheduled
            if (pageSource.contains("\"status\":\"SCHEDULED\"") ||
                pageSource.contains("\"status\":\"PREMATCH\"") ||
                pageSource.contains("PreMatch")) {
                return "SCHEDULED";
            }
        }

        // Try DOM elements as fallback
        try {
            WebElement statusEl = webDriver.findElement(By.cssSelector("[data-testid='match-status']"));
            String text = statusEl.getText().trim();
            if (!text.isEmpty()) {
                log.debug("Found status from DOM: {}", text);
                return text;
            }
        } catch (Exception e) {
            log.debug("Could not extract status from DOM: {}", e.getMessage());
        }

        log.warn("Could not extract status, defaulting to SCHEDULED");
        return "SCHEDULED";
    }

    private Integer extractHomeScoreFromPage(WebDriverWait wait, String pageSource) {
        // Try multiple patterns for home score
        String[] patterns = {
            "\"homeTeam\"\\s*:\\s*\\{[^}]*\"score\"\\s*:\\s*\"([^\"]+)\"",
            "\"homeTeam\"\\s*:\\s*\\{[^}]*\"score\"\\s*:\\s*([0-9]+)",
            "homeTeam[^}]*score[^:]*:\\s*\"([^\"]+)\"",
            "homeTeam[^}]*score[^:]*:\\s*([0-9]+)"
        };

        for (String patternStr : patterns) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patternStr);
            java.util.regex.Matcher matcher = pattern.matcher(pageSource);
            if (matcher.find()) {
                String score = matcher.group(1);
                log.debug("Extracted home score '{}' using pattern: {}", score, patternStr);
                if (!score.equals("-") && !score.isEmpty() && !score.equals("null")) {
                    try {
                        return Integer.parseInt(score);
                    } catch (NumberFormatException e) {
                        log.debug("Home score '{}' is not a number", score);
                    }
                }
                return null; // Found the pattern but score is "-" or empty
            }
        }

        // Try DOM elements as fallback
        try {
            WebElement scoreEl = webDriver.findElement(By.cssSelector("[data-testid='home-score']"));
            String text = scoreEl.getText().trim();
            if (!text.equals("-") && !text.isEmpty()) {
                return Integer.parseInt(text);
            }
        } catch (Exception e) {
            log.debug("Could not extract home score from DOM: {}", e.getMessage());
        }

        return null;
    }

    private Integer extractAwayScoreFromPage(WebDriverWait wait, String pageSource) {
        // Try multiple patterns for away score
        String[] patterns = {
            "\"awayTeam\"\\s*:\\s*\\{[^}]*\"score\"\\s*:\\s*\"([^\"]+)\"",
            "\"awayTeam\"\\s*:\\s*\\{[^}]*\"score\"\\s*:\\s*([0-9]+)",
            "awayTeam[^}]*score[^:]*:\\s*\"([^\"]+)\"",
            "awayTeam[^}]*score[^:]*:\\s*([0-9]+)"
        };

        for (String patternStr : patterns) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patternStr);
            java.util.regex.Matcher matcher = pattern.matcher(pageSource);
            if (matcher.find()) {
                String score = matcher.group(1);
                log.debug("Extracted away score '{}' using pattern: {}", score, patternStr);
                if (!score.equals("-") && !score.isEmpty() && !score.equals("null")) {
                    try {
                        return Integer.parseInt(score);
                    } catch (NumberFormatException e) {
                        log.debug("Away score '{}' is not a number", score);
                    }
                }
                return null; // Found the pattern but score is "-" or empty
            }
        }

        // Try DOM elements as fallback
        try {
            WebElement scoreEl = webDriver.findElement(By.cssSelector("[data-testid='away-score']"));
            String text = scoreEl.getText().trim();
            if (!text.equals("-") && !text.isEmpty()) {
                return Integer.parseInt(text);
            }
        } catch (Exception e) {
            log.debug("Could not extract away score from DOM: {}", e.getMessage());
        }

        return null;
    }

    private String extractMinuteFromPage(WebDriverWait wait, String pageSource) {
        // Try to extract minute from timePeriod in JSON
        String minutePattern = "\"timePeriod\":\"([^\"]+)\"";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(minutePattern);
        java.util.regex.Matcher matcher = pattern.matcher(pageSource);
        if (matcher.find()) {
            String minute = matcher.group(1);
            if (!minute.isEmpty() && !minute.equals("null")) {
                return minute;
            }
        }

        // Try DOM elements as fallback
        try {
            WebElement minuteEl = webDriver.findElement(By.cssSelector("[data-testid='match-minute']"));
            return minuteEl.getText().trim();
        } catch (Exception e) {
            log.debug("Could not extract minute from DOM: {}", e.getMessage());
        }

        return null;
    }

    private MatchSnapshot fallbackFetch(String url, Exception e) {
        log.error("Circuit breaker fallback for OneFootball: {}", url, e);
        return MatchSnapshot.builder()
            .found(false)
            .build();
    }
}
