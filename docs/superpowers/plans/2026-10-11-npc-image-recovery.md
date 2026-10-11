# NPC image recovery implementation plan

Goal: recover missing Duong Tang sprites without clearing data, in both native PC and web clients.

Evidence: NPC49 uses parts467/468/469 with icons4521–4542, loaded through SmallImage. createImage installs an empty image and makes one remote request; update never retries. ReceiveImage(null) caches a permanent empty image. User confirms intermittent missing NPC visuals in native PC too.

- [x] Compile actual SmallImage source with controlled clock/network/image shells; reproduce dropped response and invalid response.
- [x] Add bounded retries with backoff, remove pending requests on success, preserve valid cached images after late failures, pause retries offline.
- [x] Apply the same fix to both tabs in original PRJ_2Tab_550K and isolated PRJ_2Tab_Web; back up the original changed files.
- [x] Verify valid/local images, recovery, offline pause, late success, and request rate. Review implementation.
- [x] Build WebGL and an isolated Windows client; leave server JAR/database unchanged.

This addresses a reproducible image-loader failure. It does not claim a real account/map reproduction; verify NPC visuals on the user's affected live session after installing the fixed client.

Follow-up diagnosis identified a second, independent server cause: permanent NPC49 was listed as a lunar-new-year NPC, so seasonal visibility sync hid the guide when that event was disabled. EventCatalog no longer gives NPC49 seasonal ownership. PermanentGuideVisibilityTest verifies menu availability and visibility on maps 0, 7, 14 and 124 with events OFF and ON, and preserves ownership of other seasonal NPCs. After the server update, the user confirmed the guide appeared on reconnect without clearing data.

The separate browser project is maintained in https://github.com/huysadboizdev/webgamenrohuydev and excluded from this server repository. Native client recovery changes are maintained in https://github.com/huysadboizdev/modnrohuydev. Publication checks compile all 672 server Java sources and run the relevant event/NPC regressions; native client checks compile the actual changed C# code and build from an isolated Unity project.
