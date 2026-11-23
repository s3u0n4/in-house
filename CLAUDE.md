# inhouse-manager — 롤 내전 관리 웹

League of Legends 내전(커스텀 게임) 참가자, 팀 나누기, 경기 결과와 전적을 관리하는 웹 애플리케이션.
사용자가 Spring을 처음 배우면서 만드는 첫 프로젝트다.

## 작업 규칙 (Claude가 반드시 지킬 것)
- **코드는 사용자가 직접 작성한다.** Claude는 방법과 예시 코드를 답변으로 설명만 하고, 소스 파일을 직접 만들거나 수정하지 않는다. (읽기, 확인은 괜찮다.)
- 예외: 이 `CLAUDE.md`는 Claude가 관리한다. **사용자가 코드를 작성하거나 단계를 진행할 때마다 아래 "진행 상황"과 "다음 할 일"을 최신으로 갱신한다.**
- 사용자는 Spring 입문자다. 개념을 쉽게, 한 단계씩, 어노테이션과 코드 한 줄 한 줄의 의미와 확인 방법까지 한국어로 설명한다.
- "어떻게 해?"는 설명 요청이지 대신 해달라는 요청이 아니다.

## 기술 스택
- Java 21, Spring Boot 4.1.2, Gradle
- Spring Web MVC, Spring Data JPA, Validation, Lombok
- H2 파일 DB: `./data/inhouse` (`data/`는 gitignore 대상)
- 서버 포트 8081, H2 콘솔 `http://localhost:8081/h2-console` (JDBC URL `jdbc:h2:file:./data/inhouse`, user `sa`, 비밀번호 없음)
- Spring Boot 4라서 예전 자료와 import 경로가 다른 것이 있다. 예: `@DataJpaTest` → `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`
- 화면 방식: **Thymeleaf** (2026-11-20 결정). 서버가 HTML을 만들어 보내는 방식이며, 템플릿은 `src/main/resources/templates/`에 둔다. 나중에 필요하면 `@RestController`를 추가해 API를 붙일 수 있다.

## 패키지 구조
기능(도메인)별 패키지. 루트 패키지는 `com.inhouse.inhouse_manager`.
```
src/main/java/com/inhouse/inhouse_manager/
 ├─ InhouseManagerApplication.java
 └─ player/
     ├─ Player.java            엔티티
     ├─ Position.java          enum (TOP, JUNGLE, MID, ADC, SUPPORT)
     ├─ PlayerRepository.java  JpaRepository<Player, Long>
     └─ PlayerService.java     등록/조회 규칙
src/test/java/com/inhouse/inhouse_manager/
 ├─ InhouseManagerApplicationTests.java
 └─ player/
     ├─ PlayerRepositoryTest.java
     └─ PlayerServiceTest.java
```

## 도메인 설계 (계획)
| 엔티티 | 의미 | 주요 필드 |
|---|---|---|
| Player | 내전 참가자 | name, riotId(unique), mainPosition, subPosition |
| Match | 내전 한 판 | 경기 날짜, 승리 팀(BLUE/RED) |
| MatchParticipant | 경기-플레이어 연결 (다대다 중간 테이블) | match, player, 팀, 포지션, (선택) 챔피언과 KDA |

엔티티 작성 관례: `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`, `@Setter`/`@Data` 사용 안 함, enum 필드에는 `@Enumerated(EnumType.STRING)`.

## 진행 상황
- [x] 프로젝트 생성, 중복 테스트 파일(`com.inhouse.manager` 패키지) 삭제
- [x] H2 파일 DB와 JPA 설정, H2 콘솔 접속 확인
- [x] `Position` enum, `Player` 엔티티 작성 → `PLAYER` 테이블 생성 확인 (커밋: "Player 엔티티 추가")
- [x] `PlayerRepository` 작성 (`findByRiotId`, `existsByRiotId`)
- [x] `PlayerRepositoryTest` (`@DataJpaTest`, 테스트 2개: 저장 후 riotId 조회 / 없는 riotId는 false) — 2026-11-19 통과 확인
  - 테스트 메서드 이름은 한글로 쓰는 방식을 사용 중 (main 코드 이름은 영어)
  - 커밋: "PlayerRepository 및 테스트 추가"
- [x] `PlayerService` (`register`: riotId 중복이면 예외 / `findAll` / `findById`: 없으면 예외) + `PlayerServiceTest` 3개
  - 2026-11-20 전체 테스트 6개 통과 확인, 커밋: "PlayerService 추가"
  - 참고: `PlayerServiceTest`의 static import가 `AssertionsForClassTypes`로 되어 있다. 지금은 동작하지만 리스트 검증 등에는 `org.assertj.core.api.Assertions`가 더 맞으니 다음에 테스트를 추가할 때 바꾸도록 안내할 것.
  - 서비스 테스트는 `@DataJpaTest` + `@Import(PlayerService.class)`로 작성한다 (메모리 DB 사용, 실행 중인 앱의 파일 DB와 충돌하지 않음).
  - 의존성 주입은 `private final` 필드 + Lombok `@RequiredArgsConstructor` (생성자 주입).
  - 예외는 당분간 `IllegalArgumentException` 사용. 나중에 Controller 단계에서 예외 처리 방식을 정리할 예정.

- [x] 화면 방식 결정: Thymeleaf (2026-11-20). 입문자라 Java+HTML만으로 결과를 바로 볼 수 있는 쪽을 골랐다.
- [x] Thymeleaf 의존성 추가, `PlayerController`(`@Controller`, `GET /players`) + `templates/players.html` 목록 화면 작성 (2026-11-23). 브라우저에서 빈 목록 안내 문구가 나오는 것을 확인했다.

## 다음 할 일
> **현재 위치 (2026-11-23):** `spring-boot-starter-thymeleaf` 의존성 추가를 확인했다 (Gradle에서 thymeleaf 3.1.5가 받아짐). 이 줄이 test 의존성들 사이에 있어서, `implementation` 줄들 쪽으로 옮기라고 권했다 (동작에는 영향 없음). `PlayerController`(`GET /players`)와 `templates/players.html`(th:each 표, 빈 목록 안내)을 작성했다. `/players`가 200을 응답하고 "등록된 플레이어가 없습니다."가 나오는 것을 확인했다. 아직 커밋하지 않았다. 다음은 H2 콘솔에서 데이터를 넣어 표를 확인하고, 커밋한 뒤 2번(등록 폼)으로 가는 것이다.

1. Thymeleaf 의존성 추가 (`spring-boot-starter-thymeleaf`) → `PlayerController` (`@Controller`, `GET /players`) + `templates/players.html` 목록 화면 → 브라우저에서 확인
2. 플레이어 등록 폼 (`GET /players/new` 폼 화면, `POST /players` 저장 후 리다이렉트), 폼 데이터 객체와 `@Valid` 검증, riotId 중복 예외를 화면에 표시하는 방법
3. Match, MatchParticipant 엔티티 → 경기 기록 → 전적 조회 → 팀 밸런스 나누기
