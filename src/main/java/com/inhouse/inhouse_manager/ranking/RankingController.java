package com.inhouse.inhouse_manager.ranking;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class RankingController {
    private final RankingService rankingService;

    /** sort: RATE(승률, 기본), WINS(다승), POWER(전력). 잘못된 값이면 400 오류 화면 */
    @GetMapping("/ranking")
    public String ranking(@RequestParam(defaultValue = "RATE") RankingSort sort, Model model) {
        model.addAttribute("rows", rankingService.ranking(sort));
        model.addAttribute("sort", sort);
        model.addAttribute("sorts", RankingSort.values());
        return "ranking";
    }
}
