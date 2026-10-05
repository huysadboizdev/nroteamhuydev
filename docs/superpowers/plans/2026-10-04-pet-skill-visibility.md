# Pet Skill Visibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Do not delegate unless authorized by the applicable workflow.

**Goal:** Đệ thường/Mabu chỉ hiện 4 skill, cuối cùng ở 20 tỉ; Uub/Kid Beer/Jiren hiện 5 skill, skill cuối chỉ mở ở 60 tỉ.

**Architecture:** Server quản lý loại đệ, số skill hiển thị và điều kiện mở/dùng bằng một bộ quy tắc chung. Gói tin -107 gửi đúng số phần tử, client đọc và vẽ theo số này. Giữ dữ liệu skill cũ trong SQL, không xóa ô đã lưu chỉ để sửa giao diện.

**Tech Stack:** Java 17 server, MySQL/MariaDB, Unity C# Game1/Game2.

## Kết quả đối chiếu

- `SRC/src/nro/models/services/Service.java:1613`: gói -107 cố định count=5 nhưng vòng lặp chạy theo toàn bộ danh sách skill; pet mới có thể có 7 ô. Đây là nguyên nhân dòng thứ 5 của đệ thường/Mabu và lệch count/payload.
- `SRC/src/nro/models/player/Pet.java:1052`: type 2/3/4 tự mở skill 5 ở 40 tỉ. `openSkill5()` chưa tự chặn theo loại đệ và sức mạnh; `useSkill5()` cần áp dụng cùng quy tắc.
- `PetService.java`: type 0 thường, 1 Mabu, 2 Uub, 3 Kid Beer, 4 Jiren; pet mới tạo 7 ô skill. Không nhầm với NewPet/pet đi theo từ trang bị hoặc mục Đệ tử 2 riêng của client.
- `MrBlue.java`: dữ liệu tải lại chỉ bù 5 ô cho type 3/4, chưa bù tương tự cho Uub type 2. `PlayerDAO.java` lưu toàn bộ danh sách skill vào JSON trong cột `player.pet`.
- SQL thực tế: cột `player.pet` là TEXT; hiện có 2 đệ type 0, mỗi đệ lưu 7 ô. Cả 10 cấp của template 24/25/26 đều có power_require=60.000.000.000. Chuỗi skill dùng định dạng cũ được Manager chuẩn hóa khi đọc; JSON_VALID=0 của chuỗi thô không được coi là lý do thay toàn bộ dữ liệu.
- `SRC/sql/nro1.sql` và `team2026.sql` dùng cùng cấu trúc lưu pet; template skill cuối trong dump đã có mốc 60 tỉ.
- Game1/Game2 `Controller.cs` đọc count rồi tạo `arrPetSkill` đúng kích thước; `Panel.cs` tính danh sách và vẽ theo `arrPetSkill.Length`. Không cần truyền thêm loại pet để client tự đoán.
- Bằng chứng SQL chỉ đọc: `.analysis/pet-skill-plan/sql-audit.txt`. Chưa sửa mã chạy, dữ liệu tài khoản hoặc JAR trong bước lập kế hoạch.

## Quy tắc đề xuất

| Loại đệ | Số skill | Mốc mở |
|---|---:|---|
| Thường, Mabu | 4 | Có sẵn; 150 triệu; 1,5 tỉ; 20 tỉ |
| Uub, Kid Beer, Jiren | 5 | Các mốc trên; skill cuối 60 tỉ |

Đệ thường/Mabu không có dòng thứ 5 hoặc dòng “Không thể mở kỹ năng này”. Đệ nâng cấp chưa đủ 60 tỉ hiện đúng một dòng khóa ở mốc 60 tỉ. Đủ 60 tỉ mở skill theo hành tinh, giữ loại và chỉ số skill hiện có. Không đổi điều kiện chuyển Mabu sang Uub hoặc tự cấp các tầng mở giới hạn sức mạnh trả phí.

Dữ liệu cũ: giữ các ô đã lưu, kể cả ô dư. Nếu đệ nâng cấp đã được mở skill 5 ở 40 tỉ do logic cũ, đề xuất giữ skill trong SQL nhưng chỉ cho hiện trạng thái đã mở và sử dụng khi đủ 60 tỉ; dưới mốc đó hiện dòng khóa 60 tỉ. Quy tắc này cần được người dùng duyệt cùng kế hoạch.

## Task 1: Kiểm thử hồi quy

**Files:** tạo `SRC/test/nro/models/player/PetSkillRulesTest.java`, cập nhật hai runner `.analysis/verify_gold_economy.py`, `.analysis/build_release_jar.py`; thêm kiểm tra client trong `.analysis/pet-skill-plan`.

- [x] Tái hiện type 0/1 nhận count 5; xác nhận packet cần count 4 và đúng 4 phần tử.
- [x] Type 2/3/4 cần count 5; placeholder cuối phải ghi 60 tỉ.
- [x] 40 tỉ, 59.999.999.999 không mở/dùng skill 5; 60.000.000.000 mở; 60.000.000.001 vẫn dùng đúng skill.
- [x] Test cả ba hành tinh và cả gọi trực tiếp `openSkill5()` để tránh bỏ qua điều kiện.
- [x] Test skill cũ, danh sách 4/5/7 ô và dữ liệu ngắn khi load/relogin; giữ skill và chỉ số đã học.
- [x] Hai client nhận 4 → 5 → 4 skill: không còn dòng dư, cuộn/chọn không vượt danh sách.

## Task 2: Quy tắc server và packet

**Files:** tạo `SRC/src/nro/models/player/PetSkillRules.java`; sửa `Pet.java`, `Service.java`, `PetService.java`, `database/MrBlue.java`.

- [x] Gom count theo type và mốc skill cuối 60 tỉ thành quy tắc dùng chung.
- [x] Kiểm tra quyền mở skill cuối trong cả luồng tự cập nhật và hàm mở trực tiếp.
- [x] Kiểm tra quyền dùng skill cuối, xử lý danh sách ngắn an toàn; không truy cập ô 5 của đệ thường/Mabu.
- [x] Gói -107 ghi đúng count và chỉ ghi đúng số skill đó; placeholder theo vị trí, không ghi các ô dư vào payload.
- [x] Uub/Kid Beer/Jiren được bù đủ ô khóa khi tạo/load; giữ tương thích JSON cũ, không xóa SQL hàng loạt.

## Task 3: SQL và client

**Files:** SQL audit; Game1/Game2 `Controller.cs`, `Panel.cs` chỉ sửa nếu kiểm thử phát hiện giả định cố định hoặc lỗi cuộn/chọn.

- [x] Đối chiếu lại template 24/25/26 trong database và dump. Mốc đã đúng 60 tỉ thì không viết migration sửa danh mục chung của người chơi.
- [x] Xác nhận loader/save giữ dữ liệu cũ; không thu hồi skill bằng UPDATE/xóa mảng.
- [x] Xác nhận client thay mới mảng theo count, bỏ hoàn toàn dòng thứ 5 cho thường/Mabu và hiện khóa 60 tỉ cho đệ nâng cấp ở cả Game1/Game2.

## Task 4: Kiểm tra và triển khai sau khi duyệt

- [x] Chạy kiểm thử liên quan, toàn bộ server, JAR đóng gói và biên dịch Unity.
- [x] Kiểm tra trên client đệ thường, Mabu và đệ nâng cấp, trước/sau mốc 60 tỉ; không tăng sức mạnh/xóa skill tài khoản thật để thử.
- [x] Bảo trì có lưu dữ liệu, xác nhận server dừng, backup JAR cũ vào `SRC/jar-backup` với timestamp riêng và SHA-256.
- [x] Cài JAR mới, đối chiếu hash, khởi động lại và kiểm tra packet/server thực tế. Gom JAR ứng dụng dư theo AGENTS.md.
- [x] Người dùng Stop Play → Play lại trong Unity để nạp mã client nếu có thay đổi.

## Verification and deployment evidence

- Source regressions: 145/145; packaged JAR regressions: 145/145.
- Game1/Game2 real packet decoding, list sizing and paint checks: 4/4; whole Unity C# compilation exit 0. Client production code required no changes.
- Client verification used real server packet fixtures and extracted client methods with UI stubs; no live player account was modified to simulate power or skill states. Reopening the pet panel refreshes the packet; client code reload is unnecessary for this change.
- Spec review passed after fixing learned surplus slots blocking fifth unlock. Missing fifth preserves current skill selection.
- SQL audit read-only: existing templates already require 60 billion; no migration.
- Graceful maintenance saved data and stopped the server before JAR replacement. Timestamped backup and new JAR SHA-256 verified.
- Running JVM PID 35672 and TCP 14445 verified. Live isolated pet profiles 0..5 confirm four/five slots and exact 60-billion threshold.
- Evidence: `.analysis/pet-skill-plan/verification.json`, `deployment.json`, `review.txt`, `build.log`, `sql-audit.txt`, `client-tests/test.log`.
