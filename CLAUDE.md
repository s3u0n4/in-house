# inhouse-manager — 롤 내전 관리 웹

League of Legends 내전(커스텀 게임) 참가자, 팀 나누기, 경기 결과와 전적을 관리하는 웹 애플리케이션.
사용자가 Spring을 처음 배우면서 만드는 첫 프로젝트다.

기획 문서: [요구사항명세서](docs/requirements.md), [화면설계서](docs/screen-design.md), [배포 안내](docs/deployment.md). 기능을 바꾸면 문서도 같이 고친다.

## 작업 규칙 (Claude가 반드시 지킬 것)
- **2026-11-25부터 Claude가 코드를 직접 작성한다.** 사용자가 요청했다 ("내가 직접하는 거 그냥 너가 알아서 해주고"). 그 전까지는 사용자가 직접 작성하고 Claude는 설명만 했다. 사용자가 다시 직접 쓰겠다고 하면 설명 모드로 돌아간다.
- 구현하면 테스트를 돌려서 확인하고, 의미 있는 단위로 커밋한다.
- **커밋하면 바로 GitHub에 푸시한다** (2026-11-25 사용자 요청: "깃헙 푸쉬도 자동으로 해줘"). 원격은 `origin` = `https://github.com/s3u0n4/in-house.git`, 브랜치는 `main`. 푸시가 실패하면 강제 푸시하지 말고 사용자에게 알린다.
- 사용자는 Spring 입문자다. 무엇을 만들었는지, 새로 나온 개념(어노테이션 등)이 무엇인지, 어떻게 확인하는지를 쉬운 한국어로 요약해서 알려준다.
- 이 `CLAUDE.md`의 "진행 상황"과 "다음 할 일"을 항상 최신으로 갱신한다.
- 시큐리티 때문에 curl로 POST 하면 403이 난다. 확인용 POST는 브라우저에서 하거나, 페이지 안에서 `input[name=_csrf]` 값을 꺼내 fetch로 보낸다.
- 테스트나 확인용 서버는 사용자의 파일 DB를 건드리지 않게 메모리 DB로 띄운다 (예: `./gradlew bootRun --args='--server.port=8082 --spring.datasource.url=jdbc:h2:mem:smoke'`). 사용자는 보통 IntelliJ에서 8081 서버를 켜 두고 있다.

## 기술 스택
- Java 21, Spring Boot 4.1.2, Gradle
- Spring Web MVC, Spring Data JPA, Validation, Lombok, Thymeleaf
- H2 파일 DB: `./data/inhouse` (`data/`는 gitignore 대상)
- 스프링 시큐리티 (2026-11-25, 2026-11-26 배포 준비로 변경): 일반 사용자 로그인 없음, 관리자 계정 1개. 아이디/비밀번호는 `config/application.properties`(gitignore, 스프링 부트가 자동으로 읽음)의 `inhouse.admin.username`/`inhouse.admin.password`, 배포 서버는 환경 변수 `INHOUSE_ADMIN_*`. **GitHub 저장소가 public이므로 비밀번호를 커밋하지 말 것.** 누구나: 보기, 팀 나누기, 등록 신청(POST /players → 승인 대기). 관리자만: 그 밖의 모든 POST(기본값이 관리자 전용), 수정·경기 기록 화면, `/admin/**`, H2 콘솔. 관리자 버튼은 템플릿에서 `${isAdmin}`으로 숨긴다. 모든 POST에 CSRF 토큰 필요(`th:action`이 자동으로 넣음)
- 배포(2026-11-26 준비만, 아직 안 함): `prod` 프로필(`application-prod.properties`) = PostgreSQL(드라이버 `org.postgresql:postgresql`), 접속 정보와 관리자 비밀번호는 환경 변수, 비밀번호 없으면 서버가 안 켜짐. 자세한 건 `docs/deployment.md`
- 서버 포트 8081, H2 콘솔 `http://localhost:8081/h2-console` (관리자 로그인 필요. JDBC URL `jdbc:h2:file:./data/inhouse`, user `sa`, 비밀번호 없음)
- Spring Boot 4라서 예전 자료와 import 경로가 다른 것이 있다. 예: `@DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`
- 화면: Thymeleaf (2026-11-20 결정). 공통 레이아웃은 `templates/fragments/layout.html`(head, header 조각), CSS는 `static/css/style.css` 하나.
- 디자인: 깔끔한 흰색 + 하늘색 테마 하나만 쓴다 (2026-11-28 사용자 요청, 그 전의 e스포츠 중계 다크 테마를 대신함). 옅은 하늘색 바탕, 흰 카드, 하늘색 포인트 `#0ea5e9`, 블루/레드는 팀 구분에만, 글꼴은 Pretendard. 색은 `style.css`의 `:root` 변수로 관리한다. 등록·수정·로그인·관리자 화면은 가운데 배치(`main`의 `centered`/`narrow`/`auth` 클래스). 자세한 규칙은 화면설계서 3장에 있다.

## 패키지 구조
기능(도메인)별 패키지. 루트 패키지는 `com.inhouse.inhouse_manager`.
```
src/main/java/com/inhouse/inhouse_manager/
 ├─ InhouseManagerApplication.java
 ├─ HomeController.java          GET / → redirect:/players
 ├─ admin/
 │   ├─ SecurityConfig.java      시큐리티 설정(누가 무엇을 하는지) + 관리자 계정(InMemoryUserDetailsManager)
 │   ├─ AdminAdvice.java         모든 화면에 isAdmin, pendingCount(승인 대기 수) 전달
 │   └─ AdminController.java     GET /login, GET /admin/pending, POST /admin/players/{id}/approve|reject, GET /admin/trash, POST /admin/{players|matches}/{id}/restore
 ├─ common/
 │   └─ NotFoundException.java   @ResponseStatus(404). 없는 id 조회 시 → templates/error/404.html
 ├─ player/
 │   ├─ Player.java, Position.java, Tier.java(아이언 1 ~ 챌린저 10, 마스터 이상 LP 점수), PlayerRepository.java
 │   ├─ LaneTiers.java           라인별 티어 5개 + LP (@Embeddable record). Player.laneTiers, 옛 플레이어는 null
 │   ├─ PlayerService.java       등록(관리자)/등록 신청(누구나, pending)/승인/거절/수정/삭제(소프트)/조회. findAll 등은 승인된 사람만
 │   ├─ PlayerController.java    /players, /players/new, /players/edit(목록 수정), /players/{id}, /players/{id}/edit, /players/{id}/delete
 │   ├─ PlayerForm.java          등록·수정 폼 + 검증 (라인별 티어·LP 필드, 부≠주 포지션)
 │   ├─ PlayerListForm.java      목록 수정 폼 (Row extends PlayerForm + id, @Valid 리스트)
 │   └─ PlayerUpdate.java        record, 여러 명 수정용 (PlayerService.updateAll, 한 트랜잭션)
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
 ├─ ranking/                    (2026-11-26 추가)
 │   ├─ RankingSort.java         enum RATE(보정 승률)/WINS(다승)/POWER(전력) + 화면 설명
 │   ├─ RankingRow.java          record(rank, player, stats, power). rank null = 순위 밖
 │   ├─ RankingService.java      삭제 안 된 플레이어 정렬 + 공동 순위(1,1,3). 화면 자릿수로 반올림해 비교
 │   └─ RankingController.java   GET /ranking?sort=
 └─ team/
     ├─ TeamBalancer.java        126가지 분할 × 120가지 포지션 배치 완전 탐색 (순수 자바). 불균형 = 전력 차이 + 0.5 × 라인 맞대결 차이(같은 라인끼리 전력 차이 합). 주 포지션이 아니면 전력 감점(부 -1, 그 외 -2)
     ├─ TeamService.java         id 10개 → 후보 만들기(라인별 티어 없으면 참가자 주 라인 티어 평균) → TeamBalancer
     └─ TeamController.java      /teams, /teams/result (GET, shuffle=seed면 5개 라인 각각 교체 여부를 랜덤으로, 1~4쌍 교체)
src/main/resources/templates/
 ├─ fragments/layout.html, error.html, error/404.html, login.html, admin-trash.html, admin-pending.html
 ├─ players.html, players-edit.html(목록 수정), player-form.html(등록·수정 같이 씀), player-detail.html
 ├─ matches.html, match-form.html, match-detail.html
 ├─ team-select.html, team-result.html
 └─ ranking.html
src/test/java/com/inhouse/inhouse_manager/
 ├─ InhouseManagerApplicationTests.java  (메모리 DB로 컨텍스트 로드)
 ├─ admin/SecurityTest           MockMvc로 권한 확인 (누구나/관리자, 등록 신청, CSRF)
 ├─ player/PlayerRepositoryTest, PlayerServiceTest
 ├─ match/MatchServiceTest
 ├─ stats/StatsServiceTest
 ├─ ranking/RankingServiceTest
 └─ team/TeamBalancerTest, TeamServiceTest
```

## 코드 관례
- 엔티티: `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@Setter`/`@Data` 사용 안 함, enum 필드에는 `@Enumerated(EnumType.STRING)`. 값 변경은 `update(...)` 같은 의미 있는 메서드로 (변경 감지).
- 폼 객체는 `@Getter @Setter` 사용 가능 (엔티티가 아니므로).
- 의존성 주입은 `private final` 필드 + `@RequiredArgsConstructor`.
- 예외: 입력/업무 규칙 위반은 `IllegalArgumentException`(Controller에서 잡아 `rejectValue`/`reject`), 없는 id는 `NotFoundException`(404 화면).
- 삭제는 모두 소프트 삭제 (플레이어, 경기 모두 `deletedAt`). 되돌리기는 관리자만 휴지통에서. 경기 조회 쿼리(목록, 상세, 전적)는 `deletedAt is null`을 붙인다.
- 플레이어 소프트 삭제와 승인 대기: `Player.deletedAt`이 있으면 삭제된 사람, `Player.pending`이면 승인 대기(등록 신청). `isActive()` = 둘 다 아님. 목록용 조회는 `PlayerService.findAll`/`findAllOrderByName`(삭제·승인 대기 제외), 수정·삭제는 `findActiveById`, 상세는 `findVisibleById`(승인 대기면 404). 경기 기록 화면은 `player.displayName`("이름 (삭제됨)")을 쓴다. 새 조회를 만들 때 삭제된 사람과 승인 대기를 빼야 하는지 꼭 확인할 것.
- 누구나 입력하는 값(이름 등)을 자바스크립트 코드(`onsubmit="..."`)에 직접 넣지 않는다. 확인창 문구는 `th:data-confirm` + `confirm(this.dataset.confirm)`.
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
> - 2026-11-25: 사용자가 추천을 받고 고른 "e스포츠 중계" 테마를 세 번째로 추가했다. 이때부터 커밋 후 자동 푸시를 시작했다.
> - 2026-11-25: 사용자 요청으로 중계 테마만 남겼다. LoL/OP.GG 테마, 테마 전환 버튼, `ThemeController`, `ThemeAdvice`를 지우고 `esports.css`를 `style.css`로 바꿨다. (예전 `theme` 쿠키가 브라우저에 남아 있어도 아무 영향 없다.)
> - 2026-11-25: 사용자가 "롤 테마로 보인다"고 했다. 서버는 중계 CSS를 보내고 있었고, 브라우저가 같은 주소(`/css/style.css`)의 예전 롤 CSS를 캐시에서 쓰고 있었다. `application.properties`에 정적 파일 내용 해시 설정(`spring.web.resources.chain.strategy.content.*`)을 추가해서, 이제 CSS 주소가 `/css/style-<해시>.css`로 바뀐다(Thymeleaf `@{}` 링크에 자동 적용).
> - 2026-11-25: 티어 기능 추가 (사용자 요청: "티어 입력도 받아서 팀마다 티어 분포 비슷하게"). `Tier` enum, Player.tier(DB null 허용, 폼에서는 필수). 팀 나누기는 전력 점수(티어 + 내전 승률 보정 ±2)와 티어 분포를 함께 본다. 테스트 31개. 사용자 DB의 기존 플레이어 10명은 티어가 비어 있으니 수정 화면에서 입력해야 한다(비어 있어도 팀 나누기는 참가자 평균으로 동작).
> - 2026-11-25: 사용자 요청으로 실제 DB에 임의 티어의 테스트 플레이어 10명(라이엇 ID `#RAND`, id 12~21)을 추가하고 팀 나누기를 시험했다. 전력 차이 0.0, 티어 분포 차이 6.0(이 10명으로 가능한 최솟값), 10명 모두 주 포지션.
> - 2026-11-25: "짜인 팀에서 2명씩 매칭해서 랜덤으로 팀 바꾸기" 추가. 같은 라인끼리 짝지어 1~2쌍을 맞바꾸고, 불균형이 +2 넘게 늘어나는 조합은 제외(후보가 3개 미만이면 불균형이 작은 3개). 처음엔 아무 쌍이나 바꿔서 전력 차이가 11까지 벌어졌고, `java.util.Random`은 seed가 비슷하면 같은 값이 나와서 `SplittableRandom`으로 바꿨다. 테스트 35개.
> - 2026-11-25: 사용자 요청("2명씩이 아니라 5명 다 바꾸는데 바뀐 사람만 표시")으로 랜덤 교체 대상을 5개 라인 전체로 넓혔다(1~2쌍 → 1~4쌍, 30가지 조합). 균형 필터는 유지했다.
> - 2026-11-25: 플레이어 "목록 수정" 화면 추가 (여러 명을 표에서 한 번에 수정, 오류면 전부 저장 안 함). 처음 방문 때 주소에 `;jsessionid`가 붙어서 `server.servlet.session.tracking-modes=cookie` 설정을 추가했다. 테스트 37개.
> - 2026-11-25: 목록 수정에 삭제 체크칸 추가 (`PlayerService.saveAll`, 삭제할 줄은 검사 건너뜀 → 컨트롤러에서 `jakarta.validation.Validator`로 줄마다 직접 검사). 팀 나누기 참가자 선택을 카드 드래그 방식으로 바꿨다. 브라우저 기본 드래그(HTML5 DnD)는 터치에서 안 되고 자동화 시험도 불안정해서 Pointer 이벤트로 직접 구현했고, 휴대폰은 탭으로 고른다. agent-browser의 `mouse`/`drag --human` 좌표는 화면 좌표와 어긋나므로, 드래그 시험은 페이지 안에서 PointerEvent를 만들어 했다. 테스트 39개.
> - 2026-11-25: 사용자 요청으로 경기 기록이 있는 플레이어도 삭제 가능하게 바꿨다. 다른 사람의 경기 기록과 전적을 지키려고 소프트 삭제(`deletedAt`, 라이엇 ID는 `deleted:{id}:{원래}`로 바꿔 재등록 가능)로 구현했다. 테스트 41개.
> - 2026-11-25: 경기 기록도 소프트 삭제로 바꾸고, 플레이어 삭제도 전부 소프트 삭제로 통일했다. 스프링 시큐리티로 관리자(사용자 본인) 로그인과 휴지통(되돌리기)을 만들었다. 비밀번호는 `config/application.properties`(gitignore)에 무작위로 만들어 두었다. 사용자는 Gradle 새로고침(시큐리티 의존성) 후 재시작해야 한다. 테스트 47개.
> - 2026-11-26: 사용자 요청으로 팀 나누기 결과 화면의 설명 문구를 쉬운 말로 바꿨다 ("두 팀 실력 차이", "티어 구성 차이 (0에 가까울수록 공평)", "어떻게 나누나요?" 문단). 계산은 그대로다.
> - 2026-11-26: 사용자 요청("라인이 겹쳐서 부포나 아예 다른 포지션으로 가는 상황도 계산")으로 포지션 감점을 넣었다. 맡은 포지션이 부 포지션이면 전력 -1, 그 외 -2 (`Assignment.power()`). 팀 전력과 전력 차이에 반영되어, 라인이 겹치는 사람들이 두 팀에 고르게 나뉜다. 결과 화면 팀 머리에 "(라인 감점 -n)" 표시. 부포로 가는 경우, 둘 다 아닌 경우, 부포인 사람을 먼저 보내는 경우 테스트 추가. "한 팀만 부포(-1)"와 "두 팀 다 다른 라인(-2 대 -2)" 중에서는 두 팀 감점이 같은 쪽을 고르는 지금 방식을 사용자가 유지하기로 했다(테스트로 고정). 테스트 53개.
> - 2026-11-26: 사용자 요청("라인마다 본인이 생각하는 티어를 입력해서 하자")으로 라인별 티어를 추가했다. 사용자 선택: 5칸 모두 필수, 솔랭 티어·주/부 포지션은 유지. `LaneTiers`(@Embeddable record, 컬럼 top_tier ~ support_tier)를 `Player.laneTiers`에 넣고, 팀 나누기는 맡은 라인의 티어로 계산(감점 없음). 라인별 티어가 없는 옛 플레이어는 솔랭 티어 + 포지션 감점. 균형이 비슷한 조합끼리는 두 팀 전력 합이 큰 쪽을 먼저 고르게 바꿨다("35 대 35"와 "25 대 25"를 같게 보던 문제). 등록 폼에 "솔랭 티어로 빈칸 채우기" 버튼. 테스트 59개. (목록에 "라인 티어 미입력"으로 표시, 목록 수정에서 한 번에 입력 가능)
> - 2026-11-26: 사용자 요청으로 실제 DB(8081)의 플레이어 10명(`#RAND`, id 12~21, 이때 남아 있던 전부)에게 라인별 티어를 임의로 채웠다. 규칙: 주 포지션 = 솔랭 티어, 부 포지션 = 한 단계 아래, 나머지 = 1~3단계 아래 무작위(최저 아이언). 8081의 목록 수정 화면으로 저장했다.
> - 2026-11-26: 사용자 요청("솔랭 티어 말고 라인 티어로, 마스터·그마·챌은 점수를 적을 수 있게")으로 솔랭 티어 입력·표시를 없애고(`Player.tier` 필드 삭제, DB `tier` 컬럼은 남아 있지만 안 씀, `Tier.UNRANKED` 삭제), 마스터 이상 라인에 LP 칸(선택, 0~3000)을 추가했다. 점수 = 8 + LP/500 (비우면 8/9/10). 목록·카드·휴지통의 티어 배지는 "주 라인 티어"(주 포지션의 라인 티어). 라인별 티어가 없는 옛 플레이어는 참가자 주 라인 티어 평균 + 포지션 감점. 실제 DB 복사본으로 서버를 띄워 기존 데이터가 그대로 읽히는 것을 확인했다. 테스트 67개.
> - 2026-11-26: 사용자 요청("입력값을 바꿔서 직접 테스트")으로 메모리 DB 서버에서 31가지 입력을 시험했다(등록·수정·목록 수정·팀 나누기·랜덤 교체·브라우저 LP 칸·휴대폰 폭). 찾은 문제 2개를 고쳤다: (1) LP에 abc/1.5, 조작된 티어·포지션 값을 보내면 "Failed to convert property value..." 영어 오류가 그대로 보임 → `messages.properties`에 typeMismatch 한국어 문구 추가 (2) 부 포지션을 주 포지션과 같게 저장할 수 있었음 → `PlayerForm.isSubPositionDifferent` (@AssertTrue). `PlayerFormTest` 추가, 테스트 70개.
> - 2026-11-26: 랭킹 화면 추가 (`/ranking`, 헤더 메뉴 "랭킹"). 탭 3개: 승률(보정 승률, 기본)·다승·전력(주 라인 티어 + 내전 보정, `TeamBalancer.ratingBonus`를 같이 씀). 같은 기록은 같은 순위, 경기 없는 사람(전력은 라인 티어 없는 사람)은 순위 "-"로 맨 뒤. 1~3위 강조, 휴대폰에서는 열 3개 숨김. 실제 DB 복사본으로 화면 확인. 테스트 76개.
> - 2026-11-26: 사용자 요청("랜덤 팀 나누기 할 때 라인마다 밸런스를 짜고 그 사람끼리만 교체")으로 불균형 점수의 "티어 분포 차이"를 "라인 맞대결 차이"(`TeamSplit.laneDiff`, 같은 라인 두 사람의 전력 차이 합)로 바꿨다. 팀 안에서 똑같이 좋은 배치가 여럿이면 상대와 라인이 가장 잘 맞는 배치를 쓴다(`bestLineups` + `bestLaneMatch`). 랜덤 교체는 원래대로 같은 라인 두 사람끼리만. 이 변경이 예전 결정("두 팀 감점 같게 우선")과 부딪혀서 사용자에게 물었고, **라인 맞대결 우선**으로 바꿨다(테스트 `라인_맞대결을_우선해서_한_명만_부_포지션으로_보낸다`). 실제 DB 복사본에서 응답 0.06~0.09초. 테스트 78개.
> - 2026-11-26: 배포 준비 (사용자 요청: "로그인은 따로 안 하고 관리자 로그인만, 기록 변경은 관리자만, 등록은 관리자 승인"). 시큐리티를 "POST는 등록 신청만 누구나, 나머지는 관리자"로 바꾸고, 수정·경기 기록 화면도 관리자 전용. 관리자 버튼은 숨김. `Player.pending`(컬럼 `boolean default false`라 기존 플레이어는 승인된 상태로 들어감) + 승인 대기 화면(`/admin/pending`, 헤더에 수 표시, 로그인하면 이 화면으로). 승인 대기 화면의 거절 확인창을 처음에 이름을 JS에 직접 넣게 만들었다가 `th:data-confirm`으로 고쳤다. DB는 PostgreSQL 추천(무료 배포 서비스의 디스크는 재배포 때 지워지는 곳이 많음) → `prod` 프로필 + 드라이버 + `docs/deployment.md`. jar를 prod로 띄워(메모리 DB로 대신) 비밀번호 없으면 안 켜지는 것, 로그인, Secure 쿠키 확인. PostgreSQL 자체로는 아직 못 돌려 봤다(이 컴퓨터에 없음). `SecurityTest`(MockMvc) 추가, 테스트 91개. **배포할 서비스(Render, Railway 등)는 사용자와 아직 안 정했다.**
> - 2026-11-28: 사용자 요청("플레이어 등록이랑 관리자 부분을 화면 정가운데에, 디자인은 깔끔하게 흰색과 하늘색")으로 `style.css`를 흰색 + 하늘색 테마로 전면 교체했다(클래스 이름은 그대로라 다른 템플릿은 안 바꿈). 글꼴은 Pretendard. 등록·수정 폼은 760px 카드로 가운데, 로그인은 440px 카드로 화면 가로·세로 가운데, 승인 대기·휴지통은 제목을 가운데로. 승/패 배지는 VICTORY/DEFEAT 대신 "승리/패배" 글자 그대로. 메모리 DB 서버(8082)에서 데스크톱 1440px, 휴대폰 375px 화면을 찍어 확인. 테스트 91개 통과.
> **현재 위치 (2026-11-26 세션 종료):** 오늘 작업은 모두 커밋하고 GitHub(`main`)에 푸시했다. 테스트 91개 통과.
> - **다음 세션 시작 때 먼저 확인:** 사용자가 IntelliJ에서 **Gradle 새로고침(PostgreSQL 드라이버, security-test 추가) 후 8081 재시작**을 했는지. 재시작 전 8081은 예전 코드(누구나 수정 가능, 랭킹·승인 대기 없음)일 수 있다. 재시작하면 수정·삭제 버튼이 숨겨지니 "관리자"로 로그인해야 한다.
> - **멈춘 곳: 배포 방식 결정 대기.** 사용자가 "요즘 현업에서 쓰는 기술"로 배포하고 싶다고 했다. Claude 추천: Docker + GitHub Actions(CI/CD)부터 만들고, 서버는 (A) Render/Railway + Neon PostgreSQL로 먼저 띄운 뒤 익숙해지면 (B) AWS(EC2 + RDS)로 옮기기. 사용자가 A/B와 GitHub Actions 사용 여부를 아직 답하지 않았다. 정해지면: `Dockerfile` 작성(이 컴퓨터에 Docker 없음 → 설치 안내), `.github/workflows`(푸시마다 테스트, 통과하면 배포), 서비스별 단계 안내. 가입·결제·DB 생성·비밀번호 입력은 사용자가 직접 (비밀번호를 Claude에게 받지 않는다). PostgreSQL에 실제로 붙여 본 적은 아직 없다 → 첫 배포 때 확인.
> - 8081 서버에 지금 떠 있는 것은 오늘 오후 기준 코드였다 (라인 티어/LP 이후, 랭킹·승인 이전).
> - 사용자 DB(8081) 상태 (2026-11-26): 플레이어 20명 = `#RAND` 10명(id 12~21) + 사용자 요청으로 추가한 `#NEW` 10명(id 22~31, 라인별 티어 무작위). 모두 라인별 티어가 있고, 마스터 이상 라인은 LP도 채웠다. 나머지는 삭제(휴지통). 사용자 요청으로 이 20명 중 무작위 10명씩 팀 나누기 결과(일부는 랜덤 교체)로 경기 8개를 넣었다(2026-11-13 ~ 11-25, 승패는 전력 차이를 약하게 반영한 무작위). 그 전의 경기 기록은 삭제된 테스트 플레이어들 것. 테스트 플레이어는 사용자가 원하면 목록 수정에서 삭제.
> - 관리자 비밀번호는 `config/application.properties`에 있다 (사용자가 바꿨을 수 있음). 절대 커밋하지 않는다.

후보 (사용자와 정할 것):
1. 경기 결과 수정 (지금은 삭제 후 다시 기록)
2. 챔피언, KDA 기록
3. 컨트롤러 테스트 (`@WebMvcTest`, 시큐리티와 CSRF 포함)
4. 휴지통에서 영구 삭제
5. 배포 서비스 정하고 실제 배포 (준비는 끝남, `docs/deployment.md`) + Riot API 연동
