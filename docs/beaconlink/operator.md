# Beacon.Link peer operator guide: headless peer and launcher public mode

**English** | [한국어](operator.ko.md)

Every SC4J is a Beacon.Link peer: the launcher runs one while it is open, and a game session runs one. The network consists of these peers; Fog of War Labs runs no central server. A peer that strangers can reach is a full peer: new players join through it, it stores who is in which topic, and it is the home peer of players behind NATs. Two programs run the same full peer:

- the launcher in public mode (its node with "Help other players connect" on, the default), for a desktop that stays on;
- the headless node `sc4j-beaconlink-node`, for a machine without a desktop (a VPS, a home server), for example as a Linux service.

The [protocol reference](protocol.md) (next to this guide, in `docs/beaconlink/` of the repository and in `docs/` of the node distribution) describes the wire.

**Contents**

- [What a peer does and never does](#what-a-peer-does-and-never-does)
- [Launcher public mode](#launcher-public-mode)
- [Headless peer](#headless-peer): [state folder](#state-folder), [log lines](#log-lines)
- [Running as a Linux service](#running-as-a-linux-service)
- [Limits](#limits)
- [Counters](#counters)
- [Privacy](#privacy)

## What a peer does and never does

A full peer:

- proves other peers with hellos, keeps a Kademlia routing table of the full peers and answers lookups (`FIND_PEERS`);
- tests whether strangers reach it (the dial-back self-test) and helps other peers test themselves;
- stores who is in the topics closest to its ID (mods, chat channels, players) and gossips the records of its topics with the other members: room advertisements, presences (one per connection of a player), chat lines, sign-outs;
- serves players as their home peer: registers them after they prove their address with a token (every connection of a player key, a launcher, a game or another computer, is registered beside the others, at most 8 per key; a registration whose name the player's stamp does not bind to the key is refused), tells them the address it sees them at (no outside STUN service is needed), publishes their rooms, presences, chat lines and sign-outs in their topics, pushes them the records of the topics they follow, relays signals between them so that they can open a direct channel (hole punching), and relays sealed whispers to the connection they name (it cannot read them); a player's "sign out everywhere else" that it holds makes it drop the player's older connections and refuse them (`SIGNED_OUT`);
- answers pings (the launcher's peer check).

A peer carries no game traffic of games it is not part of, relays no channel datagram, and cannot forge rooms, presences, chat lines or signals: every record is signed by its author. It can only drop or delay them, which is why players use several peers and topics are reached over several paths.

## Launcher public mode

Every launcher runs the computer's node while it is open, logged in or not and while games run. The first launcher of an install holds it (`node.lock`); a second launcher of the same install runs none and takes the node within 5 minutes after the first closes. The node keeps one key (`peer.key`) and one port, so other players' peers files keep a valid entry for it.

**Role** (`node.json`): automatic by default, the launcher's public mode. The node listens on its UDP port (default 7455; a free port, logged, when another program holds it) on every IPv4 interface and becomes a full peer as soon as its self-test proves that strangers reach it; otherwise it stays a leaf. Off ("Help other players connect" cleared): leaf-only on a free port; it still joins the network and keeps `peers.json` up to date. No UPnP or other port mapping: a node behind a NAT is a full peer only with a manual forward.

**Serving other players.** On the PC that should serve other players:

1. Launcher, Settings, Beacon.Link: keep "Help other players connect" on, keep or change the UDP port (default 7455), Apply. The node restarts on that port on every IPv4 interface.
2. Router: forward UDP (not TCP) port 7455 to this PC's LAN address; give the PC a fixed LAN address (a DHCP reservation).
3. Windows Defender Firewall: allow inbound UDP 7455 for the launcher's Java runtime (Windows asks when the launcher's node first listens).
4. Leave the launcher running. No game needs to be open.

**The panel** shows the network state ("Connected", "No peer answers", "No peers"), "External access OK", "External access not checked" or "No external access (UDP 7455)", the local and external IP addresses and the peer counts. "External access OK" comes when the self-test passed or a stranger's hello arrived. On the very first seed nobody can test it yet; it shows "not checked" until the first other peer connects.

**No external access**: check the router forward and the firewall rule; if the external IP is in 100.64.0.0/10, or differs from the router's WAN address, the line is behind a carrier-grade NAT and cannot be forwarded: this PC cannot be a full peer. With a dynamic IP use a dynamic DNS name as the seed address.

Another launcher of the install runs the node: the panel says so ("Node runs in another launcher").

**Launcher files** (in the install, `<install>/userdata/beaconlink/` unless named otherwise):

| File | Content |
|---|---|
| `node.json` | `{"format": 1, "role": "automatic", "port": 7455}`, role `automatic` or `off`; missing: automatic on 7455 |
| `peer.key` | the node's key |
| `node.lock` | held by the launcher that runs the node |
| `peers.json` | learned peers |
| `nodes.json` | the user's node list, the Peers button of the panel |
| `<install>/launcher/seeds.json` | the shipped seeds, which stand in for `nodes.json` until the user saves one |

## Headless peer

The distribution (`sc4j-beaconlink-node`, built with `gradlew :sc4j-beaconlink-node:installDist` into `sc4j-beaconlink-node/build/install/sc4j-beaconlink-node/`) holds `bin/sc4j-beaconlink-node` (`.bat` on Windows), `lib/` (the node, the peer library, the protocol and the generic base; no engine, no mod, no graphics), `docs/` (this guide `operator.md`, the protocol reference `protocol.md` and their Korean versions `operator.ko.md` and `protocol.ko.md`) and the licence files (`LICENSE`, `NOTICE`, `licenses/`). It needs a Java runtime of the version SC4J builds with (Java 25; a headless runtime is enough).

```text
sc4j-beaconlink-node --state <dir> --bind <ip>:<port> [--peer <host>:<port>]...
```

| Option | Meaning |
|---|---|
| `--state <dir>` | the node's own folder: key, learned peers, logs; created if absent. Required. |
| `--bind <ip>:<port>` | the IP address and UDP port the node listens on, for example `203.0.113.7:7455`. Required, an IP literal, no default: the node never listens on an address the operator did not name. IPv6 in brackets, `[2001:db8::1]:7455` (the peer layer routes IPv4 only). |
| `--peer <host>:<port>` | a seed to join through; repeat for more. Host names are resolved once at the start. The first seed runs without one. |
| `--help`, `-h` | print the usage |

**Exit codes**: `0` normal end (also after `--help`), `1` run failure (for example the address cannot be bound), `2` command line error.

The node runs the same peer as the launcher's node: the default limits and timings, joining through its seeds and its learned peers. It runs until the process is stopped (Ctrl+C, SIGTERM, a service stop); at the stop it merges its learned peers into `peers.json` and logs its counts.

**Environment variables** of the start script:

| Variable | Meaning |
|---|---|
| `JAVA_OPTS`, `SC4J_BEACONLINK_NODE_OPTS` | JVM options, for example `-Xmx256m` |
| `SC4J_BEACONLINK_NODE_CACHE` | the folder for the JVM's temporary files (`<it>/tmp`), by default `<install>/.cache`. The start script creates `<it>/tmp` at every start, so that folder must be writable; set the variable when the install itself is read-only (a service with `ProtectSystem=strict`). |

**Network**: the node uses one UDP port. Open or forward that port to the bound address. A node behind a NAT is reachable only if the port is forwarded and the bound address is the inside address of the forward.

### State folder

| File | Content |
|---|---|
| `identity.key` | the node's peer key (Ed25519, key file `BLKF` version 1), created at the first start. Keep it private; a new key is a new peer. |
| `peers.json` | the learned peers: the full peers of its routing table, at most 256, merged into the file every 5 minutes and at the stop (the same shared format as an install's `peers.json`; a peer is dropped after 3 starts without an answer or 30 days without a success); greeted at the next start, so a restart does not need a seed. |
| `logs/` | the node's log files |

The node writes nothing outside this folder and the cache folder.

### Log lines

- `Node <key> listens at /<ip>:<port>.` and `Peer key: <64 hexadecimal digits>` at every start. Players never need the key; it is for the advanced pinned-key entries of extra seeds.
- `Joining the network through <n> seeds and learned peers.`, then `Network state: CONNECTED` once a peer answers, or `Network state: NO_PEER_REACHABLE` when no seed or learned peer answered within 30 s. A first seed (`0 seeds`) logs no state until the first other peer arrives, then `CONNECTED`.
- `Reachable: an unrequested hello arrived from ...` or `... probe ...`: the node is a full peer. `Not reachable: the probe of a dial-back to ... did not arrive.`: strangers cannot reach it (port forward, firewall, wrong bind address).
- `Peer counts: handled ..., dropped ...` every hour, and `Peer <key> stops: ...` at the stop (see [Counters](#counters)).
- `Graylisted the peer <key> at <address> for <time> ms.` (see [Limits](#limits)).

## Running as a Linux service

An example: a VPS with Rocky Linux 9, 1 vCPU, 1 GB of memory, a public IP on its interface, Java from `java-25-openjdk-headless`. The node runs as a systemd service under a system user without login, the program read-only in `/opt`, its state in `/var/lib`.

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

- User and folders: `StateDirectory=` and `CacheDirectory=` create `/var/lib/sc4j-node` and `/var/cache/sc4j-node` owned by the service user and keep them writable under `ProtectSystem=strict`; everything else, `/opt` included, is read-only for the node.
- Cache folder: the start script creates its temporary folder at every start. `SC4J_BEACONLINK_NODE_CACHE` moves it out of the read-only `/opt`; without it the folder is `/opt/sc4j-beaconlink-node/.cache`, which a read-only `/opt` refuses.
- Memory: every table of the peer is bounded (section [Limits](#limits)); the example uses a heap of `-Xmx256m`. `MemoryMax=` caps the whole process (heap, metaspace, thread stacks, buffers); keep it well above the heap.
- Bind address: the address of the interface that carries the public IP (`ip -4 addr`). Behind a NAT, the inside address and a forward of the UDP port.
- Firewall: allow inbound UDP on the port, for example `firewall-cmd --permanent --add-port=7455/udp && firewall-cmd --reload` or `ufw allow 7455/udp`. Only UDP; no TCP port is used.
- Check from another machine: the launcher's peer check (Settings, Beacon.Link, Peers, Ping) shows the round trip time, the version (`sc4j-beaconlink-peer 0.11`) and the external IP address the node observed for the checking machine, or `No answer`.

**Update**

1. Build the new distribution (`gradlew :sc4j-beaconlink-node:installDist`) and pack it (`tar -cf sc4j-beaconlink-node.tar -C sc4j-beaconlink-node/build/install sc4j-beaconlink-node`); copy it to the server (`/tmp`).
2. `systemctl stop sc4j-node` (the node writes its learned peers and logs its counts).
3. Replace the program: `rm -rf /opt/sc4j-beaconlink-node && tar -xf /tmp/sc4j-beaconlink-node.tar -C /opt`. The state folder is not touched; nothing in `/opt` needs to be kept.
4. Apply unit changes if any (`systemctl daemon-reload`), then `systemctl start sc4j-node`.
5. Check the log: the same `Peer key`, `listens at`, `Network state: CONNECTED` once another peer answers.

**Moving the node** to another machine: copy the state folder (the key keeps its identity); peers that learned the old address drop it after three starts without an answer and learn the new one.

**Rotating the key**: stop the node, move `identity.key` away, start it. Other peers treat the node as a new peer; the old entry leaves their tables when it stops answering. Extra seeds that pinned the old key report the wrong key until they pin the new one.

## Limits

Every limit is a bound on memory or work per datagram; nothing a sender does grows a table without a bound.

| Limit | Default |
|---|---|
| datagram length | 1200 bytes; longer datagrams are dropped |
| rate per source IP address (checked first, before any signature work) | burst 256, then 128 per second |
| rate per sender key (after the signature and the nonce are checked) | burst 32, then 16 per second |
| rate buckets held | 8192 addresses and 8192 keys; a full table evicts only buckets that are full again |
| replay window | 30 s around the node's clock (timestamps outside are dropped) |
| remembered nonces | 65536 in all, at most 2048 of one source IP address; a full cache drops new messages (it never forgets) |
| routing table | 8 per k-bucket, 4 cached; 2 of one /24 per bucket, 8 of one /24 and 16 of one /16 per table; 256 leaves outside the table |
| registrations | 4096, granted for at most 300 s |
| topics taken part in | 256; rooms per topic 512 (withdrawals included), 8 per host key and 4 per host connection, expiry at most 600 s ahead; presences per topic 256, 8 connections per player; one sign-out per player; chat backlog 32 lines |
| records of one author | rooms: burst 8, then 1 per second; chat lines: burst 5, then 1 per second (lines of a backlog, dated over 5 s ago: 32, then 1 per second); presences and sign-outs: burst 16, then 1 per 5 s |
| subscribed clients | 256 per topic, 90 s per subscription |
| stored announcements | 2048 topics, 64 members each, 10 minutes |
| mesh per topic | 6 neighbours (4 to 12), at least 2 of the node's own choice |
| probes of the self-test | 4 at once, then 1 per second to one IP address |
| penalties | 40 points graylist a key at an address for 10 minutes; the key and the address alone too when the address is proved |
| answers to an unproved address (ping, first registration, hello, lookup request, dial-back, topic request, refusals) | never longer than the request |

Penalties ([protocol reference, "Receiving"](protocol.md#receiving)): an invalid record or a forged proof 10 points, a record over its author's rate 2, an expired record, a topic message from outside the mesh, a reply to no request, an unpadded request or a coordinate outlier 1. A proved address is graylisted as a whole IP address, all ports: many players behind one carrier-grade NAT share one address, its rate and its graylist entry. A node for such networks may need higher address limits, which are not exposed on the command line yet.

## Counters

A dropped datagram is never answered. The node counts why and logs the counts every hour and at the stop: `OVERSIZE`, `ADDRESS_RATE`, `MALFORMED`, `UNEXPECTED_REPLY`, `OWN_KEY`, `OUTSIDE_WINDOW`, `FORGED`, `KEY_RATE`, `REPLAYED`, `REPLAY_SOURCE_FULL`, `REPLAY_CACHE_FULL`, `UNPADDED`, `NOT_A_PEER`, `INVALID_PROOF`, `INVALID_ENTRY` (per unused entry of a lookup or topic answer), `UNREQUESTED_PROBE`, `NOT_IN_TOPIC`, `INVALID_RECORD` (per dropped record), `COORDINATE_OUTLIER` (per rejected latency sample), `GRAYLISTED` and `DEFECT`.

- `DEFECT` is a bug of the node: a handler failed on a datagram (the datagram is lost, the node runs on). Any `DEFECT` count is worth a bug report with the log lines around `Failed to handle a datagram`.
- `OUTSIDE_WINDOW` in numbers: check the server's clock (NTP).
- `COORDINATE_OUTLIER`: a remote host's round trip time did not fit its claimed place in the latency space (a lying host, or a host whose network changed); a few are normal.
- `ADDRESS_RATE`, `KEY_RATE`, `GRAYLISTED`: senders over their limits; normal in small numbers, a flood otherwise (the limits hold it).

Dial-back probes are normal: other peers ask the node to forward their self-test, and the node sends small `PROBE` datagrams to addresses it was asked to test (at most 4 at once, then 1 per second to one IP address); in turn peers the node never contacted send it probes for its own test. A probe that answers no waiting test is counted `UNREQUESTED_PROBE` and ignored. A forged request can make the node and a third peer each send one small datagram to an address that did not ask; this is bounded by the rates and kept, because the probe is the test.

## Privacy

A full peer's IP address is in other peers' routing tables and topic member lists: it is public. A node sees the addresses, display names, keys and mod fingerprints of the players registered at it, the rooms, presences, chat lines and sign-outs of its topics (all signed and public by design), and the sender, target and size of whispers it relays (not their text). Signals between players are signed by their sender but not encrypted: they carry the channel offers (ephemeral keys and address candidates). Game traffic never passes the node. Players see each other's addresses when they play together. The launcher states this for players in short: the help text of "Help other players connect" and the Beacon.Link privacy statement of its About box.
