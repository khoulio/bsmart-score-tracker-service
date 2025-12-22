package com.bsmart.scoretracker.service;

import com.bsmart.scoretracker.dto.external.ExternalCompetitionDTO;

import java.util.List;

public interface CompetitionSyncService {

    /**
     * Synchronise toutes les compétitions depuis l'API externe
     */
    int synchronizeAllCompetitions();

    /**
     * Synchronise une compétition spécifique
     */
    boolean synchronizeCompetition(Long externalId);

    /**
     * Récupère les compétitions depuis l'API externe sans synchroniser
     */
    List<ExternalCompetitionDTO> fetchExternalCompetitions();
}
