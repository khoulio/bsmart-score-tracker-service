package com.bsmart.scoretracker.controller.web;

import com.bsmart.scoretracker.dto.MatchDTO;
import com.bsmart.scoretracker.dto.MatchEventDTO;
import com.bsmart.scoretracker.dto.MatchMetadata;
import com.bsmart.scoretracker.exception.ResourceNotFoundException;
import com.bsmart.scoretracker.model.enums.MatchStatus;
import com.bsmart.scoretracker.service.MatchMetadataService;
import com.bsmart.scoretracker.service.PhaseService;
import com.bsmart.scoretracker.service.MatchEventService;
import com.bsmart.scoretracker.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/matches")
@RequiredArgsConstructor
public class AdminMatchController {

    private final MatchService matchService;
    private final PhaseService phaseService;
    private final MatchEventService matchEventService;
    private final MatchMetadataService metadataService;

    @GetMapping
    public String list(@RequestParam(required = false) Long phaseId,
                      @RequestParam(required = false) MatchStatus status,
                      Model model) {
        model.addAttribute("pageTitle", "Matches");

        if (phaseId != null) {
            try {
                var phase = phaseService.getPhaseById(phaseId);
                model.addAttribute("phase", phase);
                model.addAttribute("phaseId", phaseId);

                List<MatchDTO> matches = matchService.getMatchesByPhase(phaseId);
                model.addAttribute("matches", matches);
            } catch (ResourceNotFoundException e) {
                model.addAttribute("errorMessage", "Compétition non trouvée");
            }
        } else if (status != null) {
            List<MatchDTO> matches = matchService.getMatchesByStatus(status);
            model.addAttribute("matches", matches);
            model.addAttribute("filterStatus", status);
        } else {
            model.addAttribute("matches", matchService.getAllMatches());
        }

        return "matches/list";
    }

    @GetMapping("/create")
    public String createForm(@RequestParam Long phaseId, Model model) {
        try {
            var phase = phaseService.getPhaseById(phaseId);
            model.addAttribute("pageTitle", "Nouveau Match");
            model.addAttribute("phase", phase);
            model.addAttribute("phaseId", phaseId);

            MatchDTO match = new MatchDTO();
            match.setPhaseId(phaseId);
            match.setTrackingEnabled(false); // Default to disabled for manual matches
            match.setStatus(MatchStatus.SCHEDULED);
            model.addAttribute("match", match);
            model.addAttribute("isEdit", false);
            model.addAttribute("isManualUpdate", false);

            return "matches/form";
        } catch (ResourceNotFoundException e) {
            return "redirect:/admin/phases";
        }
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("match") MatchDTO dto,
                        BindingResult result,
                        @RequestParam Long phaseId,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            try {
                var phase = phaseService.getPhaseById(phaseId);
                model.addAttribute("phase", phase);
                model.addAttribute("phaseId", phaseId);
            } catch (ResourceNotFoundException e) {
                // Ignore
            }
            return "matches/form";
        }

        try {
            dto.setPhaseId(phaseId);
            MatchDTO created = matchService.createMatch(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                "Match '" + created.getHomeTeam() + " vs " + created.getAwayTeam() + "' créé avec succès");
            return "redirect:/admin/matches?phaseId=" + phaseId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erreur lors de la création: " + e.getMessage());
            return "redirect:/admin/matches/create?phaseId=" + phaseId;
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                          @RequestParam Long phaseId,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            MatchDTO match = matchService.getMatchById(id);
            var phase = phaseService.getPhaseById(phaseId);

            model.addAttribute("pageTitle", "Modifier Match");
            model.addAttribute("match", match);
            model.addAttribute("phase", phase);
            model.addAttribute("phaseId", phaseId);
            model.addAttribute("isEdit", true);
            model.addAttribute("isManualUpdate", false);

            return "matches/form";
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Match non trouvé");
            return "redirect:/admin/matches?phaseId=" + phaseId;
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute("match") MatchDTO dto,
                        BindingResult result,
                        @RequestParam Long phaseId,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            try {
                var phase = phaseService.getPhaseById(phaseId);
                model.addAttribute("phase", phase);
                model.addAttribute("phaseId", phaseId);
                model.addAttribute("isEdit", true);
            } catch (ResourceNotFoundException e) {
                // Ignore
            }
            return "matches/form";
        }

        try {
            dto.setPhaseId(phaseId);
            matchService.updateMatch(id, dto);
            redirectAttributes.addFlashAttribute("successMessage",
                "Match modifié avec succès");
            return "redirect:/admin/matches?phaseId=" + phaseId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erreur lors de la modification: " + e.getMessage());
            return "redirect:/admin/matches/" + id + "/edit?phaseId=" + phaseId;
        }
    }

    @GetMapping("/{id}/manual-update")
    public String manualUpdateForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            MatchDTO match = matchService.getMatchById(id);
            model.addAttribute("pageTitle", "Mise à jour manuelle");
            model.addAttribute("match", match);
            model.addAttribute("allStatus", MatchStatus.values());
            model.addAttribute("isManualUpdate", true);
            return "matches/form";
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Match non trouvé");
            return "redirect:/admin/matches";
        }
    }

    @PostMapping("/{id}/manual-update")
    public String manualUpdate(@PathVariable Long id,
                               @RequestParam Integer scoreHome,
                               @RequestParam Integer scoreAway,
                               @RequestParam MatchStatus status,
                               @RequestParam(required = false) Integer scoreHomeTAB,
                               @RequestParam(required = false) Integer scoreAwayTAB,
                               RedirectAttributes redirectAttributes) {
        try {
            matchService.manualUpdate(id, scoreHome, scoreAway, status, scoreHomeTAB, scoreAwayTAB);
            redirectAttributes.addFlashAttribute("successMessage", "Match mis à jour manuellement avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la mise à jour manuelle: " + e.getMessage());
        }
        return "redirect:/admin/matches/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                        @RequestParam Long phaseId,
                        RedirectAttributes redirectAttributes) {
        try {
            matchService.deleteMatch(id);
            redirectAttributes.addFlashAttribute("successMessage",
                "Match supprimé avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/admin/matches?phaseId=" + phaseId;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            MatchDTO match = matchService.getMatchById(id);
            List<MatchEventDTO> events = matchEventService.getEventsByMatch(id);

            model.addAttribute("pageTitle", match.getHomeTeam() + " vs " + match.getAwayTeam());
            model.addAttribute("match", match);
            model.addAttribute("events", events);
            model.addAttribute("phaseId", match.getPhaseId());

            return "matches/detail";
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Match non trouvé");
            return "redirect:/admin/matches";
        }
    }

    @PostMapping("/cleanup-finished")
    public String cleanupFinishedMatches(RedirectAttributes redirectAttributes) {
        try {
            matchService.deleteFinishedMatches();
            redirectAttributes.addFlashAttribute("successMessage",
                "Tous les matchs terminés ont été supprimés avec succès.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erreur lors de la suppression des matchs terminés: " + e.getMessage());
        }
        return "redirect:/admin/matches";
    }

    @PostMapping("/{id}/toggle-tracking")
    public String toggleTracking(@PathVariable Long id,
                                 @RequestParam Long phaseId,
                                 RedirectAttributes redirectAttributes) {
        try {
            MatchDTO match = matchService.getMatchById(id);
            if (match.getTrackingEnabled()) {
                matchService.disableTracking(id);
                redirectAttributes.addFlashAttribute("successMessage", "Tracking désactivé");
            } else {
                matchService.enableTracking(id);
                redirectAttributes.addFlashAttribute("successMessage", "Tracking activé");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/matches?phaseId=" + phaseId;
    }

    @PostMapping("/{id}/refresh")
    public String manualRefresh(@PathVariable Long id,
                               @RequestParam(required = false) Long phaseId,
                               RedirectAttributes redirectAttributes) {
        try {
            matchService.refreshMatch(id);
            redirectAttributes.addFlashAttribute("successMessage",
                "Match rafraîchi avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erreur lors du rafraîchissement: " + e.getMessage());
        }

        if (phaseId != null) {
            return "redirect:/admin/matches?phaseId=" + phaseId;
        } else {
            return "redirect:/admin/matches/" + id;
        }
    }

    @PostMapping("/extract-metadata")
    @ResponseBody
    public ResponseEntity<MatchMetadata> extractMetadata(@RequestParam String url) {
        try {
            MatchMetadata metadata = metadataService.extractMetadataFromUrl(url);
            return ResponseEntity.ok(metadata);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
