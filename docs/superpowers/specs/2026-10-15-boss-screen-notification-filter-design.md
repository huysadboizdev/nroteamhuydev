# Boss screen notification filter design

## Goal

Hide only the requested boss announcements from the upper-right “Thông báo Boss” panel. Keep the VIP chat ticker visible and leave the player-facing boss lookup available. Apply the behavior to Android/APK and Web/PC clients.

## User-confirmed scope

Hide these events in the upper-right panel:

- 12-hour boss events at Oshin.
- Mabu 14-hour boss events at Oshin.
- All dungeon bosses, including Doanh trại and Khí gas.
- Ăn Trộm, Ở Dơ, Sói, and Mặt Trời.

Do not hide the entire ordinary “Boss” category or Bư 2-hour bosses as a group. “Boss nhiệm vụ” and other boss notices remain visible unless a specific event independently matches one of the selected rules above.

The filter affects only the upper-right panel. The bottom VIP ticker continues to show the original messages. Boss records remain available in the player lookup board, including Ăn Trộm, Ở Dơ, Sói, and Mặt Trời. Boss tracking, spawn/death processing, and notification collection remain active.

## Recommended design

Classify screen notices by stable semantic boss identity plus location/encounter metadata. The 12-hour and Mabu 14-hour rules require identifying the Oshin map; the dungeon rule requires an explicit dungeon/instance classification, including Doanh trại and Khí gas. The named special bosses use their semantic IDs. Do not suppress every entry in the lookup board’s `GENERAL` category.

Apply suppression only at presentation time. Keep each event and VIP ticker message, and use the same visibility predicate for drawing and pointer/touch hit testing so an invisible row cannot intercept input.

The current ticker event is plain text, while the lookup board already carries structured category and semantic boss ID fields. Add a versioned, backward-compatible classification payload for the screen overlay on both Android/APK and Web/PC; keep the displayed ticker string unchanged. If the current message flow cannot carry metadata compatibly, use an explicit shared catalog mirrored from the server catalog and verify its parity. Do not rely on fuzzy name/map substring checks.

Keep the player lookup independent from the screen filter. Ensure the four named special bosses remain available there; if necessary, change only lookup-history visibility for these entries while preserving unrelated announcement and equipment/drop policy.

## Alternatives considered

1. Turn off the global `notifBoss` switch. This would also hide unrelated notices and does not fit the selected list.
2. Match names and map labels on clients. This avoids a protocol update but is fragile across name variants and map changes, and dungeon membership is difficult to infer reliably.
3. Use structured semantic identity and location/encounter metadata (recommended). This is a small, versioned server/client change and supports precise filtering without changing ticker presentation.

## Behavior details

- Hide 12-hour boss events only when they occur at Oshin; hide Mabu 14-hour events only at Oshin.
- Hide all dungeon boss events, explicitly including Doanh trại and Khí gas.
- Hide Ăn Trộm, Ở Dơ, Sói, and Mặt Trời wherever they appear.
- Keep unrelated ordinary “Boss” events, Bư 2-hour bosses as a group, and “Boss nhiệm vụ” notices visible unless they meet a rule above.
- Preserve the manual global on/off setting; apply the new per-event filter in addition to it.
- If an event cannot be classified, fail open and show it in the panel.

## Compatibility and migration

Version the structured notification metadata or use a backward-compatible message variant. Older clients continue to display the ticker as before. New clients safely handle old messages without metadata by showing the notice in the panel. Unknown IDs or locations also remain visible.

## Validation

- Server classification tests cover Oshin vs. non-Oshin 12-hour and Mabu 14-hour events, dungeon membership (including Doanh trại and Khí gas), and the four named bosses.
- Negative cases prove unrelated ordinary bosses, Bư 2-hour bosses as a group, and “Boss nhiệm vụ” remain visible.
- Lookup tests prove the four named special bosses remain available in the player board while announcement/drop rules stay intact.
- Android/APK and Web/PC tests prove filtered events neither render nor capture input in the upper-right panel, while the ticker remains unchanged.
- Protocol tests cover old/new messages and fail-open behavior for unknown metadata.
- Manual checks on both clients verify selected and non-selected examples in the overlay, ticker, and lookup board.

## Out of scope

- Hiding VIP ticker messages.
- Changing boss spawn detection, tracking, combat, rewards, or lookup categories.
- Building or packaging APK, EXE, JAR, or WebGL output as part of this design step.
