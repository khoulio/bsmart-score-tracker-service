# BSmart Score Tracker - Collection Postman

Cette collection Postman contient tous les endpoints de l'API BSmart Score Tracker Service.

## 📁 Fichiers

- `BSmart-Score-Tracker.postman_collection.json` - Collection complète avec tous les endpoints
- `LOCAL.postman_environment.json` - Environnement LOCAL (http://localhost:9093)
- `PROD.postman_environment.json` - Environnement PROD (http://85.215.131.40:8222)

## 🚀 Import dans Postman

### 1. Importer la Collection

1. Ouvrir Postman
2. Cliquer sur **Import** (en haut à gauche)
3. Sélectionner `BSmart-Score-Tracker.postman_collection.json`
4. Cliquer sur **Import**

### 2. Importer les Environnements

1. Cliquer sur **Import**
2. Sélectionner `LOCAL.postman_environment.json` et `PROD.postman_environment.json`
3. Cliquer sur **Import**

### 3. Sélectionner un Environnement

1. Dans le coin supérieur droit de Postman
2. Cliquer sur le menu déroulant des environnements
3. Sélectionner **LOCAL - Score Tracker** ou **PROD - Score Tracker**

## 📋 Organisation de la Collection

La collection est organisée en **6 catégories** :

### 1. **Competitions** (5 endpoints)
- ✅ GET - Récupérer toutes les compétitions
- ✅ GET - Récupérer une compétition par ID
- ✅ POST - Créer une compétition
- ✅ PUT - Mettre à jour une compétition
- ✅ DELETE - Supprimer une compétition

### 2. **Phases** (6 endpoints)
- ✅ GET - Récupérer toutes les phases
- ✅ GET - Récupérer les phases par compétition
- ✅ GET - Récupérer une phase par ID
- ✅ POST - Créer une phase
- ✅ PUT - Mettre à jour une phase
- ✅ DELETE - Supprimer une phase

### 3. **Matches** (13 endpoints)
- ✅ GET - Récupérer tous les matches
- ✅ GET - Récupérer les matches par phase
- ✅ GET - Récupérer les matches par statut
- ✅ GET - Récupérer un match par ID
- ✅ GET - Récupérer un match par External ID (Wecanprono)
- ✅ POST - Créer un match
- ✅ POST - Créer/Mettre à jour un match depuis Wecanprono
- ✅ PUT - Mettre à jour un match
- ✅ DELETE - Supprimer un match
- ✅ POST - Activer le tracking
- ✅ POST - Désactiver le tracking
- ✅ POST - Rafraîchir un match (force)
- ✅ POST - Extraire les métadonnées depuis une URL

### 4. **Match Events** (1 endpoint)
- ✅ GET - Récupérer tous les événements d'un match

### 5. **Sync - Competitions** (3 endpoints)
- ✅ POST - Synchroniser toutes les compétitions
- ✅ POST - Synchroniser une compétition spécifique
- ✅ GET - Récupérer les compétitions externes (sans sync)

### 6. **Sync - Phases & Matches** (1 endpoint)
- ✅ POST - Synchroniser toutes les phases et matches

## 🔧 Configuration des Environnements

### LOCAL
```
base_url: http://localhost:9093
environment: local
```

### PROD
```
base_url: http://85.215.131.40:8222/bsmart-score-tracker-service
environment: production
```

**Note**: En production, l'API est accessible via l'API Gateway sur le port 8222.

## 📝 Exemples d'utilisation

### Créer un Match
```json
POST {{base_url}}/api/matches
{
  "phaseId": 1,
  "homeTeam": "PSG",
  "awayTeam": "Marseille",
  "kickoffUtc": "2024-12-26T20:00:00Z",
  "venue": "Parc des Princes",
  "provider": "ONE_FOOTBALL",
  "matchUrl": "https://onefootball.com/match/123456",
  "trackingEnabled": true
}
```

### Récupérer les Matches en Cours
```
GET {{base_url}}/api/matches?status=IN_PLAY
```

### Synchroniser toutes les Compétitions
```
POST {{base_url}}/api/sync/competitions/all
```

## 🎯 Statuts de Match

Les statuts disponibles sont :
- `SCHEDULED` - Match programmé
- `IN_PLAY` - Match en cours
- `PAUSED` - Mi-temps
- `FINISHED` - Match terminé

## 🔗 Providers Supportés

- `ONE_FOOTBALL` - OneFootball.com
- `LIVE_SCORE` - LiveScore.com

## 📌 Notes Importantes

1. **Variables de Path** : Les variables comme `:id` sont pré-remplies avec des valeurs par défaut (ex: 1). Modifiez-les selon vos besoins.

2. **Tracking Automatique** : Les matches avec `trackingEnabled: true` sont automatiquement trackés par le scheduler.

3. **External ID** : L'`externalId` correspond au `rencontre_id` de Wecanprono et permet de lier les matches entre les deux systèmes.

4. **API Gateway** : En production, toutes les requêtes passent par l'API Gateway sur le port 8222.

## 🤝 Intégration Wecanprono

L'endpoint `/api/matches/wecanprono` permet à Wecanprono de :
- Créer un nouveau match avec un `externalId`
- Mettre à jour un match existant (identifié par `matchUrl`)
- Le match créé sera automatiquement tracké si les conditions sont remplies

---

**Auteur**: BSmart Team
**Date**: 26 Décembre 2024
**Version**: 1.0.0
