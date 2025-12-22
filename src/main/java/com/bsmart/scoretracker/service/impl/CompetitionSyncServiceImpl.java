package com.bsmart.scoretracker.service.impl;

import com.bsmart.scoretracker.client.WecanpronoApiClient;
import com.bsmart.scoretracker.dto.external.ExternalCompetitionDTO;
import com.bsmart.scoretracker.model.Competition;
import com.bsmart.scoretracker.repository.CompetitionRepository;
import com.bsmart.scoretracker.service.CompetitionSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompetitionSyncServiceImpl implements CompetitionSyncService {

    private final WecanpronoApiClient apiClient;
    private final CompetitionRepository competitionRepository;

    @Override
    @Transactional
    public int synchronizeAllCompetitions() {
        log.info("Starting synchronization of all competitions from WECANPRONO-SERVICE");

        List<ExternalCompetitionDTO> externalCompetitions = apiClient.fetchCompetitions();

        if (externalCompetitions.isEmpty()) {
            log.warn("No competitions received from external API");
            return 0;
        }

        int syncCount = 0;

        for (ExternalCompetitionDTO externalComp : externalCompetitions) {
            try {
                syncCompetition(externalComp);
                syncCount++;
            } catch (Exception e) {
                log.error("Error synchronizing competition {}: {}",
                    externalComp.getName(), e.getMessage(), e);
            }
        }

        log.info("Synchronized {} out of {} competitions",
            syncCount, externalCompetitions.size());

        return syncCount;
    }

    @Override
    @Transactional
    public boolean synchronizeCompetition(Long externalId) {
        log.info("Synchronizing competition with external ID: {}", externalId);

        ExternalCompetitionDTO externalComp = apiClient.fetchCompetitionById(externalId);

        if (externalComp == null) {
            log.warn("Competition {} not found in external API", externalId);
            return false;
        }

        try {
            syncCompetition(externalComp);
            return true;
        } catch (Exception e) {
            log.error("Error synchronizing competition {}: {}",
                externalId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<ExternalCompetitionDTO> fetchExternalCompetitions() {
        return apiClient.fetchCompetitions();
    }

    /**
     * Synchronise une compétition externe avec la base de données locale
     */
    private void syncCompetition(ExternalCompetitionDTO externalComp) {
        // Chercher si la compétition existe déjà (par external_id ou slug)
        Optional<Competition> existingOpt = competitionRepository
            .findByExternalId(externalComp.getId());

        if (existingOpt.isEmpty()) {
            existingOpt = competitionRepository.findBySlug(externalComp.getSlug());
        }

        Competition competition;

        if (existingOpt.isPresent()) {
            // Mettre à jour
            competition = existingOpt.get();
            log.debug("Updating existing competition: {}", competition.getName());
        } else {
            // Créer nouvelle
            competition = new Competition();
            competition.setCode(generateCode(externalComp));
            log.debug("Creating new competition: {}", externalComp.getName());
        }

        // Mapper les données
        mapExternalToLocal(externalComp, competition);

        // Marquer comme synchronisé
        competition.setLastSyncAt(LocalDateTime.now());

        // Sauvegarder
        competitionRepository.save(competition);

        log.info("Competition synchronized: {} (external_id: {})",
            competition.getName(), competition.getExternalId());
    }

    /**
     * Mappe les données de l'API externe vers l'entité locale
     */
    private void mapExternalToLocal(ExternalCompetitionDTO external, Competition local) {
        local.setExternalId(external.getId());
        local.setName(external.getName());
        local.setSlug(external.getSlug());
        local.setLogoUrl(external.getUrl());
        local.setNbUsers(external.getNbUsers());

        // Parser les dates
        if (external.getDateStart() != null) {
            local.setDateStart(parseDate(external.getDateStart()));
        }
        if (external.getDateEnd() != null) {
            local.setDateEnd(parseDate(external.getDateEnd()));
        }

        // Mapper les booléens
        local.setIsOpen(external.getIsOpen());
        local.setIsLeague(external.getIsLeague());
        local.setIsStarted(external.getIsStarted());
        local.setIsFeatured(external.getIsFeaturedCompetition());
        local.setBackgroundUrl(external.getUrlBackgroundForNotification());
        local.setSponsorLogoUrl(external.getLogoSponsor());

        // Extraire le pays depuis le nom (si possible)
        if (local.getCountry() == null) {
            local.setCountry(extractCountryFromName(external.getName()));
        }
    }

    /**
     * Parse une date au format ISO-8601
     */
    private LocalDateTime parseDate(String dateString) {
        try {
            return OffsetDateTime.parse(dateString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .toLocalDateTime();
        } catch (Exception e) {
            log.warn("Error parsing date: {}", dateString);
            return null;
        }
    }

    /**
     * Génère un code unique pour la compétition
     */
    private String generateCode(ExternalCompetitionDTO external) {
        if (external.getSlug() != null && !external.getSlug().isEmpty()) {
            return external.getSlug().toUpperCase();
        }
        return "EXT_" + external.getId();
    }

    /**
     * Extrait le pays depuis le nom de la compétition
     */
    private String extractCountryFromName(String name) {
        // Recherche d'emojis de drapeaux ou de noms de pays
        if (name.contains("🇫🇷") || name.toLowerCase().contains("france") ||
            name.toLowerCase().contains("ligue 1")) {
            return "France";
        }
        if (name.contains("🇪🇸") || name.toLowerCase().contains("spain") ||
            name.toLowerCase().contains("laliga")) {
            return "Spain";
        }
        if (name.contains("🏴") || name.toLowerCase().contains("england") ||
            name.toLowerCase().contains("premier league")) {
            return "England";
        }
        if (name.contains("🌍") || name.toLowerCase().contains("can") ||
            name.toLowerCase().contains("maroc")) {
            return "Africa";
        }
        if (name.contains("🇪🇺") || name.toLowerCase().contains("champions league")) {
            return "Europe";
        }
        if (name.contains("🌎") || name.toLowerCase().contains("coupe du monde")) {
            return "World";
        }

        return "International";
    }
}
