# Boss Admin Live — kế hoạch đề xuất

Trạng thái: chờ người dùng duyệt; chỉ khảo sát và viết kế hoạch, chưa sửa chức năng.

## Mục tiêu

Trang `C:/xampp/htdocs/webgoc2026/admin/bosses.php` hiển thị toàn bộ loại boss, kể cả không có override, boss chưa xuất hiện/bị tắt và boss có cơ chế riêng. Phân loại nhiệm vụ, bình thường, giới hạn sát thương lên người chơi. Gom các bản sao cùng loại thành một dòng quản lý. Cấu hình HP/sức đánh/giới hạn dame/rơi đồ được nạp khi server đang chạy; web chỉ thông báo áp dụng thành công sau ACK của server.

## Hiện trạng đã kiểm tra

- `bosses.php` lấy bảng dưới từ `boss_live_override`, nên các boss chưa cấu hình không hiện. Select dùng `boss_ids.json`, không phải catalog đầy đủ từ runtime.
- Web validator, SQL và `BossLiveReload.Stats` giới hạn HP/damage INT. `BossData` HP int[], NPoint/protocol cần được đối chiếu trước mọi mở rộng miền số.
- `BossLiveReload.applyAtSpawn` chỉ áp dụng khi init giai đoạn/xuất hiện. `LiveSyncCatalogCommands` đã có RELOAD_BOSS, stage trước publish và luồng queue/receipt có thể mở rộng.
- `MiniBossIdentity` cấp ID động mỗi cá thể; Ăn Trộm/Mặt Trời có reset riêng, không dùng ID runtime để lưu cấu hình loại.
- `BossNoticeCatalog` có phân loại nhiệm vụ theo semantic ID và form, có thể tái sử dụng sau đối chiếu TaskService.
- `HuntingTrioDamage` đánh theo 5% HP tối đa mục tiêu; một số boss sự kiện tính theo HP mục tiêu. Đây khác giới hạn người chơi đánh vào boss (ví dụ Broly/Yardart).
- Rơi đồ phân tán trong reward()/die() các lớp và MiniBossPinkCapsuleDrop; phải nối tất cả các đường thưởng để đổi tỷ lệ không rơi trùng.

## Thiết kế

### Danh mục chuẩn

Tạo `BossAdminCatalog` export SQL/JSON cho web từ danh sách BossID + factory/manager + lớp mini/luyện tập/sự kiện + dữ liệu giai đoạn. Không tạo boss chỉ để liệt kê vì constructor có tác dụng phụ. Mỗi entry có boss_key ổn định, semantic ID, variant, tên Việt Nam, nhãn phân loại, các phase thật, map, chỉ số mặc định/cơ chế tính, khả năng cấu hình. Lưu số bản đang sống như thông tin phụ; không dùng spawn ID làm khóa config.

Gom bản sao Ăn Trộm/Mặt Trời thành một loại, config áp dụng toàn bộ cá thể của loại đó. Boss cùng tên nhưng khác dungeon, cơ chế hoặc chuỗi biến hình là variant riêng, không gom bằng xóa số ở tên. Những phase có khác hành vi/phần thưởng phải giữ riêng. Không thay số lượng spawn thực tế trong game.

Danh sách gồm Tất cả, Nhiệm vụ, Bình thường, Giới hạn dame lên người. Nhãn nhiệm vụ và giới hạn dame độc lập: một boss có hai đặc tính vẫn lọc được ở hai nhóm, chỉ một entry trong Tất cả. Bình thường là không có hai đặc tính trên. Nhãn phụ: mini, sự kiện, phụ bản, luyện tập, hỗ trợ nhiệm vụ, đang tắt.

### Giao diện

Thay form chọn ID bằng bảng tìm kiếm/lọc/phân trang. Cột: Boss, nhóm, giai đoạn/map, HP hiện hành/mặc định, sức đánh hoặc công thức, giới hạn dame lên người, số cá thể sống, cấu hình/version và trạng thái áp dụng, Sửa.

Modal Sửa: Thông tin, Chỉ số, Rơi đồ. Có chọn phase thật; Mặc định/Tùy chỉnh; HP, sức đánh, giới hạn tuyệt đối hoặc %HP mục tiêu. Phân biệt rõ sát thương boss gây lên người và trần sát thương boss nhận. Boss tính theo người chơi cho phép chỉnh tham số/công thức được hỗ trợ, không giả vờ HP cố định đã áp dụng khi lớp boss ghi đè lại.

Rơi đồ: item ID/tìm tên/icon, tỉ lệ %, số lượng min/max, options ID/param, đối tượng nhận và điều kiện đang có. Tách roll độc lập và nhóm chọn một món theo trọng số. Nêu xác suất nhóm và xác suất có điều kiện của món để tránh cộng nhầm. 0%=tắt, 100%=chắc chắn nếu đủ điều kiện; giữ đồ/nhiệm vụ bắt buộc tách khỏi nhóm tùy chỉnh.

### Dữ liệu và Live Sync

Thêm catalog, cấu hình loại/phase có version, drop rules/groups, lịch sử thay đổi. Nullable là dùng mặc định; không tự seed override làm mất cơ chế gốc. Đối chiếu dữ liệu cũ theo ID+phase, không xóa config cũ trước khi migration kiểm chứng.

Một lần lưu tạo revision và request ID duy nhất trong giao dịch cùng command. Server stage/validate toàn bộ bản cấu hình, nhận thành công mới thay immutable snapshot trong RAM. Không query SQL trong từng hit hoặc từng roll. Revision cũ không đè revision mới. ACK hiển thị Đang chờ/Đã áp dụng/Lỗi và phiên bản đã nạp; lỗi giữ cấu hình runtime trước đó, không báo thành công giả.

Hai chế độ chỉ số: Lần xuất hiện tiếp theo (mặc định) và Áp dụng boss đang sống (chọn rõ trong UI). Chế độ sống giữ tỷ lệ HP còn lại, không hồi đầy, không hồi sinh boss chết, không đổi map/thứ tự phase/lịch hồi sinh hay gọi reward/die. Tiến hành ở ranh giới tick với trạng thái/khóa boss hiện có, kiểm tra lại life generation trước cập nhật. Lớp có reset/dynamic stats dùng adapter riêng để không ghi đè ngược config.

Giới hạn sát thương áp dụng từ hit tiếp theo, ở điểm tính thiệt hại phù hợp sau xác định trúng/né/khiên. Damage cố định, %HP và trần dame là ba cơ chế khác nhau; có policy riêng. Không thay damage chung để tính %HP của từng người. Độc/bỏng và tác động trì hoãn có quy tắc rõ ràng riêng.

Drop dùng một snapshot/revision cho một lần chết; giữ kill credit, chủ đồ, nhiệm vụ/thành tích, điều kiện và hiệu ứng cũ. Override một quy tắc thay thế chính quy tắc đó, không chạy reward cũ lẫn mới sinh đồ trùng. Áp dụng từ lượt thưởng kế tiếp; không sửa vật phẩm đã rơi. Death claim chống gọi lại reward, pet kill và reset cá thể.

Miền số được validate từ PHP→SQL→Java→network. Giai đoạn đầu giữ miền INT đang được game hỗ trợ; không chỉ đổi SQL BIGINT rồi quảng cáo HP64bit. Nếu người dùng cần HP hơn 2.147.483.647, bổ sung hạng mục riêng nâng toàn chuỗi/client với bộ test overflow.

## Các chặng triển khai và nghiệm thu

- [ ] 1. Audit đầy đủ factories/managers/TaskService/reward/damage/reset. Xuất ma trận từng loại: key/variant/phase, nhiệm vụ, outgoing policy, incoming policy, drops và nguồn code. Đối chiếu mọi boss đều có entry, không âm thầm bỏ lớp cơ chế riêng.
- [ ] 2. Catalog + migration + giao diện chỉ đọc. Kiểm tra Tất cả có cả boss chưa override; một dòng Ăn Trộm/Mặt Trời; variant/giai đoạn không bị gom nhầm; lọc đủ các nhóm.
- [ ] 3. Chỉ số và outgoing damage Live Sync. Test default/override/reset, alive ratio/nextspawn, dynamic stats, dead boss, nhiều cá thể, reload concurrency/out-of-order/SQL failure, max numeric boundary.
- [ ] 4. Chuẩn hóa reward theo quy tắc, migrate từng họ boss với baseline so sánh. Test 0/100%, fractional rates, conditional/group drops, options/quantity, pet credit, quest/badge preservation, concurrent/double die; chưa migrate xong không coi feature complete.
- [ ] 5. Modal sửa + quyền admin/CSRF + request retry/ACK/revision + audit/rollback. Test danh mục tìm kiếm, lưu/reset/rollback, server offline không báo applied, hai admin không ghi đè lặng lẽ (optimistic concurrency), batch atomically valid.
- [ ] 6. Rà soát spec/code; PHP lint, Java build, DB migration trên isolated copy/replay, các regression boss/dame/giftcode, browser workflow và combat client test. Đối chiếu matrix 100% boss được liệt kê và cơ chế config đã hỗ trợ đúng.
- [ ] 7. Cài hỗ trợ Java một lần qua bảo trì lưu dữ liệu, backup DB/JAR/SHA256 theo AGENTS. Sau đó test chỉnh HP/drop/cap liên tiếp từ web khi PID/port/server uptime giữ nguyên. Ghi manifest, version và cách rollback từng revision, không restore toàn DB đè dữ liệu người chơi.

## Điểm cần chốt khi duyệt

Khuyến nghị mặc định áp dụng chỉ số lần xuất hiện sau; có nút áp dụng boss đang sống khi cần và giữ tỷ lệ HP. Tỉ lệ rơi đồ/giới hạn dame áp dụng lượt thưởng/hit kế tiếp. Hiển thị đầy đủ boss theo loại, không giảm số cá thể trong game. Lần cài hỗ trợ server Java mới đầu tiên cần bảo trì; các lần chỉnh sau không cần restart.
