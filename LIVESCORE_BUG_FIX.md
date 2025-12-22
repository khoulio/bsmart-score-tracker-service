# 🐛 LiveScore Status Detection Bug Fix

## Problème Identifié

**Date**: 2025-12-22
**Fichier affecté**: `LiveScoreScraperProvider.java`

### Symptômes

LiveScore retournait des données contradictoires :
- `eventStatus`: "EventScheduled" (match pas commencé)
- `minute`: "59'" (match clairement en cours)
- `score`: "1-0" (des buts marqués)

Le système interprétait `eventStatus` en priorité et marquait le match comme `SCHEDULED` alors qu'il était clairement `LIVE`.

### Logs du Bug

```
2025-12-22T02:17:06.842+01:00  INFO 42796 --- [   scheduling-1] c.b.s.s.p.LiveScoreScraperProvider       : Scraping LiveScore: https://www.livescore.com/en/football/guatemala/liga-nacional-apertura-play-off/antigua-guatemala-vs-csd-municipal/1708311/
2025-12-22T02:17:10.212+01:00  INFO 42796 --- [   scheduling-1] c.b.s.s.p.LiveScoreScraperProvider       : Extracted minute: 59'
2025-12-22T02:17:10.214+01:00  INFO 42796 --- [   scheduling-1] c.b.s.s.p.LiveScoreScraperProvider       : Extracted eventStatus: EventScheduled
2025-12-22T02:17:10.215+01:00  INFO 42796 --- [   scheduling-1] c.b.s.s.p.LiveScoreScraperProvider       : LiveScore scrape result - Status: EventScheduled, Score: 1-0, Minute: 59'
2025-12-22T02:17:10.224+01:00  INFO 42796 --- [   scheduling-1] c.b.s.service.impl.MatchServiceImpl      : [SCRAPE_OK] Match 4 - Status: SCHEDULED, Score: 1:0, Minute: 59', Provider: LIVE_SCORE
```

**Résultat**: ❌ Match marqué `SCHEDULED` alors qu'il était à la 59ème minute avec un score de 1-0 !

## Cause Racine

Dans `LiveScoreScraperProvider.extractStatusFromPage()`, le code vérifiait `eventStatus` en PREMIER, avant de regarder le champ `minute`.

**Code défaillant** :
```java
// Ancienne logique (BUGGY)
private String extractStatusFromPage(String pageSource, String minute) {
    // Extract eventStatus from JSON
    if (matcher.find()) {
        String eventStatus = matcher.group(1);

        if (eventStatus.equalsIgnoreCase("EventScheduled")) {
            return "SCHEDULED";  // ❌ Retourne SCHEDULED même si minute="59'"
        }
        // ...
    }
}
```

## Solution Implémentée

**Principe**: Le champ `minute` est plus fiable que `eventStatus` pour déterminer l'état réel du match.

**Nouvelle logique** :
```java
private String extractStatusFromPage(String pageSource, String minute) {
    try {
        // CRITICAL: Check minute FIRST (more reliable than eventStatus)
        // LiveScore sometimes shows "EventScheduled" even when match is live!
        if (minute != null && !minute.isEmpty()) {
            String minuteLower = minute.toLowerCase();

            // Check for full time
            if (minuteLower.contains("full time") || minuteLower.contains("ft") ||
                minuteLower.contains("finished")) {
                log.debug("Match is FINISHED based on minute: {}", minute);
                return "FT";
            }

            // Check for half-time
            if (minuteLower.contains("half") || minuteLower.contains("ht") ||
                minuteLower.equals("45'") || minuteLower.equals("45'+")) {
                log.debug("Match is HALF_TIME based on minute: {}", minute);
                return "HT";
            }

            // Check if it's a numeric minute (means match is LIVE)
            if (minuteLower.matches(".*\\d+'.*")) {
                log.debug("Match is LIVE based on minute: {}", minute);
                return "LIVE";  // ✅ Priorité au minute!
            }
        }

        // Extract eventStatus from JSON as FALLBACK only
        String pattern = "\"eventStatus\"\\s*:\\s*\"([^\"]+)\"";
        // ... reste de la logique en fallback
    }
}
```

## Tests de Validation

### Fixture HTML Créée

**Fichier**: `src/test/resources/fixtures/livescore-scheduled-but-live.html`

```html
<!DOCTYPE html>
<html>
<body>
    <script id="__NEXT_DATA__" type="application/json">
    {
        "initialEventData": {
            "event": {
                "homeTeamName": "Antigua Guatemala",
                "awayTeamName": "CSD Municipal",
                "homeTeamScore": "1",
                "awayTeamScore": "0",
                "eventStatus": "EventScheduled",  ← Inconsistent
                "status": "59'",                  ← Should override
                "stageName": "Liga Nacional: Apertura Play-off"
            }
        }
    }
    </script>
</body>
</html>
```

### Test Créé

**Fichier**: `src/test/java/com/bsmart/scoretracker/scraper/LiveScoreScraperIntegrationTest.java`

```java
@Test
@DisplayName("BUG FIX: EventScheduled mais minute='59' → Doit détecter LIVE")
void testEventScheduledButMinuteShowsLive() throws IOException {
    // Charger le HTML fixture qui reproduit le bug
    // eventStatus="EventScheduled" mais status="59'" et score=1-0
    String htmlContent = loadFixture("fixtures/livescore-scheduled-but-live.html");

    // Mock WebDriver behavior
    when(webDriver.getPageSource()).thenReturn(htmlContent);
    when(webDriver.findElement(By.tagName("body"))).thenReturn(bodyElement);

    // Execute scraping
    MatchSnapshot result = scraper.fetch("https://livescore.com/test");

    // Assertions - Le fix doit prioriser la minute sur eventStatus
    assertTrue(result.isFound());
    assertEquals("LIVE", result.getStatus(),
        "Le status doit être LIVE car minute='59' même si eventStatus='EventScheduled'");
    assertEquals(1, result.getHome());
    assertEquals(0, result.getAway());
    assertEquals("59'", result.getMinute());
}
```

### Résultats des Tests

```
[INFO] Running com.bsmart.scoretracker.scraper.LiveScoreScraperIntegrationTest
03:26:49.377 [main] INFO c.b.s.s.p.LiveScoreScraperProvider -- LiveScore scrape result - Status: LIVE, Score: 1-0, Minute: 59'
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 27.93 s
[INFO] BUILD SUCCESS
```

✅ **Tous les tests passent** :
1. testScrapeLiveMatch - Match en cours (50')
2. testScrapeFinishedMatch - Match terminé (Full time)
3. testScrapeHalftimeMatch - Mi-temps (Half time)
4. **testEventScheduledButMinuteShowsLive - BUG FIX** ✅
5. testScrapeLiveScoreNetworkError - Erreur réseau

## Impact

### Avant le Fix
- ❌ Matchs en cours marqués `SCHEDULED`
- ❌ Scores affichés mais status incorrect
- ❌ Logique anti-flapping perturbée
- ❌ Notifications de début de match non envoyées

### Après le Fix
- ✅ Détection correcte des matchs en cours
- ✅ Status LIVE quand minute = "59'"
- ✅ Status FT quand minute = "Full time"
- ✅ Status HT quand minute = "Half time"
- ✅ Fallback sur eventStatus seulement si minute indisponible

## Fichiers Modifiés

| Fichier | Type | Description |
|---------|------|-------------|
| `LiveScoreScraperProvider.java` | Code | Fix de la méthode `extractStatusFromPage()` |
| `LiveScoreScraperIntegrationTest.java` | Test | Nouveau fichier avec 5 tests |
| `livescore-scheduled-but-live.html` | Fixture | Cas de test du bug |
| `livescore-live-match.html` | Fixture | Match en cours normal |
| `livescore-finished-match.html` | Fixture | Match terminé |
| `livescore-halftime.html` | Fixture | Mi-temps |
| `TESTING_STRATEGY.md` | Doc | Documentation du bug fix |

## Recommandations

1. **Monitoring** : Surveiller les logs pour détecter des patterns similaires
2. **Fixtures** : Mettre à jour les fixtures si LiveScore change sa structure
3. **Contract Tests** : Exécuter régulièrement pour détecter les changements
4. **OneFootball** : Vérifier si OneFootball a le même problème

## Conclusion

Le fix améliore significativement la fiabilité de la détection de status pour LiveScore en priorisant les données les plus fiables (le champ `minute`) sur les données potentiellement incorrectes (`eventStatus`).

**Statut** : ✅ **Résolu et testé**
