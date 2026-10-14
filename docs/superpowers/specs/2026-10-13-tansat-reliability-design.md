# Tàn sát client reliability

User approved fixing the audited attack pipeline. Apply to Game1/Game2 source only, no release build or deployment. Preserve spawn coordinates as server ordinary mobs use fixed location; do not rewrite mob rendering coordinates.

Use a single automatic mob attack path: select skill through packet 34 without triggering doSelectSkill animation, then send one mob attack. Suspend generic AutoAttack while tanSat/tsPlayer owns automatic combat. Check connection, map transition, teleport, death, stun/sleep/blind/bind and charge states first. Revalidate targets against the current map and permission checks.

Movement must successfully queue before assuming a new local location is ready to attack. Keep void charMove wrapper for existing callers; add TryCharMove returning success and commit cxSend/cySend only after queuing. Auto combat moves once, waits a tick and resumes; rejected movement restores local position. No invented acknowledgement protocol.

Skill candidates must actually be usable; check null, cooldown, mana type, and Kaioken HP reserve. Keep existing client conservative Kaioken 10% reserve because special-set cost information is not reliably exposed here. This can select Dragon earlier than necessary with HP-saving sets, but avoids repeating server-rejected attacks.

Verification: execute actual modified controller C# in a stubbed in-memory harness (not a Unity/player build), test queued/rejected movement, guards, replacement targets, cooldown, selection without animation, and candidate fallback. Static integration checks cover Service movement and ModFunc arbitration. No claim of real-device or end-to-end gameplay verification.
