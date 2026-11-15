# SC4J 모드 제작 가이드

[English](mod-guide.md) | **한국어**

이 가이드는 SC4J 엔진용 게임, 즉 모드를 만드는 방법을 설명합니다. 2장에서는 빌드, 설치, 실행되는 첫 모드를 처음부터 만듭니다. 그 뒤의 장들은 모드의 각 부분을 설명하고, 그 부분을 구현한 데모의 클래스를 알려 줍니다. 데모([`mods/demo/`](../mods/demo/), 모드 id `demo`, "SC4J Demo: Dawnhold and Cindermaw")은 엔진만으로 만든 작은 실시간 전략 게임입니다. 그림, 소리, 메뉴, 컴퓨터 플레이어, 리플레이, 저장 게임, 네트워크 플레이가 있습니다.

이 저장소의 모든 모드는 루트 [README](../README.ko.md)에 있습니다.

짧은 예시를 빼면 가이드는 소스를 복사하지 않고 링크로 가리킵니다. 각 클래스의 Javadoc이 그 클래스의 계약입니다. 단계에 따로 적지 않으면 명령은 저장소 루트의 PowerShell에서 실행합니다. Linux나 macOS에서는 `./gradlew`와 `.bat`이 없는 시작 스크립트를 씁니다.

**목차**

| 시작하기 | 모드의 구성 | 더 알아보기 |
|---|---|---|
| [1. 모드란](#1-모드란) | [5. 콘텐츠](#5-콘텐츠-표준-데이터-팩) | [13. 리플레이와 저장 게임](#13-리플레이와-저장-게임) |
| [2. 첫 모드](#2-첫-모드) | [6. 규칙](#6-규칙-게임-정의-시스템-명령-커맨드) | [14. 네트워크 플레이](#14-네트워크-플레이) |
| [3. 매니페스트와 정체성](#3-매니페스트와-정체성) | [7. 빌드와 jar](#7-빌드와-jar) | [15. 빌드, 설치, 실행](#15-빌드-설치-실행) |
| [4. 진입 클래스](#4-진입-클래스-initialize-register-close) | [8. 맵과 첫 실행 콘텐츠](#8-맵과-첫-실행-콘텐츠) | [16. API와 버전](#16-api와-버전) |
| | [9. 헤드리스 러너](#9-헤드리스-러너) | [17. 에셋의 라이선스](#17-에셋의-라이선스) |
| | [10. 클라이언트 화면](#10-클라이언트-화면) | [18. 기존 모드 복제](#18-기존-모드-복제) |
| | [11. 컴퓨터 플레이어](#11-컴퓨터-플레이어) | [19. 모드 테스트](#19-모드-테스트) |
| | [12. 결정성](#12-결정성) | [20. 모드가 실행되는 두 방식](#20-모드가-실행되는-두-방식) |

## 1. 모드란

모드는 **jar 하나**입니다. jar 루트에는 매니페스트 `sc4j-mod.json`이 있고, 여기에 모드 이름(id, 이름, 버전), 빌드할 때 기준으로 삼은 엔진 API 버전, 진입 클래스가 적혀 있습니다. 엔진과 그 라이브러리는 jar에 들어 있지 않습니다. SC4J 설치본의 런타임이 제공합니다.

- 런타임 세션 하나에서는 모드 하나가 자체 클래스 로더에서 실행됩니다. 그래서 패키지 이름이 같아도 두 모드가 서로 만나는 일은 없습니다.
- **모드 id**는 설치본에서 모드가 가진 모든 것의 이름이 됩니다. 설정 파일 `userdata/settings/<id>.json`, 저장 폴더 `userdata/mods/<id>/`(리플레이, 저장 게임, 맵, 옵션; `ModStorage`), 로그 폴더 `userdata/logs/<id>/`.
- **모드 정체성**(`ModIdentity`)은 엔진 API 버전, 모드 id와 버전, jar 내용의 다이제스트, 모드가 직접 추가하는 데이터의 다이제스트로 이루어집니다. 두 컴퓨터가 함께 플레이하고 리플레이를 재생하려면 정체성이 같아야 합니다. jar가 바뀌면 정체성도 바뀝니다.
- 런타임은 진입 클래스를 만들고(인자 없는 public 생성자), `Mod.initialize(ModContext)`, `Mod.register(ModRegistration)`을 차례로 호출한 뒤 모드가 등록한 것을 실행하고, 마지막에 `Mod.close()`를 호출합니다.

필요한 것: 저장소를 빌드하는 JDK(JDK 25), 이 저장소(Gradle 래퍼와 개발용 설치본), 그 밖에는 없습니다. 모드 자체는 자기 폴더에 있고 SC4J 모드 플러그인으로 빌드합니다(7.1절).

## 2. 첫 모드

### 2.1 SC4J 설치본

모드는 SC4J 설치본의 `mods/` 폴더에 설치합니다. 저장소의 개발용 설치본은 `build/dev-install/`(런처, 런타임, 저장소의 모드)입니다.

```powershell
.\gradlew.bat devInstall
```

다른 SC4J 설치본도 똑같이 쓸 수 있습니다. 설치본 폴더에는 `launcher/`, `runtime/`, `mods/`가 있고, 한 번 사용하면 `userdata/`가 생깁니다.

### 2.2 모드의 파일

모드는 독립된 Gradle 프로젝트입니다. 여기서는 저장소 폴더 옆의 `my-mod` 폴더입니다.

| 파일 | 내용 |
|---|---|
| `settings.gradle` | 저장소와 프로젝트 이름(7.1절) |
| `build.gradle` | 모드 플러그인과 모드 id(7.1절) |
| `gradle.properties` | `sc4j.install=<install>`: 빌드가 설치할 설치본(Windows 경로는 슬래시로 씁니다) |
| `src/main/resources/sc4j-mod.json` | 매니페스트(3절) |
| `src/main/java/com/example/mymod/` | 모드의 클래스: 진입 클래스 `MyMod`와 화면 `FirstScene` |

모드 id는 소문자와 숫자 묶음을 하이픈 하나로 이은 것(`my-mod`)입니다. 다른 사람이 쓰지 않는 id를 고릅니다. 매니페스트:

```json
{
  "format": 1,
  "id": "my-mod",
  "name": {"en": "My Mod"},
  "version": "0.1.0",
  "engine": "0.1",
  "entry": "com.example.mymod.MyMod"
}
```

진입 클래스는 한 줄을 로그에 쓰는 헤드리스 러너와 창 실행의 첫 화면을 등록합니다(4장).

```java
package com.example.mymod;

import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.engine.mod.Mod;
import org.fogofwarlabs.sc4j.engine.mod.ModContext;
import org.fogofwarlabs.sc4j.engine.mod.ModRegistration;

public final class MyMod implements Mod {
    private Logger logger;

    @Override
    public void initialize(ModContext context) {
        this.logger = context.getLogger();
    }

    @Override
    public void register(ModRegistration registration) {
        Logger log = this.logger;

        registration.registerHeadlessRunner(launch -> log.info("MOD: headless run of {0}", launch.getIdentity()));
        registration.registerFirstScene(FirstScene::new);
    }

    @Override
    public void close() {
        this.logger = null;
    }
}
```

화면은 창을 한 가지 색(`0xAARRGGBB`로 묶은 값)으로 채우고 Escape로 실행을 끝냅니다(10장).

```java
package com.example.mymod;

import org.fogofwarlabs.sc4j.engine.client.app.ClientScene;
import org.fogofwarlabs.sc4j.engine.client.app.SceneClient;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.Key;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;

final class FirstScene implements ClientScene {
    private static final int BACKGROUND = 0xFF204060;

    private final SceneClient client;

    FirstScene(SceneClient client) {
        this.client = client;
    }

    @Override
    public void open(SceneClient context) {
    }

    @Override
    public void update(long time) {
    }

    @Override
    public void draw(Canvas surface) {
        surface.fillRectangle(0, 0, surface.getWidth(), surface.getHeight(), BACKGROUND);
    }

    @Override
    public boolean handleInput(InputEvent event) {
        if (event.getType() == InputEvent.Type.KEY_PRESSED && event.getKey() == Key.ESCAPE) {
            this.client.requestQuit();

            return true;
        }

        return false;
    }

    @Override
    public void close() {
    }
}
```

### 2.3 빌드와 설치

저장소의 Gradle 래퍼로 모드 폴더를 빌드합니다.

```powershell
.\gradlew.bat -p ..\my-mod installMod
```

`..\my-mod\build\libs\my-mod.jar`를 빌드하고 `<install>/mods/`에 복사합니다. `installMod` 대신 `jar`를 쓰면 jar만 빌드합니다.

### 2.4 헤드리스로 실행

```text
<install>\runtime\bin\sc4j-runtime.bat --mod <install>\mods\my-mod.jar --headless
```

모드의 헤드리스 러너를 실행하고 종료 코드 0으로 끝납니다. 런타임은 로그를 출력하며, 마지막 줄은 다음과 같습니다.

```text
... INFO  [main] RuntimeSession - IDENTITY: my-mod 0.1.0 (engine 0.1, content ..., data ...)
... INFO  [main] MyMod - MOD: headless run of my-mod 0.1.0 (engine 0.1, content ..., data ...)
... INFO  [main] RuntimeSession - RUNTIME END: exit code 0 (SUCCESS)
```

시작 스크립트는 `JAVA_HOME`이 가리키는 JDK(없으면 `PATH`의 `java`)로 실행하며, JDK 25가 필요합니다. 모드 자체의 인자는 `--` 뒤에 씁니다(`ModContext.getArguments()`, 9장).

### 2.5 런처에서 실행

```text
<install>\launcher\bin\sc4j-launcher.bat --list
```

`mods/`의 모든 jar와 상태를 출력합니다. 새 모드에는 실행 전 설정이 없으므로 `READY`입니다.

```text
Mods in <install>\mods
READY   my-mod               my-mod.jar
```

`<install>\launcher\bin\sc4j-launcher.bat`은 런처 창을 엽니다. "My Mod"를 골라 시작합니다. 런타임이 한 가지 색의 `FirstScene`이 있는 창을 엽니다. Escape로 실행을 끝냅니다.

이제부터 모드를 자신의 게임으로 만듭니다. 2.6절은 작은 게임의 부분을 흔히 만드는 순서로 보여 주고, 이 가이드의 나머지는 각 부분을 설명합니다.

### 2.6 게임으로 만들기

작은 게임에도 움직이고 싸우는 유닛, 그 유닛을 훈련하는 건물, 이기는 방법이 필요합니다.

1. **콘텐츠**(5장): `src/main/resources/<id>/pack/` 아래의 표준 데이터 팩(`pack.json`, `content.json`). `initialize`가 `StandardPack.openResources(MyMod.class.getClassLoader(), "my-mod/pack")`으로 열고 `readContent`로 읽습니다. `register`는 그 타입을 게임의 콘텐츠에 추가하고(`PackContent.register(registration.getContent())`) 팩을 리소스 제공자로 등록합니다.
2. **규칙**(6장): 엔진 기본 시스템과 그 프레임 단계가 있는 게임 정의 `registration.getSimulation()`.
3. **맵**(8장): `src/main/resources/<id>/seed/maps/` 아래의 엔진 맵 형식 맵. 첫 실행 때 저장 폴더로 복사되며, `"victory": "annihilation"`(유닛이 없는 플레이어가 패배) 또는 `"elimination"`(건물이 없는 플레이어가 패배)이 있습니다.
4. **유닛**은 `content.json`에 둡니다(5장). 건물에는 `"traits": ["building"]`, `placementWidth`, `placementHeight`와 `collision` 상자가 있습니다. 진영의 `townHall`은 그 진영 플레이어마다 시작 위치에 놓이고, `startUnits`는 그 옆에 놓입니다. 전투 유닛에는 `groundWeapon`(`weapons` 항목), `targetAcquisitionRange`, `buildTime`(게임 루프), `"idleOrders": {"human": "guard", "computer": "guard"}`를 줍니다. `cost`에는 `resourceKinds`와 시작 자원량(`Game.create` 뒤의 `game.getEconomy().setAmount`)이, `supplyRequired`에는 `supplyProvided`가 있는 유닛이 필요합니다. `"spearman": [{"actor": "keep"}]` 같은 `requirements` 항목이 있으면 그 건물이 유닛을 훈련할 수 있습니다. 공간 인덱스(`StandardSpatialIndex`)는 가장 큰 유닛의 너비와 높이로 만듭니다. 공간 인덱스는 그보다 큰 유닛을 찾지 못합니다.
5. **명령**(6장): 대기 명령과 커맨드가 쓰는 명령 타입(`guard`, `attack`, `attack-move`)은 `readContent`에, 그 동작은 명령 시스템에 넘깁니다. `(StandardCombatSystem) game.getCombat()` 위의 `GuardBehavior`, `AttackBehavior`, `AttackMoveBehavior`입니다.
6. **훈련.** 커맨드 레지스트리에 `TrainUnitCommandType`을 다음 id로 등록합니다(레지스트리 크기는 가장 큰 id 더하기 1입니다). 컨트롤러나 씬은 `train.create(game, building, type)`을 `CommandSink`로 보냅니다. 생산 대기열 `StandardProductionSystem`이 이를 실행합니다(`new StandardProductionSystem.Builder(capacity)`와 `StandardProductionSystem.addColumns(builder, capacity)`).
7. **승리.** 맵의 트리거(`StandardMapData.getTriggers()` 위의 `StandardTriggerSystem`)를 마지막 프레임 단계로 실행합니다. `((StandardTriggerSystem) game.getTriggers()).isGameOver()`는 게임이 끝났는지 알려 주고, `getOutcome(player)`는 `VICTORY`나 `DEFEAT`를 줍니다. 헤드리스 러너의 루프와 씬의 게임 루프는 거기서 멈춥니다.
8. **화면과 헤드리스 러너**(9장과 10장): 게임을 세션으로 보여 주는 씬, 컴퓨터 플레이어로 게임을 진행하고 리플레이를 기록하는 헤드리스 러너.
9. **확인**(12장): 같은 게임을 헤드리스로 두 번 실행해도, 그 리플레이를 재생해도 게임 루프마다 같은 상태 해시가 나와야 합니다.

## 3. 매니페스트와 정체성

`sc4j-mod.json`은 jar 루트에 있습니다(2.2절, [데모](../mods/demo/src/main/resources/sc4j-mod.json)). 런처와 런타임은 클래스를 불러오지 않고 이 파일을 읽습니다(`ModManifestReader`). 문제가 있으면 런처에서 모드가 `ERROR`가 됩니다.

| 필드 | 필수 | 값 |
|---|---|---|
| `format` | 예 | `1` |
| `id` | 예 | 모드 id(2.2절) |
| `name` | 예 | 언어별: `{"en": "My Mod", "ko": "..."}` |
| `version` | 예 | 모드 버전. 정체성의 일부 |
| `engine` | 예 | 빌드 기준 엔진 API 버전, `MAJOR.MINOR`. major가 같고 모드의 minor가 더 높지 않으면 런타임이 모드를 실행합니다(`EngineVersion.CURRENT`, [API 정책 3.1](api-policy.ko.md#31-엔진-api-버전)). |
| `entry` | 예 | 진입 클래스의 전체 이름 |
| `description`, `authors`, `website`, `license` | 아니요 | 런처의 모드 상세 정보에 보입니다. 에셋 라이선스가 요구하는 표기는 `license`에 씁니다(17장). |
| `notices` | 아니요 | 짧은 법적 고지. 각각 `name`처럼 언어별입니다: `[{"en": "Example Game is a trademark of Example Company.", "ko": "..."}]`. 런처의 정보 상자가 모드 이름 아래에 표시 언어로, 없으면 영어로 보여 줍니다. |
| `images` | 아니요 | `icon`, `banner`, `screenshots`: jar 안의 PNG 경로. 런처가 보여 줍니다. |
| `settings` | 아니요 | 시작 전에 런처가 묻는 실행 전 설정(아래) |

**실행 전 설정.** 시작하기 전에 무언가(외부 데이터 폴더, 언어)가 필요한 모드는 `settings`에 선언합니다. 항목에는 `key`, `type` (`directory`, `file`, `choice`, `integer`, `string`, `boolean`), `label`, `description`, `required`, `default`, `message`와 타입별 필드가 있습니다(`sc4j-mod-manifest`의 `SettingsSchema`). 런처는 입력 양식을 보여 주고 값을 검사해 `userdata/settings/<id>.json`에 저장합니다. 모드는 `ModContext.getSettings()`로 읽습니다. `"systemDefault": true`인 로캘 `choice`의 기본값은 운영 체제 로캘에 맞는 선택지(한국의 한국어면 `koKR`)이고, 없으면 `default`입니다. 필수 설정이 있는 모드는 저장하기 전까지 `WARNING`입니다. 2장의 첫 모드에는 설정이 없습니다.

**폴더 설정.** `directory` 설정은 양식에 폴더 선택기가 있고, 선택 필드 세 개가 있습니다.

- `check.contains`: 폴더에 있어야 하는 상대 경로. 없으면 `message`를 보여 줍니다.
- `detect`: 이런 폴더가 보통 기록되는 곳. 제거 항목 이름(`uninstallEntries`)과 프로그램 폴더 아래의 폴더 이름(`programFolders`)입니다. 설정에 값이 없는 동안 런처(와 런타임)는 그곳을 읽기만 하며 찾아보고, `check`를 통과한 첫 폴더를 저장합니다.
- `shared`: 여러 모드가 값 하나를 함께 쓸 때의 이름. 예를 들어 여러 모드가 읽는 게임의 폴더입니다. 값은 `userdata/shared-settings.json`에 한 번 저장되고, 같은 이름을 선언한 모든 모드가 그 값을 보여 주고 씁니다. `shared`가 없으면 값은 그 모드만의 것입니다.

모드는 다른 설정처럼 `ModContext.getSettings().getDirectory(key)`로 폴더를 읽습니다. 다른 게임 설치본의 데이터를 읽는 모드는 그 게임의 폴더를 이렇게 `detect`와 `shared`로 선언합니다.

**정체성 데이터.** 외부 데이터를 읽는 모드는 그 데이터의 버전이나 다이제스트를 `ModRegistration.addIdentityData`로 추가합니다. 그러면 외부 데이터가 다른 두 컴퓨터는 함께 플레이하지 않습니다.

## 4. 진입 클래스: initialize, register, close

`MyMod`(2.2절), [`DemoMod`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/DemoMod.java).

- `initialize(ModContext)`: 모드에 필요한 것을 열고 확인합니다. 실행을 거부하려면 `ModException`을 던집니다. `ModContext`는 매니페스트, 저장된 설정, 저장 폴더(`ModStorage`), 설치 폴더, 모드 자체의 명령줄 인자(`--` 뒤), 로거를 줍니다. 리소스는 자기 클래스 로더로 읽습니다(`MyMod.class.getClassLoader()`).
- `register(ModRegistration)`: 모드가 추가하는 것을 엔진에 알립니다. 메서드가 반환되면 등록이 닫힙니다.

| 등록 | 내용 | 첫 모드 | 데모 |
|---|---|---|---|
| `getSimulation()` | 게임 정의: 한계, 시스템, 프레임 단계, 열, 훅(6장) | - | `DemoRules.define` |
| `getContent()` | 콘텐츠 레지스트리: 타입과 정의(5장) | - | `DemoContent.register` |
| `registerResourceProvider` | 팩, 클라이언트의 리소스 | - | 팩 |
| `registerReplayFormat`, `registerSaveFormat` | 리플레이와 저장 게임의 형식(13장) | - | 엔진 형식 |
| `registerHeadlessRunner` | `--headless` 실행(9장) | 한 줄을 로그에 쓰는 람다 | `DemoHeadlessRunner` |
| `registerFirstScene` | 창 실행의 첫 화면(10장) | `FirstScene` | `MainMenuScene` |
| `addIdentityData` | 정체성에 더하는 데이터 | - | - |

- `close()`: `initialize`가 연 것을 역순으로 닫습니다.

모드는 첫 씬과 헤드리스 러너를 등록하거나(첫 모드와 데모의 방식), 자기 프레임을 직접 실행하는 `ModApplication`을 등록합니다. 둘 다 등록할 수는 없습니다. 두 방식은 20장에서 비교합니다.

## 5. 콘텐츠: 표준 데이터 팩

엔진만 쓰는 모드는 데이터를 엔진 표준 데이터 팩에 둡니다. jar의 한 폴더 아래에 JSON 정의, PNG 그림, WAV 소리가 있고, `StandardPack.openResources(loader, root)`로 엽니다. 팩 형식(`pack.json`, 스프라이트 시트, 애니메이션, 타일셋, 글꼴, 대화 상자, 그림)은 `org.fogofwarlabs.sc4j.engine.pack` 패키지의 Javadoc에 있습니다 ([package-info](../sc4j-engine-sim/src/main/java/org/fogofwarlabs/sc4j/engine/pack/package-info.java)).

`content.json`은 콘텐츠 타입을 정의합니다. `resourceKinds`, `motionProfiles`(유닛의 이동 방식: 최고 속도, 가속도, 정지 거리, 회전 속도), `weapons`, `factions`(일꾼, 본진 건물, 보급 건물, 자원 종류, 시작 유닛), `units`(진영, 이동 프로필, 체력, 방어력, 시야, 무기, 비용, 생산 시간, 보급, 충돌 상자, 배치 크기, 특성, 대기 명령), `requirements`입니다. `StandardPack.readContent`가 이 파일을 `PackContent`로 읽고, 모드는 그 타입을 게임의 콘텐츠에 등록합니다(`PackContent.register`).

데모([팩](../mods/demo/src/main/resources/demo/pack/), [`DemoContent`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/content/DemoContent.java)): 일꾼, 건물, 전투 유닛, 금이 있는 진영 둘. 스프라이트 시트, 타일셋, 글꼴, 대화 상자, 소리, 그림.

유닛을 추가하려면 `content.json`에 넣고(그리고 진영의 `startUnits`에 넣거나 맵의 `units`로 배치하고), 그 유닛이 이해하는 명령을 줍니다(6장).

## 6. 규칙: 게임 정의, 시스템, 명령, 커맨드

**게임 정의.** `ModRegistration.getSimulation()`은 `GameDefinition.Builder`입니다. 모드는 개체 한계(`EngineLimits`)를 정하고, `useSystem(KEY, factory)`로 슬롯마다 시스템을 고르고(엔진 기본값은 `Standard*` 클래스입니다: 공간 인덱스, 경로 탐색, 이동, 충돌, 시야, 경제, 채집, 요구 조건, 전투, 투사체, 생산, 명령, 트리거, 컴퓨터 플레이어), 기본 시스템에 필요한 확장 열 (`StandardCombatSystem.addColumns` 등), 훅(`addHook`), 프레임 단계를 순서대로(`addFramePhase`) 추가합니다. 팩토리는 게임마다 실행되므로 시스템이 게임의 맵을 읽을 수 있습니다. 모드는 어느 시스템이든 같은 인터페이스의 자기 클래스로 바꿀 수 있습니다.

기본 시스템의 설정은 중립적이므로, 모드가 자기 게임의 값을 선언합니다. 이동 시스템과 투사체 시스템의 운동 모델 (`new MotionModel.Builder(directionCount)`: 한 바퀴의 방향 수, 즉시 정지 속도, 회전 전 감속), 생산 시스템의 대기열 크기 (`new StandardProductionSystem.Builder(capacity)`. 모드가 비율을 정하지 않으면 환불은 비용 전체이고 건물은 체력이 가득 찬 채로 시작합니다), 전투 시스템의 스플래시 비율과 최소 피해(`StandardCombatSystem.Builder`. 정하지 않으면 스플래시가 닿은 모든 유닛이 피해를 전부 받고, 공격이 피해를 주지 못할 수도 있습니다)입니다.

유닛이 배치되고, 보고, 경로를 찾고, 움직이려면 유닛 경계(`TableUnitBounds`), 공간 인덱스, 경로 탐색, 이동, 충돌, 시야가 필요하고, 프레임 단계는 명령, 이동, 충돌, 시야입니다. 슬롯마다 팩토리를, 단계마다 시스템의 프레임 처리를 넘깁니다.

```java
registration.getSimulation()
        .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(new MotionModel.Builder(64).build()))
        .addFramePhase(MovementSystem.KEY, system -> (StandardMovementSystem) system);
```

데모: [`DemoRules`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/rules/DemoRules.java): 모든 기본 시스템, 채집, 전투, 생산, 트리거, 컴퓨터 플레이어.

**새 게임.** `GameSetup`은 맵(`MapReference`), 플레이어 슬롯(`PlayerSlot`: 슬롯, 컨트롤러 종류, 팀, 색, 진영), 시드를 정합니다. `Game.create(definition, setup)`이 게임을 만들고, `MapPlacementUtils.placeUnits`가 맵의 유닛을 배치합니다. 데모: [`DemoGames`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/game/DemoGames.java).

**명령.** 명령 타입(`OrderType`)은 콘텐츠이고, 그 동작(`MoveBehavior`, `AttackBehavior`, `GatherBehavior`, `BuildBehavior` 등, 또는 자기 `OrderBehavior`)은 게임마다 명령 시스템에 등록합니다. 명령 시스템은 등록을 함수로 받습니다. `new StandardOrderSystem((game, behaviors) -> behaviors.register(move, new MoveBehavior(16)))`은 명령 타입 `move`에 도착 거리 16 월드 픽셀의 동작을 줍니다. 데모: 명령 11개([`DemoOrders`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/order/DemoOrders.java)).

**커맨드.** 플레이어가 하는 모든 일은 모드의 커맨드 스트림에 있는 커맨드로 게임에 전달됩니다(고정 id를 가진 `CommandType`들의 `CommandRegistry`: `IssueOrderCommandType`, `TrainUnitCommandType`, `ConstructBuildingCommandType`, 또는 자기 타입). 커맨드는 `CommandSink`를 거치고, 프레임의 `ControllerCommandSource`가 프레임 단계보다 먼저 적용합니다. 리플레이와 네트워크 턴에는 바로 이 커맨드가 담기므로 id와 커맨드 인코딩은 모드 형식의 일부입니다. 바꿀 때는 모드 버전도 새로 올립니다.

**자기만의 메커니즘.** 엔진에는 대부분의 실시간 전략 게임에 공통인 부분이 있습니다. 한 게임만의 메커니즘은 그 모드에 두고 다음 지점에 연결합니다.
- 업그레이드나 효과로 바뀌는 값: 엔진 스탯 다음에 자기 `Stat`을 등록하고(`ContentRegistry.getStats().register(new Stat("..."))`) 거기에 `StatModifier`를 붙입니다(`ContentRegistry.addModifier`).
- 타입별 값: 타입 테이블의 확장 열(`ContentRegistry.addColumn`). 유닛별 값: 유닛 풀의 확장 열, 또는 모드가 유닛 풀을 직접 관리할 때는 자기 `Unit` 하위 클래스의 필드.
- 체력보다 먼저 피해를 받는 층(보호막, 방벽, 장갑판): 자기 포인트와 규칙을 가진 `DamageLayer`. 게임을 만드는 동안 그 층이 있는 유닛 타입마다 체인에 추가합니다(시스템의 `attach`에서 `GameContent.addDamageLayer`). 표준 전투 시스템은 체인 순서대로 층에 묻고, HUD는 층마다 이름 아래에 막대를 보여 줍니다.
- 이동 규칙: 엔진 `MotionModel` 위의 자기 이동 코드, 또는 같은 `MovingEntity` 필드 위의 자기 이동 알고리즘. 예정된 `MotionStep`을 적용 전에 옮길 수 있습니다(서로 밀어내는 공중 유닛 등). 또는 이동 슬롯의 자기 시스템(`useSystem`).

## 7. 빌드와 jar

### 7.1 모드 자체의 빌드

모드는 SC4J 모드 플러그인 `org.fogofwarlabs.sc4j.mod`로 빌드합니다. 이 플러그인은 엔진과 같은 버전으로 Maven Central에 함께 배포됩니다. 이 저장소 밖에 있는 모드의 `settings.gradle`:

```groovy
pluginManagement {
    repositories {
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = 'my-mod'
```

모드가 대상으로 하는 SC4J 버전을 쓴 `build.gradle`:

```groovy
plugins {
    id 'org.fogofwarlabs.sc4j.mod' version '<sc4j version>'
}

sc4jMod {
    id = 'my-mod'
}
```

- 플러그인은 자기 버전의 엔진 API(`org.fogofwarlabs.sc4j:sc4j-engine`과 `sc4j-base`, `sc4j-mod-manifest`, 엔진 모듈)를 대상으로 모드를 컴파일합니다. 런타임이 제공하므로 jar에는 들어가지 않습니다.
- `jar`는 main 소스 세트의 클래스와 리소스를 `build/libs/<id>.jar`로 재현 가능하게 묶고(시간 기록 없음, 고정 순서, 중복이면 실패), 프로젝트 폴더의 `LICENSE`와 `NOTICE`를 `META-INF/`에 넣습니다. `sc4j-mod.json`이 다른 id나 jar에 없는 엔트리 클래스를 가리키면 실패합니다. 같은 소스는 같은 jar, 곧 같은 정체성을 만듭니다.
- `run`은 플러그인 버전의 엔진 런타임에서 jar를 실행하고, 설치본의 사용자 데이터(설치본이 없으면 `build/sc4j-run`)를 씁니다. `run --args=--headless`는 창 없이 실행하고, `--debug-jvm`은 디버거를 기다립니다.
- `installMod`는 jar를 `<install>/mods/`에 복사합니다. 설치본은 Gradle 속성 `sc4j.install`입니다.
- `jar`, `run`, `installMod`는 태스크 그룹 `sc4j`에 있습니다.
- `sc4jMod`는 `sourceSets`(`main` 외에 jar에 더 넣을 소스 세트), `install`, `licenses`(`META-INF/`의 파일)도 받습니다. `tasks.named('jar') { from(...) }`로 jar에 파일을 더 넣습니다.

실행할 때 모드에 필요한 라이브러리는 jar에 넣어야 합니다(또는 설치본에 함께 있어야 합니다. 런타임의 `lib/`에는 LWJGL, FFmpeg 같은 엔진 라이브러리가 있습니다). 먼저 라이선스를 확인합니다(17장).

### 7.2 이 저장소 안의 모드

데모는 같은 플러그인을 쓰는 저장소 빌드의 프로젝트입니다([`mods/demo/build.gradle`](../mods/demo/build.gradle)). 저장소 빌드 안에서는 플러그인의 엔진 의존성이 엔진 프로젝트이고, 설치본은 `build/dev-install/`이며, 저장소의 `devInstall`이 `mods/`에 `demo.jar`를 넣어 그 설치본을 구성합니다. 자체 검사는 `checkModBoundary`와 `checkDemoNames`(데모는 다른 게임의 이름을 쓰지 않음), `determinismCheck`(12장)입니다. 테스트는 jar를 런타임으로 실행하고(`DemoModTest`) 클라이언트를 화면 밖에서 실행합니다(`DemoClientScenario`).

## 8. 맵과 첫 실행 콘텐츠

**맵 형식.** 엔진 맵 형식은 JSON 파일 하나입니다(`"format": "sc4j-map"`, 버전 1). 이름, 타일셋 이름, 타일과 보행 셀 크기, 타일 단위 크기, 범례(보행 가능, 건설 가능, 고도)가 있는 한 글자 코드 행으로 된 지형, 중립 슬롯, 시작 위치, 미리 배치된 유닛, 자원 컨테이너, 로케이션, 승리 규칙, 트리거가 들어 있습니다. 전체 필드 목록은 `JsonMapLoader`의 Javadoc에 있습니다 ([소스](../sc4j-engine-sim/src/main/java/org/fogofwarlabs/sc4j/engine/map/JsonMapLoader.java)). 모드는 `new JsonMapLoader(unitTypes, resourceKinds)`로 맵을 읽어 `MapCatalog`에 둡니다. `MapCatalog`는 맵마다 `MapReference`(이름과 다이제스트)를 줍니다. 승리 규칙과 트리거는 트리거 시스템이 있는 게임에서만 동작합니다. 모드는 게임 맵의 트리거 위의 `StandardTriggerSystem` (맵의 `StandardMapData`로 `new StandardTriggerSystem.Builder().addTriggers(data.getTriggers()).build()`, 승리 규칙이 처음)을 마지막 프레임 단계로 등록합니다.

**격자와 투영.** 시뮬레이션은 보행 셀로 된 정사각형 격자(`TerrainGrid`) 위에서 월드 픽셀 단위로 동작합니다. 클라이언트가 이를 보여 주는 방식은 클라이언트의 투영입니다. `SquareProjection`(위에서 내려다봄) 또는 `IsometricProjection`(마름모; 데모, [`DemoWorldUtils`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/client/DemoWorldUtils.java)). 육각형이나 엇갈린 격자는 아직 엔진에 없습니다. 격자와 투영 인터페이스는 이를 추가할 수 있게 되어 있습니다.

**첫 실행 콘텐츠.** 새 설치본의 저장 폴더는 비어 있습니다. 맵을 저장 폴더에 두는 모드(플레이어가 자기 맵을 추가하는 곳)는 자기 맵을 직접 거기에 넣어야 합니다. 그러지 않으면 첫 시작 때 맵이 없습니다(그래서 18장의 복제 모드는 빈 맵 목록으로 시작합니다). 일반적인 방법은 이렇습니다. jar는 `<id>/seed/` 아래에 맵을 담고, `initialize`가 목록의 시드 파일을 **저장 폴더에 같은 경로의 파일이 없을 때만** 복사합니다.

```java
for (String file : List.of("maps/first-field.json")) {
    Path target = context.getStorage().resolve(file);

    if (Files.exists(target)) {
        continue;
    }

    try (InputStream input = MyMod.class.getClassLoader().getResourceAsStream("my-mod/seed/" + file)) {
        if (input == null) {
            throw new IOException("The mod jar has no seed file " + file);
        }

        Files.createDirectories(target.getParent());
        Files.copy(input, target);
    }
}
```

그래서 새 설치본은 모드의 맵으로 시작하고, 플레이어 자신의 맵은 그 옆에 놓이며, 플레이어가 바꾼 맵은 덮어쓰지 않습니다(지운 맵은 다음 시작 때 다시 생깁니다). 그다음 모드가 `maps/`의 모든 `*.json`을 파일 이름 순서로 읽고, 올바른 맵이 아닌 파일은 경고와 함께 뺍니다. 맵을 하나 더 넣으려면 다른 맵과 겹치지 않는 `name`으로 `src/main/resources/<id>/seed/maps/` 아래에 두고, `<id>/seed/` 기준 경로 (`"maps/second-field.json"`)를 목록에 추가하고, 다시 빌드해 설치합니다(`installMod`). 다음 시작 때 저장 폴더로 복사됩니다. 다른 첫 실행 파일(옵션, 튜토리얼 리플레이)도 같은 방법으로 넣습니다. jar에서 시드 맵을 바꾸거나 지워도 이전 실행이 저장 폴더에 남긴 복사본은 바뀌지 않습니다. 맵을 작업하는 동안에는 그 복사본을 지웁니다(또는 다른 `--storage`로 실행합니다).

데모는 내장 맵을 jar에서 바로 읽고([`DemoMaps`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/map/DemoMaps.java)), 네트워크 호스트가 보낸 맵을 그 옆에 둡니다. 두 방식 모두 괜찮습니다.

## 9. 헤드리스 러너

`HeadlessRunner.run(ModLaunch)`이 `--headless` 실행입니다. 창, 오디오, 입력이 없으며 테스트, 서버, 분석에 씁니다. `ModLaunch`는 리플레이(`getReplay()`, `getReplayFormat()`)나 참가할 게임(`getJoin()`)을 요청했는지 알려 주고, 정체성, 게임 정의, 리소스 제공자, 리플레이와 저장 형식, 실행의 Beacon.Link를 담고 있습니다. 모드 자체의 인자는 `ModContext.getArguments()`입니다.

데모: [`DemoHeadlessRunner`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/DemoHeadlessRunner.java)는 `--map`에서 컴퓨터 플레이어 둘의 게임을 끝까지 진행하고 리플레이를 기록합니다. 또는 `--replay`를 재생하고, 상태 해시 체크포인트가 다르면 실패합니다. `--frame-hashes`는 게임 루프마다 해시 하나를 씁니다. 127.0.0.1의 네트워크 게임(`--host`, 런타임의 `--join`)과 Beacon.Link 게임 (`--beaconlink-host`, `--beaconlink-join`, 런타임의 `--seed <host:port>`와 함께)도 진행합니다.

## 10. 클라이언트 화면

런타임은 창, 오디오, 입력을 열고, 모드의 `FirstSceneFactory`로 첫 씬을 만들어 `SceneClient` 위에서 프레임을 실행합니다(20장). 화면은 `ClientScene`입니다(`open`, `update`, `draw`, `handleInput`, `close`). 씬은 `client.getScenes().replace(...)`로 다음 씬으로 바꾸고 `pushModal`로 팝업을 엽니다. `client.requestQuit()`은 실행을 끝냅니다. `SceneClient`는 실행 요청, 저장 폴더, `Canvas`, `AudioMixer`, 커서 표면을 줍니다.

**게임 보여 주기.** `GameSessionFactory`가 `Game`을 클라이언트에 연결합니다. `createLocal`(사람과 컴퓨터 플레이어), `createReplay`, `createNetwork`. 세션의 `GameView`는 클라이언트가 그릴 때 읽는 읽기 전용 뷰이고, 사람의 커맨드는 세션의 `CommandSink`로 갑니다. `CameraScroll`은 키와 화면 가장자리로 `Camera`를 스크롤하며, 연속(`ContinuousScrollRate`) 또는 틱마다 표에 따른 단계 (`SteppedScrollRate`)로 움직입니다. 월드는 `Camera`와 투영을 거친 `WorldLayer`들의 `WorldView`입니다.

- 그림 없이: `TerrainLayer`(`TerrainColors`의 단색)와 `UnitLayer`(팀 색의 유닛 영역).
- 그림과 함께: 팩의 타일셋과 스프라이트 시트 위의 `TilesetLayer`와 `UnitSpriteLayer`. 배율이 있는 화면에서는 팩의 고해상도 판 `<id>-2x`부터 `<id>-4x`까지에서 그립니다(`DetailUtils`. 고해상도 스프라이트 시트는 `spriteSheetDetails`에 나열합니다).
- `GameScreen`: 엔진 기본 게임 화면. 선택, 커맨드 카드, 미니맵, 자원 표시줄, 선택 패널, 안개가 HUD 스킨 안에 있습니다. 팩의 글꼴이 필요합니다.

데모의 클라이언트([패키지](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/client/)): `DemoClient`(공유 서비스: 글꼴, 스프라이트, 스킨, 소리, 커서), `MainMenuScene`과 `DemoMenuScene`(팩 대화 상자를 메뉴로), `GameSetupScene`, `LoadingScene`, `DemoGameScene`과 `DemoGameScreen`(커맨드 카드와 데모의 스마트 오른쪽 버튼 `DemoSmartAction`이 있는 `GameScreen`), `ReplayViewerScene`(엔진의 저장 게임 목록 `GameFileListScene`), `OutcomeScene`, `NetworkScene`, `NetworkRoomScene`, `BeaconLinkScene`, `DemoSkin`, `DemoSprites`, `DemoSounds`, `DemoNotifications`.

**창 없이 화면 테스트하기.** `ImageCanvas`는 메모리 안의 `Canvas`입니다. 이 위에 무음 오디오 장치로 `SceneClient`를 만들고, 첫 씬을 시작하고, `client.handleInput`으로 입력을 보내고 `client.runFrame`으로 프레임을 실행합니다. 데모의 `DemoClientScenario`가 이렇게 합니다. 런타임에는 화면 밖 모드가 없으므로, 이 저장소 밖의 모드는 자기 빌드에서 같은 일을 합니다. 모드의 클래스와 `<install>/runtime/lib`의 모든 jar가 클래스 경로에 있는 main 클래스를 가진 소스 세트, 임시 폴더 위의 `ModStorage` (`getRoot`와 `resolve`. 런타임 자체의 구현은 API가 아닙니다), `ModIdentity.create(EngineVersion.CURRENT, manifest, digest, List.of())`와 모드가 `register`에서처럼 만든 게임 정의를 가진 `ModLaunch`, 그림은 `ImageIO.write(canvas.getImage(), "png", file)`입니다. 클라이언트의 슬래시 명령과 키 바인딩은 모드가 정합니다(`ConsoleCommandRegistry`, `KeyBindingTable`).

## 11. 컴퓨터 플레이어

컴퓨터 플레이어는 게임에만 의존해 커맨드를 내는 `PlayerController`입니다. 엔진 기본값은 시스템입니다. `StandardComputerController`를 쓰는 `StandardComputerPlayerSystem`(일꾼이 채집하고, 건물과 전투 유닛을 만들고, 공격 부대를 보냅니다; `ComputerSettings`)이며, `DemoRules`가 씁니다. `ControllerKind.COMPUTER` 슬롯은 네트워크 게임의 모든 컴퓨터에서 똑같이 실행됩니다.

## 12. 결정성

락스텝 네트워크 게임과 리플레이는 모든 컴퓨터에서 같은 커맨드로 같은 시뮬레이션 결과를 내야 합니다. 엔진 자체의 시뮬레이션 패키지는 이를 검사합니다(루트 `build.gradle`의 `checkSimulationDeterminism`). 자기 시뮬레이션 코드(시스템, 동작, 컨트롤러, 게임을 바꾸는 모든 것)도 같은 규칙을 지켜야 합니다.

- `Math.random`, 시계(`System.currentTimeMillis`, `System.nanoTime`, `java.time`), `java.util` 난수 생성기(`Random`, `SecureRandom`, `SplittableRandom`, `ThreadLocalRandom`)를 쓰지 않습니다. 난수는 설정으로 시드가 정해진 게임의 난수 생성기에서 얻습니다.
- 시뮬레이션이 순서를 보는 해시 순서 컬렉션(`HashMap`, `HashSet`, `Hashtable`, `IdentityHashMap`, `WeakHashMap`, `ConcurrentHashMap`)과 identity hash code를 쓰지 않습니다. 리스트, 배열, `LinkedHashMap`, 슬롯 순서의 개체 풀을 씁니다.
- 부동소수점(`float`, `double`)을 쓰지 않습니다. 위치, 속도, 피해는 정수나 엔진 고정소수점 값입니다.
- 게임은 프레임 시작에 적용되는 커맨드와 시스템의 프레임 단계를 통해서만 바뀝니다. 씬, 입력 처리기, 다른 스레드에서 직접 바꾸지 않습니다.
- 모든 것의 순서가 정해져 있습니다. 시스템과 단계는 등록 순서, 컨트롤러는 목록 순서, 커맨드는 제출 순서입니다.

클라이언트(그리기, 소리, 메뉴)는 무엇이든 쓸 수 있습니다. 데모의 `determinismCheck`처럼 확인합니다. 같은 게임을 두 번 진행해 게임 루프마다 상태 해시를 비교하고(`Game.computeStateHash`, `--frame-hashes`), 리플레이를 재생해 같은 해시가 나오는지 봅니다.

## 13. 리플레이와 저장 게임

`EngineReplayFormat`(`.sc4r`, 매직 `SC4R`)은 설정과 모든 커맨드를 기록하고, n 게임 루프마다 상태 해시 체크포인트를 남깁니다 (`createRecorder`, `ControllerCommandSource`에 설정). `read`와 `createController`는 이를 재생하고 체크포인트 불일치를 셉니다. `EngineSaveFormat`(매직 `SC4S`)은 게임 상태를 저장합니다. 둘 다 실행의 정체성으로 만들어지고 같은 정체성에서만 불러옵니다. 같은 엔진 API 버전, 모드 id, 버전, jar입니다. 자기 형식을 쓰는 모드는 `ReplayFormat` / `SaveFormat`을 구현합니다. 파일은 저장 폴더 (`replays/`, `saves/`)에 두며, 엔진의 `GameFileListScene`이 목록을 보여 줍니다. 데모: 클라이언트의 리플레이 뷰어와 저장 게임, 헤드리스 러너의 리플레이.

## 14. 네트워크 플레이

엔진의 네트워크 계층(`sc4j-engine-net`)은 락스텝 게임을 실행합니다. 모든 컴퓨터가 같은 시뮬레이션을 실행하고 턴마다 커맨드만 주고받습니다.

- `NetworkLobby`: 시작 전의 방. 멤버, 슬롯, 진영, 준비 표시, 맵 전송(모드의 `LobbyMaps`. 기본 `FolderLobbyMaps`는 `ModStorage.getMapFolders()` 위에서 `maps/`의 맵을 다이제스트로 찾고, 받은 맵은 `maps/downloads/`에 저장합니다), 시작(설정을 담은 `NetworkGameStart`). 로비는 정체성을 비교해 다른 모드나 버전을 거부합니다.
- `DatagramLockstepTransport`와 `LockstepSession`: 진행 중인 게임의 턴. `GameSessionFactory.createNetwork`가 이를 보여 줍니다.
- `LanRoomDiscovery`: 로컬 네트워크의 방.
- Beacon.Link(`ModLaunch.getBeaconLink()`): 모든 SC4J가 실행하는 피어를 거치는 인터넷의 방, 게임, 커뮤니티(14.1절과 14.2절).

데모는 이 모두를 씁니다. [`DemoLobby`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/net/DemoLobby.java), [`DemoNetworkGame`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/net/DemoNetworkGame.java), [`DemoNetworkSettings`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/net/DemoNetworkSettings.java), 네트워크 씬과 헤드리스 네트워크 실행입니다. 네트워크 코드는 데모의 테스트와 `determinismCheck`처럼 127.0.0.1에서만 테스트합니다.

### 14.1 Beacon.Link 방

`ModLaunch.getBeaconLink()`는 실행의 `BeaconLink`이며, 실행이 Beacon.Link를 제공하지 않으면 `null`입니다. 명령줄에 시드 출처(`--seeds <file>`, `--seed <host:port>`, `--peers <file>`)가 있을 때만 제공합니다. `--account <dir>`는 계정을 정하며, 없으면 `Guest` 라는 이름의 게스트로 플레이합니다(`BeaconLink.GUEST_NAME`). 런처는 이 모두를 넘깁니다. `BeaconLink.open()` 전에는 아무것도 열리지 않습니다. `open()`은 `BeaconLinkSession`을 반환합니다. 계정의 연결 하나입니다(`getParticipant()`, `LinkParticipant`: 플레이어와 연결 번호). 모든 호출은 작업을 시작하기만 합니다. 세션의 스레드가 자기 루프에서 `poll()`을 호출하고, 끝날 때 `close()`를 호출합니다. `getFailure()`(`LinkFailure`)는 노드에 닿을 수 없는 이유를, `getNodes()`는 노드마다의 상태를 알려 줍니다.

- **호스트.** `publishRoom(settings, password)`는 방을 게시하고(비밀번호가 비어 있으면 공개 방) `HostedRoom`을 반환합니다. `getState()`(`PUBLISHING`, `PUBLISHED`, `WITHDRAWN`, `FAILED`), 플레이어가 드나들 때 `update(settings)`, 참가 확인을 통과한 참가자마다 `acceptChannel()`, `withdraw()`. `RoomSettings`에는 방 이름과 맵 이름(각각 UTF-8 최대 48바이트), 플레이어 수, 정원, 게임 시작 여부, 모드가 정의하고 찾은 방에서 다시 읽는 최대 64바이트의 모드 데이터(데모: 턴 속도)가 있습니다.
- **검색.** `searchRooms()`는 `RoomSearch`를 반환합니다. `getRooms()`는 실행 자신의 모드 정체성을 가진 방이고, `getOtherRooms()`는 모드의 다른 버전의 방과 그 호스트가 줄 거부 사유(`FoundRoom.getJoinRefusal()`)입니다. 모든 노드가 답하거나 실패하면 `isComplete()`가 true가 됩니다. `FoundRoom`은 호스트, 설정, `isJoinable()`, `isPrivate()`, 모드 id와 버전, `getLatencyEstimate()`(`LatencyClass`: 100 ms까지 `GOOD`, 200 ms까지 `FAIR`, `POOR`, `UNKNOWN`)를 줍니다.
- **참가.** `joinRoom(room, password)`는 호스트로 `ParticipantChannel`을 엽니다. `CONNECTING` 다음 `OPEN`이 되거나, `ChannelFailure`와 함께 `CLOSED`가 됩니다. `JOIN_REFUSED`에는 `JoinRefusal`이 따라옵니다(다른 엔진 버전이나 모드 정체성, 꽉 찬 방, 시작된 게임, 틀린 비밀번호).
- **플레이.** `getOverlay()`는 세션의 `LinkOverlay`를 반환합니다. 채널을 통해 참가자 단위로 데이터그램을 보냅니다(최대 1023바이트, 신뢰성 없음). 포트 1(`LOBBY_PORT`)은 엔진의 `NetworkLobby`, 포트 2(`GAME_PORT`)는 엔진의 락스텝 전송(`LinkLockstepTransport`)이고, 포트 16부터 255까지(`FIRST_MOD_PORT`부터)는 모드 자체의 프로토콜에 쓸 수 있습니다. `NetworkLobby.host(room, overlay, ...)`와 `NetworkLobby.join(channel, overlay, ...)`는 로컬 네트워크와 같은 로비와 락스텝 게임을 줍니다.

데모: [`DemoLobby`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/net/DemoLobby.java)(호스트, 참가, 오버레이 위의 게임), [`BeaconLinkScene`](../mods/demo/src/main/java/org/fogofwarlabs/sc4j/demo/client/BeaconLinkScene.java)(방 목록, 호스트, 참가, 채팅), `DemoHeadlessRunner`(`--beaconlink-host`, `--beaconlink-join`).

### 14.2 Beacon.Link 커뮤니티

`BeaconLinkSession.getCommunity()`는 세션의 `CommunitySession`을 반환합니다. 처음 호출할 때 만들어집니다.

- **접속 상태.** `getSelf()`, `setStatus(PlayerStatus)`(`AVAILABLE`, `AWAY`, `IN_ROOM`, `IN_GAME`), `setAwayText`(UTF-8 최대 32바이트), `setAppearOffline`. `getPresence(player)`는 플레이어의 `PlayerPresence`를 줍니다. 여러 연결(런처, 게임, 다른 컴퓨터)을 합친 것이며, 이름과 태그, 상태, 모드 id와 버전, `canPlayTogether()`, 채널, 자리 비움 문구, 지연 추정, 장치 수가 있습니다.
- **채팅.** `joinChannel(name)`(동시에 최대 4개 채널. 채널은 모든 모드가 공유합니다)은 `ChatChannel`을 반환합니다. `say`와 `emote` (UTF-8 최대 255바이트. 다섯 줄을 연달아 보낸 뒤에는 초당 한 줄), `getLines()`, `getMembers()`, `leave()`. `listChannels()`는 알려진 채널과 멤버 수를 나열합니다.
- **귓속말.** `whisper(player, text)`는 귓속말을 보냅니다. 결과(`DELIVERED`, `NOT_ONLINE`)는 리스너로 옵니다.
- **친구와 무시한 플레이어.** `addFriend`, `removeFriend`(친구 최대 16명), `ignore`, `unignore`, `isIgnored`. 무시 목록은 계정에 속합니다. 무시한 플레이어의 채팅 줄과 귓속말은 전달되지 않으며, 그 방은 모드가 숨깁니다.
- **리스너.** `addListener(CommunityListener)`: `onChatLine`, `onMembersChanged`, `onWhisper`, `onWhisperResult`. `poll()` 안에서 호출됩니다.

`BeaconLinkSession.signOutOtherDevices()`는 계정의 다른 연결을 끝냅니다. `sc4j-engine`의 `engine.client.community` 패키지에는 커뮤니티 화면의 기본 부품이 있습니다. `ChannelChat`(슬래시 명령, 친구, 차단한 플레이어가 있는 채널 채팅), `RoomFilter`, `ChatLogBox`, `TableBox`와 그 텍스트이며, 데모의 `BeaconLinkScene`이 씁니다. 와이어 프로토콜은 [Beacon.Link 프로토콜 참조](beaconlink/protocol.ko.md)에, 다른 플레이어를 위한 피어 운영은 [Beacon.Link 운영 가이드](beaconlink/operator.ko.md)에 있습니다.

## 15. 빌드, 설치, 실행

- **Jar**: 루트의 `sc4j-mod.json`, 클래스와 리소스. 엔진 jar는 넣지 않으며 재현 가능해야 합니다. 바꿀 때마다 정체성이 새로 생깁니다. 함께 플레이하거나 서로의 리플레이를 재생하려면 플레이어들이 정확히 같은 jar를 가져야 합니다.
- **설치**: jar를 `<install>/mods/`에 넣습니다(`installMod`, 또는 직접). 런처는 거기 있는 모든 jar를 보여 줍니다. `READY`, `WARNING` (고칠 설정이 있음), `ERROR`(망가진 jar나 매니페스트, 이 런타임이 실행할 수 없는 엔진 버전, 없는 그림, 다른 jar와 같은 id).
- **런타임**: `sc4j-runtime --mod <jar> [--install <dir>] [--settings <json>] [--storage <dir>] [--log <dir>] [--account <dir>] [--seeds <file>] [--seed <host:port>]... [--peers <file>] [--headless] [--replay <file>] [--join <address>] [-- <mod arguments>]` (`--help`가 출력합니다). 종료 코드: 0 정상, 1 실행 실패, 2 명령줄이나 설정 오류, 3 모드 로드나 검증 실패. 로그는 `<install>/userdata/logs/<id>/sc4j.log`입니다.
- **런처**: 모드 목록을 보여 주고, 모드 설정을 받고, 모드의 설정, 저장 폴더, 로그 폴더로 런타임을 시작합니다.

## 16. API와 버전

정책 자체는 [엔진 API 정책](api-policy.ko.md)에 있습니다. 요약하면:

- API는 `sc4j-base`, `sc4j-mod-manifest`, `sc4j-engine-core`, `-sim`, `-net`, `-client`, `sc4j-engine`의 public, protected 클래스와 멤버이며, 각 모듈의 `api/*.api` 파일에 나열되어 있습니다. 그 Javadoc이 계약입니다.
- API가 아닌 **내부** 요소: `engine.mod.loading`, `engine.client.render.gl`, `engine.client.audio.openal`, GLFW 호스트 구현(`GlfwClientHost`, `GlfwHostWindow`, `GlfwInput`, `GlfwKeyCodes`, `GlfwCursorSurface`), Windows 플랫폼 서비스 (`WindowsTextInputMethod`, `WindowsSystemPointerSpeed`, `WindowsHostTickClock`). 어느 버전에서든 예고 없이 바뀌거나 없어질 수 있고 호환성 약속이 없습니다. `@ApiStatus.Internal`이 붙어 있어 IDE가 사용 위치를 표시합니다. 그리기는 `engine.client.render`로 하고(자기 렌더러는 `GraphicsDevice.getCurrent()`로 GPU 캔버스와 셰이더 프로그램을 만듭니다), 소리는 `engine.client.audio`(`AudioDevice.openDefault()`)로 재생하며, 자기 창은 `engine.client.host` 인터페이스(`ClientHost.initialize()`)로 실행합니다. `HostWindow`는 키와 포인터(`createInput`, `isKeyDown`, `setPointerMode`, `createCursorSurface`), 포커스, 클립보드, 끌어다 놓은 파일, 전체 화면 감마 램프, 입력기(`getTextInputMethod`)를 제공합니다. `SystemPointerSpeed`와 `HostTickClock`은 플랫폼 서비스입니다.
- 엔진 API 버전은 `MAJOR.MINOR`입니다(`EngineVersion.CURRENT`, 현재 0.1). 매니페스트의 `engine` 필드에 넣습니다. 새 major 버전은 모드를 깨뜨릴 수 있고, 새 minor 버전은 추가만 합니다. API가 0.x인 동안에는 안정성을 약속하지 않습니다. 호환되지 않는 변경이 일어날 수 있으며 `.api` 차이로 드러납니다.
- 시뮬레이션 결과가 바뀌면 적어도 minor 버전을 올립니다. 리플레이와 저장 게임은 엔진 버전 사이에 호환되지 않습니다.

## 17. 에셋의 라이선스

jar에 넣은 모든 것은 jar를 받는 모든 플레이어에게 전달됩니다.

- 자기 작업물이나, 모드에 담아 재배포할 수 있는 라이선스의 자료(CC0 같은 퍼블릭 도메인, 또는 조건을 지킬 수 있는 라이선스)를 씁니다. 라이선스 파일과 고지문을 에셋과 함께 두어 jar에 넣고, 라이선스가 요구하는 표기를 매니페스트의 `license`와 `description`에 보여 줍니다.
- 데모: [`mods/demo/THIRD-PARTY.md`](../mods/demo/THIRD-PARTY.md)가 그림의 출처를 밝히며 `demo.jar`에 들어 있습니다.
- 고지를 요구하는 라이선스의 그림: 그 `NOTICE.txt`와 `license/` 폴더를 그림과 함께 두고, 빌드가 jar에 넣게 하고, 표기를 매니페스트에 넣습니다.
- 다른 게임 설치본의 파일(그림, 소리, 데이터, 맵)은 절대 jar나 저장소에 넣지 않습니다. 다른 게임의 데이터를 쓰는 모드는 실행할 때 폴더 설정(3장)을 통해 플레이어 본인의 설치본에서 읽습니다.

## 18. 기존 모드 복제

처음부터 만들지 않고 기존 모드를 바꾸려면 복제합니다. 빌드할 때 그 모드의 소스를 복사하고, 몇몇 파일을 자기 판으로 덮어쓰고(일반 파일로 편집하는 오버레이), 자기 모드 id를 씁니다. 복제한 모드가 자신이 바꾼 파일을 바꾸면 잠금이 빌드를 멈춥니다. SC:R 모드의 복제 모드인 SC:R Crossover에는 별도 가이드 [SC:R 복제 모드 가이드](https://github.com/fogofwarlabs/sc4j-scr-crossover/blob/main/docs/scr-crossover.ko.md)가 있습니다. 복제 단계, 오버레이와 잠금, 정체성, 콘텐츠 확장, 바꾼 값, 자체 그래픽을 가진 추가 유닛, 그림 라이선스, 빌드, 설치, 런처를 다룹니다.

- 복제 모드는 다른 모드입니다. 자기 id, 설정, 저장 폴더, 로그 폴더를 가집니다. 저장 폴더도 비어 있는 채로 시작하므로, 플레이어에게 필요한 맵을 넣어 둡니다(8장).
- 복제 모드는 정체성이 같은 플레이어와만 플레이합니다(1장). 모드 id를 담지 않는 자체 네트워크 프로토콜에는 복제 모드만의 값이 필요합니다. 예를 들어 호스트가 참가할 때 확인하는 버전입니다.

## 19. 모드 테스트

- 이 저장소 밖의 모드는 자기 빌드에서 테스트합니다. jar를 빌드하고, 임시 설치본에 설치하고, 설치본의 런타임을 헤드리스로 두 번, 리플레이로 한 번 실행해 프레임 해시를 비교합니다(12장).
- 배포되는 엔진 테스트 픽스처(`testImplementation testFixtures('org.fogofwarlabs.sc4j:<module>:<version>')`, [API 정책](api-policy.ko.md#1-api란))는 커뮤니티와 Beacon.Link를 손으로 재생하고(`sc4j-engine`의 `RecordingCommunitySession`, `RecordingBeaconLink`), 에뮬레이션한 NAT 뒤에서 노드와 클라이언트를 127.0.0.1에서 실행하고(`sc4j-beaconlink`의 `ClientScenario`), 설치 기록을 대신합니다(`sc4j-mod-manifest`의 `FakeInstallationRecords`).
- 저장소 안에서는 jar를 테스트 JVM의 `RuntimeSession.run`으로 실행하고(`DemoModTest`), 클라이언트를 화면 밖에서 실행하고 (`DemoClientScenario`), 런타임 JVM 둘을 서로 맞붙입니다(`determinismCheck`).

## 20. 모드가 실행되는 두 방식

모드는 `register`에서 어떻게 실행될지 런타임에 알립니다. 방식은 둘이며, 모드는 그중 하나만 씁니다. 둘 다 등록하면 등록이 `IllegalStateException`을 던집니다.

| | 엔진 씬 | 자체 애플리케이션 |
|---|---|---|
| 등록 | `registerFirstScene(FirstSceneFactory)`와 `registerHeadlessRunner(HeadlessRunner)` | `registerApplication(ModApplication)` |
| 창 실행 | 런타임이 창, 캔버스, 오디오, 커서, 입력을 열고, 첫 씬을 `SceneClient`에서 시작하고, 엔진 호스트(`ClientHost`) 위에서 `WindowClient`로 프레임을 실행합니다. | 런타임은 창을 열지 않고 `ModApplication.run(ModLaunch)`을 호출합니다. 모드가 창을 열고 같은 엔진 호스트 위에서 자기 프레임을 실행합니다(`ClientHost.initialize()`, `createWindow`, 자기 `HostApplication`으로 `ClientHost.run`). |
| `--headless` 실행 | `HeadlessRunner` | `isHeadlessCapable()`이 `true`를 반환하면 같은 `run` |
| 사용하는 모드 | 2장의 첫 모드, 데모 | [SC:R 모드](https://github.com/fogofwarlabs/sc4j-scr). 원본 게임의 프레임 타이밍을 따르기 위해 |

**엔진 씬.** `WindowClient`는 `HostApplication`입니다. 프레임마다 호스트가 창 이벤트를 폴링하고, `WindowClient`가 입력을 씬에 넘기고, `SceneClient` 프레임을 실행하고, 수직 동기화로 화면에 표시합니다. 모드는 씬(10장)을 작성하며 프레임 루프를 직접 다루지 않습니다.

**자체 애플리케이션.** 모드가 프레임 루프를 직접 작성합니다. 모드의 `HostApplication`이 프레임 하나에서 할 일(`runFrame`)과 호스트가 맞출 프레임 속도(`getFrameRateLimit`)를 정합니다. 엔진 씬이라면 `WindowClient`가 하는 일도 모드가 합니다. 창, 캔버스, 오디오, 커서를 순서대로 열고 닫습니다. SC:R 모드는 프레임이 원본 게임을 따라야 해서 이 방식을 씁니다. 시뮬레이션은 호스트의 시간에 따라 진행되고, 메뉴와 게임에서 원본 게임의 프레임 속도 제한이 적용됩니다.

**선택 기준.** 거의 항상 엔진 씬입니다. 모드가 프레임 루프를 직접 제어해야 할 때만 자체 애플리케이션을 고릅니다. 나머지는 두 방식이 같습니다. 매니페스트, 콘텐츠, 시뮬레이션, 리플레이와 저장 게임, 네트워크 플레이, Beacon.Link(`ModLaunch.getBeaconLink()`)입니다.
