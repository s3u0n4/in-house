package com.inhouse.inhouse_manager.player;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PlayerController {
    private final PlayerService playerService;

    @GetMapping("/players")
    public String list(Model model) {
        model.addAttribute("players", playerService.findAll());
        return "players";
    }
}
