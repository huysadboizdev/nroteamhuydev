# Rà soát phụ bản ngày 07/10/2026

Rà soát mã nguồn và kiểm thử độc lập với JAR `SRC/20.jar` trước cập nhật, SHA-256 `99d036367fc5749732b6cf4ac1d74c7c98f25e5c4dd8a89d375d787f5f75606b`. Không sửa dữ liệu người chơi, không thay đồng hồ server, không restart server trong phần audit này. Áp dụng `systematic-debugging`, `test-driven-development`, `verification-before-completion`.

## Các lỗi đã tái hiện và sửa ở source

| Mức | Lỗi | Bằng chứng | Sửa |
| --- | --- | --- | --- |
| Cao | Hai bang đồng thời mở Kho báu có thể cùng nhận một instance; Rắn độc, Khí gas và Doanh trại dùng cùng kiểu cấp phát không đặt chỗ | `DungeonAllocationRaceTest` trên JAR cũ: `actual owners=2`, exit 1; log `allocation-red.log` | Đặt chỗ instance và liên kết bang dưới khóa chung, nhả khóa trước khởi tạo/chuyển map; cờ `volatile isInitializing`; đóng/dọn cuối dùng cùng khóa để không xóa cờ đặt chỗ mới |
| Cao | Boss Kho báu cấp 108–110 có HP âm do nhân `int` trước khi gán vào `long` | Kiểm thử gọi `BanDoKhoBau.init()` thật: cấp 108 có HP `-2134967296`; log `boss-health-red.log` | Nhân với `20_000_000L` trước giới hạn; kiểm tra cấp 1, 100, 107, 108, 109, 110 |
| Vừa | Người chơi vẫn online sau phiên Rắn độc cũ có thể bỏ qua chờ 7 ngày vì `joinCDRD` còn true | `SnakeWayWeeklyAdmissionTest` tái hiện stale flag, exit 1; log `snake-weekly-red.log` | Chỉ miễn chờ khi `joinCDRD` thuộc đúng phiên bang đang mở, đối chiếu thời điểm mở; vào lại phiên hiện hành vẫn giữ tiến độ NPC |
| Vừa | Khởi tạo Kho báu thất bại vẫn mất bản đồ 611; trừ vật phẩm im lặng thất bại có thể mở miễn phí | `TreasureChargeTest` trước sửa thất bại với `failed initialization must retain treasure map611`; log `treasure-charge-red.log` | Khởi tạo xong mới trừ đúng một bản đồ dưới khóa người chơi, trước khi mở admission; chặn giao dịch đang đối soát; kiểm tra lượng trừ, nếu không trừ được thì đóng/dọn phiên |

Các đường lỗi khởi tạo giữ `isClosing=true` trước khi bỏ cờ khởi tạo, tránh cửa sổ vào map chưa hoàn chỉnh. Mọi thao tác đặt chỗ dùng bang/thành viên đã chụp; nếu người mở bị kick hoặc đổi bang, lỗi dọn phiên chỉ cập nhật bang sở hữu đã chụp. Kiểm thử đổi bang trong fixture xác nhận không xóa thời điểm mở của bang mới.

Trong phần tích hợp chính còn sửa đọc tiến độ `talkToThanMeo` từ phần tử thứ tư và giữ thời điểm Rắn độc khi nhân vật không có bang lúc login; xác minh tích hợp nằm trong báo cáo build/triển khai của tác vụ chính.

## Phạm vi rà soát

| Nội dung | Kiểm tra |
| --- | --- |
| Kho báu | Mở phiên, bản đồ 611/lượt, cấp boss, hết giờ, thời gian nhặt, admission, cấp phát đồng thời, lỗi khởi tạo |
| Rắn độc | Mở/vào lại phiên, chờ 7 ngày, tiến độ NPC, thời gian cuối, đóng/dọn, admission |
| Khí gas | Điều kiện bang/trưởng bang, giới hạn lượt, transport, cấp phát, đóng/dọn, admission |
| Doanh trại | Giới hạn ngày, thành viên vào cùng, loot 5 phút, đóng/dọn, admission/cấp phát |
| Mabư 12h/14h | Đọc lịch/vào map, đường cưỡng chế về, slot giữ Mabư; chưa có kiểm thử client thực cho trận hoàn chỉnh |
| Sao đen | 18 khu tổng, sức chứa 50, khu đầy, vào đồng thời, chuyển đối chiều giữ ngọc, chọn khu |
| Siêu thần thủy | Đọc cấp phát khu, timeout, Pocolo rời map khi chủ rời; chưa tái hiện trận client hoàn chỉnh |
| Hirudegarn | Lịch, startup, 20 khu, admission/race, 3 giai đoạn, deadline, reset, thoát/login |
| Commeson | State/timeout/timezone/thưởng một lần, chặn giao dịch/biến động túi khi đang đối soát; không chạy kiểm thử ghi SQL trong audit chỉ đọc này |

Catalog SQL live được đọc bằng `SELECT id,zones,max_player FROM map_template ...`; xác nhận Hirudegarn `126/20/15`, các map Sao đen có 2 hoặc 3 khu và 50 người/khu. Các map Doanh trại, Kho báu, Rắn độc, Khí gas và Mabư được đối chiếu ID/cấu hình; đây là kiểm tra catalog, không mô phỏng chiến đấu hay kiểm tra client mạng.

## Kiểm thử và artifact

JDK 21.0.12, Java `--release 17`. Test fixture biên dịch với `-proc:none` và classpath JAR hiện hành/thư viện; các class sửa được biên dịch độc lập có xử lý Lombok. Không ghi đè JAR đang chạy.

- JAR trước cập nhật: `DungeonTimeoutRegressionTest` 4 ca, `DungeonTimingContractTest` 5 ca, `DungeonClosingBehaviorTest` 7 ca, `DungeonAdmissionTest` 6 ca đều qua; Sao đen 5 mode, Hirudegarn 10 mode, Commeson state và mutation guard qua. Chạy Sao đen lần đầu thiếu argument là lỗi câu lệnh kiểm thử; sau đó chạy đủ cả 5 mode, exit 0.
- Source sửa: phân bổ hai bang/đồng bang; admission/tick/rollback của 4 loại; bang thay đổi trong rollback; boss HP 6 cấp; Rắn độc 3 tình huống; thanh toán bản đồ, pending, duplicate và trừ vật phẩm no-op đều qua.
- Hồi quy sau thay admission: Sao đen 5 mode, Hirudegarn 10 mode đều qua. Log lỗi cố ý do fixture tiêm lỗi chuyển map/boss/khởi tạo được dự kiến; kết quả quyết định là assertion và exit code.
- Thư mục bằng chứng: `.analysis/dungeon-audit-20261007/`. Logs chính: `regression-results.log`, `final-dungeon-results.log`, `final-event-results.log`, `final-payment-lifecycle.log`, `captured-clan-green.log`, `treasure-charge-green.log`.
- Test mới: `DungeonAllocationRaceTest`, `DungeonInitializationTest`, `TreasureBossHealthTest`, `SnakeWayWeeklyAdmissionTest`, `TreasureChargeTest` trong `SRC/test/nro/models/services_dungeon/`.

Trạng thái báo cáo: các lỗi xác nhận đã sửa và kiểm thử ở source. Build toàn bộ, kiểm thử tích hợp và cập nhật JAR do tác vụ chính thực hiện sau audit. Không thể kết luận mọi phụ bản hoàn toàn hết lỗi: chưa chơi hết tuyến bằng client Unity, chưa thử tải mạng thực, chưa kiểm tra đầy đủ mọi phần thưởng hoặc giao dịch SQL sau restart. Đặc biệt cấp phát khu Siêu thần thủy chỉ được đọc source; chưa có test concurrency riêng cho tuyến đó.
