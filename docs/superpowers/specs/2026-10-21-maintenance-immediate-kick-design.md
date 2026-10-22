# Immediate Player Disconnect During Maintenance

## Goal

When the web maintenance command is accepted, disconnect all connected players immediately, then persist logout state after already admitted gameplay work has left the maintenance barrier. Maintenance may report success only after all logout saves and metadata writes complete.

## Current behavior

The coordinator closes admission, then waits for the exclusive gameplay barrier. Only after acquiring it does the runtime call `Client.drainForWebMaintenance()`. That method runs logout cleanup, player/account saves, and disconnect; therefore network disconnection is delayed by a busy gameplay operation.

## Proposed behavior

1. When the coordinator observes a durable `DRAINING` state, close gameplay admission as today.
2. Before waiting for the exclusive barrier, close the network sessions for the current online players and other active sessions. This must not detach players, run map/trade cleanup, or save snapshots.
3. Route disconnect callbacks received while maintenance admission is paused into a deferred logout collection. Do not run logout cleanup or database saves from the collector thread while a gameplay operation may still be active.
4. Continue to acquire the exclusive barrier with the existing timeout/retry behavior. Keep the command in `DRAINING` while waiting; new logins and gameplay remain blocked.
5. Under the barrier, drain pending administrative/event mutations, then run the existing checked logout cleanup and player/account saves for online and deferred sessions.
6. Publish `MAINTENANCE` only when all players are detached and all checked logout, metadata, event, and clan saves have completed. On save failure, retain the existing `FAILED` closed state and retryable in-memory logout records.

## Concurrency and failure handling

- Socket closure is idempotent and can happen before the write barrier; player state mutation and snapshot persistence cannot.
- Disconnect callbacks must be deferred while admission is paused, with logout claiming remaining idempotent so callback and maintenance snapshots cannot duplicate cleanup.
- A gameplay barrier timeout leaves the durable command `DRAINING`, sessions disconnected, and player snapshots retained in memory for the next attempt; it must not mark maintenance complete.
- A database or cleanup failure must not reopen gameplay or publish success. It remains visible as `FAILED` and pending logout records remain fenced from reconnect.
- This flow cannot persist data if the JVM or database is unavailable before the save executes.

## Scope

Change only the Java maintenance coordinator/runtime, client session logout routing, and focused Java regression tests. The web command protocol and maintenance UI stay unchanged. Do not build or deploy a JAR as part of this source change.

## Verification

Add a regression test proving that maintenance closes sockets before acquiring the gameplay barrier, while logout cleanup/player save/account save do not run until the barrier is released. Also verify retries after a barrier timeout, duplicate disconnect callback idempotency, and existing checked-save failure behavior. Run the focused maintenance and client logout tests and compile touched Java sources.
