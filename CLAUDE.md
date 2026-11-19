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
- 화면 방식(Thymeleaf vs REST API + 프론트엔드)은 아직 정하지 않았다.

## 패키지 구조
기능(도메인)별 패키지. 루트 패키지는 `com.inhouse.inhouse_manager`.
```
src/main/java/com/inhouse/inhouse_manager/
 ├─ InhouseManagerApplication.java
 └─ player/
     ├─ Player.java            엔티티
     ├─ Position.java          enum (TOP, JUNGLE, MID, ADC, SUPPORT)
     └─ PlayerRepository.java  JpaRepository<Player, Long>
src/test/java/com/inhouse/inhouse_manager/
 ├─ InhouseManagerApplicationTests.java
 └─ player/
     └─ PlayerRepositoryTest.java
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
- [ ] **진행 중:** `PlayerService` (등록 시 riotId 중복 막기, 전체 조회, id로 조회) + `PlayerServiceTest`
  - 서비스 테스트는 `@DataJpaTest` + `@Import(PlayerService.class)`로 작성한다 (메모리 DB 사용, 실행 중인 앱의 파일 DB와 충돌하지 않음).
  - 의존성 주입은 `private final` 필드 + Lombok `@RequiredArgsConstructor` (생성자 주입).
  - 예외는 당분간 `IllegalArgumentException` 사용. 나중에 Controller 단계에서 예외 처리 방식을 정리할 예정.

## 다음 할 일
1. `PlayerService` 작성 → `PlayerServiceTest` 통과 → 커밋
2. 화면 방식 결정 (Thymeleaf vs REST API)
3. Controller: 플레이어 등록/목록 화면 또는 API
4. Match, MatchParticipant 엔티티 → 경기 기록 → 전적 조회 → 팀 밸런스 나누기
