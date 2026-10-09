# Achievement reward repair — ready for deployment

All 20 achievement missions display and grant 1,000 ruby. Existing received flags and progress remain; previously claimed achievements do not pay again. The client reward labels in Game1 and Game2 now say Hồng ngọc. The signed-short overflow at mission 18 is eliminated because 1,000 fits the existing protocol.

Claims validate completion, index, received status, player identity, pending wallet operations and wallet bounds. Wallet and received marker persist in one InnoDB transaction. Ambiguous commit outcomes freeze mutations and reconcile idempotently; logout and maintenance wait for reconciliation. A full item bag no longer prevents a currency reward.

Progress repairs cover missing/broken legacy rows, nonnegative bounded counters, retained attained power/tree levels, elapsed online time including home maps, flight distance including small steps, distinct PvP opponents and both challenger positions, gem/sale quantities, successful pea transfers, skill completion hooks, and actual self-revive at-place. Historical PvP totals remain; historical opponent IDs cannot be reconstructed, so distinct-opponent tracking applies to future wins.

Verification completed:

- Full Java source compilation with Java 21, `--release 17`.
- Unity 2022.3.62f2 Roslyn compilation using the project's existing Assembly-CSharp response file, with output redirected to `.analysis/achievement-ruby`; exit 0. Existing compiler warnings remain.
- Candidate JAR packet tests: all 20 wire rewards equal 1,000; incomplete, repeated and invalid claims blocked; counter overflow prevented.
- Candidate reward tests: all 20 claims, full bag, concurrent requests, ordinary database failure, ambiguous commit before/after persistence, logout recovery, wallet bounds, serialization, distinct opponents, online time and flight.
- Isolated MySQL database transaction tests: 40 concurrent claims pay once; identity checks and rollback; preserved wallet tail, gems and other claimed missions; malformed legacy counter does not block other missions and its parseable received marker remains. Test database removed after completion.
- Candidate hook tests: both PvP winner positions, opponent deduplication, mob categories and picked gem quantity.
- Candidate regressions: TaskProgressTest, DiscipleCompanionTest and TigerPetAuditTest passed.

Candidate: `SRC/jar-backup/20-achievement-ruby-candidate-20261009-085919.jar`.

SHA-256: `a062f2085709a8cdfff9347d63e26a507a79180401e32e3373b834c0cf540933`.

Previous live JAR SHA-256: `d4b8cfb02fee162a4835a8e8663c703579a812f92bc5e94658399dea0a667bf8`. Backup: `SRC/jar-backup/20-before-achievement-ruby-20261009-090635.jar`. Installed JAR SHA-256 matches the candidate above.

Migration: `SRC/sql/migrations/20261009_achievement_ruby_rewards.sql`; both sanitized SQL seeds also updated. No live player progress reset or retroactive payout.

Deployment authorized by user and completed at 09:06 Bangkok. Application saved maintenance log reports SUCCESSFULLY MAINTENANCE. Old server was confirmed stopped before JAR replacement. Achievement template snapshot saved in `.analysis/achievement-ruby/achievement-template-before.sql`; migration applied with no player reset. Restarted server PID 23100 listens on port 14445. Live template query returned 20 rows, minimum/maximum reward both 1000. Installed JAR reward/hook/companion regression tests passed. Startup has the same pre-existing missing-map-tile messages as before maintenance.

Game interaction with an actual logged-in character has not been tested. Compiler, packet, fixture and isolated database results do not replace a post-deployment game check.
