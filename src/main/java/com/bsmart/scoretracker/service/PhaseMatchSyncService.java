package com.bsmart.scoretracker.service;

import com.bsmart.scoretracker.dto.external.ExternalPhaseDTO;

import java.util.List;

/**
 * Service de synchronisation des phases et matches depuis WECANPRONO-SERVICE
 */
public interface PhaseMatchSyncService {



    /**
     * Synchronise toutes les compétitions qui ont un externalId
     *
     * @return Nombre total de phases synchronisées
     */
    int synchronizeAllSyncedCompetitions();
}
