# Tapion / Hirudegarn: lịch, hồi boss và cưỡng chế

Đã triển khai lúc 2026-10-05T23:09:49.711446+07:00 (Asia/Bangkok, UTC+7). Server PID 46184, cổng game 14445.

Map 126 mở hằng ngày **15:00–16:00 và 22:00–23:00**, theo giờ Việt Nam. Mốc bắt đầu được vào; đúng mốc kết thúc khóa vào, dừng Hirudegarn gây sát thương/phát thưởng/hồi và bắt đầu tàu cưỡng chế về thành phố Vegeta map 19. Vào muộn hoặc restart không kéo dài phiên. Đăng nhập có vị trí lưu ở 126 được đưa về map 19 trước khi vào thế giới, kể cả trong giờ mở.

Có **20 khu, 15 người/khu**, mỗi khu một Hirudegarn độc lập. Giữ ba giai đoạn và phần thưởng hiện có mỗi lần chết giai đoạn; chuyển giai đoạn ở lượt xử lý nhanh kế tiếp. Chết giai đoạn cuối phát hiệu ứng kết thúc, lượt nhanh tiếp theo hồi boss giai đoạn đầu đầy HP. Bỏ các mốc chờ 5/15/600 giây. Reset cờ đếm chết, hiệu ứng và combat cho đời boss/phiên mới; callback đồng thời không nhân đôi phần thưởng.

Nhân vật admin cũng chịu lịch và cưỡng chế; nhân vật chết được hồi sinh để đưa ra. Thất bại tạm thời được thử lại. Kiểm tra origin và giờ sau khi chờ khóa nhân vật tránh kéo người đã thoát hoặc người đang ở phiên mới. Tách khóa vòng đời boss khỏi thao tác có thể chờ khóa nhân vật/đệ tử; kiểm tra định danh phiên và sức chứa tại chốt membership khi chuyển vào.

## Xác minh

- Biên dịch đầy đủ 595 source Java 17; đóng gói class trùng byte với class đã test.
- 27 nhóm kiểm thử qua: lịch, 20 khu, sức chứa/vào đồng thời, chuyển map qua mốc đóng hoặc phiên khác, ba giai đoạn và thưởng mỗi lần chết, hết giờ chặn damage/thưởng/hồi, reset phiên, tàu cho nhân vật chết/admin và retry, callback đóng cũ, chuẩn hóa map login; hồi quy sao đen, hợp thể, phó bản và bốn ca tàu Mabư.
- Migration chạy lặp hai lần trên catalog thử nghiệm riêng; chỉ thay số khu map 126, giữ mọi cột còn lại. SQL live xác nhận `126 / 20 / 15`; seed và hai dump gốc đồng bộ.
- Probe chỉ đọc trong JVM mới xác nhận 20 khu, một Hirudegarn/khu, tám mốc lịch và chuẩn hóa map login. Xác nhận 18 khu sao đen, tất cả 50 người/khu vẫn giữ đúng.
- JVM xác nhận đúng một luồng `hirudegarn-event-126`. Đăng ký từ `Manager.initMap()` sau nội dung map; nhịp cập nhật Player/item chung vẫn 1 giây. Xác minh runtime đã phát hiện bản trung gian chỉ đăng ký trong `Map.run()` không được gọi; lỗi đã sửa và bản cuối được build/kiểm thử/cài lại qua bảo trì.
- Tải fixture: `HIRUDEGARN_TICK_BENCHMARK PASS zones20 fixturePlayers300 rounds300 averageMs=0.158 worstMs=5.041 closingMs=47.643`. Đây là workload trong tiến trình, không tương đương 300 client mạng thật hoặc tải farm/vật phẩm dài hạn.
- Chưa quan sát trực tiếp bằng client về hoạt ảnh tàu, chuyển hình boss hoặc logout/relogin thật. Kiểm thử packet và fixture đã qua; runtime probe không thay đồng hồ hoặc dữ liệu người chơi trên server.
- Không có 30 giây gia hạn. Thời gian nhận packet/hoàn tất hoạt ảnh và scheduler chịu độ trễ thực tế, không bảo đảm cùng mili giây với mốc đóng.

## Backup và khôi phục

- Bảo trì dùng hàm lưu dữ liệu ứng dụng; đã xác nhận các JVM chạy `20.jar` dừng trước thay JAR.
- JAR hiện hành: `SRC/20.jar`; SHA-256 `bbe01782f637cd5237e7331a9842085951f95f4890ce01206c56abb5bb844d38`.
- JAR cũ: `SRC/jar-backup/20-before-hirudegarn-20261005-230937.jar`; SHA-256 `21b8c6862e30606e85dc83ab8e2d3978635906dcf31a9943528cc38c8bfabc86`.
- Snapshot DB sau khi lưu: `.analysis/hirudegarn-deploy-20261005-230937/team2026-before-hirudegarn.sql`.
- Backup ổn định trước thay đổi Tapion: `SRC/jar-backup/20-before-hirudegarn-20261005-230539.jar`; SHA-256 `9f549bc7726e3a5507d180b7ab544a1d6dc64c429ac3497a07a309bf20fd4764`.
- SQL rollback về số khu ban đầu map 126: `.analysis/hirudegarn-deploy-20261005-230539/hirudegarn-config-rollback.sql`. Backup JAR ngay trước lượt cài cuối là bản trung gian; dùng backup ổn định này để khôi phục khi cần.
- Metadata: `.analysis/hirudegarn-deployment-latest.json`, bản sao `SRC/jar-backup`.

Rollback: bảo trì lưu dữ liệu, xác nhận server dừng; chuyển JAR hiện hành vào backup mới có thời gian rồi khôi phục backup ổn định trước thay đổi Tapion vào `SRC/20.jar`, chạy SQL rollback ban đầu ở trên, khởi động và kiểm tra. Không import full snapshot đè dữ liệu người chơi mới phát sinh.

Source, migration, seed và tài liệu đang ở workspace; chưa commit/push GitHub.
