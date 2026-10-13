# CLAUDE.md

이 파일은 이 저장소에서 작업하는 Claude Code(claude.ai/code)를 위한 안내 문서입니다.

## 언어 규칙 (항상 적용)

1. 사용자에게 하는 모든 응답은 한국어로 작성한다.
2. 새로 생성하는 모든 문서는 한국어로 작성한다.

## 개요

`kr.andold:utils`는 작성자의 다른 프로젝트(예: household-helper)에서 공통으로 사용하는 Java 21 유틸리티 라이브러리입니다(Spring 없는 순수 Maven jar). GitHub Packages(`https://maven.pkg.github.com/andold/utils`)에 배포되므로 public 메서드 시그니처는 사실상 하위 프로젝트가 쓰는 API입니다. 기존 시그니처를 바꾸기보다 overload를 추가하는 방식을 우선합니다.

## 명령어

```bash
mvn compile                                  # 빌드
mvn test                                     # 전체 테스트 실행 (JUnit 5)
mvn test -Dtest=UtilityTest                  # 단일 테스트 클래스
mvn test -Dtest=UtilityTest#testToStringJsonObject   # 단일 테스트 메서드
mvn install                                  # 사용하는 프로젝트에서 시험하기 위해 로컬 ~/.m2에 설치
mvn deploy                                   # GitHub Packages에 배포 (deploy.sh / deploy.bat)
```

- 배포하려면 `~/.m2/settings.xml`에 **personal access token (classic)** 을 사용하는 `github` server 항목이 있어야 합니다. GitHub Packages는 fine-grained 토큰을 받지 않습니다.
- `pom.xml`에 surefire 플러그인 버전이 지정되어 있지 않습니다. `mvn test`에서 실행된 테스트가 0개로 나오면 기본 surefire 버전이 JUnit 5를 지원하지 못할 만큼 오래된 것입니다.
- 코드 전반에 Lombok(`@Slf4j`, `@Data`, `@Builder`, `@Getter`)을 사용하며, annotation processor는 `maven-compiler-plugin`에 설정되어 있습니다.

## 릴리스 절차 (git 이력 기준)

1. GitHub 이슈 "Release Vx.y.z"를 만들고 `<이슈번호>-release-vxyz` 이름으로 브랜치를 만든다.
2. `pom.xml` 버전을 `x.y.z-SNAPSHOT`에서 `x.y.z`로 바꾸고 `Release Vx.y.z #<이슈번호>` 메시지로 커밋한 뒤, 배포하고 PR을 머지한다.
3. 다음 `-SNAPSHOT` 버전으로 올리고 `새로운 버전 시작 - After Release Vx.y.z #<이슈번호>` 메시지로 커밋한다.

## 아키텍처

- **`Utility`**: 규모가 큰 static 헬퍼 클래스입니다. 날짜·숫자 파싱, Jackson 기반 JSON(JSON lines 포함), POI를 이용한 Excel 텍스트 추출, 리플렉션 기반 `copyPropertiesNotNull`/`copyPropertiesTargetNull`, 문자열 ellipsis·escape, null에 안전한 `size`/`compare`/`min`/`max` 등을 제공합니다. 새 헬퍼를 만들기 전에 여기에 이미 있는지 확인합니다.
- **`ChromeDriverWrapper`**: Selenium `ChromeDriver`를 상속하고 wait/find/getText/click 계열 overload를 많이 추가한 클래스입니다. 예전 방식인 `int milli` 타임아웃과 새 방식인 `Duration` 타임아웃 overload가 함께 있으며, 새 코드는 `Duration`을 씁니다. 최근에는 DOM *attribute* 대신 DOM *property*(`getDomProperty`, `domPropertyToBe`)를 쓰는 쪽으로 옮겨 가고 있습니다. `Navigate`(builder)는 페이지 이동 한 단계를 기술합니다. 도착 확인용 identifier, 이동 수단(link/url/mouseHover), 먼저 거쳐야 하는 페이지(`expected`, 재귀적으로 이동)를 담고 `navigate(Navigate)`에서 사용합니다.
- **`ZookeeperClient`**: 리더 선출을 담당합니다. 각 인스턴스가 선출 경로 아래에 `EPHEMERAL_SEQUENTIAL` znode(`c_` 접두어)를 만들고, 가장 작은 자식 노드가 master(`isMaster()`)가 됩니다. 세션이 만료되면 연결을 닫고 다시 실행합니다.
- **`job` 패키지**: `JobService`는 static 우선순위 큐 4개(`queue0`이 가장 높고 … `queue3`)를 비웁니다. `run()` 한 번에 `JobInterface`(`Callable<STATUS>`) 하나를 단일 스레드 executor에서 `getTimeout()`초 제한으로 실행합니다. `STATUS` enum에는 한국어 라벨이 붙어 있습니다.
- **`persist` 패키지**: `CrudService<X 파라미터, Y 도메인, Z 엔티티>` 인터페이스의 default 메서드가 key 기준 동기화를 구현합니다. `differ(befores, afters)`는 `key()`와 `compare()`로 생성/중복/수정/삭제(`CrudList`)로 나누고, `upload()`/`download()`는 JSON lines를 다루며, `dedup()`은 key가 같은 도메인을 병합합니다. 구현체는 기본 CRUD·변환 메서드만 제공하면 됩니다.

## 컨벤션

- 로그 형식: `log.x("{} method(『{}』) - ...", Utility.indentStart()/indentMiddle()/indentEnd(), ...)`. 각 메서드의 시작과 끝을 표시하고 값은 `『』`로 감싸며, 경과 시간은 주로 `Utility.toStringPastTimeReadable(started)`로 남깁니다. 새 메서드도 이 형식을 따릅니다.
- 들여쓰기는 탭을 사용합니다. 커밋 메시지와 주석은 한국어로 쓰는 경우가 많습니다.
