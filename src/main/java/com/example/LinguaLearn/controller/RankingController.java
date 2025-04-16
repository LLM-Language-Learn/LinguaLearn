package com.example.LinguaLearn.controller;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.LinguaLearn.model.RankingEntry;
import com.example.LinguaLearn.service.RankingService;

@Controller
public class RankingController {
    
    @Autowired
    private RankingService rankingService;

    @GetMapping("/ranking")
    public String showRankingPage(Model model) throws ExecutionException, InterruptedException {
        List<RankingEntry> rankings = rankingService.getTopRankings(10);
        model.addAttribute("rankings", rankings);
        return "ranking";
    }
}