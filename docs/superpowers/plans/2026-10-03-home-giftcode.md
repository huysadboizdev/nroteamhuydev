# Home giftcode implementation plan

> **For agentic workers:** Use executing-plans inline; no delegation needed. Track the steps below.

**Goal:** Open the existing giftcode form from Gohan, Moori and Paragus home menus.

**Architecture:** Add select 2 to the shared OngGohan handler using Input.createFormGiftCode; inherited by both other NPCs. Existing Input/GiftCodeService own submission and validation.

**Tech Stack:** Java 17, JDK 21, offline Java tests, existing Python release builder.

- [x] Update `SRC/test/nro/models/npc_list/HomeNpcSupportTest.java`: expect four menu choices, giftcode before close; capture -125 form packets; exercise actual Input form creation and submission forwarding at all three homes with GiftCodeService substituted at the DB boundary. Check close/wrong map/wrong menu/outside houses do not open a form. Run new tests against old handlers and confirm assertion failure.
- [x] In `SRC/src/nro/models/npc_list/OngGohan.java`, import `nro.models.services_func.Input`; add menu label `"Nhập giftcode"` before `"Đóng"`; add `case 2 -> Input.gI().createFormGiftCode(player);` in the guarded switch.
- [x] Add giftcode mode to `.analysis/verify_gold_economy.py` and `.analysis/build_release_jar.py` HomeNpcSupportTest mode lists. Run with JDK bin on PATH: `python .analysis/build_release_jar.py`. Require compile exit 0, archive check PASS and packaged tests PASS.
- [x] Reuse verified deployment logic with distinct giftcode backup/metadata names. Stop any running server through `bt`, wait for saved shutdown, verify stopped, backup old JAR and verify both hashes; install new JAR and restart launcher hidden. Record metadata and check listening port 14445 plus startup console.
- [x] Record result and real-client testing limitations in docs. Do not change real player balances to test giftcodes.
