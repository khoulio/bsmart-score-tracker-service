package com.bsmart.scoretracker.controller.web;

import com.bsmart.scoretracker.dto.CompetitionDTO;
import com.bsmart.scoretracker.exception.ResourceNotFoundException;
import com.bsmart.scoretracker.service.CompetitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/competitions")
@RequiredArgsConstructor
public class AdminCompetitionController {

    private final CompetitionService competitionService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pageTitle", "Compétitions");
        model.addAttribute("competitions", competitionService.getAllCompetitions());
        return "competitions/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "Nouvelle Compétition");
        model.addAttribute("competition", new CompetitionDTO());
        model.addAttribute("isEdit", false);
        return "competitions/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("competition") CompetitionDTO dto,
                        BindingResult result,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "competitions/form";
        }

        try {
            CompetitionDTO created = competitionService.createCompetition(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                "Compétition '" + created.getName() + "' créée avec succès");
            return "redirect:/admin/competitions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erreur lors de la création: " + e.getMessage());
            return "redirect:/admin/competitions/create";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            CompetitionDTO competition = competitionService.getCompetitionById(id);
            model.addAttribute("pageTitle", "Modifier Compétition");
            model.addAttribute("competition", competition);
            model.addAttribute("isEdit", true);
            return "competitions/form";
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Compétition non trouvée");
            return "redirect:/admin/competitions";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute("competition") CompetitionDTO dto,
                        BindingResult result,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "competitions/form";
        }

        try {
            competitionService.updateCompetition(id, dto);
            redirectAttributes.addFlashAttribute("successMessage",
                "Compétition modifiée avec succès");
            return "redirect:/admin/competitions";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erreur lors de la modification: " + e.getMessage());
            return "redirect:/admin/competitions/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            competitionService.deleteCompetition(id);
            redirectAttributes.addFlashAttribute("successMessage",
                "Compétition supprimée avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                "Erreur lors de la suppression: " + e.getMessage());
        }
        return "redirect:/admin/competitions";
    }
}
