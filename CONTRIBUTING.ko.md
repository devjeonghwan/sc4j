# SC4J 기여 안내

[English](CONTRIBUTING.md) | **한국어**

도와주셔서 고맙습니다. 이 문서는 개발 환경, 테스트 단계, 코드 스타일, 커밋과 풀 리퀘스트에 바라는 점을 정리합니다. 프로젝트 소개는 [README](README.ko.md)에 있습니다.

- [개발 환경](#개발-환경)
- [빌드와 테스트](#빌드와-테스트)
- [코드 스타일](#코드-스타일)
- [커밋과 풀 리퀘스트](#커밋과-풀-리퀘스트)
- [릴리스](#릴리스)
- [기여물의 라이선스](#기여물의-라이선스)

## 개발 환경

| 도구 | 버전 |
|---|---|
| JDK | 25 |
| Gradle | 9.1.0, 래퍼(`gradlew`, `gradlew.bat`)로 실행하므로 따로 설치할 필요 없음 |
| IDE | 자유. IntelliJ IDEA는 Gradle 가져오기로 동작 |

IntelliJ IDEA: 저장소 루트 폴더를 열고 Gradle 프로젝트로 가져옵니다. 프로젝트 SDK와 Gradle JVM(Settings, Build Tools, Gradle)을 JDK 25로 설정합니다.

아래 명령은 저장소 루트의 PowerShell에서 실행합니다. Linux에서는 `./gradlew`를 씁니다.

## 빌드와 테스트

테스트는 단계로 나뉩니다. 첫 단계는 저장소 밖의 것이 필요 없고, 나머지 단계는 플레이어 본인의 스타크래프트: 리마스터 설치본이나 다른 외부 폴더가 필요합니다.

| 명령 | 실행 내용 | 필요한 것 |
|---|---|---|
| `.\gradlew.bat build` | 컴파일, 단위 테스트(`test`), `check`의 검사: API 목록, 모듈 경계, 결정성 | 저장소 밖의 것 없음 |
| `.\gradlew.bat integrationTest` | `integration` 태그 테스트(`@RequiresInstallation`), GPU 렌더 스모크 테스트 포함 | `sc4j.data`, 스모크 테스트에는 GPU |
| `.\gradlew.bat corpusTest` | `corpus` 태그 테스트(`@CorpusTest`): 오래 걸리는 실행과 외부 리플레이, 맵 폴더 테스트 | `sc4j.data`, 폴더 속성이 없는 테스트는 건너뜀 |
| `.\gradlew.bat :sc4j-runtime:distributionTest` | 사용자 패키지를 만들어 풀고 런타임을 헤드리스로 시작 | 저장소 밖의 것 없음 |

`integrationTest`는 `sc4j.data`가 없거나 폴더가 아니면 시작하기 전에 실패합니다. GPU 렌더 스모크 테스트는 Gradle 속성 `sc4j.render.smoke`가 `true`일 때 실행되며(`gradle.properties`의 기본값), `-Psc4j.render.smoke=false`로 끕니다.

모듈 하나나 테스트 하나만 실행하기:

```powershell
.\gradlew.bat :sc4j-engine-sim:test
.\gradlew.bat :mods:scr-demo:test --tests org.fogofwarlabs.sc4j.scrdemo.convert.ArtConverterTest
```

## 코드 스타일

먼저 바꾸는 곳 주변의 코드를 따릅니다. 아래는 코드 전체에서 지키는 규칙입니다.

**배치**

- 공백 4칸으로 들여쓰고 탭은 쓰지 않습니다. 한 줄은 150열 정도까지이며, 긴 인자 목록은 길이가 아니라 의미 단위로 나눕니다.
- `if`, `else`, `for`, `while`, `do` 본문에는 한 문장이라도 항상 중괄호를 씁니다.
- 빈 줄로 의미가 다른 문장 묶음을 나눕니다. 연속된 단순 대입은 `=`의 열을 맞춥니다.
- 클래스 멤버 순서: 상수, static 필드, 중첩 타입, static 메서드, 인스턴스 필드, 생성자, 인스턴스 메서드.
- 파일의 기존 줄 끝 형식을 유지합니다.

**이름과 타입**

- 줄이지 않은 단어를 씁니다(`index`, `image`, `packetLength`). `Id`는 괜찮습니다.
- 메서드는 동사로 시작합니다. 조회는 `get`, `find`, `read`, 변경은 `set`, 확인은 `is`, `has`, `can`, 생성은 `create`, `of`, `from`, `parse`. 반대 동작은 짝이 맞는 이름을 씁니다(`readPacket`과 `writePacket`).
- 의미 있는 수치는 이름 있는 `static final` 상수나 enum으로 둡니다: 크기, id, 플래그, 오프셋, 시간. 단순한 `0`, `1`, `-1`은 그대로 써도 됩니다.
- 구현 클래스는 확장할 의도가 없으면 `final`입니다. 도우미 클래스는 `*Utils`라는 이름에 `final`, private 생성자를 가집니다. `record`는 쓰지 않고, 데이터 객체는 명시적 getter를 가진 일반 클래스로 만듭니다.
- 인스턴스 필드는 생성자에서 초기화하고 `this.`로 접근합니다. 지역 변수는 타입을 명시합니다(`var` 없음).
- 참조형 필드, 인자, 반환값에는 모두 `@NotNull` 또는 `@Nullable`(JetBrains annotations)을 붙입니다.

**예외**

새 예외는 `org.fogofwarlabs.sc4j.base.exception.Exceptions`의 factory 메서드로 만들고, 문자열을 이어 붙이는 대신 번호 placeholder를 씁니다.

```java
throw Exceptions.newIllegalStateException("Invalid frame index: {0}", index);
```

**Javadoc과 주석**

- 모든 public 클래스와 public API 메서드, 생성자에는 Javadoc을 씁니다. `javadoc`은 경고 없이 실행되어야 합니다.
- Javadoc과 주석은 영어로 쓰고, 요약은 3인칭 동사(`Creates`, `Reads`, `Returns`)로 시작합니다. Markdown 대신 `{@code}`와 `{@link}`를 씁니다.
- 태그 순서는 `@param`, `@return`, `@throws`입니다. 설명은 소문자로 시작하고 마침표로 끝내지 않으며, `@throws`는 `if ...`로 씁니다.
- 주석은 코드가 하는 일과 이유를 설명합니다. 변경의 경과를 적지 않습니다.

**엔진 규칙**

- 엔진 쪽 모듈은 어떤 모드도 알지 않습니다. SC:R 모드에 의존하거나 이름을 쓰면 빌드가 실패합니다(`checkModBoundary`).
- 시뮬레이션 코드는 결정적이어야 합니다. 시계, `java.util` 난수 생성기, 시뮬레이션이 순서를 보는 해시 순서 컬렉션, 부동소수점을 쓰지 않습니다([모드 제작 가이드 12장](docs/mod-guide.ko.md#12-결정성)).
- 엔진 API를 바꾸면 API 목록도 갱신합니다. [엔진 API 정책](docs/api-policy.ko.md#6-목록-갱신)을 봅니다.

**테스트**

테스트 클래스 이름은 `<Subject>Test`이고, 테스트 메서드는 확인하는 내용을 말하는 `snake_case` 이름을 씁니다.

## 커밋과 풀 리퀘스트

- 커밋 하나에는 주제 하나만 담습니다. 첫 줄은 짧은 영어 요약이고, 본문에는 무엇을 왜 바꿨는지 씁니다.
- 풀 리퀘스트 전에 `.\gradlew.bat build`가 통과해야 합니다. SC:R 모드를 건드렸고 설치본이 있으면 `integrationTest`도 실행합니다. 실행한 테스트 단계를 풀 리퀘스트에 적습니다.
- API를 바꾼 커밋에는 다시 생성한 `.api` 파일을 함께 넣고, 버전에 미치는 영향을 커밋 메시지에 적습니다.
- 동작을 바꾸면 테스트도 함께 넣습니다. 빌드 결과물, 로컬 경로, 게임 데이터는 커밋하지 않습니다.
- 새 그림, 소리 같은 에셋은 모드에 담아 재배포할 수 있는 라이선스여야 합니다. 라이선스 파일과 고지문을 에셋 옆에 두고 [NOTICE](NOTICE)에 항목을 추가합니다.
- CI 워크플로가 `main`이나 `develop`으로 들어오는 모든 풀 리퀘스트의 단위 테스트(`test`)를 실행합니다. 병합하려면 `test` 검사를 통과해야 합니다.

## 릴리스

`develop`은 다음 버전을 `X.Y.Z-SNAPSHOT`으로 가지며 Central Portal에 스냅샷을 게시합니다. `main`은 릴리스 버전을 가지며 푸시마다 Maven Central에 게시하고 `vX.Y.Z` 태그를 답니다.

릴리스는 `gradle.properties`의 `version=X.Y.Z`를 정하는 `develop`에서 `main`으로의 풀 리퀘스트입니다.

## 기여물의 라이선스

SC4J는 [Apache License 2.0](LICENSE)으로 배포됩니다. 따로 밝히지 않으면 포함을 위해 제출한 모든 기여물은 같은 조건으로 라이선스됩니다(License 5절).
