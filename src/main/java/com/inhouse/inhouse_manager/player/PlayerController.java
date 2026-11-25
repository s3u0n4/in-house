package com.inhouse.inhouse_manager.player;

import com.inhouse.inhouse_manager.stats.StatsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PlayerController {
    private final PlayerService playerService;
    private final StatsService statsService;

    @GetMapping("/players")
    public String list(Model model) {
        List<Player> players = playerService.findAll();
        model.addAttribute("players", players);
        model.addAttribute("stats", statsService.statsByPlayer(players.stream().map(Player::getId).toList()));
        return "players";
    }

    /** 목록 수정: 모든 플레이어를 표 하나에서 고친다 */
    @GetMapping("/players/edit")
    public String editAllForm(Model model) {
        model.addAttribute("playerListForm", PlayerListForm.from(playerService.findAll()));
        addFormOptions(model, null);
        return "players-edit";
    }

    @PostMapping("/players/edit")
    public String editAll(@Valid @ModelAttribute PlayerListForm playerListForm, BindingResult bindingResult,
                          Model model, RedirectAttributes redirectAttributes) {
        // 같은 라이엇 ID를 두 줄에 적었으면 두 번째 줄에 오류를 붙인다
        List<PlayerListForm.Row> rows = playerListForm.getRows();
        Map<String, Integer> firstRowByRiotId = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            String riotId = rows.get(i).getRiotId();
            if (riotId == null || riotId.isBlank()) {
                continue;
            }
            Integer firstRow = firstRowByRiotId.putIfAbsent(riotId.strip(), i);
            if (firstRow != null) {
                bindingResult.rejectValue("rows[" + i + "].riotId", "duplicate",
                        (firstRow + 1) + "번째 줄과 라이엇 ID가 같습니다.");
            }
        }
        if (bindingResult.hasErrors()) {
            addFormOptions(model, null);
            return "players-edit";
        }
        try {
            playerService.updateAll(rows.stream().map(PlayerListForm.Row::toUpdate).toList());
        } catch (IllegalArgumentException e) {
            // 목록에 없는 플레이어(예: 그사이 새로 등록된 사람)와 라이엇 ID가 겹치는 경우
            bindingResult.reject("duplicate", e.getMessage());
            addFormOptions(model, null);
            return "players-edit";
        }
        redirectAttributes.addFlashAttribute("message", "플레이어 " + rows.size() + "명의 정보를 저장했습니다.");
        return "redirect:/players";
    }

    @GetMapping("/players/new")
    public String createForm(Model model){
        model.addAttribute("playerForm", new PlayerForm());
        addFormOptions(model, null);
        return "player-form";
    }

    @PostMapping("/players")
    public String create(@Valid @ModelAttribute PlayerForm playerForm, BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes){
        if (bindingResult.hasErrors()){
            addFormOptions(model, null);
            return "player-form";
        }
        try {
            playerService.register(
                    playerForm.getName(),
                    playerForm.getRiotId(),
                    playerForm.getMainPosition(),
                    playerForm.getSubPosition(),
                    playerForm.getTier()
            );
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("riotId", "duplicate", e.getMessage());
            addFormOptions(model, null);
            return "player-form";
        }
        // 플래시 속성: redirect 된 다음 화면에서 딱 한 번만 보이는 값
        redirectAttributes.addFlashAttribute("message", "플레이어를 등록했습니다.");
        return "redirect:/players";
    }

    @GetMapping("/players/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("player", playerService.findById(id));
        model.addAttribute("record", statsService.recordOf(id));
        return "player-detail";
    }

    @GetMapping("/players/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("playerForm", PlayerForm.from(playerService.findById(id)));
        addFormOptions(model, id);
        return "player-form";
    }

    @PostMapping("/players/{id}/edit")
    public String edit(@PathVariable Long id, @Valid @ModelAttribute PlayerForm playerForm,
                       BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormOptions(model, id);
            return "player-form";
        }
        try {
            playerService.update(id,
                    playerForm.getName(),
                    playerForm.getRiotId(),
                    playerForm.getMainPosition(),
                    playerForm.getSubPosition(),
                    playerForm.getTier()
            );
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("riotId", "duplicate", e.getMessage());
            addFormOptions(model, id);
            return "player-form";
        }
        redirectAttributes.addFlashAttribute("message", "플레이어 정보를 수정했습니다.");
        return "redirect:/players/" + id;
    }

    @PostMapping("/players/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            playerService.delete(id);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/players/" + id;
        }
        redirectAttributes.addFlashAttribute("message", "플레이어를 삭제했습니다.");
        return "redirect:/players";
    }

    /** 등록과 수정이 같은 폼 화면을 쓴다. playerId가 null이면 등록, 있으면 수정 */
    private void addFormOptions(Model model, Long playerId) {
        model.addAttribute("positions", Position.values());
        model.addAttribute("tiers", Tier.values());
        model.addAttribute("playerId", playerId);
    }
}
