# 🧪 Stratégie de Tests - Score Tracker Service

## 📊 Vue d'ensemble

Le challenge principal : **Tester du scraping de sites web qui évoluent en temps réel**

### ❌ Problèmes des tests classiques :
- Les matchs changent constamment
- On ne contrôle pas quand un match commence/finit
- Les sites peuvent changer leur structure HTML
- Impossible de garantir des tests reproductibles

### ✅ Notre solution :
**Séparer les tests en 3 catégories indépendantes**

---

## 1️⃣ Tests Unitaires (Logic Tests)

**Objectif** : Tester la logique métier SANS dépendances externes

### Ce qu'on teste :
- ✅ Normalisation des statuts (OneFootball/LiveScore → MatchStatus)
- ✅ Validation des transitions (SCHEDULED → IN_PLAY → FINISHED)
- ✅ Détection du provider depuis l'URL
- ✅ Logique anti-flapping
- ✅ Protection contre les rollbacks de score

### Avantages :
- 🚀 Rapides (< 1 seconde)
- ✅ Fiables (pas de réseau)
- 🔄 Reproductibles à 100%
- 🛡️ Pas de mocks complexes

### Exemple :
```java
@Test
void testNormalizeOneFootballStatus() {
    // Arrange
    TrackingEngineService engine = new TrackingEngineServiceImpl(...);

    // Act
    MatchStatus result = engine.normalizeStatus("LIVE", ProviderType.ONE_FOOTBALL);

    // Assert
    assertEquals(MatchStatus.IN_PLAY, result);
}
```

### Fichier :
`src/test/java/com/bsmart/scoretracker/service/TrackingEngineLogicTest.java`

**Couverture attendue** : 80% de la logique métier

---

## 2️⃣ Tests d'Intégration (avec HTML Fixtures)

**Objectif** : Tester le scraping SANS appeler les vrais sites web

### Stratégie : **Record & Replay**

1. **Enregistrer** : Capturer le HTML d'un vrai match (à différents moments)
2. **Rejouer** : Utiliser ces snapshots dans les tests

### Structure des fixtures :

```
src/test/resources/fixtures/
├── onefootball-live-match.html        # Match en cours (45')
├── onefootball-finished-match.html    # Match terminé (FT)
├── onefootball-halftime.html          # Mi-temps (HT)
├── livescore-live-match.html          # LiveScore en cours (50')
├── livescore-finished-match.html      # LiveScore terminé (FT)
├── livescore-halftime.html            # LiveScore mi-temps (HT)
└── livescore-scheduled-but-live.html  # BUG FIX: eventStatus="Scheduled" mais minute="59'"
```

### Comment créer une fixture :

```bash
# 1. Ouvrir un match sur OneFootball
# 2. Copier le HTML complet
# 3. Sauvegarder dans fixtures/

# Ou avec curl (sans JavaScript):
curl "https://onefootball.com/en/match/2574599" > onefootball-test.html
```

### Exemple de fixture (HTML simplifié) :

```html
<!DOCTYPE html>
<html>
<body>
    <script id="__NEXT_DATA__" type="application/json">
    {
        "homeTeam": {"name": "Mali", "score": "1"},
        "awayTeam": {"name": "Zambia", "score": "0"},
        "timePeriod": "45'",
        "competition": {"name": "Africa Cup of Nations"}
    }
    </script>
</body>
</html>
```

### Test avec mock WebDriver :

```java
@Test
void testScrapeLiveMatch() throws IOException {
    // Arrange
    String htmlContent = loadFixture("fixtures/onefootball-live-match.html");
    when(webDriver.getPageSource()).thenReturn(htmlContent);

    // Act
    MatchSnapshot result = scraper.fetch("https://test.com");

    // Assert
    assertEquals("LIVE", result.getStatus());
    assertEquals(1, result.getHome());
    assertEquals(0, result.getAway());
}
```

### Avantages :
- 🚀 Rapides (pas de réseau)
- ✅ Reproductibles
- 🔄 Testent le parsing réel
- 📸 Snapshots vérifiables

### Fichiers :
- `src/test/java/com/bsmart/scoretracker/scraper/OneFootballScraperIntegrationTest.java`
- `src/test/java/com/bsmart/scoretracker/scraper/LiveScoreScraperIntegrationTest.java`

### ⚠️ Bug Fix Critique - LiveScore Status Detection

**Problème découvert** : LiveScore peut retourner `eventStatus="EventScheduled"` même quand le match est en cours !

**Logs du bug** :
```
Extracted minute: 59'
Extracted eventStatus: EventScheduled
LiveScore scrape result - Status: EventScheduled, Score: 1-0, Minute: 59'
SCRAPE_OK: Match 4 - Status: SCHEDULED, Score: 1:0  ❌ FAUX!
```

**Solution implémentée** : Prioriser le champ `minute` sur `eventStatus`

```java
private String extractStatusFromPage(String pageSource, String minute) {
    // CRITICAL: Check minute FIRST (more reliable than eventStatus)
    if (minute != null && !minute.isEmpty()) {
        String minuteLower = minute.toLowerCase();

        // Check if it's a numeric minute (means match is LIVE)
        if (minuteLower.matches(".*\\d+'.*")) {
            return "LIVE";  // ✅ Priorité au minute!
        }
    }

    // Extract eventStatus from JSON as FALLBACK only
    // ...
}
```

**Test de validation** :
```java
@Test
@DisplayName("BUG FIX: EventScheduled mais minute='59' → Doit détecter LIVE")
void testEventScheduledButMinuteShowsLive() {
    // Fixture: eventStatus="EventScheduled" mais status="59'" et score=1-0
    String htmlContent = loadFixture("fixtures/livescore-scheduled-but-live.html");

    MatchSnapshot result = scraper.fetch("https://livescore.com/test");

    // Le fix doit prioriser la minute sur eventStatus
    assertEquals("LIVE", result.getStatus());  // ✅ LIVE au lieu de SCHEDULED
    assertEquals("59'", result.getMinute());
}
```

**Résultat** : ✅ **5/5 tests passent** (27.93s)
- testScrapeLiveMatch - Match en cours (50')
- testScrapeFinishedMatch - Match terminé (Full time)
- testScrapeHalftimeMatch - Mi-temps (Half time)
- **testEventScheduledButMinuteShowsLive - BUG FIX ✅**
- testScrapeLiveScoreNetworkError - Erreur réseau

**Couverture attendue** : 90% des scrapers

---

## 3️⃣ Tests de Scénarios (End-to-End Logic)

**Objectif** : Simuler l'évolution complète d'un match

### Ce qu'on teste :

✅ **Scénario complet** : SCHEDULED → IN_PLAY → HALF_TIME → IN_PLAY → FINISHED
✅ **Anti-flapping** : 3 confirmations requises
✅ **Protection pré-kickoff** : Ignore données avant le début
✅ **Auto-correction** : FINISHED → IN_PLAY si erreur détectée
✅ **Score rollback protection** : Empêche 2-1 → 1-0
✅ **Score inversion correction** : Corrige 0-1 → 1-0

### Exemple de test :

```java
@Test
void testCompleteMatchScenario() {
    Match match = createTestMatch();

    // === ÉTAPE 1: Match commence (0-0, minute 1') ===
    when(scraper.fetch(any())).thenReturn(
        MatchSnapshot.builder()
            .status("LIVE")
            .home(0).away(0)
            .minute("1'")
            .build()
    );

    trackingEngine.trackMatch(match);

    // Anti-flapping: 1ère détection, reste SCHEDULED
    assertEquals(MatchStatus.SCHEDULED, match.getStatus());
    assertEquals(MatchStatus.IN_PLAY, match.getStatusCandidate());

    // === ÉTAPE 2 & 3: Confirmations ===
    trackingEngine.trackMatch(match);
    trackingEngine.trackMatch(match);

    // Après 3 confirmations: status appliqué
    assertEquals(MatchStatus.IN_PLAY, match.getStatus());

    // === ÉTAPE 4: Premier but ===
    when(scraper.fetch(any())).thenReturn(
        MatchSnapshot.builder()
            .status("LIVE")
            .home(1).away(0)
            .minute("25'")
            .build()
    );

    trackingEngine.trackMatch(match);
    assertEquals(1, match.getScoreHome());

    // ... (mi-temps, reprise, fin du match)
}
```

### Fichier :
`src/test/java/com/bsmart/scoretracker/service/TrackingScenarioTest.java`

**Couverture attendue** : 95% des flux métier

---

## 4️⃣ Tests de Contrat (Contract Tests)

**Objectif** : Vérifier que nos assumptions sur OneFootball/LiveScore sont encore valides

### Principe :
Au lieu de tester le scraping complet, on vérifie juste que les **sélecteurs CSS** et **patterns JSON** existent toujours.

### Quand exécuter :
- ❌ Pas à chaque commit (trop lent, dépend du réseau)
- ✅ Manuellement avant un déploiement
- ✅ Nightly builds (1x par jour)
- ✅ Quand on soupçonne un changement du site

### Exemple :

```java
@Test
@Tag("contract")
@Tag("slow")
void testOneFootballContractIsValid() {
    // Appel réel à OneFootball (URL de test connue)
    String html = fetchRealPage("https://onefootball.com/en/match/XXXX");

    // Vérifier que les patterns existent toujours
    assertTrue(html.contains("\"homeTeam\""), "homeTeam JSON field missing");
    assertTrue(html.contains("\"awayTeam\""), "awayTeam JSON field missing");
    assertTrue(html.contains("\"score\""), "score field missing");

    // Si ce test échoue → OneFootball a changé sa structure
}
```

### Commandes Maven :

```bash
# Tests normaux (unit + integration)
mvn test

# Tests de contrat seulement
mvn test -Dgroups="contract"

# Exclure les tests de contrat
mvn test -DexcludedGroups="contract"
```

---

## 5️⃣ Tests Manuels (pour les matchs en direct)

**Objectif** : Tester en conditions réelles pendant un vrai match

### Checklist de test manuel :

```
□ Match SCHEDULED avant le coup d'envoi
  - Status = SCHEDULED
  - Scores = null
  - Protection pré-kickoff active

□ Match IN_PLAY (1ère mi-temps)
  - Status passe à IN_PLAY (après 3 confirmations)
  - Scores mis à jour en temps réel
  - Minute progresse

□ But marqué
  - Score incrémente correctement
  - Event créé dans match_events
  - Pas de rollback

□ Mi-temps
  - Status = HALF_TIME
  - halfTimeSeen = true
  - Scores conservés

□ 2ème mi-temps
  - Status repasse à IN_PLAY
  - Minute repart de 46'

□ Fin du match
  - Status = FINISHED
  - trackingEnabled = false
  - Score final conservé
```

### Comment tester :

1. Trouver un match qui commence dans 1h sur OneFootball
2. Créer le match dans le système
3. Activer le tracking
4. Suivre dans les logs et l'interface admin
5. Vérifier chaque transition

---

## 📊 Résumé de la Stratégie

| Type | Vitesse | Fiabilité | Couverture | Fréquence |
|------|---------|-----------|------------|-----------|
| **Unit Tests** | ⚡ < 1s | ✅✅✅ 100% | 80% logic | Chaque commit |
| **Integration (Fixtures)** | ⚡ < 5s | ✅✅✅ 100% | 90% scrapers | Chaque commit |
| **Scenario Tests** | ⚡ < 3s | ✅✅✅ 100% | 95% flows | Chaque commit |
| **Contract Tests** | 🐌 ~10s | ⚠️ Variable | Structure HTML | 1x/jour |
| **Manual Tests** | 🐌 ~90min | ✅✅ Réaliste | E2E complet | Avant release |

---

## 🚀 Commandes Utiles

```bash
# Lancer tous les tests unitaires
mvn test

# Lancer un test spécifique
mvn test -Dtest=TrackingEngineLogicTest

# Lancer une méthode spécifique
mvn test -Dtest=TrackingEngineLogicTest#testNormalizeOneFootballLiveStatus

# Générer un rapport de couverture
mvn test jacoco:report

# Voir le rapport
open target/site/jacoco/index.html

# Tests en mode verbose
mvn test -X

# Skip tests (pour build rapide)
mvn clean install -DskipTests
```

---

## 🎯 Objectifs de Couverture

- **Minimum acceptable** : 70%
- **Objectif** : 85%
- **Excellent** : 90%+

### Zones critiques (100% requis) :
- ✅ normalizeStatus()
- ✅ processStatusChange()
- ✅ processScoreChange()
- ✅ Anti-flapping logic
- ✅ Protection pré-kickoff

---

## 📝 Bonnes Pratiques

### ✅ DO:
- Utiliser des fixtures HTML pour les tests d'intégration
- Mocker WebDriver dans les tests
- Tester TOUS les cas limites (null, empty, invalid)
- Documenter pourquoi un test existe
- Utiliser des noms de tests descriptifs
- Versionner les fixtures HTML

### ❌ DON'T:
- Ne PAS appeler de vrais sites dans les tests unitaires
- Ne PAS dépendre de matchs en cours
- Ne PAS ignorer les tests qui échouent
- Ne PAS tester le framework (Spring, Mockito)
- Ne PAS avoir de tests flaky (résultats aléatoires)

---

## 🔧 Création de Nouvelles Fixtures

Quand OneFootball/LiveScore change sa structure :

```bash
# 1. Identifier un match de test stable
MATCH_URL="https://onefootball.com/en/match/2574599"

# 2. Ouvrir dans le navigateur avec DevTools
# 3. Copier le HTML complet
# 4. Sauvegarder dans fixtures/

# 5. Simplifier le HTML (garder seulement le JSON)
# 6. Créer plusieurs variantes (LIVE, HT, FT)

# 7. Mettre à jour les tests si nécessaire
```

---

## 🎓 Ressources

- JUnit 5: https://junit.org/junit5/
- Mockito: https://site.mockito.org/
- Testcontainers (si besoin Selenium réel): https://www.testcontainers.org/
- Jacoco (couverture): https://www.jacoco.org/

---

**Prêt pour des tests robustes et fiables ! 🧪✅**
