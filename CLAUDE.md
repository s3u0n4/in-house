# inhouse-manager — 롤 내전 관리 웹

League of Legends 내전(커스텀 게임) 참가자, 팀 나누기, 경기 결과와 전적을 관리하는 웹 애플리케이션.
사용자가 Spring을 처음 배우면서 만드는 첫 프로젝트다.

기획 문서: [요구사항명세서](docs/requirements.md), [화면설계서](docs/screen-design.md). 기능을 바꾸면 문서도 같이 고친다.

## 작업 규칙 (Claude가 반드시 지킬 것)
- **2026-11-25부터 Claude가 코드를 직접 작성한다.** 사용자가 요청했다 ("내가 직접하는 거 그냥 너가 알아서 해주고"). 그 전까지는 사용자가 직접 작성하고 Claude는 설명만 했다. 사용자가 다시 직접 쓰겠다고 하면 설명 모드로 돌아간다.
- 구현하면 테스트를 돌려서 확인하고, 의미 있는 단위로 커밋한다.
- 사용자는 Spring 입문자다. 무엇을 만들었는지, 새로 나온 개념(어노테이션 등)이 무엇인지, 어떻게 확인하는지를 쉬운 한국어로 요약해서 알려준다.
- 이 `CLAUDE.md`의 "진행 상황"과 "다음 할 일"을 항상 최신으로 갱신한다.
- 테스트나 확인용 서버는 사용자의 파일 DB를 건드리지 않게 메모리 DB로 띄운다 (예: `./gradlew bootRun --args='--server.port=8082 --spring.datasource.url=jdbc:h2:mem:smoke'`). 사용자는 보통 IntelliJ에서 8081 서버를 켜 두고 있다.

## 기술 스택
- Java 21, Spring Boot 4.1.2, Gradle
- Spring Web MVC, Spring Data JPA, Validation, Lombok, Thymeleaf
- H2 파일 DB: `./data/inhouse` (`data/`는 gitignore 대상)
- 서버 포트 8081, H2 콘솔 `http://localhost:8081/h2-console` (JDBC URL `jdbc:h2:file:./data/inhouse`, user `sa`, 비밀번호 없음)
- Spring Boot 4라서 예전 자료와 import 경로가 다른 것이 있다. 예: `@DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`
- 화면: Thymeleaf (2026-11-20 결정). 공통 레이아웃은 `templates/fragments/layout.html`(head, header 조각), CSS는 `static/css/style.css` 하나.
- 디자인: 롤 클라이언트 느낌의 어두운 테마 (2026-11-25, 사용자 요청). 남색 바탕, 금색 테두리, 헥스텍 청록 포인트, 영문 장식 글꼴 Cinzel. 색은 `style.css`의 `:root` 변수로 관리한다. 라이엇 로고와 이미지는 쓰지 않는다. 자세한 규칙은 화면설계서 3장에 있다.
- 테마 전환: 헤더의 `LoL | OP.GG` 버튼(쿠키 `theme`). OP.GG 테마는 `static/css/opgg.css`(밝은 회색, 파란 헤더, 승리/패배 줄 색). 두 CSS가 같은 클래스 이름을 쓰니 템플릿을 고치면 두 테마를 모두 확인한다.

## 패키지 구조
기능(도메인)별 패키지. 루트 패키지는 `com.inhouse.inhouse_manager`.
```
src/main/java/com/inhouse/inhouse_manager/
 ├─ InhouseManagerApplication.java
 ├─ HomeController.java          GET / → redirect:/players
 ├─ common/
 │   ├─ NotFoundException.java   @ResponseStatus(404). 없는 id 조회 시 → templates/error/404.html
 │   ├─ ThemeController.java     GET /theme/{name} → 쿠키 저장 후 Referer 경로로 redirect
 │   └─ ThemeAdvice.java         @ControllerAdvice로 모든 화면에 theme 값 전달
 ├─ player/
 │   ├─ Player.java, Position.java, PlayerRepository.java
 │   ├─ PlayerService.java       등록/수정/삭제(경기 기록 있으면 불가)/조회
 │   ├─ PlayerController.java    /players, /players/new, /players/{id}, /players/{id}/edit, /players/{id}/delete
 │   └─ PlayerForm.java          등록·수정 폼 + 검증
 ├─ match/
 │   ├─ Team.java                enum BLUE/RED (label: 블루/레드)
 │   ├─ Match.java               테이블 matches. participants @OneToMany(cascade ALL, orphanRemoval)
 │   ├─ MatchParticipant.java    match, player @ManyToOne(LAZY), team, position
 │   ├─ MatchRepository.java, MatchParticipantRepository.java   join fetch 쿼리
 │   ├─ MatchService.java        record(10자리 검증), findAll, findById, delete
 │   ├─ MatchController.java     /matches, /matches/new(?blue=&red= 미리 채우기), /matches/{id}, /matches/{id}/delete
 │   └─ MatchForm.java           bluePlayerIds, redPlayerIds (포지션 순서 리스트 5칸)
 ├─ stats/
 │   ├─ PlayerStats.java         record(games, wins): losses, winRateText, rating=(승+1)/(경기+2)
 │   ├─ PlayerRecord.java        상세 화면용 (전체, 포지션별, 최근 10경기)
 │   └─ StatsService.java        참가 기록으로 매번 계산 (저장 안 함)
 └─ team/
     ├─ TeamBalancer.java        126가지 분할 × 120가지 포지션 배치 완전 탐색 (순수 자바)
     ├─ TeamService.java         id 10개 → 후보 만들기 → TeamBalancer
     └─ TeamController.java      /teams, /teams/result (GET)
src/main/resources/templates/
 ├─ fragments/layout.html, error.html, error/404.html
 ├─ players.html, player-form.html(등록·수정 같이 씀), player-detail.html
 ├─ matches.html, match-form.html, match-detail.html
 └─ team-select.html, team-result.html
src/test/java/com/inhouse/inhouse_manager/
 ├─ InhouseManagerApplicationTests.java  (메모리 DB로 컨텍스트 로드)
 ├─ player/PlayerRepositoryTest, PlayerServiceTest
 ├─ match/MatchServiceTest
 ├─ stats/StatsServiceTest
 └─ team/TeamBalancerTest, TeamServiceTest
```

## 코드 관례
- 엔티티: `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@Setter`/`@Data` 사용 안 함, enum 필드에는 `@Enumerated(EnumType.STRING)`. 값 변경은 `update(...)` 같은 의미 있는 메서드로 (변경 감지).
- 폼 객체는 `@Getter @Setter` 사용 가능 (엔티티가 아니므로).
- 의존성 주입은 `private final` 필드 + `@RequiredArgsConstructor`.
- 예외: 입력/업무 규칙 위반은 `IllegalArgumentException`(Controller에서 잡아 `rejectValue`/`reject`), 삭제 불가는 `IllegalStateException`, 없는 id는 `NotFoundException`(404 화면).
- 저장·수정·삭제는 POST + redirect(PRG), 성공 문구는 `RedirectAttributes.addFlashAttribute("message", ...)`, 실패 문구는 `"errorMessage"`.
- 서비스 테스트는 `@DataJpaTest` + `@Import(...)`, AssertJ는 `org.assertj.core.api.Assertions`. 테스트 메서드 이름은 한글.
- Thymeleaf에서 record 값은 `${s.games()}`처럼 메서드 호출로 쓴다.

## 진행 상황
- [x] 프로젝트 생성, H2 파일 DB와 JPA 설정
- [x] `Player` 엔티티, `PlayerRepository`, `PlayerService` + 테스트 (사용자 작성, ~2026-11-20)
- [x] Thymeleaf 목록 화면, 등록 폼과 검증, `HomeController` (사용자 작성, 2026-11-23~24)
- [x] 요구사항명세서, 화면설계서 작성 (Claude, 2026-11-25)
- [x] 문서 기준 전체 구현 (Claude, 2026-11-25): 플레이어 상세·수정·삭제, 경기 기록·목록·상세·삭제, 전적, 팀 나누기, 공통 레이아웃·CSS, 404/오류 화면
  - 테스트 28개 통과. 메모리 DB 서버(8082)로 전체 흐름 확인 (등록 → 팀 나누기 → 경기 기록 미리 채우기 → 저장 → 전적 반영 → 삭제 제한, 404/400 화면, 휴대폰 폭 375px)
  - 처음에는 빈 `playerIds` 값이 들어오면 "존재하지 않는 플레이어" 문구가 잘못 나왔다. `TeamService`에서 null을 걸러내도록 고쳤다.

## 다음 할 일
> - 2026-11-25: 팀 나누기 화면에 "전체" 선택 체크박스를 추가했다 (사용자 요청, JS만 사용, 일부만 골랐을 때는 가로줄 상태로 표시).
> - 2026-11-25: 전체 디자인을 롤 테마로 바꿨다 (`style.css` 전면 교체, 헤더 로고, 승/패 배지를 "승리/패배"로 변경, 인라인 style 제거). 시험 서버에서 화면을 데스크톱과 375px 폭으로 찍어 확인했다.
> - 2026-11-25: 사용자가 "op.gg 디자인도 보여줘"라고 해서 OP.GG 느낌의 테마를 추가하고 헤더에 전환 버튼을 달았다. 기본값은 LoL 테마다. 최근 경기 줄에 `row-win`/`row-lose` 클래스를 붙였다.
> **현재 위치 (2026-11-25):** 요구사항명세서의 필수 기능과 권장 기능을 모두 구현했다. 사용자가 8081 서버를 재시작했고, 모든 화면이 정상 응답하는 것을 확인했다(경기 테이블 자동 생성, 404 화면 포함). 사용자의 요청으로 테스트용 플레이어 8명(`테스트탑1`~`테스트서폿2`, 라이엇 ID는 `#TEST`로 끝남)을 사용자 DB에 등록했다. 총 10명이다. 사용자가 팀 나누기 → 경기 기록으로 첫 경기(match id 1, 2026-11-25, 블루 승)를 기록했고, 목록, 상세, 전적이 모두 맞게 반영된 것을 확인했다. 이제 테스트 플레이어를 지우려면 경기부터 삭제해야 한다.

후보 (요구사항명세서 1.3 "제외" 항목):
1. 경기 결과 수정
2. 챔피언, KDA 기록
3. 컨트롤러 테스트 (`@WebMvcTest`)
4. 로그인, Riot API 연동
