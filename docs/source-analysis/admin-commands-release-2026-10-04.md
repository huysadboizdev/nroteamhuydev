# Phát hành sửa admin ngày 04/10/2026

Đã triển khai source, kiểm thử, sao lưu và cài bản mới vào `SRC/20.jar`. Server đã khởi động; cổng 14445 do JVM 15308 sở hữu. Probe attach xác nhận class admin được nạp từ chính JAR này và kiểm tra quyền trên fixture rời, không thao tác nhân vật thật.

## Hành vi

- `boss` mở bảng ngay. Lựa chọn gắn đúng instance boss và phiên, hết hạn sau 120 giây; Thách đấu tới Zone và tọa độ hiện tại. Boss chết/rời khu/lựa chọn cũ có thông báo. Packet giới hạn 127 hàng theo signed byte của client.
- `hs` gỡ khống chế và liên kết trói, Mafuba, slot Ma Bư; xóa timer/cờ gây giữ lại. Tính lại chỉ số, hồi đầy HP/KI, hồi sinh tại chỗ nếu chết và reset hồi chiêu. Giữ buff có lợi và PK không liên quan. Callback kỹ năng được dừng trước khi lấy khóa nhân vật; snapshot Mafuba cũ không bắt lại target.
- `kol` đủ counter nhiệm vụ hiện tại của nhánh thường/VIP đã chọn; mặc định thường. Fide đủ cả ba counter. Không đổi stage, receipt hay cộng thưởng. Nhận qua menu cũ; VIP vẫn cần vé 1825. Tiến độ được xác nhận lưu trước thông báo thành công; kết quả commit chưa rõ được đối soát và giữ state khi logout.
- Kiểm tra session/role admin khi nhập lệnh, mở form/menu và thực thi. Router so khớp token đầy đủ, giữ alias và phản hồi cú pháp. Packet/form/menu giả mạo của tài khoản thường và session mất quyền bị chặn.
- Ban ghi đúng account `ban=1`, kiểm tra ownership/rowcount rồi mới báo/kick. Buff tiền cộng bằng long trước clamp, báo số thực nhận. Cấp đồ kiểm tra catalog, option, quantity, sức chứa, copy option riêng. Lệnh `n` không phát thưởng task cũ; `m` báo rõ map/khu/tile không hợp lệ.
- Đổi tên đồng bộ DB và chỉ mục online, kiểm tra tên trùng theo collation; khóa ghi tên dùng chung với tạo nhân vật. Thẻ đổi tên chỉ trừ một lần sau kết quả xác nhận. Commit chưa rõ giữ intent và chặn state xung đột tới khi đối soát và lưu đầy đủ. Trade, packet inventory chờ khóa và tặng ngọc đều kiểm tra pending. Kick thực hiện logout ngoài khóa target để tránh đảo thứ tự khóa giao dịch ruby.

## Kiểm chứng

- Full compile Java UTF-8, release 17: source và test đều exit 0.
- JAR đóng gói được đặt đầu classpath: **116/116 nhóm PASS**, gồm **72 regression + 44 nhóm admin**. Kiểm tra ZIP không trùng entry, chỉ 46 class ứng dụng thay đổi từ 23 file Java; các entry khác giữ nguyên.
- Có RED trước sửa và GREEN sau sửa cho quyền, input, tiền/cấp đồ, boss, hs/KOL, SQL, race đổi tên, packet inventory, trade và deadlock Kick/ruby.
- JDBC dùng schema fixture riêng cho Ban/đổi tên/KOL, ownership, zero-row, lỗi DB, commit thực rồi báo lỗi, relogin, concurrency và các trường hợp pending. Mô phỏng thất bại full-save dùng saver được inject; kiểm tra hook/rowcount PlayerDAO bằng source và regression. Không Ban/cấp tài sản cho tài khoản thật để thử.
- SQL chỉ đọc đối chiếu live và hai dump; không cần migration, hai dump không đổi. Quyền account giữ nguyên; kết quả role cuối: `[{"is_admin": "1", "accounts": "3"}]`. Không có tên trùng theo truy vấn audit.
- Runtime probe: quyền null/thường/admin/session cũ, giới hạn boss, API hs/KOL và cờ pending đều PASS. Log startup xác nhận nghe cổng 14445. Các map thiếu tile 78,95,101,121,125,130,134 vẫn được báo unavailable; không sửa dữ liệu map trong đợt này.

**Giới hạn nghiệm thu:** đã kiểm tra code/packet client 213/214 và fixture luồng chọn boss; chưa chơi thử bằng giao diện client thật. Checklist còn mở ở [manual-client-checks.md](C:/Users/Admin/Downloads/nroteam/Teamobi2026/.analysis/admin-fix/manual-client-checks.md). Không coi fixture là nghiệm thu gameplay.

## Backup và khôi phục

- Source trước sửa: `C:/Users/Admin/Downloads/nroteam/Teamobi2026/.analysis/admin-implementation-20261004-142657/source-before`; 569 file Java đã được đối chiếu hash trước sửa.
- JAR cũ được MOVE vào [20-before-admin-fix-20261004-161633-741910.jar](C:/Users/Admin/Downloads/nroteam/Teamobi2026/SRC/jar-backup/20-before-admin-fix-20261004-161633-741910.jar); metadata nằm cạnh JAR. Đã gửi bảo trì lưu dữ liệu, xác minh không còn JVM/cổng/vòng restart trước khi thay. Vòng restart tức thời khiến lần đầu dừng trước bước cài; JAR chỉ được thay sau kiểm tra server đã dừng.
- SHA-256 JAR cũ/backup: `52be7070c78f78e9ffb8fe04ddb2909ed99b575a7043ce02c65fa3623b87b0ed`.
- SHA-256 JAR mới: `0fe2738a7bd861ad3a120a2edac7f857dae9857f99b66561ce55abc77cf3c030`.
- Metadata cuối: [deployment.json](C:/Users/Admin/Downloads/nroteam/Teamobi2026/.analysis/admin-fix/deployment.json); log test: [candidate-tests.log](C:/Users/Admin/Downloads/nroteam/Teamobi2026/.analysis/admin-fix/candidate-tests.log); probe: [runtime-verification.txt](C:/Users/Admin/Downloads/nroteam/Teamobi2026/.analysis/admin-fix/runtime-verification.txt).
- Khôi phục theo [rollback.md](C:/Users/Admin/Downloads/nroteam/Teamobi2026/.analysis/admin-fix/rollback.md): bảo trì và backup JAR hiện tại trước khi phục hồi bản cũ, kiểm tra hash; giữ tiến độ, số dư và receipt mới, không ghi SQL snapshot cũ đè lên giao dịch đã phát sinh.
