# Boss equipment audit and proposed release

User-approved new rates are independent per rewarded defeat: sealed level9–12 5%; divine 1%; sealed divine 0.5%. Shop pool is IDs230–281 (52 templates), confirmed in live SQL to have levels9–12; divine pool IDs555–567 confirmed level13. New seals are exactly one option34/35/36 and follow the existing restriction normalization. Equipment uses the credited player's planet; pet kills credit the master.

| Boss | Prior code loot | New requested pools |
|---|---|---|
| Town Cell / XenBoHung | Gold190, dragon balls18–20 at80%; no equipment | Sealed9–12 at5% |
| Arena Cell / SieuBoHung | Gold190; plain shop equipment30%; divine5%; dragon balls15–20 at80% | Sealed9–12 5%, divine1%, sealed divine0.5% |
| Black Goku / Super Black Goku | Gold190; plain shop equipment5%; divine5%; dragon balls15–20/992 at10% | All three requested pools |
| Zamasu | Inherits Black Goku rewards | All three requested pools |
| Cumber | Gold190; plain shop equipment5%; divine5%; dragon balls15–20/992 at10% | All three requested pools |
| Cooler | Gold190; plain shop equipment5%; divine5%; dragon balls15–20 at80% | All three requested pools |
| Drabura1/2/3 (Ma Bu12h) | Gold190; plain equipment1%; divine1%; dragon balls15?20 at10% | All three requested pools |
| Ma Bu14h / Mabu2H | Event points and task credit; no equipment | Divine1%, sealed divine0.5%; no new sealed9–12 |
| Chilled | Only CHILL_1/CHILL_2 constants; no BossData, class, factory or spawn registration | Cannot apply to a running boss; requires a separate boss implementation |

Existing non-divine rewards are retained. Native divine construction is suppressed only inside selected boss reward scopes and replaced by the approved low-rate pools; old divine rolls are not stacked with new rolls. Nothing changes on unrelated bosses, quest-support instances or the existing guaranteed hunting/Mabu tournament equipment policies. Admin replace disables default equipment; append retains it. No active custom SQL drop overrides were present during audit. Claims are guarded by the existing once-per-life reward gate. Fixed Drabura's revival path to reset that gate after successful revival so a subsequent defeat can pay again.

Verification: failing test on previous JAR confirmed missing policy; Drabura revival regression failed before the revival repair. Full source compiles with Java --release17. Deterministic rate boundaries, all52shop templates, seal counts, all10target IDs, item ownership, native gold/objective preservation, suppressed divine constructor leakage, duplicate claims, admin override modes, pet credit, and Drabura revival tests pass. Existing sealed equipment, all9hunting loot modes, admin drop and boss lifecycle tests pass. Candidate JAR also passes new equipment/revival tests, original sealed/admin tests, Zamasu and achievement reward regressions.

Candidate: `SRC/jar-backup/20-boss-equipment-candidate-20261009-102631.jar`, SHA-256 `e9cb60329841653294d4d5d1677494e4455f47c11ba0e6fa420d9838be7c5041`. Six changed class entries from four source files. Existing active Zamasu JAR SHA-256 `9186392e80a456c6a3563f4c3b30a216b6cba16a70b514d7ace636db58156dcf`. No SQL migration needed. Pending saved maintenance/backup/install/runtime check.

Automatic approval review rejected saved maintenance and active JAR replacement: explicit approval is required for this separate loot deployment. The deployment did not run; active JAR hash verified unchanged.


Superseded release: the seven Prison/Bill combat bosses and previous loot changes are bundled in the tested Prison/Bill candidate. See docs/prison-bill-boss-integration-20261009.md. Production installation still awaits direct deployment approval; active JAR remains unchanged.


Installed2026-10-09 after explicit saved-maintenance approval in the Prison/Bill release. Active JAR SHA256 69c9f10f534e964a3b11a79b7f6cae7cf2dcc6dc0aa94404672ffbf329d19a04; PID8604. Includes rare loot changes and Drabura revival fix.
