# Đổi skill đệ tử bằng hồng ngọc Implementation Plan

> **For agentic workers:** Inline implementation was requested and approved by the user.

**Goal:** Add ruby shop vouchers to reroll pet skills 2 and 3 together, and replace skill 4 with Biến Khỉ.

**Architecture:** Add item 2042 for skill 2–3 using icon 5223 and item 2043 for skill 4 using icon 718. Add both to Santa's ruby shop and implement eligibility checks and consumption in the item-use flow; preserve existing item 1760 because it is sold in another shop.

**Tech Stack:** Java server and MySQL seed/migration SQL.

---

### Task 1: Add shop items

**Files:** `SRC/sql/nro1.seed.sql`, `SRC/sql/team2026.seed.sql`, new migration under `SRC/sql/migrations/`.

- [x] Define item 2042 with icon 5223 and item 2043 with icon 718.
- [x] Add item 2042 at 100 ruby and item 2043 at 2,000 ruby to the existing Santa ruby tab.

### Task 2: Apply skill changes

**Files:** `SRC/src/nro/models/services_func/UseItem.java`, `SRC/src/nro/models/player/Pet.java`.

- [x] Require learned skill 2 and skill 3 before item 2042 rerolls both; only consume after successful change.
- [x] Require learned skill 4 before item 2043 sets `Skill.BIEN_KHI`; only consume after successful change.
- [x] Avoid returning the current skill for skill 3 rerolls and notify the client after skill changes.

### Task 3: Review

- [x] Inspect all changed hunks and preserve existing unrelated workspace changes.
- [x] Do not run tests or create a JAR/APK; these were not requested.
