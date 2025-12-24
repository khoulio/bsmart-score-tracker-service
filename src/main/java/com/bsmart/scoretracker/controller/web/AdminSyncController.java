package com.bsmart.scoretracker.controller.web;

import com.bsmart.scoretracker.dto.external.ExternalCompetitionDTO;
import com.bsmart.scoretracker.dto.external.ExternalPhaseDTO;
import com.bsmart.scoretracker.service.CompetitionService;
import com.bsmart.scoretracker.service.CompetitionSyncService;
import com.bsmart.scoretracker.service.PhaseMatchSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/sync")
@RequiredArgsConstructor
public class AdminSyncController {

    private final CompetitionSyncService competitionSyncService;
    private final PhaseMatchSyncService phaseMatchSyncService;
    private final CompetitionService competitionService;

    @GetMapping("/competitions")
    public String syncPage(Model model) {
        model.addAttribute("pageTitle", "Synchronisation des Compétitions");

        // Récupérer les compétitions locales
        model.addAttribute("localCompetitions", competitionService.getAllCompetitions());

        // Récupérer les compétitions de l'API externe
        try {
            List<ExternalCompetitionDTO> externalCompetitions = competitionSyncService.fetchExternalCompetitions();
            model.addAttribute("externalCompetitions", externalCompetitions);
            model.addAttribute("externalCount", externalCompetitions.size());
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erreur lors de la récupération des compétitions externes: " + e.getMessage());
            model.addAttribute("externalCount", 0);
        }

        return "sync/competitions";
    }

    @PostMapping("/competitions/sync-all")
    public String syncAll(RedirectAttributes redirectAttributes) {
        try {
            int syncedCount = competitionSyncService.synchronizeAllCompetitions();
            redirectAttributes.addFlashAttribute("successMessage",
                syncedCount + " compétitions synchronisées avec succès depuis WECANPRONO-SERVICE");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erreur lors de la synchronisation: " + e.getMessage());
        }
        return "redirect:/admin/sync/competitions";
    }



}
