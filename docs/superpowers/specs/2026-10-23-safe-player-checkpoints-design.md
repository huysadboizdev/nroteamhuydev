# Safe Player Checkpoints for Web Maintenance

## Goal

Keep a recent, consistent player save available when gameplay stalls and the
web maintenance command cannot immediately drain the server.

## Current behavior

The coordinator closes gameplay admission and disconnects transports, then waits
for the exclusive gameplay barrier. It saves players only after that barrier is
acquired. A gameplay thread blocked on a player or lifecycle monitor can retain
its gate lease and prevent the save phase from starting. Online player state is
otherwise persisted mainly during logout; there is no periodic full-player
checkpoint.

## Proposed behavior

1. Add a checkpoint worker on a 30-second cadence while the maintenance phase
   is `OPEN`, independent of player ticks and the web-command worker. It must
   stop starting checkpoint cycles as soon as maintenance closes admission.
2. For a checkpoint cycle, acquire the exclusive gameplay barrier before
   reading player state. This prevents concurrent gate-admitted gameplay from
   changing values while they are serialized. Save each eligible online player
   through the existing checked full-save path, then release the barrier.
3. Keep checkpoints bounded and record each player's last successful save time
   and last failure/skip reason. Pending transaction, event-cleanup, and logout
   guards remain in force. A failed or skipped player does not suppress saves
   for other players.
4. At maintenance, retain the current order: close admission/transports, then
   attempt the normal exclusive drain and final logout saves. If drain times out,
   keep the durable state `DRAINING` and admission closed. Publish the latest
   checkpoint times and identify players with stale or unavailable checkpoints
   in the existing maintenance state message. Do not mark maintenance complete
   or force-write contended RAM state.
5. When gameplay eventually drains, complete the existing checked final saves
   and publish `MAINTENANCE` only after logout, metadata, pending-operation,
   event, and clan persistence checks succeed.
6. Keep the maintenance command and web protocol unchanged. The existing web
   page already renders the state message; keep the checkpoint summary short
   enough for the current 255-character field, including a count when the list
   of affected players must be truncated.

## Consistency and operational cost

The global write barrier makes a checkpoint consistent with respect to code
that participates in `ServerMaintenanceGate`. Checkpoints use the existing
single-row player save, so the most recent successful player row remains
durable if a later cycle cannot acquire the barrier. If gameplay is already
stuck, the worker skips that cycle and reports the age of the previous save.
Progress after that save may need to be replayed.

The exclusive barrier briefly blocks new gameplay while the checkpoint cycle
saves online players. The pause lasts as long as the checked saves and database
operations take, so the worker must run at a controlled cadence, avoid overlap,
and record cycle duration. If the server cannot acquire the barrier promptly,
it skips the checkpoint instead of adding another long wait. Maintenance
remains closed while its normal drain is pending.

## Scope

Change the Java checkpoint worker, maintenance coordinator/runtime, and focused
Java regression tests. No player schema or web command protocol change is
required. No JAR/APK/EXE build or deployment is included.

## Verification

Add focused tests for checkpoint cadence and exclusive-barrier consistency,
skipping a checkpoint when the barrier is held, per-player save failure
isolation, maintenance checkpoint reporting while `DRAINING`, and eventual
completion through the existing final-save path. Keep existing barrier,
logout-retention, and pending-operation protections intact.
