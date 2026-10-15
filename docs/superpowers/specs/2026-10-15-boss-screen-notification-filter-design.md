# Boss screen notification filter design

## Goal

Hide selected boss announcements from the upper-right on-screen “Thông báo Boss” panel while keeping the bottom VIP chat ticker and the player-facing boss lookup board usable. Apply the behavior to Android/APK and Web/PC clients.

## User-confirmed scope

The upper-right panel should hide announcements for:

- 12-hour bosses and Bư 2-hour bosses.
- Doanh trại and Khí gas bosses.
- Every entry in the lookup board’s ordinary “Boss” category, including all bosses that appear on normal maps. Do not include the separate “Boss nhiệm vụ” category.
- Ăn Trộm, Ở Dơ, and Mặt Trời.

Only this panel is hidden. The VIP ticker continues to display every original message. Boss data and events remain available to the player lookup board, including the three named special bosses. Existing boss tracking, spawn/death processing, and notification collection remain active.

## Recommended design

Use the existing structured boss identity and category from the boss notice catalog to decide whether an event belongs in the screen-only suppression set. The suppression decision must be presentation-only: retain each event and its VIP ticker message, and apply the same predicate when drawing the upper-right panel and hit-testing its rows so an invisible notice cannot intercept a tap.

The current ticker event is a plain text `ServerNotify` string, while the lookup board already has structured category and semantic boss ID fields. Extend the client-facing boss notification data with a stable classification (category and semantic boss ID, or an equivalent versioned field) and consume it on both Android/APK and Web/PC. Keep the visible ticker text byte-for-byte unchanged. If protocol compatibility makes a structured event impractical, use a shared explicit client catalog mirrored from the server catalog and verify parity; do not rely on fuzzy substring checks for boss names or map names.

On the server, separate the history visibility rule from announcement/loot policy. Remove only the Ăn Trộm, Ở Dơ, and Mặt Trời exclusions from lookup history, preserving their existing announcement and equipment/drop exclusions. Keep the category split between ordinary “Boss” and “Boss nhiệm vụ” unchanged.

## Alternatives considered

1. Turn off the global `notifBoss` switch. This is simple but hides unrelated notices and does not implement the requested category selection.
2. Match display names and map labels on clients. This avoids a protocol update but is fragile when names, accents, forms, or maps change; new ordinary bosses can leak into the panel.
3. Use structured semantic classification for the screen panel (recommended). This adds a small, versioned client/server integration but follows the same category identity as the lookup board and avoids changing ticker presentation.

## Behavior details

- The filter applies only to the upper-right active boss overlay and its pointer/touch hit testing.
- The bottom VIP ticker continues showing the unmodified incoming message.
- The player lookup board continues showing categories and events as before, with Ăn Trộm, Ở Dơ, and Mặt Trời restored to lookup history.
- “Boss” means category `GENERAL`; “Boss nhiệm vụ” means category `QUEST` and remains visible in the upper-right panel unless it is independently part of one of the explicitly selected special groups.
- Group rules for 12-hour, Bư 2-hour, Doanh trại, and Khí gas should use semantic IDs/catalog metadata rather than display text.
- Preserve the existing manual global on/off setting for the panel; the new filter is applied in addition to that setting.

## Compatibility and migration

If classification is added to the wire format, version the notification event or add a backward-compatible message variant. Older clients should continue rendering the existing ticker text and must not crash on the new data. New clients receiving an unclassified event should leave it visible in the panel rather than accidentally suppressing an unrelated boss.

## Validation

- Server policy tests: selected regular categories and named special bosses are excluded from screen presentation, while quest-category bosses remain visible unless separately selected.
- History policy tests: Ăn Trộm, Ở Dơ, and Mặt Trời appear in the player lookup history; announcement/drop policy behavior remains unchanged.
- Client tests for Android/APK and Web/PC: filtered events do not render or capture input in the upper-right panel; non-filtered events render normally; VIP ticker text is unchanged.
- Protocol tests: old and new notification forms parse safely; unknown category/ID fails open to visible.
- Manual check on both clients: verify representative 12-hour, Bư 2-hour, Doanh trại, Khí gas, ordinary “Boss”, “Boss nhiệm vụ”, Ăn Trộm, Ở Dơ, and Mặt Trời events across overlay, ticker, and lookup board.

## Out of scope

- Hiding VIP ticker messages.
- Changing boss spawn detection, tracking, combat, rewards, or lookup categories.
- Building or packaging APK, EXE, JAR, or WebGL output as part of this design step.
