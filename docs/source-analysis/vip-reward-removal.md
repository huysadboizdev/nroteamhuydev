# Cắt phần thưởng VIP — 04/10/2026

Đã sửa `SRC/src/nro/models/npc_list/ToriBot.java` và JAR đang dùng `SRC/20.jar`:

- VIP 3: bỏ 2 capsule kích hoạt (ID 1655), giữ pet Bọ Cánh Cứng và các quà còn lại.
- VIP 4: bỏ trứng Mabu (ID 568), 5 capsule kích hoạt (ID 1655), pet Bé Rồng Cute (ID 1771).
- Menu VIP 3/4 đã bỏ mô tả các món bị cắt.

Đối chiếu mã nguồn và SQL gốc: Bọ Cánh Cứng (ID 1248) được cấp ở VIP 1–3; VIP 4 không cấp Bọ Cánh Cứng, mà cấp Bé Rồng Cute, gọi là pet rồng nhí trong menu. Người dùng chọn cắt pet khỏi VIP 4, nên đã bỏ pet 1771 thực được cấp tại gói này. Không thu hồi vật phẩm đã nhận trước cập nhật.

Kiểm thử `SRC/test/nro/models/npc_list/VipRewardTest.java` gọi trực tiếp hai hàm phát quà thực bằng reflection và kiểm tra toàn bộ ID/số lượng. Bản cũ thất bại đúng do còn các món cần bỏ; bản sửa và JAR cài đặt đạt cả hai trường hợp. Chỉ `nro/models/npc_list/ToriBot.class` thay đổi trong JAR, mọi entry khác giữ nguyên byte.

Server đã được dừng qua `Maintenance.startSeconds(5)` có lưu dữ liệu trước khi thay JAR. Backup: `SRC/jar-backup/20-before-vip-rewards-20261004-072304.jar`.

- SHA-256 bản cũ/backup: `a09d708b3f3151aac65875873bbe9cd21da99c34efbaf42caf363cfa9649f2b7`.
- SHA-256 bản mới: `3bea5244db203d68ee7567884ed1c6d2e6b1e21330037abf90aee32f2a96dbfa`.

Metadata triển khai: `.analysis/vip-reward-check/deployment.json` và file JSON cùng tên backup. Chưa thử mua VIP từ client game.

## Cập nhật tiếp: bỏ quà nhận đệ tử ở cả bốn VIP

Theo yêu cầu tiếp theo ngày 04/10/2026, đã bỏ ba lời gọi `PetService.createNormalPet` ở VIP 1–3 và dòng tặng đệ tử trong menu. VIP 4 đã bỏ trứng Mabu từ lần trước. Không tạo hoặc thay đệ tử khi nhận bất kỳ gói VIP nào; các vật phẩm còn lại giữ nguyên.

Kiểm thử mở rộng đạt 8/8 trường hợp trên JAR mới và JAR cài đặt: cả bốn cấp VIP, mỗi cấp với nhân vật chưa có đệ tử và đã có đệ tử. Đã bảo trì lưu dữ liệu, lưu backup riêng và SHA-256 trong metadata, cập nhật JAR và khởi động lại server cổng 14445. Xem metadata hiện tại để lấy tên backup và checksum của lần cập nhật tiếp này.

## Cập nhật tiếp: bỏ phần thưởng Bọ Cánh Cứng

Theo yêu cầu người dùng, đã bỏ vật phẩm Bọ Cánh Cứng ID 1248 khỏi VIP 1–3, bao gồm tạo vật phẩm, cấp vào túi và mô tả menu. VIP 4 đã không tặng pet từ lần cập nhật trước. Không thu hồi vật phẩm đã nhận.

Nguyên nhân phát hiện trước khi cắt quà: VIP cấp ID 1248 trong khi `UseItem.useItem` và `Player.sendNewPet` xử lý pet bọ ở ID 1243. Kiểm tra SELECT database xác nhận hai ID là hai template khác nhau. Probe trên JAR cũ tái hiện bấm sử dụng 1248 không trang bị, không tạo pet, chỉ gửi cập nhật túi.

Kiểm thử danh sách quà mới đạt 8/8 trên JAR đóng gói và JAR cài đặt; mọi quà còn lại giữ nguyên. JAR chỉ thay `ToriBot.class`. Backup riêng mang tiền tố `20-before-vip-no-beetle-`; tên đầy đủ, checksum và trạng thái server nằm trong metadata triển khai hiện tại.
