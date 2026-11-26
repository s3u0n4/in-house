package com.inhouse.inhouse_manager.player;

import jakarta.servlet.http.HttpServletRequest;
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

        // 1) 삭제하지 않는 줄만 입력값 검사 (PlayerForm의 @NotBlank, @NotNull 등)
        //    삭제할 줄까지 검사하면, 티어가 비어 있는 옛 플레이어를 지울 때도 "티어를 선택하세요"에 막힌다
        for (int i = 0; i < rows.size(); i++) {
            PlayerListForm.Row row = rows.get(i);
            if (row.isDelete()) {
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
        } catch (IllegalArgumentException e) {
            // 목록에 없는 플레이어(예: 그사이 새로 등록된 사람)와 라이엇 ID가 겹치는 경우
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

    /**
     * 관리자가 등록하면 바로 목록에 나오고, 다른 사람이 등록하면 "등록 신청"이 되어 관리자가 승인해야 나온다.
     * (누가 관리자인지는 스프링 시큐리티가 로그인 정보로 알려준다: request.isUserInRole)
     */
    @PostMapping("/players")
    public String create(@Valid @ModelAttribute PlayerForm playerForm, BindingResult bindingResult, Model model,
                         HttpServletRequest request, RedirectAttributes redirectAttributes){
        boolean admin = request.isUserInRole("ADMIN");
        if (bindingResult.hasErrors()){
            addFormOptions(model, null);
            return "player-form";
        }
        try {
            if (admin) {
                playerService.register(playerForm.getName(), playerForm.getRiotId(), playerForm.getMainPosition(),
                        playerForm.getSubPosition(), playerForm.toLaneTiers());
            } else {
                playerService.requestRegistration(playerForm.getName(), playerForm.getRiotId(),
                        playerForm.getMainPosition(), playerForm.getSubPosition(), playerForm.toLaneTiers());
            }
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("riotId", "duplicate", e.getMessage());
            addFormOptions(model, null);
            return "player-form";
        }
        // 플래시 속성: redirect 된 다음 화면에서 딱 한 번만 보이는 값
        redirectAttributes.addFlashAttribute("message", admin ? "플레이어를 등록했습니다."
                : "등록 신청을 보냈어요. 관리자가 승인하면 플레이어 목록에 나와요.");
        return "redirect:/players";
    }

    @GetMapping("/players/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("player", playerService.findVisibleById(id));
        model.addAttribute("record", statsService.recordOf(id));
        return "player-detail";
    }

    @GetMapping("/players/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("playerForm", PlayerForm.from(playerService.findActiveById(id)));
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
                    playerForm.toLaneTiers()
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
        playerService.delete(id);
        redirectAttributes.addFlashAttribute("message", "플레이어를 삭제했습니다. 관리자가 휴지통에서 되돌릴 수 있어요.");
        return "redirect:/players";
    }

    /** 등록과 수정이 같은 폼 화면을 쓴다. playerId가 null이면 등록, 있으면 수정 */
    private void addFormOptions(Model model, Long playerId) {
        model.addAttribute("positions", Position.values());
        model.addAttribute("laneTierOptions", Tier.values());
        model.addAttribute("playerId", playerId);
    }
}
