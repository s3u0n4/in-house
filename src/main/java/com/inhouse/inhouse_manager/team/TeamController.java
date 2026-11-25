package com.inhouse.inhouse_manager.team;

import com.inhouse.inhouse_manager.player.Player;
import com.inhouse.inhouse_manager.player.PlayerService;
import com.inhouse.inhouse_manager.stats.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class TeamController {
    private final TeamService teamService;
    private final PlayerService playerService;
    private final StatsService statsService;

    /** playerIds: "참가자 다시 고르기"로 돌아왔을 때 미리 체크해 둘 플레이어 */
    @GetMapping("/teams")
    public String selectForm(@RequestParam(required = false) List<Long> playerIds, Model model) {
        addSelectOptions(model, playerIds);
        return "team-select";
    }

    // 데이터를 바꾸지 않으니 GET으로 받는다. 새로고침해도 안전하다
    @GetMapping("/teams/result")
    public String result(@RequestParam(required = false) List<Long> playerIds, Model model) {
        try {
            model.addAttribute("split", teamService.split(playerIds));
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addSelectOptions(model, playerIds);
            return "team-select";
        }
        model.addAttribute("playerIds", playerIds);
        return "team-result";
    }

    private void addSelectOptions(Model model, List<Long> selectedIds) {
        List<Player> players = playerService.findAllOrderByName();
        model.addAttribute("players", players);
        model.addAttribute("stats", statsService.statsByPlayer(players.stream().map(Player::getId).toList()));
        model.addAttribute("selectedIds", selectedIds == null ? List.of() : selectedIds);
        model.addAttribute("requiredCount", TeamBalancer.PLAYER_COUNT);
    }
}
