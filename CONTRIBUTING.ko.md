# SC4J 기여 안내

[English](CONTRIBUTING.md) | **한국어**

개발 환경, 테스트, 코드 스타일, 커밋, 릴리스. 명령은 [README](README.ko.md#빌드와-실행)처럼 실행합니다.

## 개발 환경

- JDK 25. Gradle 9.1.0은 래퍼로 실행합니다.
- IntelliJ IDEA: 저장소 루트를 열고 Gradle 빌드를 가져온 뒤, 프로젝트 SDK와 Gradle JVM(Settings, Build Tools, Gradle)을 JDK 25로 설정합니다.

## 빌드와 테스트

| 명령 | 실행 내용 |
|---|---|
| `.\gradlew.bat build` | 컴파일, 단위 테스트(`test`), `check`: API 목록, 모듈 경계, 결정성 |
| `.\gradlew.bat :sc4j-runtime:distributionTest` | 사용자 패키지를 만들어 풀고 런타임을 헤드리스로 시작 |
| `.\gradlew.bat :sc4j-beaconlink:simulationTest` | 피어 약 천 개의 Beacon.Link 시뮬레이션(각각 몇 분) |
| `.\gradlew.bat :sc4j-engine-sim:test` | 모듈 하나 |
| `.\gradlew.bat :mods:demo:test --tests org.fogofwarlabs.sc4j.demo.DemoModTest` | 테스트 클래스 하나 |

`sc4j-engine-client`의 OpenGL, GLFW 검사는 Gradle 속성 `sc4j.render.smoke`가 `true`(`gradle.properties`의 기본값)인 동안 숨은 창을 엽니다. `-Psc4j.render.smoke=false`로 끕니다.

## 코드 스타일

주변 코드와 아래 규칙을 따릅니다.

- 공백 4칸, 탭 없음, 한 줄 150열 정도. 긴 인자 목록은 의미 단위로 나눕니다.
- `if`, `else`, `for`, `while`, `do` 본문에는 항상 중괄호.
- 빈 줄로 문장 묶음을 나누고, 연속된 단순 대입은 `=`의 열을 맞춥니다.
- 멤버 순서: 상수, static 필드, 중첩 타입, static 메서드, 인스턴스 필드, 생성자, 인스턴스 메서드.
- 파일의 줄 끝 형식을 유지합니다.
- 줄이지 않은 단어(`index`, `packetLength`). `Id`는 괜찮습니다.
- 메서드는 동사로 시작합니다: `get`, `find`, `read` / `set` / `is`, `has`, `can` / `create`, `of`, `from`, `parse`. 반대 동작은 짝을 맞춥니다(`readPacket`, `writePacket`).
- 의미 있는 수치는 `static final` 상수나 enum. 단순한 `0`, `1`, `-1`은 그대로 둡니다.
- 구현 클래스는 확장할 의도가 없으면 `final`. 도우미는 private 생성자를 가진 `final` `*Utils` 클래스. `record`는 쓰지 않고, 데이터 클래스에는 명시적 getter를 둡니다.
- 인스턴스 필드는 생성자에서 초기화하고 `this.`로 읽습니다. 지역 변수는 타입을 명시합니다(`var` 없음).
- 참조형 필드, 인자, 반환값은 모두 `@NotNull` 또는 `@Nullable`(JetBrains annotations)입니다.
- 새 예외는 `org.fogofwarlabs.sc4j.base.exception.Exceptions`에서 번호 placeholder로 만듭니다: `throw Exceptions.newIllegalStateException("Invalid frame index: {0}", index);`
- 모든 public 클래스와 public API 메서드, 생성자에는 Javadoc을 씁니다. `javadoc`은 경고 없이 실행되어야 합니다.
- Javadoc과 주석은 영어입니다. 요약은 3인칭 동사(`Creates`, `Returns`)로 시작하고, Markdown 대신 `{@code}`와 `{@link}`를 씁니다. 태그 순서는 `@param`, `@return`, `@throws`이고, 설명은 소문자로 시작해 마침표 없이 끝나며, `@throws`는 `if ...`로 씁니다.
- 주석은 쉽게 알 수 없는 코드가 하는 일을 적고, 변경의 경과는 적지 않습니다.
- 엔진 쪽 모듈은 어떤 모드도 이름으로 쓰지 않습니다. `checkModBoundary`는 SC:R 모드의 패키지나 타입 이름에서 실패합니다.
- 시뮬레이션 코드는 결정적입니다([모드 제작 가이드 12장](docs/mod-guide.ko.md#12-결정성)).
- 엔진 API를 바꾸면 목록을 갱신합니다([API 정책](docs/api-policy.ko.md#6-목록-갱신)).
- 테스트 클래스는 `<Subject>Test`, 테스트 메서드는 확인 내용을 말하는 `snake_case` 이름입니다.

## 커밋과 풀 리퀘스트

- 커밋 하나에 주제 하나. 제목은 식별자를 백틱으로 감싼 짧은 영어 명령문입니다(``Fix `ReplayReader` for empty files``). 본문이 필요하면 바뀐 부분을 `* ` 줄로 적습니다.
- 버전 변경 커밋은 ``Bump version to `X.Y.Z` ``입니다.
- 풀 리퀘스트는 `develop`으로 보내며 squash로 병합합니다. 풀 리퀘스트 제목이 커밋 제목이 됩니다.
- 풀 리퀘스트 전에 `.\gradlew.bat build`가 통과해야 하며, 실행한 테스트를 적습니다.
- API 변경 커밋에는 다시 생성한 `.api` 파일을 넣고 버전 영향을 커밋 메시지에 적습니다.
- 동작 변경에는 테스트를 함께 넣습니다. 빌드 결과물, 로컬 경로, 게임 데이터는 넣지 않습니다.
- 새 에셋은 모드에 담아 재배포할 수 있는 라이선스여야 합니다. 라이선스 파일과 고지문을 옆에 두고 [NOTICE](NOTICE)에 항목을 추가합니다.
- CI는 `main`이나 `develop`으로 들어오는 모든 풀 리퀘스트에서 `test`를 실행하며, 병합 전에 `test` 검사가 통과해야 합니다.

## 릴리스

`develop`은 `X.Y.Z-SNAPSHOT`을 가지며 Central Portal에 스냅샷을 게시합니다. 릴리스는 `develop`에서 `gradle.properties`에 `version=X.Y.Z`를 정한 뒤, `develop`에서 `main`으로 ``Release version `X.Y.Z` `` 제목의 풀 리퀘스트를 열어 merge commit으로 병합합니다. `main`에 푸시할 때마다 Maven Central에 게시하고, `SC4J-X.Y.Z.zip`과 `SC4J-X.Y.Z.zip.sha256`이 붙은 태그와 GitHub 릴리스 `vX.Y.Z`를 만듭니다.

## 기여물의 라이선스

따로 밝히지 않으면 기여물은 [Apache License 2.0](LICENSE)으로 라이선스됩니다(5절).
