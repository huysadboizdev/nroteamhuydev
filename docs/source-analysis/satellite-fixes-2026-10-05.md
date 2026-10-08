# Sửa vệ tinh — 05/10/2026

Đã triển khai `SRC/20.jar` lúc 23:45 UTC+7; server chạy lại thành công lúc 23:46, PID39296, cổng14445.

## Thay đổi

- Tối đa3 vệ tinh đang hiệu lực mỗi khu. Việc đếm, đặt và trừ vật phẩm giữ khóa player → zone; dùng item giữ player monitor từ trước khi đọc template, tránh race khi item cuối cùng bị dispose. Đặt thất bại không trừ item. Mỗi vệ tinh có quantity1; option được sao chép riêng.
- Hết hiệu lực sau30phút ở mọi map, gồm21–23. Kiểm tra hết hạn trước khi cấp hiệu ứng, xóa ở lần cập nhật kế tiếp; không hồi thêm ở tick hết hạn.
- `Zone.getPlayersSnapshot` đọc từ danh sách được đồng bộ; cập nhật item dùng snapshot dưới lockzone, nhả lock trước khi xử lý player. `SatelliteService` kiểm tra lại map/phạm vi/người sống/bang bên trong lockplayer. Cờ và mốc thời gian được công bố bằng volatile; `Satellite.update` đồng bộ với cấp hiệu ứng. Chặn cấp hiệu ứng từ snapshot khu cũ sau đổi khu.
- Hồi HP/KI chỉ gửi packet khi giá trị thực sự đổi, gồm trường hợp làm tròn10% thành0. HP dùng packet có sẵn -30/subcommand9, giữ đủ HP/HP tối đa. Payload cho người xem giảm14→13byte, không tạo đọc thiếu/ngoại lệ ở client Game1/Game2. Không giảm số người nhận trong khu và không bỏ cập nhật HP của người quan sát.
- Bot tự nhặt bỏ qua type22, tránh xóa vệ tinh. Bot và vòng dọn ngọc Namec chuyển sang snapshot và phương thức xóa item có lock, loại các thao tác xóa danh sách trực tiếp có thể đua với vòng vệ tinh.

Giữ tác dụng:342 hồi10%KI;343 tăng20%tiềm năng gốc;344 phòng thủ quái theo cơ chế cũ;345 hồi10%HP. Giữ cooldown cờ3giây, phạm vi dưới200, người đặt/cùng bang đã lưu lúc đặt và không cộng dồn cùng loại. Không thay cơ chế phòng thủ thành giáp/PvP/boss. Không sửa Unity hoặc SQL.

## Kiểm chứng

Regression chạy trên JAR cũ tái hiện:8người đặt cùng lúc từ2→10vệ tinh; một item tạo2vệ tinh; hết hạn vẫn hồi; snapshotkhu cũ vẫn hồi; hồi0vẫn gửi; packetHPcũ14byte; thiếu snapshot; botxóa vệ tinh. Fixture riêng không thay trạng thái game thật.

Bản sửa biên dịch toàn bộ596source;39nhóm qua, gồm12nhóm vệ tinh và hồi quy blackball,petfusion,Mabư,Hirudegarn/benchmark. Các nhóm vệ tinh bao gồm giới hạn/cùngitem/hết hạn/membership/hồi0/protocol/snapshot/eligibility/khu đầy/bot/Inventorydispose thật/cooldown. Kết quả ở `.analysis/satellite-verify`.

Fixture benchmark1000lượt,3vệ tinh:

| Người mô phỏng | Trung bình mỗi lượt | Cao nhất | Lượt gửi mỗi đợt hồi | Payload tổng |
|---|---|---|---|---|
|50|0,090ms|3,104ms|2.550|32.350byte|
|200|0,995ms|3,146ms|40.200|519.400byte|

Network được mô phỏng. Mỗi đợt đều cưỡng bức thiếu HP/KI và reset cờ trong fixture để đo đợt bận nhất; game thật giữ cooldown. 200người là trường hợp stress, không thay sức chứa khu. Hồi HP cho cả khu vẫn tăng số lượt gửi theo số người; không cam kết không lag khi chơi đông từ kết quả này.

Probe chỉ đọc trên JVM39296 xác nhận class vệ tinh mới, giới hạn3/thời hạn1800000ms/cờvolatile/cooldownđồng bộ, snapshot hoạt động; lúc đo0người/0vệ tinh và không deadlock. Hai probe khác xác nhận map126:20khu15người,1boss/khu,1thread riêng, giờ15–16/22–23; blackballs18khu50người; hướng dẫnOmega vẫn35%KI/35%hútHP/20%giáp. Chưa kiểm chứng hình ảnh/client đang chơi đông.

## Triển khai và khôi phục

Đã dùng bảo trì lưu dữ liệu, xác nhận JVMdừng trước khi thayJAR, snapshotSQL và đối chiếuSHA256. Backup:
`SRC/jar-backup/20-before-satellite-20261005-234446.jar`.

JAR cũ SHA256:`dc0e0d5f31c6700949865428c891b45120d21851a24853d0b5533ffaccac0dc7`.
JAR mới SHA256:`e590328c68ae65ebe229ea2d820deeaa25fc36b7970213c7a754c9e7c1692ea4`.

Lần khởi động đầu17184 bị MySQLtừ chối; lớp kết nối/config không đổi, CLIlocalhost/127.0.0.1 thành công, thử lại39296 thành công. Không đổi cấu hình hoặc quyền MySQL để xử lý. Các cảnh báo thiếu tile trong lần khởi động thành công là cảnh báo đã có từ trước.

Metadata cạnh backup và `.analysis/satellite-deployment-latest.json`, gồmSHA256backup/JAR/snapshot,sourcehash,classes,report/probe vàlog. Thư mụcSRCchỉ có20.jar ứng dụng. Muốn khôi phục: bảo trì lưu dữ liệu, dừng JVM, phục hồi JARbackup; không cần rollbackSQL vì không chạy migration. Chưa commit/pushGitHub.
