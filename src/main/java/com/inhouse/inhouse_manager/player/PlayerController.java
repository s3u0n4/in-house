package com.inhouse.inhouse_manager.player;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class PlayerController {
    private final PlayerService playerService;

    @GetMapping("/players")
    public String list(Model model) {
        model.addAttribute("players", playerService.findAll());
        return "players";
    }

    @GetMapping("/players/new")
    public String createForm(Model model){
        model.addAttribute("playerForm", new PlayerForm());
        model.addAttribute("positions", Position.values());
        return "player-form";
    }
    @PostMapping("/players")
    public String create(@Valid @ModelAttribute PlayerForm playerForm, BindingResult bindingResult, Model model){
        if (bindingResult.hasErrors()){
            model.addAttribute("positions", Position.values());
            return "player-form";
        }
        try {
            playerService.register(
                    playerForm.getName(),
                    playerForm.getRiotId(),
                    playerForm.getMainPosition(),
                    playerForm.getSubPosition()
            );
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("riotId", "duplicate", e.getMessage());
            model.addAttribute("positions", Position.values());
            return "player-form";
        }
        return "redirect:/players";
    }
}
