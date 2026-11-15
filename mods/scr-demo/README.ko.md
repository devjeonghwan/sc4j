# SC:R Clone Demo

[English](README.md) | **한국어**

SC4J의 데모 2입니다. 자기 정체성을 가진 [SC:R 모드](../scr/README.ko.md)의 복제 모드로, 모드 id는 `scr-demo`, jar는 `scr-demo.jar`입니다. 자체 LAN 참가 버전, 바꾼 Terran Marine 값, 자체 그래픽을 가진 추가 유닛 둘이 있습니다. Barracks에서 훈련하는 Terran Unity-chan과, Hive가 변태하는 용인 Zerg Myriarch입니다.

`scr.jar`처럼 jar에는 스타크래프트: 리마스터의 에셋이나 데이터 파일이 없습니다. 게임의 그림, 소리, 데이터는 실행할 때 플레이어 본인의 설치본에서 읽습니다.

[SC:R 복제 모드 가이드](docs/scr-demo.ko.md)가 이 데모를 예시로 모든 단계를 설명합니다.

- [SC:R 모드와의 관계](#scr-모드와의-관계)
- [구성](#구성)
- [복제와 오버레이](#복제와-오버레이)
- [빌드와 실행](#빌드와-실행)
- [그림 라이선스](#그림-라이선스)
- [라이선스](#라이선스)

## SC:R 모드와의 관계

복제본은 저장소에 넣지 않습니다. 빌드가 SC:R 모드([`../scr/`](../scr/README.ko.md), 그 `mod.properties`의 `modules` 목록)의 모든 모듈에서 `src/main`을 복사하고 그 위에 오버레이를 덮어씁니다. 바꾸지 않은 SC:R 소스는 SC:R 모드의 컴파일러 인자로 컴파일되므로 클래스가 `scr.jar`와 같습니다.

복제 모드는 다른 모드입니다. 설정, 저장 폴더, 로그 폴더가 따로입니다. 온라인에서는 SC:R 모드의 방을 보여 주지 않고, LAN에서는 참가 버전으로 SC:R 모드 플레이어를 거부하며 그쪽도 이 모드를 거부합니다. 두 모드는 스타크래프트: 리마스터 폴더 설정 하나를 함께 씁니다.

## 구성

| 경로 | 내용 |
|---|---|
| `overlay/` | 복제한 소스를 대신하거나 더하는 파일. 구조는 `overlay/<module>/src/main/...` |
| `overlay.lock`, `overlay-base/` | 오버레이 파일을 만든 SC:R 모드 파일 각각의 SHA-256과 복사본. `acceptOverlay`가 씀 |
| `src/clone` | 복제 도구. 빌드할 때만 씀 |
| `src/convert` | 그림 변환기. 빌드할 때만 씀 |
| `src/mod` | 데모 자체의 클래스와 리소스: 콘텐츠 확장, 값, 추가 유닛 |
| `src/test` | 테스트. 빌드한 jar를 읽음 |
| `third-party/` | 추가 유닛의 그림 세트와 라이선스 파일 |
| `docs/` | SC:R 복제 모드 가이드 |

## 복제와 오버레이

| 작업 | 하는 일 |
|---|---|
| `cloneScrMod` | `overlay.lock`을 확인하고, SC:R 모드 소스를 `build/clone`에 복사한 뒤 `overlay/`를 덮어씀 |
| `overlayDiff` | 오버레이 파일 아래에서 SC:R 모드가 바꾼 내용을 보여 줌 |
| `acceptOverlay` | 현재 SC:R 모드 파일을 `overlay.lock`과 `overlay-base/`에 기록 |
| `convertArt` | 모든 그림 세트의 베이크된 프레임을 유닛의 그래픽 파일로 변환 |
| `jar` | `build/libs/scr-demo.jar` 빌드 |

오버레이가 대신하는 파일을 SC:R 모드가 바꾸면 빌드가 멈추고 그 파일을 알려 줍니다. `overlayDiff`를 실행하고, 바뀐 내용을 오버레이 파일에 손으로 병합한 뒤 `acceptOverlay`를 실행합니다.

오버레이는 작게 유지합니다. 작은 리소스를 대신하는 것을 먼저, 다음으로 SC:R 모드의 확장 지점 뒤에 둔 자기 클래스를, 마지막으로만 SC:R 모드 소스를 대신합니다. 데모 2는 리소스 둘(`sc4j-mod.json`, `lan-lobby.properties`)을 대신하고 콘텐츠 확장 하나를 추가합니다.

## 빌드와 실행

JDK 25가 필요합니다. 플레이하려면 스타크래프트: 리마스터 1.23.10 설치본이 필요하지만, 빌드에는 필요하지 않습니다. 아래 명령은 저장소 루트의 PowerShell에서 실행합니다. Linux에서는 `./gradlew`를 씁니다.

| 명령 | 하는 일 |
|---|---|
| `.\gradlew.bat -p mods\scr-demo jar` | `mods/scr-demo/build/libs/scr-demo.jar` 빌드 |
| `.\gradlew.bat -p mods\scr-demo test` | 저장소 밖의 것이 필요 없는 테스트 |
| `.\gradlew.bat -p mods\scr-demo integrationTest` | 설치본이 필요한 테스트(Gradle 속성 `sc4j.data`) |
| `.\gradlew.bat -p mods\scr-demo devInstall` | 개발용 설치본 `build/dev-install/`에 `mods/scr.jar`와 `mods/scr-demo.jar` 추가 |
| `.\gradlew.bat runDevLauncher` | 개발용 설치본의 런처 열기 |

다른 설치본에는 `scr-demo.jar`를 그 `mods/` 폴더에 복사합니다.

## 그림 라이선스

| 자료 | 라이선스 | 자세히 |
|---|---|---|
| 유닛 그림: Unity-chan | Unity-chan License 3.0 | [`third-party/unity-chan/`](third-party/unity-chan/) |
| 유닛 그림: Unity-chan이 든 소총(Quaternius) | CC0 1.0 | [`third-party/unity-chan/`](third-party/unity-chan/) |
| 유닛 그림: Myriarch(OSCAR CREATIVO의 DRAGO 3D 모델) | CC BY 4.0 | [`third-party/myriarch/`](third-party/myriarch/) |

빌드가 그림 세트마다 `NOTICE.txt`와 `license/`를 jar의 `third-party/<set>/` 아래에 묶고, 매니페스트의 `license` 줄이 표기를 밝힙니다. 이 프레임이 든 jar를 공개할 때는 이를 유지합니다.

## 라이선스

SC:R Clone Demo는 Apache License 2.0으로 배포됩니다. 그림 세트는 위의 각 라이선스를 따릅니다.

StarCraft는 Blizzard Entertainment, Inc.의 상표입니다. SC4J는 Blizzard Entertainment와 제휴하거나 승인받지 않았습니다. SC:R Clone Demo는 플레이어 본인의 스타크래프트: 리마스터 설치본에서 게임 데이터를 읽으며, 저장소와 배포본에는 그런 데이터가 들어 있지 않습니다.
