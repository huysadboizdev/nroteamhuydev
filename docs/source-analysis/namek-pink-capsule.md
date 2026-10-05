# Capsule hồng ngọc — Tiểu đội sát thủ Namek, 2026-10-04

Đã cài JAR mới và cập nhật catalog live cùng hai dump SQL. Server PID17612 chạy cổng14445; runtime probe xác nhận class mới, catalog14 và text/type/icon của item722. Có thể cần đăng nhập lại để client tải catalog mới. Chưa nghiệm thu bằng gameplay client thực tế.

## Cơ chế

- Số4/Số3/Số2/Số1/Tiểu đội trưởng Namek (-311..-315) mỗi boss rơi5 capsule ID722 riêng lẻ quantity1, public ngay; người không tham gia đánh hoặc vào khu sau vẫn nhặt được nếu đồ còn trên đất. Đây là5 viên chung, không phải5 viên/người; ai nhanh có thể lấy hết.
- Cải trang429..433 và ngọc rồng6s/7s ID19/20 chỉ đúng người kill được nhặt tới khi hết hạn. Cờ quyền nhặt được đặt trước khi publish itemMap; các nguồn drop khác giữ cơ chế cũ.
- Giữ vàng77, options cải trang, điểm sự kiện +1 hoặc+5, map/HP/chu kỳ boss. Không sửa Tiểu đội sát thủ thường.
- Nhặt vào hành trang; mỗi lần dùng tiêu hao1 viên và cộng `inventory.ruby`, không phải gem/VND/điểm. Capsule cũ722 cũng dùng được. Không chuyển đổi hoặc thu hồi số dư hồng ngọc861 đang có.
- Phân phối80% đều1–5 (16%/số),20% đều6–100 (khoảng0,210526%/số). Trung bình13/capsule,65/boss,325/lượt đủ5boss khi mọi viên được mở; các số trung bình không phải thưởng bảo đảm.
- KOL5 tiếp tục đếm quantity722 nhặt thành công từ đất. Mở capsule không giảm tiến độ.

## Kiểm chứng

- RED trên JAR cũ: chưa có service mở capsule, owner trở thành public ở46s, boss chưa rơi5capsule. Log trong `.analysis/namek-pink-capsule/red-*.log`.
- 58/58 nhóm PASS trên JAR candidate và JAR cài đặt, không dùng class production rời trong classpath. Suite mới +44 nhóm KOL/pet/VIP/inventory hồi quy; DB tests chỉ trên schema riêng `kol_verify_20261004_111410`.
- Kiểm tra deterministic phân phối/biên1,5,6,100; mở/quantity/ruby/tràn số/packetfailure; quyền owner/copy/expiry/publicpickup; drop5boss/guard/respawn; KOLpickup. Đọc chi tiết mode đã chạy trong `java-tests.json`, `green*.log`, `jar-tests.log`.
- Database snapshot trước/sau chứng minh chỉ name vàdescription row722 thay đổi; giữ type27/icon6782, item861, mọi item khác, inventory/số dư/tiến độ/ledger. Hai dump patch đúng1 row722 mỗi file. Không dùng tài khoản thật để phát thưởng kiểm thử.
- Java review spec trước, quality sau; các vấn đề validation-before-guard, publish private flag và tranh chấp mở/nhặt capsule được kiểm tra trước release.
- Inventory/capsule/ruby dùng persistence hiện có. Không bổ sung ledger cho mọi hành động dùng đồ; không cam kết phục hồi tuyệt đối nếu process crash giữa mutation RAM và autosave. Giới hạn mua VIP sau crash của bản trước giữ nguyên.

## Backup và khôi phục

Đã bảo trì có lưu dữ liệu, xác định PID/workdir, kiểm tra server dừng và cổng đóng trước thay JAR. Bản cũ được MOVE vào `SRC/jar-backup/20-before-namek-pink-capsule-20261004-125132.jar`.

- SHA-256 cũ: `3ae6d7833bc89e12be24ac78179ea0039dd63f6c15b8cee9c118c1a073ca187d`.
- SHA-256 mới: `ad73e48fae9d02360c1dac0242d71e7932081b336d372783cc2bb8c8de6d7813`.
- Metadata: `.analysis/namek-pink-capsule/deployment.json`, `.analysis/latest-deployment.json`, JSON cạnh backup.
- Source/SQL backup: `source-sql-backup.json`; hash SQL: `sql-file-changes.json`; DB: `database-before.json`, `database-maintenance-before.json`, `database-after.json`, `database-runtime.json`; runtime: `runtime-verification.txt`.
- Bản compile-base JAR cũ được lưu trong jar-backup sau khi hết dùng; không để appJAR dư trong thư mục chạy; không di chuyển thư viện hay toolJAR đang cần.

Rollback: bảo trì/lưu dữ liệu, backup JAR đang dùng rồi phục hồi bản cũ và text722 từ snapshot nếu cần. Không thu hồi capsule/ruby đã nhận, không DROP KOLledger/walletreceipt. Client phải tải lại catalog khi khôi phục text; dùng version mới tránh cache cũ. Bản cũ không có chức năng mở722, nên cân nhắc lượng capsule đã phát trước rollback.
