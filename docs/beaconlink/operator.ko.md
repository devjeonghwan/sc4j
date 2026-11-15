# Beacon.Link 피어 운영 가이드: 헤드리스 피어와 런처 공개 모드

[English](operator.md) | **한국어**

모든 SC4J는 Beacon.Link 피어입니다. 런처는 열려 있는 동안 피어 하나를 실행하고, 게임 세션도 피어 하나를 실행합니다. 네트워크는 이 피어들로 이루어지며, Fog of War Labs는 중앙 서버를 운영하지 않습니다. 모르는 상대가 닿을 수 있는 피어가 전체 피어(full peer)입니다. 새 플레이어는 전체 피어를 거쳐 참가하고, 전체 피어는 누가 어느 토픽에 있는지 저장하며, NAT 뒤의 플레이어에게 홈 피어가 되어 줍니다. 같은 전체 피어를 두 프로그램이 실행합니다.

- 공개 모드의 런처("다른 플레이어 연결 돕기"를 켠 노드, 기본값). 계속 켜 두는 데스크톱용입니다.
- 헤드리스 노드 `sc4j-beaconlink-node`. 데스크톱이 없는 컴퓨터(VPS, 가정용 서버)용이며, Linux 서비스로도 실행할 수 있습니다.

[프로토콜 참조](protocol.ko.md)(이 가이드 옆, 저장소의 `docs/beaconlink/`와 노드 배포본의 `docs/`에 있음)가 와이어 형식을 설명합니다.

**목차**

- [피어가 하는 일과 하지 않는 일](#피어가-하는-일과-하지-않는-일)
- [런처 공개 모드](#런처-공개-모드)
- [헤드리스 피어](#헤드리스-피어): [상태 폴더](#상태-폴더), [로그 줄](#로그-줄)
- [Linux 서비스로 실행](#linux-서비스로-실행)
- [한계](#한계)
- [카운터](#카운터)
- [개인 정보](#개인-정보)

## 피어가 하는 일과 하지 않는 일

전체 피어는:

- hello로 다른 피어를 확인하고, 전체 피어의 Kademlia 라우팅 테이블을 유지하며, 조회(`FIND_PEERS`)에 답합니다.
- 모르는 상대가 자신에게 닿는지 시험하고(다이얼백 자체 시험), 다른 피어의 자체 시험을 돕습니다.
- 자기 ID에 가장 가까운 토픽(모드, 채팅 채널, 플레이어)에 누가 있는지 저장하고, 그 토픽의 레코드를 다른 멤버와 가십으로 주고받습니다. 방 광고, 접속 상태(플레이어의 연결마다 하나), 채팅 줄, 로그아웃입니다.
- 플레이어의 홈 피어 역할을 합니다. 토큰으로 주소를 증명한 플레이어를 등록하고(플레이어 키의 연결마다, 즉 런처, 게임, 다른 컴퓨터마다 나란히 등록하며 키 하나에 최대 8개. 플레이어 스탬프가 이름을 키에 묶지 않는 등록은 거부), 자신에게 보이는 플레이어의 주소를 알려 주고(외부 STUN 서비스가 필요 없음), 플레이어의 방, 접속 상태, 채팅 줄, 로그아웃을 그 토픽에 게시하고, 플레이어가 구독한 토픽의 레코드를 보내 주고, 플레이어끼리 직접 채널을 열 수 있게 신호를 중계하고(홀 펀칭), 봉인된 귓속말을 지정된 연결로 중계합니다(내용은 읽을 수 없음). 플레이어의 "다른 곳에서 모두 로그아웃"을 가지고 있으면 그 플레이어의 이전 연결을 끊고 거부합니다(`SIGNED_OUT`).
- 핑에 답합니다(런처의 피어 확인).

피어는 자신이 참여하지 않은 게임의 트래픽을 나르지 않고, 채널 데이터그램을 중계하지 않으며, 방, 접속 상태, 채팅 줄, 신호를 위조할 수 없습니다. 모든 레코드는 작성자가 서명합니다. 피어는 이를 버리거나 늦출 수만 있으며, 그래서 플레이어는 여러 피어를 쓰고 토픽에는 여러 경로로 닿습니다.

## 런처 공개 모드

모든 런처는 열려 있는 동안, 로그인 여부와 게임 실행 여부에 관계없이 이 컴퓨터의 노드를 실행합니다. 설치본에서 처음 연 런처가 노드를 가지며(`node.lock`), 같은 설치본의 두 번째 런처는 노드를 실행하지 않다가 첫 런처가 닫힌 뒤 5분 안에 노드를 넘겨받습니다. 노드는 키 하나(`peer.key`)와 포트 하나를 유지하므로, 다른 플레이어의 피어 파일에 있는 이 노드의 항목은 계속 유효합니다.

**역할**(`node.json`): 기본값은 자동, 곧 런처 공개 모드입니다. 노드는 모든 IPv4 인터페이스에서 자기 UDP 포트(기본값 7455. 다른 프로그램이 쓰고 있으면 빈 포트를 쓰고 로그에 남김)로 수신하고, 자체 시험으로 모르는 상대가 닿는다는 것이 확인되면 바로 전체 피어가 됩니다. 그렇지 않으면 리프로 남습니다. 끔("다른 플레이어 연결 돕기" 해제): 빈 포트에서 리프 전용으로 동작합니다. 네트워크에는 여전히 참가하고 `peers.json`을 최신으로 유지합니다. UPnP나 다른 포트 매핑은 쓰지 않습니다. NAT 뒤의 노드는 직접 포트 포워딩을 해야만 전체 피어가 됩니다.

**다른 플레이어를 위해 서비스하기.** 다른 플레이어를 도울 PC에서:

1. 런처, 설정, Beacon.Link: "다른 플레이어 연결 돕기"를 켜 두고, UDP 포트(기본값 7455)를 그대로 두거나 바꾼 뒤 "적용"을 누릅니다. 노드가 모든 IPv4 인터페이스의 그 포트로 다시 시작합니다.
2. 공유기: UDP(TCP 아님) 포트 7455를 이 PC의 LAN 주소로 포워딩하고, PC에 고정 LAN 주소를 줍니다(DHCP 예약).
3. Windows Defender 방화벽: 런처의 Java 런타임에 들어오는 UDP 7455를 허용합니다(런처의 노드가 처음 수신할 때 Windows가 묻습니다).
4. 런처를 켜 둡니다. 게임은 열려 있지 않아도 됩니다.

**패널**은 네트워크 상태("연결됨", "응답하는 피어 없음", "피어 없음"), "외부 접속 가능", "외부 접속 확인 안 됨" 또는 "외부 접속 안 됨 (UDP 7455)", 로컬과 외부 IP 주소, 피어 수를 보여 줍니다. "외부 접속 가능"은 자체 시험이 통과했거나 모르는 상대의 hello가 도착했을 때 나옵니다. 맨 처음 시드는 아직 시험해 줄 상대가 없으므로, 다른 피어가 처음 연결할 때까지 "확인 안 됨"으로 보입니다.

**외부 접근 불가**: 공유기 포워딩과 방화벽 규칙을 확인합니다. 외부 IP가 100.64.0.0/10 안에 있거나 공유기의 WAN 주소와 다르면 회선이 통신사 NAT(CGNAT) 뒤에 있어 포워딩할 수 없으며, 이 PC는 전체 피어가 될 수 없습니다. 유동 IP라면 동적 DNS 이름을 시드 주소로 씁니다.

설치본의 다른 런처가 노드를 실행 중이면 패널에 그렇게 나옵니다("다른 런처에서 실행 중").

**런처 파일**(설치본의 `<install>/userdata/beaconlink/`, 따로 적지 않은 경우):

| 파일 | 내용 |
|---|---|
| `node.json` | `{"format": 1, "role": "automatic", "port": 7455}`, 역할은 `automatic` 또는 `off`. 없으면 7455에서 자동 |
| `peer.key` | 노드의 키 |
| `node.lock` | 노드를 실행하는 런처가 잡고 있는 파일 |
| `peers.json` | 학습한 피어 |
| `nodes.json` | 사용자의 노드 목록. 패널의 "피어" 버튼 |
| `<install>/launcher/seeds.json` | 함께 배포되는 시드. 사용자가 노드 목록을 저장할 때까지 `nodes.json` 대신 쓰입니다. |

## 헤드리스 피어

배포본(`sc4j-beaconlink-node`, `gradlew :sc4j-beaconlink-node:installDist`로 `sc4j-beaconlink-node/build/install/sc4j-beaconlink-node/`에 빌드)에는 `bin/sc4j-beaconlink-node`(Windows에서는 `.bat`), `lib/`(노드, 피어 라이브러리, 프로토콜, 범용 기반. 엔진, 모드, 그래픽은 없음), `docs/`(이 가이드 `operator.md`, 프로토콜 참조 `protocol.md`와 그 한국어판 `operator.ko.md`, `protocol.ko.md`), 라이선스 파일(`LICENSE`, `NOTICE`, `licenses/`)이 들어 있습니다. SC4J를 빌드하는 버전의 Java 런타임(Java 25)이 필요하며, 헤드리스 런타임으로 충분합니다.

```text
sc4j-beaconlink-node --state <dir> --bind <ip>:<port> [--peer <host>:<port>]...
```

| 옵션 | 의미 |
|---|---|
| `--state <dir>` | 노드 자신의 폴더: 키, 학습한 피어, 로그. 없으면 만듭니다. 필수. |
| `--bind <ip>:<port>` | 노드가 수신할 IP 주소와 UDP 포트. 예: `203.0.113.7:7455`. 필수이며 IP 리터럴이고 기본값은 없습니다. 노드는 운영자가 지정하지 않은 주소에서 수신하지 않습니다. IPv6는 대괄호로, `[2001:db8::1]:7455`(피어 계층은 IPv4만 라우팅합니다). |
| `--peer <host>:<port>` | 참가할 때 거칠 시드. 여러 개면 반복합니다. 호스트 이름은 시작할 때 한 번 확인합니다. 첫 시드는 이 옵션 없이 실행합니다. |
| `--help`, `-h` | 사용법 출력 |

**종료 코드**: `0` 정상 종료(`--help` 뒤에도), `1` 실행 실패(예: 주소에 바인딩할 수 없음), `2` 명령줄 오류.

노드는 런처의 노드와 같은 피어를 실행합니다. 기본 한계와 시간 값을 쓰고, 시드와 학습한 피어를 거쳐 참가합니다. 프로세스가 멈출 때까지 (Ctrl+C, SIGTERM, 서비스 중지) 실행되며, 멈출 때 학습한 피어를 `peers.json`에 병합하고 카운트를 로그에 남깁니다.

시작 스크립트의 **환경 변수**:

| 변수 | 의미 |
|---|---|
| `JAVA_OPTS`, `SC4J_BEACONLINK_NODE_OPTS` | JVM 옵션. 예: `-Xmx256m` |
| `SC4J_BEACONLINK_NODE_CACHE` | JVM 임시 파일 폴더(`<it>/tmp`). 기본값 `<install>/.cache`. 시작 스크립트가 시작할 때마다 `<it>/tmp`를 만들므로 이 폴더는 쓰기 가능해야 합니다. 설치본 자체가 읽기 전용이면(`ProtectSystem=strict` 서비스) 이 변수를 설정합니다. |

**네트워크**: 노드는 UDP 포트 하나를 씁니다. 그 포트를 바인딩한 주소로 열거나 포워딩합니다. NAT 뒤의 노드는 포트가 포워딩되어 있고 바인딩한 주소가 포워딩의 내부 주소일 때만 닿을 수 있습니다.

### 상태 폴더

| 파일 | 내용 |
|---|---|
| `identity.key` | 노드의 피어 키(Ed25519, 키 파일 `BLKF` 버전 1). 처음 시작할 때 만들어집니다. 비공개로 보관합니다. 새 키는 새 피어입니다. |
| `peers.json` | 학습한 피어: 라우팅 테이블의 전체 피어, 최대 256개. 5분마다, 그리고 멈출 때 파일에 병합합니다(설치본의 `peers.json`과 같은 공유 형식. 응답 없는 시작이 3번이거나 성공 없이 30일이 지나면 빠집니다). 다음 시작 때 인사하므로, 다시 시작할 때 시드가 필요 없습니다. |
| `logs/` | 노드의 로그 파일 |

노드는 이 폴더와 캐시 폴더 밖에는 아무것도 쓰지 않습니다.

### 로그 줄

- 시작할 때마다 `Node <key> listens at /<ip>:<port>.`와 `Peer key: <64 hexadecimal digits>`. 플레이어에게는 키가 필요 없습니다. 추가 시드에 키를 고정하는 고급 항목에 씁니다.
- `Joining the network through <n> seeds and learned peers.` 다음에, 피어 하나가 답하면 `Network state: CONNECTED`, 30초 안에 시드나 학습한 피어가 아무도 답하지 않으면 `Network state: NO_PEER_REACHABLE`. 첫 시드(`0 seeds`)는 다른 피어가 처음 올 때까지 상태를 남기지 않다가 그 뒤 `CONNECTED`를 남깁니다.
- `Reachable: an unrequested hello arrived from ...` 또는 `... probe ...`: 노드가 전체 피어입니다. `Not reachable: the probe of a dial-back to ... did not arrive.`: 모르는 상대가 닿을 수 없습니다(포트 포워딩, 방화벽, 잘못된 바인딩 주소).
- 한 시간마다 `Peer counts: handled ..., dropped ...`, 멈출 때 `Peer <key> stops: ...`([카운터](#카운터) 참고).
- `Graylisted the peer <key> at <address> for <time> ms.`([한계](#한계) 참고).

## Linux 서비스로 실행

예: Rocky Linux 9, vCPU 1개, 메모리 1 GB, 인터페이스에 공인 IP가 있는 VPS, `java-25-openjdk-headless`의 Java. 노드는 로그인할 수 없는 시스템 사용자로 systemd 서비스로 실행되고, 프로그램은 `/opt`에 읽기 전용으로, 상태는 `/var/lib`에 있습니다.

```sh
dnf install java-25-openjdk-headless tar
useradd --system --no-create-home --shell /sbin/nologin sc4j
tar -xf sc4j-beaconlink-node.tar -C /opt           # the installDist folder, as /opt/sc4j-beaconlink-node
```

`/etc/systemd/system/sc4j-node.service`:

```ini
[Unit]
Description=SC4J Beacon.Link node
After=network-online.target
Wants=network-online.target

[Service]
User=sc4j
Group=sc4j
Environment=JAVA_OPTS=-Xmx256m
Environment=SC4J_BEACONLINK_NODE_CACHE=/var/cache/sc4j-node
ExecStart=/opt/sc4j-beaconlink-node/bin/sc4j-beaconlink-node --state /var/lib/sc4j-node --bind 203.0.113.7:7455
StateDirectory=sc4j-node
CacheDirectory=sc4j-node
Restart=always
RestartSec=10
MemoryMax=512M
NoNewPrivileges=true
ProtectSystem=strict
ProtectHome=true
PrivateTmp=true

[Install]
WantedBy=multi-user.target
```

```sh
systemctl daemon-reload
systemctl enable --now sc4j-node
journalctl -u sc4j-node -f                         # or /var/lib/sc4j-node/logs/
```

- 사용자와 폴더: `StateDirectory=`와 `CacheDirectory=`가 서비스 사용자 소유의 `/var/lib/sc4j-node`와 `/var/cache/sc4j-node`를 만들고, `ProtectSystem=strict`에서도 쓰기 가능하게 유지합니다. 그 밖의 모든 곳은 `/opt`를 포함해 노드에게 읽기 전용입니다.
- 캐시 폴더: 시작 스크립트는 시작할 때마다 임시 폴더를 만듭니다. `SC4J_BEACONLINK_NODE_CACHE`가 이 폴더를 읽기 전용 `/opt` 밖으로 옮깁니다. 이 변수가 없으면 폴더는 `/opt/sc4j-beaconlink-node/.cache`인데, 읽기 전용 `/opt`에서는 만들 수 없습니다.
- 메모리: 피어의 모든 테이블에는 상한이 있습니다([한계](#한계) 절). 예에서는 힙을 `-Xmx256m`으로 둡니다. `MemoryMax=`는 프로세스 전체(힙, 메타스페이스, 스레드 스택, 버퍼)를 제한하므로 힙보다 충분히 크게 둡니다.
- 바인딩 주소: 공인 IP가 있는 인터페이스의 주소(`ip -4 addr`). NAT 뒤라면 내부 주소와 UDP 포트 포워딩.
- 방화벽: 그 포트로 들어오는 UDP를 허용합니다. 예: `firewall-cmd --permanent --add-port=7455/udp && firewall-cmd --reload` 또는 `ufw allow 7455/udp`. UDP만 씁니다. TCP 포트는 쓰지 않습니다.
- 다른 컴퓨터에서 확인: 런처의 피어 확인(설정, Beacon.Link, 피어, Ping)이 왕복 시간, 버전(`sc4j-beaconlink-peer 0.11`), 노드가 본 확인 컴퓨터의 외부 IP 주소를 보여 주거나 "응답 없음"을 보여 줍니다.

**업데이트**

1. 새 배포본을 빌드하고(`gradlew :sc4j-beaconlink-node:installDist`) 묶어서(`tar -cf sc4j-beaconlink-node.tar -C sc4j-beaconlink-node/build/install sc4j-beaconlink-node`) 서버(`/tmp`)로 복사합니다.
2. `systemctl stop sc4j-node`(노드가 학습한 피어를 쓰고 카운트를 로그에 남깁니다).
3. 프로그램을 바꿉니다: `rm -rf /opt/sc4j-beaconlink-node && tar -xf /tmp/sc4j-beaconlink-node.tar -C /opt`. 상태 폴더는 건드리지 않습니다. `/opt`에서 남겨 둘 것은 없습니다.
4. 유닛 파일이 바뀌었으면 적용하고(`systemctl daemon-reload`), `systemctl start sc4j-node`.
5. 로그를 확인합니다: 같은 `Peer key`, `listens at`, 다른 피어가 답하면 `Network state: CONNECTED`.

**노드 옮기기**: 상태 폴더를 다른 컴퓨터로 복사합니다(키가 정체성을 유지합니다). 이전 주소를 학습한 피어는 응답 없는 시작이 세 번 지나면 그 주소를 버리고 새 주소를 학습합니다.

**키 교체**: 노드를 멈추고 `identity.key`를 다른 곳으로 옮긴 뒤 시작합니다. 다른 피어는 이 노드를 새 피어로 다룹니다. 이전 항목은 응답이 없어지면 그들의 테이블에서 빠집니다. 이전 키를 고정한 추가 시드는 새 키를 고정할 때까지 잘못된 키를 보고합니다.

## 한계

모든 한계는 데이터그램 하나당 메모리나 작업의 상한입니다. 보내는 쪽이 무엇을 하든 테이블이 상한 없이 커지지 않습니다.

| 한계 | 기본값 |
|---|---|
| 데이터그램 길이 | 1200바이트. 더 긴 데이터그램은 버립니다. |
| 출발지 IP 주소별 속도(서명 작업 전에 먼저 확인) | 순간 256, 그 뒤 초당 128 |
| 보낸 쪽 키별 속도(서명과 nonce 확인 뒤) | 순간 32, 그 뒤 초당 16 |
| 보관하는 속도 버킷 | 주소 8192개와 키 8192개. 테이블이 차면 다시 가득 찬 버킷만 내보냅니다. |
| 재전송 창 | 노드 시계 기준 앞뒤 30초(밖의 타임스탬프는 버림) |
| 기억하는 nonce | 전체 65536개, 출발지 IP 주소 하나당 최대 2048개. 캐시가 차면 새 메시지를 버립니다(잊지 않음). |
| 라우팅 테이블 | k-버킷당 8개, 캐시 4개. 같은 /24는 버킷당 2개, 테이블당 같은 /24 8개와 같은 /16 16개. 테이블 밖 리프 256개 |
| 등록 | 4096개, 최대 300초 동안 허용 |
| 참여하는 토픽 | 256개. 토픽당 방 512개(철회 포함), 호스트 키당 8개와 호스트 연결당 4개, 만료는 최대 600초 뒤. 토픽당 접속 상태 256개, 플레이어당 연결 8개. 플레이어당 로그아웃 하나. 채팅 기록 32줄 |
| 작성자 하나의 레코드 | 방: 순간 8, 그 뒤 초당 1. 채팅 줄: 순간 5, 그 뒤 초당 1(5초보다 오래된 기록의 줄: 32, 그 뒤 초당 1). 접속 상태와 로그아웃: 순간 16, 그 뒤 5초당 1 |
| 구독한 클라이언트 | 토픽당 256, 구독당 90초 |
| 저장하는 알림 | 토픽 2048개, 각 멤버 64명, 10분 |
| 토픽별 메시 | 이웃 6(4에서 12), 적어도 2는 노드가 직접 고른 이웃 |
| 자체 시험의 프로브 | 동시에 4개, 그 뒤 IP 주소 하나에 초당 1 |
| 벌점 | 40점이면 한 주소의 키를 10분 동안 그레이리스트에 올립니다. 주소가 증명되었으면 키 단독과 주소 단독도 올립니다. |
| 증명되지 않은 주소에 대한 응답(핑, 첫 등록, hello, 조회 요청, 다이얼백, 토픽 요청, 거부) | 요청보다 길지 않음 |

벌점([프로토콜 참조, "수신"](protocol.ko.md#수신)): 잘못된 레코드나 위조된 증명 10점, 작성자 속도를 넘은 레코드 2점, 만료된 레코드, 메시 밖에서 온 토픽 메시지, 요청 없는 응답, 패딩 없는 요청, 좌표 이상값 1점. 증명된 주소는 IP 주소 전체(모든 포트)로 그레이리스트에 오릅니다. 통신사 NAT 하나 뒤의 많은 플레이어는 주소 하나, 그 속도, 그 그레이리스트 항목을 함께 씁니다. 이런 네트워크를 위한 노드에는 더 높은 주소 한계가 필요할 수 있지만, 아직 명령줄로 바꿀 수 없습니다.

## 카운터

버린 데이터그램에는 절대 답하지 않습니다. 노드는 버린 이유를 세고 한 시간마다, 그리고 멈출 때 로그에 남깁니다. `OVERSIZE`, `ADDRESS_RATE`, `MALFORMED`, `UNEXPECTED_REPLY`, `OWN_KEY`, `OUTSIDE_WINDOW`, `FORGED`, `KEY_RATE`, `REPLAYED`, `REPLAY_SOURCE_FULL`, `REPLAY_CACHE_FULL`, `UNPADDED`, `NOT_A_PEER`, `INVALID_PROOF`, `INVALID_ENTRY`(조회나 토픽 응답에서 쓰지 않은 항목마다), `UNREQUESTED_PROBE`, `NOT_IN_TOPIC`, `INVALID_RECORD`(버린 레코드마다), `COORDINATE_OUTLIER`(거부한 지연 표본마다), `GRAYLISTED`, `DEFECT`.

- `DEFECT`는 노드의 버그입니다. 데이터그램 처리기가 실패한 것이며(데이터그램은 잃고 노드는 계속 실행됨), `DEFECT`가 하나라도 있으면 `Failed to handle a datagram` 주변의 로그 줄과 함께 버그를 보고해 주세요.
- `OUTSIDE_WINDOW`가 많으면 서버 시계(NTP)를 확인합니다.
- `COORDINATE_OUTLIER`: 원격 호스트의 왕복 시간이 그 호스트가 주장한 지연 공간의 위치와 맞지 않았습니다(거짓말하는 호스트, 또는 네트워크가 바뀐 호스트). 몇 개는 정상입니다.
- `ADDRESS_RATE`, `KEY_RATE`, `GRAYLISTED`: 한계를 넘은 송신자. 적으면 정상이고, 많으면 폭주입니다(한계가 막아 줍니다).

다이얼백 프로브는 정상입니다. 다른 피어가 노드에게 자체 시험 전달을 부탁하면, 노드는 시험을 부탁받은 주소로 작은 `PROBE` 데이터그램을 보냅니다(동시에 최대 4개, 그 뒤 IP 주소 하나에 초당 1). 반대로 노드가 연락한 적 없는 피어가 노드 자신의 시험을 위해 프로브를 보내기도 합니다. 기다리는 시험이 없는 프로브는 `UNREQUESTED_PROBE`로 세고 무시합니다. 위조한 요청으로 노드와 제3의 피어가 요청하지 않은 주소에 각각 작은 데이터그램 하나를 보내게 할 수 있습니다. 이는 속도 제한으로 묶여 있고, 프로브가 곧 시험이므로 그대로 둡니다.

## 개인 정보

전체 피어의 IP 주소는 다른 피어의 라우팅 테이블과 토픽 멤버 목록에 있으므로 공개됩니다. 노드는 자신에게 등록한 플레이어의 주소, 표시 이름, 키, 모드 지문을 보고, 자기 토픽의 방, 접속 상태, 채팅 줄, 로그아웃(모두 서명되어 있고 원래 공개)을 보며, 중계하는 귓속말의 보낸 사람, 대상, 크기를 봅니다(내용은 보지 못함). 플레이어 사이의 신호는 보낸 사람이 서명하지만 암호화하지 않습니다. 신호에는 채널 제안(임시 키와 주소 후보)이 담깁니다. 게임 트래픽은 노드를 거치지 않습니다. 함께 플레이하는 플레이어는 서로의 주소를 봅니다. 런처는 이를 플레이어에게 짧게 알립니다. "다른 플레이어 연결 돕기"의 도움말과 정보 상자의 "Beacon.Link 개인정보" 안내입니다.
