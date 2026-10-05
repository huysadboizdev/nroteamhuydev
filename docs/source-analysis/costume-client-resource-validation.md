# Costume client and resource validation — 2026-10-04

## Result and identity

Verified Android 21 Tiến Sĩ uses existing head **1603**, body **1606**, leg **1607**. Actual x4 sprite14205/14208 has brown hair and glasses; body14211..14226 wears the white lab coat, blue shirt and red trim; leg14227..14239 wears dark trousers and blue/yellow shoes. These match item icon14241 and portrait14240. No newly generated or copied raster assets were necessary.

Rejected originally proposed1710/1713/1714: head14714/14717 is a red-haired male Saiyan; body14720 is muscular bare chest/purple sash; leg14699 is white trousers. Mapping item1710 to that set would show the wrong character. Evidence: `.analysis/costume-santa-20261004/identity-icons.png`, `doctor-icons.png`, and `heads-0.png`.

`doctor-all-frames.png` composes all33 actual `Char.CharInfo` frames using the client offsets plus DB part offsets. Identity and alignment agree across standing/running/flying/combat poses. Frame25 deliberately contains the existing filler IDs2954/2955; client moveTo selects25 for teleport transitions. This is established data, not a missing-file failure. Heads1604/1605 provide alternative race appearances; canonical1603 most directly matches item icon14241.

## Part, avatar and resource evidence

- Parsed `SRC/data/update_data/part`:2099 entries,88125 bytes; consumed completely. Selected11 parts356/357/358,554/555/556,1603/1604/1605/1606/1607 match SQL exactly, including types0/1/2, signed offsets and3/17/14 entries. Binary SHA and frame data are in `resource-audit.json` / `verified-parts.json`.
- The pre-existing dump had avatar356->4687 and554->564, but lacked1603/1604/1605 mappings. Parent implementation must add1603->14241; this audit owns no SQL/server changes.
-110 relevant IDs (two old costumes, Doctor, avatars/items/ruby/training icon16897 and fillers):440 server paths x1..x4 exist. Actual PNG decoding validated at each zoom in both client namespaces.
- These IDs have0 native `Assets/Resources/res/xN/SmallImage/SmallID.png` embeds. This client normally requests missing icons from the server, receives command-67, and uses SmallImage.imgNew / imageRaw. PNG path presence alone was not treated as gameplay proof.

## Client changes and recovery

Both Game1 and Game2:

1. Panel special-shop buy action appends `Hồng ngọc` when `typeShop==3 && iconSpec==7743`. List price already paints the supplied special icon and buySpec. Other special shop icons retain existing labels. Action3005 immediately sends buyItem type3, with no separate existing confirmation dialog; no new dialog was added.
2. Controller command-67 replaces `imageRaw.Add(id,image)` with `imageRaw[id]=image`. Actual duplicate icon packets at x2 triggered a duplicate-key exception and replaced the image with a transparent1x1 fallback. The index assignment accepts both initial and replacement packets without blanking the icon.

All4 originals were backed up under `.analysis/costume-santa-20261004/client-before`. `client-change-manifest.json` records exact paths and before/after SHA256. Original UTF8 encoding and CRLF retained. `client-changes.diff` contains the4 narrow changes. Restore each original to its recorded path to roll back.

## Fresh verification

Unity2022.3.62f2 batch validation ran against a separate clone at `.analysis/costume-santa-20261004/unity-verification`; user's existing Unity PID2992 remained running. Clone used actual current Game1/Game2 scripts and resources. Sandbox first attempt could not access licensing IPC; escalated invocation connected successfully. Initial test bootstrap had missing GameScr/debugSession UI singletons; corrected probe bootstrap without changing application behavior.

**PASS950 checks**, `unity-result.txt`, no C# compile errors:

- Both Game1/Game2 actual ItemOptionWire.ReadParam retain50000 in negotiated int32 mode and legacy unsigned16 mode; exact buffer consumption.
-252-template array accepts ID251; actual ItemOption.getOptionString renders its complete Vietnamese40%/50% explanation unchanged and47:50000 retains its number. IDs159/160 are also accepted/rendered by the same normal tooltip path; server supplies actual wording.
-110 actual server PNGs at4 zooms decode through each client's Image class (880 decodes).
- Actual Controller.onMessage(-67) ruby7743 and Doctor14205 initial packets and duplicate packets at x1..x4 decode, consume the packet, and retain a non-placeholder image.
- Missing native ruby resource path is confirmed to select dynamic server loading.

Saved red regression files `unity-result-duplicate-red.txt` and `unity-result-duplicate-game2-red.txt` demonstrate failure before each namespace's cache fix; final950-check result demonstrates both fixed. Probe source is `unity-verification/Assets/Editor/CostumeClientProbe.cs`.

## Limits requiring live acceptance

This verifies client compilation, image decode/packet cache, option integer width/text, parts/offsets and offline visual identity. It does **not** claim authenticated live gameplay, actual shop packet from the changed server, UI screenshot/word-wrap fit, purchasing/equipping inventory round-trip, clean/old login cache version refresh, all three races during connected combat, stat effects, or PvP behavior. Those remain parent/server smoke-test responsibilities. Panel currency caption was source/diff inspected and compiled, not independently screenshot-tested. No open Unity editor was closed or modified by this validation.
