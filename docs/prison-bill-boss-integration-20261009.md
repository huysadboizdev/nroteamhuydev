# Prison / Bill boss integration ? 2026-10-09

Status: user approved saved maintenance and deployment. Installed and runtime verified2026-10-09. ServerPID8604 listens14445. Hashes verified; binary count2120, new21part rows match liveSQL and old2099binarypart rows unchanged.

| Boss | Boss ID | HP | SQL parts |
|---|---:|---:|---|
| Heart | -929 | 2,000,000,000 | 2099/2100/2101 |
| Kamin | -930 | 1,000,000,000 | 2102/2103/2104 |
| Oren | -931 | 1,000,000,000 | 2105/2106/2107 |
| Beerus | -932 | 2,000,000,000 | 2108/2109/2110 |
| Whis | -933 | 2,000,000,000 | 2111/2112/2113 |
| Champa | -934 | 2,000,000,000 | 2114/2115/2116 |
| Vados | -935 | 2,000,000,000 | 2117/2118/2119 |

All seven use maps154/155 (Prison planet / Bill planet), base damage100,000 and five skills copied from Super Black Goku. Each has a single phase, exact requested name and five-minute rest. Kamin creates one owned Oren companion and wakes it through the existing same-zone spawn mechanism; factory initialization excludes a standalone Oren. Kamin waits until Oren leaves before resting, with the later companion rest timestamp. Oren cannot independently time out or appear without a living parent zone. A defeated Kamin lets Oren finish; an idle living Kamin retires both. Automated tests check companion ownership, activation, no orphan spawn and coordinated lifecycle. Actual live same-zone verification remains pending deployment.

Combat identities are separate from NPCs and the existing Whis training boss. New private part IDs avoid overwriting live SQL appearances. Source: Downloads/filefolder/res moi. Heart uses source2018?2020; Kamin2024?2026; Oren2027?2029; Beerus508?510; Whis505?507; Champa511?513; Vados530?532. Source Champa2039?2041 is unusable: its frames are transparent1x1 images. The real older Champa set is used. Whis/Vados empty body frames reuse their visible standing body pose; this ensures visibility but those poses are repeated rather than unique attack artwork.

Resources: 688 PNG files staged across x1?x4. 76 conflicting icon IDs remapped to private24000?24075 IDs. No existing PNG overwritten. Every active non-sentinel part frame checked for visible alpha and PNG decoding. SQL migration adds21part rows and7head-avatar rows transactionally; both seed SQL files include the migration. Data version13?14 forces clients to reload the new part table at next login. Existing client uses generic part/icon download protocol; no client code edits required. Three admin JSON catalogs staged with all seven entries.

Loot inherits Black Goku native gold/plain equipment/dragon-ball/event rewards plus the previously approved rare equipment policy: sealed level9?12 at5%, divine at1%, sealed divine at0.5%, independently rolled. Native divine drops are suppressed within selected reward scopes to prevent stacking the old5% rate. The same release includes the prior town/arena Cell, Black Goku, Zamasu, Cumber, Cooler, Drabura12h and Mabu14h loot changes, and fixes Drabura's once-per-life reward reset after revival. Chilled still lacks a boss class/data/factory/spawn and therefore cannot yet receive this policy.

Verification: full Java source compiles with --release17. Final candidate passes PrisonBillBossTest, PrisonBillPairTest, RequestedBossEquipmentTest (all seven new targets plus prior targets), DraburaLootRevivalTest, SealedBossEquipmentLootTest, BossAdminDropServiceTest and BossDeathLifecycleTest. SQL apply/rollback passed on an isolated clone of live part/head_avatar tables. New resource IDs fit protocol shorts; frame offsets fit bytes. Candidate data version14 confirmed in bytecode. Earlier achievement/disciple regressions passed before the final data-version-only change.

Deployment: .analysis/prison-bill/deploy.py verifies source/JAR/SQL/catalog/resource hashes, captures SQL backup, performs saved maintenance, waits for full stop, archives old20.jar with unique timestamp and SHA-256 metadata, copies only new images, applies SQL, installs catalogs/JAR and starts Java hidden with logs. Pre-install failure restores old JAR/catalogs/part binary, rolls back new SQL rows and removes only created image files. Runtime startup, all seven boss states/HP/maps and client rendering still need validation after approved deployment.

Candidate: C:\Users\Admin\Downloads\nroteam\Teamobi2026\SRC\jar-backup\20-prison-bill-candidate-20261009-105028.jar
SHA-256: 69c9f10f534e964a3b11a79b7f6cae7cf2dcc6dc0aa94404672ffbf329d19a04
Active prior SHA-256: 9186392e80a456c6a3563f4c3b30a216b6cba16a70b514d7ace636db58156dcf


Deployment completed after approval: saved maintenance succeeded. First attempt met automatic restart holding the JAR and rolled back SQL/resources/catalogs. Second attempt held restart_server.bat temporarily and installed after all JVMs stopped. Original restart script restored byte-for-byte. Startup helper was ended after it hung on an inherited pipe; applicationPID8604 remains running.

JAR backup: C:\Users\Admin\Downloads\nroteam\Teamobi2026\SRC\jar-backup\20-before-prison-bill-20261009-105521.jar
SQL backup: C:\Users\Admin\Downloads\nroteam\Teamobi2026\.analysis\prison-bill\deployment-20261009-105521/part-head-avatar-before.sql

Live verification:
```text
name=Heart id=-929 state=ACTIVE hp=2000000000 hpMax=2000000000 damage=100000 map=155 zone=7 getHead=2099 getBody=2100 getLeg=2101
name=Kamin id=-930 state=ACTIVE hp=1000000000 hpMax=1000000000 damage=100000 map=154 zone=7 getHead=2102 getBody=2103 getLeg=2104
name=Oren id=-931 state=ACTIVE hp=1000000000 hpMax=1000000000 damage=100000 map=154 zone=7 getHead=2105 getBody=2106 getLeg=2107
name=Beerus id=-932 state=ACTIVE hp=2000000000 hpMax=2000000000 damage=100000 map=154 zone=8 getHead=2108 getBody=2109 getLeg=2110
name=Whis id=-933 state=ACTIVE hp=2000000000 hpMax=2000000000 damage=100000 map=154 zone=9 getHead=2111 getBody=2112 getLeg=2113
name=Champa id=-934 state=ACTIVE hp=2000000000 hpMax=2000000000 damage=100000 map=154 zone=3 getHead=2114 getBody=2115 getLeg=2116
name=Vados id=-935 state=ACTIVE hp=2000000000 hpMax=2000000000 damage=100000 map=155 zone=4 getHead=2117 getBody=2118 getLeg=2119
count=7 unique_ids=7 pair_same_zone=true data_version=14
RUNTIME_CHECK=PASS
```

Verified21newparts/7avatars/688PNGhashes/3catalogs/dataVersion14. Existing startup map tile warnings concern78/95/101/121/125/130/134; requested maps154/155 both load. Visual client check after next login remains manual.
