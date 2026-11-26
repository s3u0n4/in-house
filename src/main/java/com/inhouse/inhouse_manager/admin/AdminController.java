package com.inhouse.inhouse_manager.admin;

import com.inhouse.inhouse_manager.match.MatchService;
import com.inhouse.inhouse_manager.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 로그인 화면과 관리자 전용 화면(등록 신청 승인, 휴지통). /admin/** 은 SecurityConfig에서 관리자만 들어오게 막는다.
 */
@Controller
@RequiredArgsConstructor
public class AdminController {
    private final PlayerService playerService;
    private final MatchService matchService;

    /** 로그인 폼은 스프링 시큐리티가 POST /login 으로 받아서 처리한다. 여기서는 화면만 보여준다 */
    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @GetMapping("/admin/pending")
    public String pending(Model model) {
        model.addAttribute("pendingPlayers", playerService.findPending());
        return "admin-pending";
    }

    @PostMapping("/admin/players/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        playerService.approve(id);
        redirectAttributes.addFlashAttribute("message", "등록 신청을 승인했습니다. 이제 플레이어 목록에 나와요.");
        return "redirect:/admin/pending";
    }

    @PostMapping("/admin/players/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        playerService.reject(id);
        redirectAttributes.addFlashAttribute("message", "등록 신청을 거절했습니다.");
        return "redirect:/admin/pending";
    }

    @GetMapping("/admin/trash")
    public String trash(Model model) {
        model.addAttribute("deletedPlayers", playerService.findDeleted());
        model.addAttribute("deletedMatches", matchService.findDeleted());
        return "admin-trash";
    }

    @PostMapping("/admin/players/{id}/restore")
    public String restorePlayer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            playerService.restore(id);
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/trash";
        }
        redirectAttributes.addFlashAttribute("message", "플레이어를 되돌렸습니다.");
        return "redirect:/admin/trash";
    }

    @PostMapping("/admin/matches/{id}/restore")
    public String restoreMatch(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        matchService.restore(id);
        redirectAttributes.addFlashAttribute("message", "경기 기록을 되돌렸습니다.");
        return "redirect:/admin/trash";
    }
}
