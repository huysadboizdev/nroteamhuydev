# Bill and Prison planet boss integration

User scope: Kamin/Oren together, each1bHP; Heart, Beerus, battle Whis, Champa, Vados each2bHP. All use SuperBlackGoku skills and100kbase damage, BlackGoku reward behavior plus approved5%sealed9–12/1%divine/0.5%sealeddivine policy. Maps154/155 only. Keep existing NPCs, training Whis and old appearance IDs untouched. Implement seven separate identities; Kamin owns an APPEAR_WITH_ANOTHER Oren companion, remaining bosses standalone. Spawn one Kamin pair and one of each standalone. Five-minute rest. Coordinate pair departure/respawn so Oren cannot remain behind or independently respawn.

Asset integration: copy original3/17/14part triples for Heart2018, Kamin2024, Oren2027, Beerus508, Whis505, Champa2039, Vados530. Allocate dedicated partIDs2099–2119 after verifying live max2098. Reuse identical existing PNGs; remap conflicting iconIDs into unused signed-short range, preserving existing PNGs. Validate PNG decoding forx1–x4 and every part frame. Fill angel animation slots whose source image is the empty2955 placeholder with its existing visible body pose; keep the trailing sentinel intact. Register head_avatar for all new heads; avatars from original portraits. Generate transactional SQL migration and rollback, deterministic manifest, staged assets and sanitized seed additions. Do not replace original source resource folder.

- [ ] Generate staged asset manifest and SQL; validate all frames, collisions and zoom variants.
- [ ] Write failing factory/template/group/map/loot/asset tests on installedJAR.
- [ ] Implement catalog-based boss subclass and factory/spawn integration, group lifecycle guards, rare equipment coverage.
- [ ] Fullcompile and deterministic tests plus loot/lifecycle/achievement/companion regressions; overlay affected classes only onto activeJAR.
- [ ] Prepare reviewed deployment: database snapshot, uniqueJAR and resource/catalog backup, saved maintenance, SQL+assets install, restart, verify live boss pair/HP/maps/paths. Existing pending loot release is included in this release.
