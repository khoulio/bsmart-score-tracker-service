-- Script d'initialisation pour créer la base de données score_tracker_db
--
-- IMPORTANT: Ce script est exécuté UNIQUEMENT lors de la première initialisation
-- du conteneur PostgreSQL (quand le volume est vide).
--
-- Les redémarrages ultérieurs du conteneur IGNORENT ce script automatiquement.
-- Pas besoin de vérification "IF NOT EXISTS" car PostgreSQL gère cela nativement.

-- Créer la base de données score_tracker_db
CREATE DATABASE score_tracker_db
    WITH
    OWNER = geocodinguser
    ENCODING = 'UTF8'
    LC_COLLATE = 'en_US.utf8'
    LC_CTYPE = 'en_US.utf8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Accorder tous les privilèges à geocodinguser
GRANT ALL PRIVILEGES ON DATABASE score_tracker_db TO geocodinguser;
