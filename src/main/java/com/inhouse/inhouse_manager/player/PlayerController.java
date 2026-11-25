package com.inhouse.inhouse_manager.player;

import com.inhouse.inhouse_manager.stats.StatsService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
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
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class PlayerController {
    private final PlayerService playerService;
    private final StatsService statsService;
    /** 목록 수정에서 줄마다 직접 검사할 때 쓰는 Bean Validation 검사기 (스프링 부트가 만들어 준다) */
    private final Validator validator;

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
    public String editAll(@ModelAttribute PlayerListForm playerListForm, BindingResult bindingResult,
                          Model model, RedirectAttributes redirectAttributes) {
        List<PlayerListForm.Row> rows = playerListForm.getRows();
        Set<Long> idsWithMatches = playerService.idsWithMatches();

        // 1) 삭제하지 않는 줄만 입력값 검사 (PlayerForm의 @NotBlank, @NotNull 등)
        //    삭제할 줄까지 검사하면, 티어가 비어 있는 옛 플레이어를 지울 때도 "티어를 선택하세요"에 막힌다
        for (int i = 0; i < rows.size(); i++) {
            PlayerListForm.Row row = rows.get(i);
            if (row.isDelete()) {
                if (idsWithMatches.contains(row.getId())) {
                    bindingResult.rejectValue("rows[" + i + "].delete", "hasMatches", "경기 기록이 있어 삭제할 수 없습니다.");
                }
                continue;
            }
            for (ConstraintViolation<PlayerListForm.Row> violation : validator.validate(row)) {
                bindingResult.rejectValue("rows[" + i + "]." + violation.getPropertyPath(), "invalid", violation.getMessage());
            }
        }

        // 2) 같은 라이엇 ID를 두 줄에 적었으면 두 번째 줄에 오류를 붙인다 (삭제할 줄은 빼고 비교)
        Map<String, Integer> firstRowByRiotId = new HashMap<>();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).isDelete()) {
                continue;
            }
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
        List<PlayerUpdate> updates = rows.stream().filter(row -> !row.isDelete()).map(PlayerListForm.Row::toUpdate).toList();
        List<Long> deleteIds = rows.stream().filter(PlayerListForm.Row::isDelete).map(PlayerListForm.Row::getId).toList();
        try {
            playerService.saveAll(updates, deleteIds);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // 목록에 없는 플레이어(예: 그사이 새로 등록된 사람)와 라이엇 ID가 겹치거나, 그사이 경기 기록이 생긴 경우
            bindingResult.reject("saveFailed", e.getMessage());
            addFormOptions(model, null);
            return "players-edit";
        }
        String message = "플레이어 " + updates.size() + "명의 정보를 저장했습니다.";
        if (!deleteIds.isEmpty()) {
            message += " (" + deleteIds.size() + "명 삭제)";
        }
        redirectAttributes.addFlashAttribute("message", message);
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
        model.addAttribute("idsWithMatches", playerService.idsWithMatches());
    }
}
