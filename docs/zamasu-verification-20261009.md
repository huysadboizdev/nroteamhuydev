# Zamasu future boss — verified candidate

Added independent Zamasu boss ID -928, with one phase and a five-minute rest. Automatically creates two instances, using Super Black Goku's map list: 102, 92, 93, 94, 96, 97, 98, 99, 100. Appearance parts 903/904/905 use the existing icon resources verified in all four zoom directories. The display name remains exactly Zamasu.

HP 2,000,000,000; damage 100,000. All skill IDs, levels and cooldowns copy Super Black Goku: Thai Duong Ha San level 7/30,000 ms, Tai Tao Nang Luong level 7/300,000 ms, Khien Nang Luong level 7/300,000 ms, Kamejoko level 7/100 ms, Galick level 7/100 ms. Template arrays are independently cloned. The original Black Goku remains a two-phase boss with its original identity. Zamasu inherits Black Goku attack, ordinary reward and map departure behavior.

Server and web boss management catalogs now include Zamasu and its single phase; original catalog backups are in `.analysis/zamasu`. No item/part SQL migration is needed because appearance resources already exist.

Validation: watched the new test fail on the previous JAR with NoSuchFieldException ZAMASU; full Java compilation then succeeded; ZamasuTest passes against compiled source and candidate JAR, verifying exact stats/skills/maps/appearance, factory, phase identity, rest and independent arrays. BossDeathLifecycleTest, BossFullLifecycleTest and BossNoticeCategoryTest pass. Catalog JSON validated; admin bosses PHP syntax check passed.

Candidate `SRC/jar-backup/20-zamasu-candidate-20261009-101015.jar` changes five classes. SHA-256 `9186392e80a456c6a3563f4c3b30a216b6cba16a70b514d7ace636db58156dcf`.

Deployed after user continuation approval, using saved application maintenance. Backup: `SRC/jar-backup/20-before-zamasu-20261009-101246.jar`; both old and installed hashes verified. Server PID 16068 listening on port 14445. Read-only in-JVM probe confirms two ACTIVE Zamasu instances on maps 94 and 93, both HP/max HP 2,000,000,000, base damage 100,000 and parts 903/904/905. Installed JAR regression passed. Runtime probe: `.analysis/zamasu/runtime.txt`.
