# Điều khiển sự kiện trên web — 08/10/2026

Đã triển khai tại `webgoc2026/admin/events.php`, mục **Sự kiện** trong Control. Web gửi lệnh `EVENT_SET` qua hàng đợi có quyền admin, CSRF, mã yêu cầu chống gửi trùng và revision chống ghi đè cấu hình. Server xác nhận hoàn tất sau khi áp dụng runtime; heartbeat và trạng thái yêu cầu/thực tế hiển thị riêng.

## Hành vi

- Bật/tắt trực tiếp, không khởi động lại server cho từng thao tác. Khởi tạo boss một lần theo nhóm; boss đồng hành thuộc cùng nhóm. Tắt retire boss không phát thêm thưởng, ẩn NPC sự kiện và dọn vật phẩm còn dưới đất.
- Tắt đóng nguồn rơi/mua/đổi/dùng/nhặt đồ trước khi thu hồi. Giao dịch chứa đồ sự kiện bị hủy; các thao tác đã được tiếp nhận được hoàn tất trước khi đổi trạng thái.
- Túi, đồ đang mặc, đồ đệ tử và đồ đã bán cho NPC được dọn khi online/đăng nhập. Người đang có giao dịch SQL chưa xác định kết quả được giữ đến khi đối chiếu xong, tránh ghi đè dữ liệu đã commit.
- Rương thường, rương sưu tầm và rương vòng quay được khóa nhận/rút đồ sự kiện, rồi dọn trong bảo trì tiếp theo sau khi xác nhận lưu người chơi. Tin ký gửi chưa bán được sao lưu và xóa; tiền từ tin đã bán vẫn được nhận.
- Chỉ thu hồi ID tiêu hao đã phân loại. Mỗi lượt tắt có snapshot ID bất biến, backup trước/sau theo người chơi/field/job/operation và receipt. Tiến độ/lỗi được lưu; bảo trì lỗi phải hoàn thành đối chiếu/dọn trước khi mở cổng lại.
- Giữ quà đã nhận, gồm hộp quà Hùng Vương `1776/1777`, hộp quà Noel `648`, cải trang Buma đi biển `1804`, pet Zịt `1631` và các quà trong catalog. Dưa hấu `569` dùng chung nên giữ. Bí ngô `585` và tất/vớ Noel `649` thuộc đồ tiêu hao cần thu hồi.
- Không mở lại một sự kiện khi lượt dọn rương cũ chưa hoàn tất.

## Phạm vi thực tế

| Nhóm | Có thể bật | Trạng thái sau cài |
|---|---|---|
| Hùng Vương | Có | ON; 20 handle Sơn Tinh/Thủy Tinh |
| Hè | Có | ON; NPC, nguyên liệu, đổi/dùng đồ |
| Halloween | Có | OFF |
| Noel | Có | OFF |
| Tết, Trung thu, 8/3, 20/11, Valentine, TopUp | Khóa bật | OFF; UI nêu lý do thiếu nội dung/mapping |

Nồi bánh `66` đã được đặt trước trong map `183/184/185` để client nhận NPC qua dữ liệu map rồi áp dụng packet ẩn/hiện đang có. Giữ công thức, tỷ lệ và nguồn nguyên liệu của các sự kiện; không thêm các nguồn rơi mới ngoài phạm vi điều khiển.

## Kiểm chứng

- Biên dịch toàn bộ 639 source Java; 53 nhóm/mode kiểm thử qua trên source và JAR ứng viên: admission, drop/pickup, NPC chờ khóa, hủy trade, logout/commit, Whis, thức ăn, ký gửi, rương và lifecycle boss.
- Hai bộ kiểm thử MariaDB dùng database độc lập: revision/concurrency/restart/re-enable; cleanup/backup/receipt/malformed JSON/lost ACK/rollback/checkpoint/consign/pool một connection.
- PHP/JavaScript: quyền admin, method, CSRF, allowlist, hàng đợi, trạng thái UI, schema fallback, validation và syntax. Đọc projection web trên database thật: 10 nhóm, 4 hỗ trợ, 2 đang ON; không đổi sự kiện để thử trên tài khoản thật.
- Preflight chỉ đọc xác nhận định dạng inventory của hai người chơi hiện tại hợp lệ. Probe JVM PID `19088`: worker seasonal sống, bốn nhóm đủ điều kiện tài nguyên, Hùng Vương có 20 handle, không pending cleanup/logout. Cổng game `14445` mở đúng JVM.
- Chưa chạy thao tác bật/tắt từ Unity để kiểm tra hình ảnh trực quan; kiểm thử packet/client route và runtime không thay thế kiểm tra animation bằng mắt.

## Bản cài và khôi phục

JAR mới SHA-256: `a380a8736be66ce3eb4d211453523010f71e2c2d46385ee89608aca30884017f`.

JAR cũ: `SRC/jar-backup/20-before-event-control-20261008-091950.jar`, SHA-256 `5e0dfaf9c3f98540780c15dbc8d6de562ccceb762ecd77537ba7370c25c4fd15`.

SQL snapshot và metadata: `.analysis/event-control-deploy-20261008-091950/`; metadata mới nhất `.analysis/event-control-deployment-latest.json`. Đã bảo trì có lưu dữ liệu trước khi thay JAR, đối chiếu hash và gom JAR ứng dụng dư vào `SRC/jar-backup`; giữ thư viện/công cụ.

Migration gồm `20261008_event_control.sql`, `20261008_event_catalog_seed.sql`, `20261008_event_npc_placement.sql`; đã đồng bộ bốn SQL nguồn. Seed chạy lại giữ trạng thái/revision và không nhân đôi NPC.

Không phục hồi snapshot SQL cũ đè lên dữ liệu người chơi sau khi đã có thu hồi/phần thưởng mới. Khi cần rollback JAR, giữ bảo trì và ngừng gửi `EVENT_SET` đến khi bản có gate sự kiện hoạt động lại; JAR cũ chưa có các gate mới. Backup/receipt là lịch sử bất biến, không xóa để giả định lượt đã hoàn thành.
