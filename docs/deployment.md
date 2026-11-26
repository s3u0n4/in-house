# 배포 안내

친구들이 링크로 들어와서 보게 하려면 이 앱을 인터넷 서버에 올려야 한다. 이 문서는 "무엇을 준비해야 하는지"를 정리한다.

## 1. 데이터베이스: PostgreSQL을 쓴다

지금 개발할 때는 **H2 파일 DB**(`./data/inhouse.mv.db`)를 쓴다. 내 컴퓨터에서는 편하지만, 배포할 때는 맞지 않는다.

| | H2 파일 DB | PostgreSQL |
|---|---|---|
| 데이터가 있는 곳 | 서버 디스크의 파일 하나 | 따로 있는 DB 서버 |
| 무료 배포 서비스에서 | 다시 배포하거나 서버가 재시작되면 **디스크가 초기화되어 데이터가 사라지는 곳이 많다** | 앱 서버와 따로라서 앱을 다시 올려도 데이터가 남는다 |
| 백업 | 직접 파일 복사 | 대부분의 서비스가 자동 백업 |
| 쓰는 곳 | 연습, 테스트 | 실제 서비스에서 가장 흔히 쓰는 DB 중 하나 |

그래서 **배포 서버에서는 PostgreSQL**, 내 컴퓨터에서는 지금처럼 H2를 쓴다. 코드는 그대로이고, 설정만 `prod` 프로필로 바꾼다 (JPA가 DB 차이를 감춰 준다).

무료로 PostgreSQL을 주는 곳 (가입 후 DB를 만들면 접속 주소를 준다):
- **Neon** (neon.tech), **Supabase** (supabase.com): DB만 무료로 준다
- **Render**, **Railway**: 앱 서버와 DB를 같이 만들 수 있다 (무료 범위와 기간은 서비스마다 다르고 자주 바뀌니 가입할 때 확인)

> 지금 내 컴퓨터 H2에 있는 데이터(테스트 플레이어, 테스트 경기)는 새 DB로 옮겨지지 않는다. 배포 서버는 빈 DB에서 시작하고, 테이블은 서버가 처음 켜질 때 자동으로 만든다 (`ddl-auto=update`).

## 2. 배포 서버에 넣을 환경 변수

비밀 값은 GitHub에 올리면 안 된다 (저장소가 public). 배포 서비스의 "Environment Variables" 화면에 넣는다.

| 이름 | 값 | 예 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | `prod` |
| `SPRING_DATASOURCE_URL` | PostgreSQL 주소 (`jdbc:postgresql://`로 시작) | `jdbc:postgresql://ep-xxx.neon.tech:5432/inhouse?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자 | |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | |
| `INHOUSE_ADMIN_USERNAME` | 관리자 아이디 | |
| `INHOUSE_ADMIN_PASSWORD` | 관리자 비밀번호 (길게) | |

- DB 서비스가 `postgres://user:pass@host/db` 모양의 주소를 주면, 앞을 `jdbc:postgresql://host:5432/db`로 바꾸고 사용자와 비밀번호는 따로 넣는다.
- `INHOUSE_ADMIN_PASSWORD`가 없으면 서버가 켜지지 않는다 (임시 비밀번호로 열리는 것을 막으려고 일부러 그렇게 했다).
- 포트는 배포 서비스가 주는 `PORT`를 자동으로 쓴다.

## 3. prod 프로필이 바꾸는 것 (`src/main/resources/application-prod.properties`)

- DB 드라이버를 PostgreSQL로
- H2 콘솔 끄기, SQL 로그 끄기
- 세션 쿠키에 `Secure` (HTTPS에서만 보냄), 프록시 뒤의 https 주소 인식

## 4. 빌드와 실행

```
./gradlew bootJar
java -jar build/libs/inhouse-manager-0.0.1-SNAPSHOT.jar
```
배포 서비스에 따라 이 명령을 "Build Command"/"Start Command"에 넣거나, Dockerfile을 쓴다. 어느 서비스를 쓸지 정하면 그에 맞게 설정 파일을 추가한다.

## 5. 배포 뒤 권한

- 누구나: 플레이어·경기 기록·랭킹 보기, 팀 나누기, 플레이어 **등록 신청**
- 관리자만: 등록 신청 승인/거절, 플레이어 수정·삭제, 경기 기록·삭제, 휴지통
