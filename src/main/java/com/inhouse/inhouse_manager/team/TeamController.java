package com.inhouse.inhouse_manager.team;

import com.inhouse.inhouse_manager.player.Player;
import com.inhouse.inhouse_manager.player.PlayerService;
import com.inhouse.inhouse_manager.player.Position;
import com.inhouse.inhouse_manager.stats.StatsService;
import com.inhouse.inhouse_manager.team.TeamBalancer.TeamSplit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

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

    /**
     * 데이터를 바꾸지 않으니 GET으로 받는다. 새로고침해도 안전하다.
     * shuffle: 있으면 균형 결과에서 같은 라인끼리 몇 쌍을 무작위로 맞바꾼다 (값은 무작위 seed)
     */
    @GetMapping("/teams/result")
    public String result(@RequestParam(required = false) List<Long> playerIds,
                         @RequestParam(required = false) Long shuffle,
                         Model model) {
        TeamSplit split;
        try {
            split = teamService.split(playerIds);
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addSelectOptions(model, playerIds);
            return "team-select";
        }

        model.addAttribute("shuffleOptionCount", teamService.fairSwapOptions(split).size());
        // 라인끼리 실력이 안 맞는데 다른 라인으로 팀 전력을 맞춘 결과면 랜덤 교체를 막는다 (주소에 shuffle을 직접 넣어도)
        List<Position> unbalancedLanes = split.unbalancedLanes();
        model.addAttribute("unbalancedLanes", unbalancedLanes);
        if (shuffle != null && !unbalancedLanes.isEmpty()) {
            model.addAttribute("errorMessage", "라인별 밸런스가 맞지 않아서 랜덤으로 바꿀 수 없어요.");
            shuffle = null;
        }
        Set<Position> swappedPositions = Set.of();
        if (shuffle != null) {
            swappedPositions = teamService.pairsToSwap(split, shuffle);
            split = teamService.swapPairs(split, swappedPositions);
        }
        model.addAttribute("split", split);
        model.addAttribute("swappedPositions", swappedPositions);
        model.addAttribute("shuffled", shuffle != null);
        // "랜덤으로 섞기" 버튼에 넣을 다음 seed
        model.addAttribute("nextShuffle", ThreadLocalRandom.current().nextInt(1, 1_000_000));
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
