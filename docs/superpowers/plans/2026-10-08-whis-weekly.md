# Whis Weekly Implementation Plan

> Execute inline using executing-plans, preserving existing workspace edits.

**Goal:** Top Whis tuần bền vững, thưởng 5/4/3% tuần kế tiếp và trận không bị farm/nhận điểm sai.

**Architecture:** Policy tuần thuần Java, service SQL transaction với snapshot immutable, Whis session token riêng, NPoint dùng bonus snapshot.

**Tech Stack:** Java 17, JDBC/MariaDB InnoDB, Java main regression tests, Python build/deploy helpers.

- [x] Viết test policy, session guard, chỉ số và giao dịch tuần; chạy red trên JAR cũ.
- [x] Thêm WhisWeeklyPolicy, WhisWeeklyService, WhisFightGuard và migration bảng state/score/reward; không sửa bảng điểm sự kiện khác.
- [x] Nối admission cả TrainingService, owner-only damage và atomic thắng trong Whis; bỏ legacy full-save trong thắng.
- [x] Nối Manager startup/top updater, NPoint HP/KI/attack và NPC thông báo tuần/bonus.
- [x] Chạy tests trên compiled classes và candidate; test SQL bằng schema test riêng, rollback/failure/restart/skip-week.
- [x] Review diff phạm vi Whis, backup/migration/deploy theo AGENTS.md và kiểm chứng runtime listener/schema/top.
