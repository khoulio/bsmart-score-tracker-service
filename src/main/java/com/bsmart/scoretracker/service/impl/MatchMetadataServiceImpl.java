package com.bsmart.scoretracker.service.impl;

import com.bsmart.scoretracker.dto.MatchMetadata;
import com.bsmart.scoretracker.exception.ScraperException;
import com.bsmart.scoretracker.service.MatchMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchMetadataServiceImpl implements MatchMetadataService {

    private final WebDriver webDriver;

    @Override
    public MatchMetadata extractMetadataFromUrl(String url) {
        log.info("Extracting metadata from OneFootball: {}", url);

        try {
            webDriver.get(url);
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(15));

            // Wait for page body to load
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));

            // Additional wait for JavaScript to render
            Thread.sleep(3000);

            String pageSource = webDriver.getPageSource();

            MatchMetadata metadata = MatchMetadata.builder()
                .matchUrl(url)
                .build();

            // Auto-detect provider from URL
            detectProviderFromUrl(url, metadata);

            // Extract team names
            extractTeamNames(wait, pageSource, metadata);

            // Extract competition
            extractCompetition(wait, pageSource, metadata);

            // Extract venue
            extractVenue(wait, pageSource, metadata);

            // Extract match date
            extractMatchDate(wait, pageSource, metadata);

            log.info("Metadata extracted: {} vs {} - {} at {} ({})",
                metadata.getHomeTeam(), metadata.getAwayTeam(),
                metadata.getCompetition(), metadata.getVenue(), metadata.getKickoffUtc());

            return metadata;

        } catch (Exception e) {
            log.error("Failed to extract metadata: {}", e.getMessage(), e);
            throw new ScraperException("Metadata extraction failed: " + e.getMessage(), e);
        }
    }

    private void extractTeamNames(WebDriverWait wait, String pageSource, MatchMetadata metadata) {
        try {
            // Try OneFootball JSON extraction first
            String[] onefootballPatterns = {
                "\"homeTeam\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"",
                "\"awayTeam\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\""
            };

            Pattern homePattern = Pattern.compile(onefootballPatterns[0]);
            Matcher homeMatcher = homePattern.matcher(pageSource);
            if (homeMatcher.find()) {
                String homeTeam = cleanTeamName(homeMatcher.group(1));
                metadata.setHomeTeam(homeTeam);
                log.debug("Home team from OneFootball JSON: {}", homeTeam);
            }

            Pattern awayPattern = Pattern.compile(onefootballPatterns[1]);
            Matcher awayMatcher = awayPattern.matcher(pageSource);
            if (awayMatcher.find()) {
                String awayTeam = cleanTeamName(awayMatcher.group(1));
                metadata.setAwayTeam(awayTeam);
                log.debug("Away team from OneFootball JSON: {}", awayTeam);
            }

            // Try LiveScore JSON extraction if OneFootball failed
            if (metadata.getHomeTeam() == null || metadata.getAwayTeam() == null) {
                String[] livescorePatterns = {
                    "\"homeTeamName\"\\s*:\\s*\"([^\"]+)\"",
                    "\"awayTeamName\"\\s*:\\s*\"([^\"]+)\""
                };

                Pattern lsHomePattern = Pattern.compile(livescorePatterns[0]);
                Matcher lsHomeMatcher = lsHomePattern.matcher(pageSource);
                if (lsHomeMatcher.find() && metadata.getHomeTeam() == null) {
                    String homeTeam = cleanTeamName(lsHomeMatcher.group(1));
                    metadata.setHomeTeam(homeTeam);
                    log.debug("Home team from LiveScore JSON: {}", homeTeam);
                }

                Pattern lsAwayPattern = Pattern.compile(livescorePatterns[1]);
                Matcher lsAwayMatcher = lsAwayPattern.matcher(pageSource);
                if (lsAwayMatcher.find() && metadata.getAwayTeam() == null) {
                    String awayTeam = cleanTeamName(lsAwayMatcher.group(1));
                    metadata.setAwayTeam(awayTeam);
                    log.debug("Away team from LiveScore JSON: {}", awayTeam);
                }
            }

            // Fallback to DOM if JSON extraction failed
            if (metadata.getHomeTeam() == null || metadata.getAwayTeam() == null) {
                extractTeamNamesFromDOM(wait, metadata);
            }

            // Last resort: extract from page title
            if (metadata.getHomeTeam() == null || metadata.getAwayTeam() == null) {
                String title = webDriver.getTitle();
                if (title.contains(" vs ")) {
                    String[] teams = title.split(" vs ");
                    if (teams.length == 2) {
                        if (metadata.getHomeTeam() == null) {
                            metadata.setHomeTeam(cleanTeamName(teams[0].trim()));
                        }
                        if (metadata.getAwayTeam() == null) {
                            metadata.setAwayTeam(cleanTeamName(teams[1].trim()));
                        }
                        log.debug("Team names extracted from title: {} vs {}",
                            metadata.getHomeTeam(), metadata.getAwayTeam());
                    }
                }
            }

        } catch (Exception e) {
            log.warn("Error extracting team names: {}", e.getMessage());
        }
    }

    private void extractTeamNamesFromDOM(WebDriverWait wait, MatchMetadata metadata) {
        try {
            // Try different CSS selectors
            String[] selectors = {
                ".MatchHeader_teamName__3QYhF",
                ".MatchHeader_teamName__2_0_M",
                "[data-testid='team-name']"
            };

            for (String selector : selectors) {
                List<WebElement> teamElements = webDriver.findElements(By.cssSelector(selector));
                if (teamElements.size() >= 2) {
                    String homeTeam = cleanTeamName(teamElements.get(0).getText().trim());
                    String awayTeam = cleanTeamName(teamElements.get(1).getText().trim());

                    if (metadata.getHomeTeam() == null) {
                        metadata.setHomeTeam(homeTeam);
                    }
                    if (metadata.getAwayTeam() == null) {
                        metadata.setAwayTeam(awayTeam);
                    }

                    log.debug("Team names from DOM ({}): {} vs {}", selector, homeTeam, awayTeam);
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract team names from DOM: {}", e.getMessage());
        }
    }

    private String cleanTeamName(String teamName) {
        if (teamName == null || teamName.isEmpty()) {
            return teamName;
        }

        String cleaned = teamName;

        // Remove info after "|"
        if (cleaned.contains("|")) {
            cleaned = cleaned.split("\\|")[0].trim();
        }

        // Remove parentheses content
        cleaned = cleaned.replaceAll("\\([^)]*\\)", "").trim();

        // Remove common words that are not part of team name
        cleaned = cleaned.replaceAll("\\b(Live scores?|Preview|International Friendlies?|Friendly|Match)\\b", "").trim();

        // Clean multiple spaces
        cleaned = cleaned.replaceAll("\\s+", " ").trim();

        return cleaned;
    }

    private void extractCompetition(WebDriverWait wait, String pageSource, MatchMetadata metadata) {
        try {
            // Try OneFootball JSON extraction
            String ofPattern = "\"competition\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"";
            Pattern ofCompiledPattern = Pattern.compile(ofPattern);
            Matcher ofMatcher = ofCompiledPattern.matcher(pageSource);

            if (ofMatcher.find()) {
                metadata.setCompetition(ofMatcher.group(1));
                log.debug("Competition from OneFootball JSON: {}", metadata.getCompetition());
                return;
            }

            // Try LiveScore JSON extraction (stageName field)
            String lsPattern = "\"stageName\"\\s*:\\s*\"([^\"]+)\"";
            Pattern lsCompiledPattern = Pattern.compile(lsPattern);
            Matcher lsMatcher = lsCompiledPattern.matcher(pageSource);

            if (lsMatcher.find()) {
                metadata.setCompetition(lsMatcher.group(1));
                log.debug("Competition from LiveScore JSON: {}", metadata.getCompetition());
                return;
            }

            // Fallback to DOM
            String[] selectors = {
                ".MatchHeader_competitionName__1_0_M",
                "[data-testid='competition-name']"
            };

            for (String selector : selectors) {
                try {
                    WebElement competitionElement = webDriver.findElement(By.cssSelector(selector));
                    metadata.setCompetition(competitionElement.getText().trim());
                    log.debug("Competition from DOM: {}", metadata.getCompetition());
                    return;
                } catch (Exception e) {
                    // Try next selector
                }
            }

        } catch (Exception e) {
            log.warn("Error extracting competition: {}", e.getMessage());
        }
    }

    private void extractVenue(WebDriverWait wait, String pageSource, MatchMetadata metadata) {
        try {
            // Try JSON extraction
            String pattern = "\"venue\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"";
            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(pageSource);

            if (matcher.find()) {
                metadata.setVenue(matcher.group(1));
                log.debug("Venue from JSON: {}", metadata.getVenue());
                return;
            }

            // Fallback to DOM
            String[] selectors = {
                ".MatchHeader_venueName__1_0_M",
                "[data-testid='venue-name']"
            };

            for (String selector : selectors) {
                try {
                    WebElement venueElement = webDriver.findElement(By.cssSelector(selector));
                    metadata.setVenue(venueElement.getText().trim());
                    log.debug("Venue from DOM: {}", metadata.getVenue());
                    return;
                } catch (Exception e) {
                    // Try next selector
                }
            }

        } catch (Exception e) {
            log.warn("Error extracting venue: {}", e.getMessage());
        }
    }

    private void extractMatchDate(WebDriverWait wait, String pageSource, MatchMetadata metadata) {
        try {
            // Method 1: Extract from JSON (startDate in ISO 8601 format)
            String pattern = "\"startDate\"\\s*:\\s*\"([^\"]+)\"";
            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(pageSource);

            if (matcher.find()) {
                String isoDateTime = matcher.group(1);
                try {
                    // Parse ISO 8601 datetime (e.g., "2025-12-21T19:00:00Z")
                    LocalDateTime kickoff = LocalDateTime.parse(isoDateTime,
                        DateTimeFormatter.ISO_DATE_TIME);
                    metadata.setKickoffUtc(kickoff);
                    log.debug("Match date from JSON: {}", kickoff);
                    return;
                } catch (Exception e) {
                    log.warn("Failed to parse datetime from JSON: {}", isoDateTime);
                }
            }

            // Method 2: Extract from DOM time[datetime] attribute
            try {
                List<WebElement> timeElements = webDriver.findElements(By.cssSelector("time[datetime]"));
                for (WebElement timeElement : timeElements) {
                    String datetime = timeElement.getAttribute("datetime");
                    if (datetime != null && !datetime.isEmpty()) {
                        try {
                            LocalDateTime kickoff = LocalDateTime.parse(datetime,
                                DateTimeFormatter.ISO_DATE_TIME);
                            metadata.setKickoffUtc(kickoff);
                            log.debug("Match date from DOM: {}", kickoff);
                            return;
                        } catch (Exception e) {
                            log.debug("Could not parse datetime: {}", datetime);
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Could not extract datetime from DOM: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.warn("Error extracting match date: {}", e.getMessage());
        }
    }

    private void detectProviderFromUrl(String url, MatchMetadata metadata) {
        try {
            String urlLower = url.toLowerCase();

            if (urlLower.contains("onefootball.com")) {
                metadata.setProvider(com.bsmart.scoretracker.model.enums.ProviderType.ONE_FOOTBALL);
                log.debug("Auto-detected provider: ONE_FOOTBALL");
            } else if (urlLower.contains("livescore.com")) {
                metadata.setProvider(com.bsmart.scoretracker.model.enums.ProviderType.LIVE_SCORE);
                log.debug("Auto-detected provider: LIVE_SCORE");
            } else {
                log.warn("Could not auto-detect provider from URL: {}", url);
            }

        } catch (Exception e) {
            log.warn("Error detecting provider from URL: {}", e.getMessage());
        }
    }
}
