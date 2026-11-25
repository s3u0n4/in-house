package com.inhouse.inhouse_manager.match;

import com.inhouse.inhouse_manager.player.PlayerService;
import com.inhouse.inhouse_manager.player.Position;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MatchController {
    private final MatchService matchService;
    private final PlayerService playerService;

    @GetMapping("/matches")
    public String list(Model model) {
        model.addAttribute("matches", matchService.findAll());
        return "matches";
    }

    /** blue, red: 팀 나누기 결과에서 넘어온 플레이어 id (포지션 순서). 없으면 빈 폼 */
    @GetMapping("/matches/new")
    public String createForm(@RequestParam(required = false) List<Long> blue,
                             @RequestParam(required = false) List<Long> red,
                             Model model) {
        MatchForm matchForm = new MatchForm();
        matchForm.setPlayedAt(LocalDate.now());
        if (blue != null && blue.size() == Position.values().length) {
            matchForm.setBluePlayerIds(new ArrayList<>(blue));
        }
        if (red != null && red.size() == Position.values().length) {
            matchForm.setRedPlayerIds(new ArrayList<>(red));
        }
        model.addAttribute("matchForm", matchForm);
        addFormOptions(model);
        return "match-form";
    }

    @PostMapping("/matches")
    public String create(@Valid @ModelAttribute MatchForm matchForm, BindingResult bindingResult, Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addFormOptions(model);
            return "match-form";
        }
        Long matchId;
        try {
            matchId = matchService.record(
                    matchForm.getPlayedAt(),
                    matchForm.getWinner(),
                    matchForm.getBluePlayerIds(),
                    matchForm.getRedPlayerIds()
            );
        } catch (IllegalArgumentException e) {
            // reject: 특정 칸이 아니라 폼 전체에 대한 오류 (전체 오류)
            bindingResult.reject("invalidLineup", e.getMessage());
            addFormOptions(model);
            return "match-form";
        }
        redirectAttributes.addFlashAttribute("message", "경기를 기록했습니다.");
        return "redirect:/matches/" + matchId;
    }

    @GetMapping("/matches/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("match", matchService.findById(id));
        return "match-detail";
    }

    @PostMapping("/matches/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        matchService.delete(id);
        redirectAttributes.addFlashAttribute("message", "경기를 삭제했습니다. 관리자가 휴지통에서 되돌릴 수 있어요.");
        return "redirect:/matches";
    }

    private void addFormOptions(Model model) {
        model.addAttribute("players", playerService.findAllOrderByName());
        model.addAttribute("positions", Position.values());
        model.addAttribute("teams", Team.values());
    }
}
