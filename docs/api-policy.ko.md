# SC4J 엔진 API 정책

[English](api-policy.md) | **한국어**

이 문서는 엔진 API가 무엇인지, 그 버전이 어떻게 바뀌는지, 엔진 버전 사이에 무엇이 호환되는지를 정합니다. 모드가 컴파일할 때 쓰는 아티팩트(`sc4j-engine`)에 적용됩니다. 모드 제작자를 위한 요약은 [모드 제작 가이드](mod-guide.ko.md#16-api와-버전)에 있습니다.

**목차**

1. [API란](#1-api란)
2. [내부 요소](#2-내부-요소)
3. [버전](#3-버전): [엔진 API 버전](#31-엔진-api-버전), [무엇이 어느 번호를 바꾸는가](#32-무엇이-어느-번호를-바꾸는가), [사용 중단](#33-사용-중단), [1.0 선언 전](#34-10-선언-전)
4. [결정성, 리플레이, 저장 게임](#4-결정성-리플레이-저장-게임)
5. [와이어 형식과 파일 형식](#5-와이어-형식과-파일-형식)
6. [목록 갱신](#6-목록-갱신)

## 1. API란

API는 아래 모듈의 main 클래스에 있는 모든 `public` 또는 `protected` 클래스, 인터페이스, 생성자, 메서드, 필드(상수 값 포함)이며, 2절의 내부 요소는 제외합니다.

| 모듈 | 내용 | 목록 |
|---|---|---|
| `sc4j-base` | 예외, 로깅, 바이너리, 파일, JSON, 텍스트 도우미 | `sc4j-base/api/sc4j-base.api` |
| `sc4j-mod-manifest` | 모드 매니페스트 `sc4j-mod.json`, 그 검증, 설정, 버전, 설치 폴더 구조 | `sc4j-mod-manifest/api/sc4j-mod-manifest.api` |
| `sc4j-engine-core` | JSON, 고정소수점 연산, 맵 격자, 지형, 플레이어, 상태 해시, 콘텐츠 타입, 모드 정체성, 리소스 모델 | `sc4j-engine-core/api/sc4j-engine-core.api` |
| `sc4j-engine-sim` | 게임, 유닛, 기본 시스템, 콘텐츠, 애니메이션 프로그램, 명령, 커맨드, 컴퓨터 플레이어, 트리거, 데이터 팩, 맵, 락스텝, 리플레이, 저장 게임 | `sc4j-engine-sim/api/sc4j-engine-sim.api` |
| `sc4j-engine-net` | 데이터그램 링크, 락스텝 전송, LAN 탐색, 네트워크 로비, Beacon.Link와 커뮤니티 인터페이스 | `sc4j-engine-net/api/sc4j-engine-net.api` |
| `sc4j-engine-client` | 호스트, 씬, 입력, 렌더러 추상화, 투영, 텍스트, HUD, 커맨드 카드, 콘솔, 대화 상자, 커서, 스킨, 월드 뷰, 오디오, 동영상, 설정 저장소 | `sc4j-engine-client/api/sc4j-engine-client.api` |
| `sc4j-engine` | 모드 API(`engine.mod`), 클라이언트 애플리케이션(`engine.client.app`), 커뮤니티 화면의 기본 부품(`engine.client.community`) | `sc4j-engine/api/sc4j-engine.api` |

모드는 `sc4j-engine`에 의존하고, 다른 모듈은 함께 따라옵니다. `sc4j-base`와 `sc4j-mod-manifest`도 목록에 있는 것은 모든 모드가 엔진을 통해 이들을 대상으로 컴파일하기 때문입니다.

`sc4j-mod-manifest`, `sc4j-engine`, `sc4j-beaconlink-protocol`, `sc4j-beaconlink`의 테스트 픽스처는 모듈과 함께 배포되며(`testImplementation testFixtures('org.fogofwarlabs.sc4j:<module>:<version>')`), 같은 방식으로 API이고 `<module>/api/<module>-test-fixtures.api`에 나열됩니다. `FakeInstallationRecords`와 `TestJarUtils`(`sc4j-mod-manifest`), `RecordingBeaconLink`와 `RecordingCommunitySession`(`sc4j-engine`), `ManualProtocolClock`(`sc4j-beaconlink-protocol`), `ClientScenario`와 루프백 NAT 에뮬레이터(`sc4j-beaconlink`)입니다. 다른 모듈의 테스트 픽스처는 배포되지 않습니다.

각 목록은 모듈의 컴파일된 클래스에 대한 JDK `javap -protected -constants` 출력입니다. 클래스마다 블록 하나이고, 블록은 클래스 이름순, 멤버도 정렬되어 있습니다. 클래스와 그 클래스를 감싸는 모든 클래스가 `public` 또는 `protected`일 때 목록에 들어갑니다. 정렬 덕분에 목록은 선언 순서와 무관하므로, 소스 파일 안에서 메서드를 옮겨도 바뀌지 않습니다. 목록에는 컴파일러가 제네릭 오버라이드를 위해 추가하는 브리지 메서드도 보입니다. 애너테이션은 보이지 않으므로, null 여부 애너테이션의 변경은 리뷰에서 잡아야 하는 계약 변경입니다(3절).

API 요소의 Javadoc은 계약의 일부입니다. 모든 API 요소에는 Javadoc이 있고, `javadoc`은 경고 없이 실행됩니다.

## 2. 내부 요소

아래 요소는 엔진 자체 모듈과 런타임을 위해 public이지만 API가 아닙니다. 목록에서 빠지고, package-info에 그렇게 적혀 있으며, 모드는 쓰면 안 됩니다.

| 요소 | 이유 |
|---|---|
| `org.fogofwarlabs.sc4j.engine.mod.loading` | 런타임의 모드 로더 |
| `org.fogofwarlabs.sc4j.engine.client.render.gl` | 렌더러의 OpenGL 기본 구현. 모드는 렌더러 추상화로 그립니다(`engine.client.render`: `GraphicsDevice.getCurrent()`가 `GpuCanvas`와 모드 자체 패스의 `ShaderProgram`을 만듭니다). |
| `org.fogofwarlabs.sc4j.engine.client.audio.openal` | 오디오 출력의 OpenAL 기본 구현. 모드는 `engine.client.audio`(`AudioDevice.openDefault()`)로 소리를 재생합니다. |
| `engine.client.host.GlfwClientHost`, `GlfwHostWindow`, `GlfwInput`, `GlfwKeyCodes`, `GlfwCursorSurface` | 런타임 호스트의 GLFW 구현. 모드는 `engine.client.host` 인터페이스(`ClientHost.initialize()`, 창 서비스를 가진 `HostWindow`, `WindowInput`)와 값 타입을 쓰며, 이들은 API입니다. |
| `engine.client.host.WindowsTextInputMethod`, `WindowsSystemPointerSpeed`, `WindowsHostTickClock` | 플랫폼 서비스의 Windows 구현. 모드는 `HostWindow.getTextInputMethod()`, `SystemPointerSpeed.getDefault()`, `HostTickClock.getDefault()`를 쓰며, 이식 가능한 기본 구현(`NoTextInputMethod`, `FixedSystemPointerSpeed`, `MonotonicHostTickClock`)은 API입니다. |

패키지는 하위 패키지를, 클래스는 중첩 클래스를 포함합니다. 빌드가 검사하고 모드 플러그인이 담는 목록은 `sc4j-engine/api/internal-elements.txt`입니다. 이 목록을 바꾸면 같은 커밋에서 이 표와 package-info도 바꿉니다.

모드 플러그인의 `checkApiUse`는 모드 소스에서 코드 안의 이 요소들을 찾아(주석과 리터럴은 세지 않음), 사용할 때마다 실패합니다. 이 저장소 밖의 모드도 플러그인 엔진 버전의 목록으로 검사합니다.

## 3. 버전

### 3.1 엔진 API 버전

엔진 API 버전은 `EngineVersion.CURRENT`(`sc4j-mod-manifest`)이며 형식은 `MAJOR.MINOR`입니다. 모드는 빌드할 때 기준으로 삼은 버전을 `sc4j-mod.json`의 `engine` 필드에 적습니다. 런처와 런타임은 major 버전이 같고 모드의 minor 버전이 런타임보다 높지 않을 때만 모드를 실행합니다.

배포 버전(`gradle.properties`의 `version`)은 별개이며 API 호환성과는 관계가 없습니다.

### 3.2 무엇이 어느 번호를 바꾸는가

| 변경 | 버전 |
|---|---|
| 목록의 줄이 삭제되거나 호환되지 않게 바뀜: 클래스, 멤버, 생성자의 삭제나 이름 변경, 매개변수, 반환, 필드 타입 변경, 접근 범위 축소(`public`에서 `protected`로), 모드 코드가 알아챌 수 있는 곳에서 `final`, `abstract`, `static`의 추가나 삭제, 모드가 구현하거나 상속하는 인터페이스나 클래스에 추상 메서드 추가, 검사 예외 추가, 상수 값 변경(컴파일러가 상수를 모드 클래스에 복사함) | major |
| 목록 변경 없이 문서화된 계약이 바뀜: 메서드가 반환하거나 받는 것, 예외를 던지는 조건, 스레드나 수명 규칙, 입력의 null 여부 애너테이션을 좁히거나 출력의 것을 넓힘 | major |
| 같은 입력에 대한 시뮬레이션 결과가 바뀜(4절) | 적어도 minor |
| 추가만 함: 새 클래스, 멤버, 생성자, default 메서드, enum 상수(모드가 그 enum을 빠짐없이 switch하지 않는 경우), 호환되게 넓힌 계약 | minor |
| 사용 중단(3.3절) | minor |
| 목록 변경도 계약 변경도 없음(구현, 성능, 내부 요소) | 없음 |

목록의 줄이 바뀌면 한 줄 삭제와 한 줄 추가로 보이므로, 리뷰는 diff 모양이 아니라 이 표로 판단합니다.

### 3.3 사용 중단

없앨 요소에는 minor 릴리스에서 `@Deprecated`와 대체 요소를 밝힌 Javadoc `@deprecated` 태그를 붙입니다. `forRemoval = true`는 그 요소를 없애는 릴리스를 표시합니다. 사용 중단된 요소는 적어도 minor 릴리스 하나 동안 남고, major 릴리스에서만 삭제됩니다.

### 3.4 1.0 선언 전

`EngineVersion.CURRENT`는 0.1입니다. 엔진 API가 0.x인 동안에는 안정성을 약속하지 않습니다. 목록은 모든 변경을 기록하고, 호환되지 않는 변경이 버전 변경 없이 일어날 수 있으며, 각 변경은 그 커밋의 `.api` 차이로 드러납니다. 3.1절의 호환 규칙은 0.x 버전에도 이미 적용됩니다. 1.0부터는 3.2절과 3.3절이 모두 적용됩니다.

## 4. 결정성, 리플레이, 저장 게임

락스텝 게임과 리플레이는 모든 컴퓨터에서 같은 입력으로 같은 시뮬레이션 결과를 내야 합니다. 그래서 시뮬레이션 결과의 변경은 API 변경이 없어도 이들에게는 호환되지 않는 변경이며, 결과를 바꾸는 엔진 릴리스는 적어도 minor 버전을 올립니다. 모드 정체성(`ModIdentity`)에는 엔진 API 버전이 들어 있으므로, 엔진 버전이 다른 피어는 로비와 방에서 서로 거부합니다.

엔진 리플레이(`.sc4r`, 매직 `SC4R`)와 엔진 저장 게임(매직 `SC4S`)은 기록한 실행의 모드 정체성을 저장하고, 같은 정체성에서만 불러옵니다. 같은 엔진 API 버전, 모드 id, 모드 버전, 내용입니다. 엔진 버전 사이의 리플레이나 저장 게임 호환은 약속하지 않습니다.

## 5. 와이어 형식과 파일 형식

형식마다 API 버전과 별개인 자체 버전 번호가 있습니다. 1.0 전에는 모든 SC4J 형식과 프로토콜 번호가 1이며, 형식은 번호를 바꾸지 않고 바뀔 수 있습니다. 이전 구조를 읽거나 옮기는 코드는 없습니다. 번호가 다른 링크의 양 끝은 서로 거부하고, 읽는 쪽은 다른 번호의 파일을 거부합니다.

| 형식 | 버전 | 위치 |
|---|---|---|
| 데이터그램 위의 락스텝 전송: 턴, 재전송, 이탈, 탈락 합의 | 1 | `DatagramLockstepTransport.PROTOCOL_VERSION`(`sc4j-engine-net`) |
| LAN 방 탐색 | 1 | `LanRoomDiscovery.PROTOCOL_VERSION`(`sc4j-engine-net`) |
| 네트워크 로비 | 1 | `NetworkLobby.PROTOCOL_VERSION`(`sc4j-engine-net`) |
| Beacon.Link 프로토콜: 엔벨로프, 그 메시지와 레코드, 채널 신호와 데이터그램 | 1 | `Envelope.PROTOCOL_VERSION`(`sc4j-beaconlink-protocol`, [프로토콜 참조](beaconlink/protocol.ko.md) 참고) |
| Beacon.Link 파일: 노드 목록, 학습한 피어, 키 파일, 계정, 친구와 무시한 플레이어 | 1 | `NodeListFile.FORMAT`, `PeerStore.FORMAT`, `IdentityKeyFileUtils.VERSION`, `AccountStore`, `PlayerListStore.FORMAT`([프로토콜 참조, "파일"](beaconlink/protocol.ko.md#파일)) |
| Beacon.Link 계정 백업(`.sc4jaccount`) | 1 | `AccountBackup.VERSION`(`sc4j-beaconlink-peer`) |
| 엔진 리플레이 | 1 | `EngineReplayFormat`(`sc4j-engine-sim`) |
| 엔진 저장 게임 | 1 | `EngineSaveFormat`(`sc4j-engine-sim`) |
| JSON 맵 | 1 | `JsonMapLoader.VERSION`(`sc4j-engine-sim`) |
| 표준 팩 매니페스트(`pack.json`) | 1 | `PackManifest`(`sc4j-engine-sim`, `engine.pack` 패키지 설명) |
| 표준 팩 콘텐츠(`content.json`) | 1 | `PackContentReader`(`sc4j-engine-sim`, `engine.pack` 패키지 설명) |
| 모드 정체성의 와이어 형식 | 로비, 탐색, 리플레이 버전을 따름 | `ModIdentity`(`sc4j-engine-core`) |

## 6. 목록 갱신

`check`(그리고 `build`)는 1절의 모든 모듈에서 `apiCheck`를, 배포되는 테스트 픽스처에서 `testFixturesApiCheck`를 실행합니다. 컴파일된 클래스가 모듈의 `.api` 파일과 다르면 실패하고, 클래스마다 삭제된 줄(`-`)과 추가된 줄(`+`)을 출력합니다.

의도한 API 변경:

1. Javadoc과 함께 변경합니다.
2. `gradlew apiDump testFixturesApiDump`(모든 목록) 또는 `gradlew :<module>:apiDump`(테스트 픽스처는 `:<module>:testFixturesApiDump`)를 실행합니다.
3. `.api` 차이를 3.2절에 비추어 검토하고, 버전에 미치는 영향을 커밋 메시지에 적습니다.
4. `.api` 파일을 변경과 함께 커밋합니다. 다른 브랜치에서 한 변경은 자기 커밋에서 파일을 갱신하고, API 변경 둘을 병합한 뒤에는 병합한 상태에서 `apiDump`를 한 번 더 실행합니다.

의도하지 않은 `apiCheck` 실패는 변경이 실수로 API를 건드렸다는 뜻입니다. 목록을 갱신하지 말고 그 부분을 되돌립니다.
