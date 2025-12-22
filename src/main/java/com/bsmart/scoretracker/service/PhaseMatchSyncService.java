package com.bsmart.scoretracker.service;

import com.bsmart.scoretracker.dto.external.ExternalPhaseDTO;

import java.util.List;

/**
 * Service de synchronisation des phases et matches depuis WECANPRONO-SERVICE
 */
public interface PhaseMatchSyncService {

    /**
     * Récupère les phases et matches externes pour une compétition
     *
     * @param competitionId ID de la compétition locale
     * @return Liste des phases externes avec leurs matches
     */
    List<ExternalPhaseDTO> fetchExternalPhasesWithMatches(Long competitionId);

    /**
     * Synchronise toutes les phases et matches pour une compétition
     *
     * @param competitionId ID de la compétition locale
     * @return Nombre de phases synchronisées
     */
    int synchronizePhasesAndMatchesForCompetition(Long competitionId);

    /**
     * Synchronise une phase spécifique et ses matches
     *
     * @param competitionId ID de la compétition locale
     * @param externalPhaseId ID de la phase externe
     * @return true si la synchronisation a réussi
     */
    boolean synchronizePhase(Long competitionId, Long externalPhaseId);

    /**
     * Synchronise toutes les compétitions qui ont un externalId
     *
     * @return Nombre total de phases synchronisées
     */
    int synchronizeAllSyncedCompetitions();
}
