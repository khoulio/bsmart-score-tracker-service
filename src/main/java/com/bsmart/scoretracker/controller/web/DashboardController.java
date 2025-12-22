package com.bsmart.scoretracker.controller.web;

import com.bsmart.scoretracker.dto.MatchDTO;
import com.bsmart.scoretracker.model.enums.MatchStatus;
import com.bsmart.scoretracker.service.CompetitionService;
import com.bsmart.scoretracker.service.MatchService;
import com.bsmart.scoretracker.service.PhaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class DashboardController {

    private final CompetitionService competitionService;
    private final PhaseService phaseService;
    private final MatchService matchService;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        // Get statistics
        long totalCompetitions = competitionService.getAllCompetitions().size();
        long totalPhases = phaseService.getAllPhases().size();
        long totalMatches = matchService.getAllMatches().size();

        // Get matches by status
        List<MatchDTO> liveMatches = matchService.getMatchesByStatus(MatchStatus.IN_PLAY);
        List<MatchDTO> scheduledMatches = matchService.getMatchesByStatus(MatchStatus.SCHEDULED);
        List<MatchDTO> finishedMatches = matchService.getMatchesByStatus(MatchStatus.FINISHED);

        // Add to model
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("totalCompetitions", totalCompetitions);
        model.addAttribute("totalPhases", totalPhases);
        model.addAttribute("totalMatches", totalMatches);
        model.addAttribute("liveMatchesCount", liveMatches.size());
        model.addAttribute("scheduledMatchesCount", scheduledMatches.size());
        model.addAttribute("finishedMatchesCount", finishedMatches.size());
        model.addAttribute("liveMatches", liveMatches);

        return "dashboard/index";
    }
}
