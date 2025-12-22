package com.bsmart.scoretracker.controller.web;

import com.bsmart.scoretracker.dto.PhaseDTO;
import com.bsmart.scoretracker.exception.ResourceNotFoundException;
import com.bsmart.scoretracker.service.CompetitionService;
import com.bsmart.scoretracker.service.PhaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/phases")
@RequiredArgsConstructor
public class AdminPhaseController {

    private final PhaseService phaseService;
    private final CompetitionService competitionService;

    @GetMapping
    public String list(@RequestParam(required = false) Long competitionId, Model model) {
        model.addAttribute("pageTitle", "Phases");

        if (competitionId != null) {
            try {
                var competition = competitionService.getCompetitionById(competitionId);
                model.addAttribute("competition", competition);
                model.addAttribute("competitionId", competitionId);

                List<PhaseDTO> phases = phaseService.getPhasesByCompetition(competitionId);
                model.addAttribute("phases", phases);
            } catch (ResourceNotFoundException e) {
                model.addAttribute("errorMessage", "Compétition non trouvée");
            }
        } else {
            model.addAttribute("phases", phaseService.getAllPhases());
        }

        return "phases/list";
    }

    @GetMapping("/create")
    public String createForm(@RequestParam Long competitionId, Model model) {
        try {
            var competition = competitionService.getCompetitionById(competitionId);
            model.addAttribute("pageTitle", "Nouvelle Phase");
            model.addAttribute("competition", competition);
            model.addAttribute("competitionId", competitionId);

            PhaseDTO phase = new PhaseDTO();
            phase.setCompetitionId(competitionId);
            phase.setTrackingEnabled(true); // Default value
            model.addAttribute("phase", phase);
            model.addAttribute("isEdit", false);

            return "phases/form";
        } catch (ResourceNotFoundException e) {
            return "redirect:/admin/competitions";
        }
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("phase") PhaseDTO dto,
                        BindingResult result,
                        @RequestParam Long competitionId,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            try {
                var competition = competitionService.getCompetitionById(competitionId);
                model.addAttribute("competition", competition);
                model.addAttribute("competitionId", competitionId);
            } catch (ResourceNotFoundException e) {
                // Ignore
            }
            return "phases/form";
        }

        try {
            dto.setCompetitionId(competitionId);
            PhaseDTO created = phaseService.createPhase(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                "Phase '" + created.getName() + "' créée avec succès");
            return "redirect:/admin/phases?competitionId=" + competitionId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erreur lors de la création: " + e.getMessage());
            return "redirect:/admin/phases/create?competitionId=" + competitionId;
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                          @RequestParam Long competitionId,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            PhaseDTO phase = phaseService.getPhaseById(id);
            var competition = competitionService.getCompetitionById(competitionId);

            model.addAttribute("pageTitle", "Modifier Phase");
            model.addAttribute("phase", phase);
            model.addAttribute("competition", competition);
            model.addAttribute("competitionId", competitionId);
            model.addAttribute("isEdit", true);

            return "phases/form";
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Phase non trouvée");
            return "redirect:/admin/phases?competitionId=" + competitionId;
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute("phase") PhaseDTO dto,
                        BindingResult result,
                        @RequestParam Long competitionId,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            try {
                var competition = competitionService.getCompetitionById(competitionId);
                model.addAttribute("competition", competition);
                model.addAttribute("competitionId", competitionId);
                model.addAttribute("isEdit", true);
            } catch (ResourceNotFoundException e) {
                // Ignore
            }
            return "phases/form";
        }

        try {
            dto.setCompetitionId(competitionId);
            phaseService.updatePhase(id, dto);
            redirectAttributes.addFlashAttribute("successMessage",
                "Phase modifiée avec succès");
            return "redirect:/admin/phases?competitionId=" + competitionId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erreur lors de la modification: " + e.getMessage());
            return "redirect:/admin/phases/" + id + "/edit?competitionId=" + competitionId;
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                        @RequestParam Long competitionId,
                        RedirectAttributes redirectAttributes) {
        try {
            phaseService.deletePhase(id);
            redirectAttributes.addFlashAttribute("successMessage",
                "Phase supprimée avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/admin/phases?competitionId=" + competitionId;
    }

    @PostMapping("/{id}/toggle-tracking")
    public String toggleTracking(@PathVariable Long id,
                                 @RequestParam Long competitionId,
                                 RedirectAttributes redirectAttributes) {
        try {
            PhaseDTO phase = phaseService.getPhaseById(id);
            phase.setTrackingEnabled(!phase.getTrackingEnabled());
            phaseService.updatePhase(id, phase);

            redirectAttributes.addFlashAttribute("successMessage",
                "Tracking " + (phase.getTrackingEnabled() ? "activé" : "désactivé"));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/phases?competitionId=" + competitionId;
    }
}
