# Kiểm tra mapping điều khiển live: giftcode, EXP, boss và thông báo

Kiểm tra chỉ đọc source server, PHP, client Game1/Game2 và schema SQL thực tế ngày 07/10/2026. Không phát thông báo, đổi EXP hoặc nạp lại boss trên server thật. Chạy `LiveSyncCatalogTest` trên JAR đang dùng: PASS, dữ liệu giả.

| Chức năng | Mapping hiện tại | Kết luận |
|---|---|---|
| Nạp giftcode | RELOAD_GIFTCODE → snapshot giftcode SQL → GiftCodeManager | Reload giữ quota SQL và lịch sử đã nhận của nhân vật; giới hạn tài khoản cần củng cố |
| Đổi EXP | SET_EXP_RATE → Manager.RATE_EXP_SERVER → NPoint.calSucManhTiemNang | Đổi trực tiếp khi server chạy, không cần bảo trì; chưa lưu bền vững |
| Nạp quái/HP | RELOAD_MOB → mob_template → Mob khi hồi sinh | Không tải lại BossData; RELOAD_ALL cũng không có boss |
| Thông báo | SEND_NOTICE → sendThongBaoAllPlayer → packet -25 | Gửi toàn bộ người đang online, nhưng là info1, không phải kênh thế giới info2 packet92 |

## Giftcode

Lịch sử ở `SRC/src/nro/models/player/GiftCode.java`, lưu trong player bởi PlayerDAO và tải bởi MrBlue. GiftCodeManager kiểm tra exact code text. Reload chỉ thay catalog; xóa rồi tạo lại đúng chuỗi code không cho nhân vật đã nhận nhận lại.

Schema hiện có UNIQUE(player.account_id), nên bình thường mỗi account chỉ có một nhân vật; tuy nhiên xóa/tạo lại nhân vật hoặc reset lịch sử sẽ mất dấu nhận. Chưa có ledger độc lập theo tài khoản. Code chưa chuẩn hóa hoa/thường, SQL code không có khóa unique: tạo biến thể ABC/abc có thể được coi là code khác, tạo trùng có thể khiến reload bị từ chối.

`GiftCodeManager.java:43–50` trừ quota SQL autocommit rồi thêm lịch sử RAM; `GiftCodeService.java:37–98` trao thưởng sau đó, không cùng giao dịch và không kiểm tra đủ kết quả thêm vật phẩm. Có nguy cơ mất lượt khi trao thưởng hoặc lưu lỗi. Cộng gem/ruby bằng int trước Math.min có thể tràn. Nhánh item457 thêm option nhưng không đưa item vào túi. Schema datecreate có ON UPDATE CURRENT_TIMESTAMP khiến trừ quota sửa thời gian tạo.

Hướng sửa: chuẩn hóa danh tính code thống nhất web/server; ledger unique(account_id, canonical_code) không bị xóa theo catalog; backfill lịch sử cũ. Khóa và xác minh quota/ngày/chi tiết thưởng SQL, kiểm tra đủ chỗ, cập nhật claim + quota + dữ liệu thưởng trong cùng transaction; xử lý kết quả commit không rõ theo cơ chế recovery đã có. Không chỉ reset count hoặc thay list RAM.

## EXP

`LiveSyncCatalogCommands.java:36–40` nhận rate nguyên 1–100 và gán volatile RATE_EXP_SERVER sau commit. `LiveSyncService.java:32` chạy hàng đợi theo chu kỳ 1,5 giây, không gọi Maintenance. NPoint đọc rate ở lần tính tiếp theo. Đây là hệ số SM/TN trong luồng đánh quái, không chứng minh mọi phần thưởng nhiệm vụ cũng nhân rate.

Giá trị chỉ ở RAM; `Manager.java:1058` đọc lại Config.properties khi restart. Web `admin/index.php` đặt value=3 cố định, không phản ánh rate đang chạy. `Mob.java:263` ép kết quả long về int: khi kết quả vượt 2.147.483.647, EXP có thể sai hoặc âm dù rate hợp lệ.

Hướng sửa: lưu setting SQL khi đổi và nạp lúc startup; endpoint báo rate đã áp dụng thực tế; giữ số long xuyên luồng EXP, kiểm tra nhân chống tràn. Sau lần cài bản hỗ trợ, các lần đổi rate không cần bảo trì.

## Boss

Không có RELOAD_BOSS trong allowlist web/server. Boss dùng cấu hình Java BossData; nút hiện tại ghi Nạp lại Quái & HP. Cần tách quản lý mob và boss, định nghĩa nguồn cấu hình boss trước khi làm hot reload. Áp dụng cấu hình mới ở lần spawn kế tiếp để tránh sửa HP con đang chiến đấu hoặc tạo trùng.

Lỗi xác định gây boss chết còn trên map: `boss/luyen_tap_tu_dong/TauPayPay.java:73–85` override leaveMap chỉ gửi hiệu ứng và zone=null; không remove khỏi Zone/manager, không REST hoặc dispose. Luồng TrainingService tạo đúng lớp này, lethal injury dẫn qua AFK rồi LEAVE_MAP. Zone vẫn giữ tham chiếu và gửi lại khi người chơi trở về. HP0 làm client hiện nền thanh máu xám/trắng. `TrainingBoss.java:199–219` đã có cleanup finally nhưng bị override bỏ qua. Sửa bằng cách giữ hiệu ứng và tái sử dụng cleanup chắc chắn.

Các lỗi số học xác định khác:

- `DeathOrAliveArena.java:97`: nhân HP int; Dracula và người chơi HP2,1 tỷ cho kết quả âm thay vì2,331 tỷ.
- `Whis.java:34`: HP550.000 × LV3905 bị thu hẹp int thành âm.
- `XenBoHung.java:66–67`: nhân phần trăm int trước chia; hút HP1,5 tỷ cho mức tăng âm.
- `NPoint.java:1587`: hp int -= sát thương long có thể tràn; chưa xác định luồng đánh boss thông thường gửi sát thương vượt int vì các tuyến chính đã clamp.

Không kết luận mọi boss đều mắc lỗi còn trên map. Boss chung đã có xử lý chết và timeout CHAT_E10 giây. Cần tên boss/map/khu cụ thể nếu triệu chứng thuộc boss khác Tàu Pảy Pảy luyện tập.

## Thông báo thế giới

`LiveSyncCatalogCommands.java:45` gọi Service.sendThongBaoAllPlayer, packet -25. Client Controller case-25 đưa vào GameScr.info1. ChatGlobalService dùng packet92 có tên người gửi, nội dung và ngoại hình; client case92 đưa vào info2/kênh thế giới khi tên người gửi không rỗng.

Hướng sửa: gửi đúng packet92 bằng tên hệ thống/Admin và dữ liệu ngoại hình hợp lệ; không trừ ngọc, không yêu cầu tài khoản admin đang online. Giữ thông báo thường/bảo trì ở -25. Thống nhất độ dài nội dung theo khả năng hiển thị kênh thế giới và kiểm thử giải mã ở cả hai tab client.

## Thứ tự triển khai đề xuất

1. Sửa cleanup boss luyện tập và số học HP/EXP; hồi quy chết → rời map → quay lại, boss nhiều giai đoạn, không thưởng lặp.
2. Lưu/đọc rate thực tế và đổi kênh thông báo đúng packet92; kiểm thử không gọi bảo trì.
3. Giao dịch giftcode và ledger tài khoản, migration/backfill không cho nhận lại code cũ; kiểm thử reload, xóa/tạo lại, concurrent, túi đầy và lỗi commit.
4. Tách nút quái/boss; triển khai reload cấu hình boss sau khi xác định catalog có thể chỉnh, chỉ áp dụng lúc spawn kế tiếp.

Đây là kết quả kiểm tra và đề xuất, chưa sửa các chức năng trong đợt audit này.
