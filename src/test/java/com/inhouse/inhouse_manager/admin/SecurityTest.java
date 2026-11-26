package com.inhouse.inhouse_manager.admin;

import com.inhouse.inhouse_manager.player.LaneTiers;
import com.inhouse.inhouse_manager.player.Player;
import com.inhouse.inhouse_manager.player.PlayerRepository;
import com.inhouse.inhouse_manager.player.Position;
import com.inhouse.inhouse_manager.player.Tier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 누가 무엇을 할 수 있는지 실제 요청을 보내서 확인한다 (MockMvc: 서버를 띄우지 않고 요청을 흉내 낸다).
 * - 로그인 안 한 사람: 보기, 팀 나누기, 등록 신청만
 * - 관리자: 전부
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:securityTest")
@AutoConfigureMockMvc
@Transactional
class SecurityTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PlayerRepository playerRepository;

    Long playerId;

    @BeforeEach
    void setUp() {
        playerId = playerRepository.save(new Player("철수", "철수#KR1", Position.MID, null,
                new LaneTiers(Tier.GOLD, Tier.GOLD, Tier.GOLD, Tier.GOLD, Tier.GOLD))).getId();
    }

    private MockHttpServletRequestBuilder registerForm(String name, String riotId) {
        return post("/players").with(csrf())
                .param("name", name).param("riotId", riotId).param("mainPosition", "TOP")
                .param("topTier", "GOLD").param("jungleTier", "GOLD").param("midTier", "GOLD")
                .param("adcTier", "GOLD").param("supportTier", "GOLD");
    }

    @Test
    void 로그인_안_해도_보기와_팀_나누기와_등록_신청_화면은_열린다() throws Exception {
        for (String url : new String[]{"/players", "/players/" + playerId, "/matches", "/ranking", "/teams", "/players/new"}) {
            mockMvc.perform(get(url)).andExpect(status().isOk());
        }
    }

    @Test
    void 로그인_안_하면_수정_화면과_경기_기록_화면과_관리자_화면은_로그인으로_보낸다() throws Exception {
        for (String url : new String[]{"/players/edit", "/players/" + playerId + "/edit", "/matches/new",
                "/admin/pending", "/admin/trash"}) {
            mockMvc.perform(get(url)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        }
    }

    @Test
    void 로그인_안_하면_수정_삭제_경기_기록_승인을_보낼_수_없다() throws Exception {
        for (String url : new String[]{"/players/" + playerId + "/delete", "/players/" + playerId + "/edit",
                "/players/edit", "/matches", "/admin/players/" + playerId + "/approve"}) {
            mockMvc.perform(post(url).with(csrf())).andExpect(redirectedUrl("/login"));
        }
        // 실제로 지워지지 않았다
        assertThat(playerRepository.findById(playerId).orElseThrow().isDeleted()).isFalse();
    }

    @Test
    void 로그인_안_한_사람이_등록하면_승인_대기가_된다() throws Exception {
        mockMvc.perform(registerForm("영희", "영희#KR1")).andExpect(redirectedUrl("/players"));

        Player requested = playerRepository.findByRiotId("영희#KR1").orElseThrow();
        assertThat(requested.isPending()).isTrue();
        // 승인 전에는 목록에도, 상세 주소에도 나오지 않는다
        mockMvc.perform(get("/players")).andExpect(content().string(not(
                containsString("영희#KR1"))));
        mockMvc.perform(get("/players/" + requested.getId())).andExpect(status().isNotFound());
    }

    @Test
    void 관리자가_등록하면_바로_승인된다() throws Exception {
        mockMvc.perform(registerForm("영희", "영희#KR1").with(user("admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/players"));

        assertThat(playerRepository.findByRiotId("영희#KR1").orElseThrow().isPending()).isFalse();
    }

    @Test
    void 관리자는_등록_신청을_승인한다() throws Exception {
        mockMvc.perform(registerForm("영희", "영희#KR1"));
        Long requestedId = playerRepository.findByRiotId("영희#KR1").orElseThrow().getId();

        mockMvc.perform(get("/admin/pending").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("영희#KR1")));
        mockMvc.perform(post("/admin/players/" + requestedId + "/approve").with(csrf()).with(user("admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/admin/pending"));

        assertThat(playerRepository.findById(requestedId).orElseThrow().isPending()).isFalse();
    }

    @Test
    void CSRF_토큰_없이_보내면_막힌다() throws Exception {
        mockMvc.perform(post("/players").param("name", "x")).andExpect(status().isForbidden());
    }

    @Test
    void 관리자_버튼은_관리자에게만_보인다() throws Exception {
        mockMvc.perform(get("/players"))
                .andExpect(content().string(not(containsString("목록 수정"))))
                .andExpect(content().string(containsString("등록 신청")));
        mockMvc.perform(get("/players").with(user("admin").roles("ADMIN")))
                .andExpect(content().string(containsString("목록 수정")));
    }
}
