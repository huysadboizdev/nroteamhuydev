# Máy đo đứng yên và rơi đồ theo set — đã triển khai

Ngày 06/10/2026, đã cập nhật src, SQL live, bốn dump/seed và JAR server. Đã bảo trì có lưu dữ liệu trước thay JAR, sao lưu SQL và JAR cũ có timestamp/hash/metadata trong `SRC/jar-backup`. Server chạy lại trên cổng14445, JVM8484 lúc triển khai.

## Quy tắc rơi đồ

| Set đủ 5 món đang mặc, đúng ô/hành tinh/còn hạn | Vùng | Tỷ lệ | Rơi |
|---|---|---:|---|
| Thần Linh | Tương Lai 92–94,96–100,102–103 | 20% | Một trong663–667,x1 |
| Thần Linh | Cold105–110 | 60% | Một trong663–667,x1 |
| Hủy Diệt | Ngục Tù155 | 100% | Một trong1066–1070,x1 |
| Hủy Diệt, Cỏ bốn lá1635 còn hiệu lực | Ngục Tù155 | 100% | Một trong1066–1070,x20 |

Tỷ lệ cố định, không nhân luck/Gohan/Cỏ vào tỷ lệ. Đệ và đệ đi kèm xét set/buff của sư phụ. Thay nhánh food/shard cũ, giữ Bí kíp1229 và loot khác. Loại boss/bot/máy đo khỏi quy tắc chuyên biệt. Đồ trong túi/rương không tính vào bộ đang mặc.

Mỗi lần chết giữ một `DeathClaim` riêng, đặt trước các thao tác có thể chờ database. Khi quái hồi sinh, thế hệ mới có claim riêng; callback cũ có thể nhận đúng quà sau khi chờ, nhưng không cấp lại hoặc dùng nhầm claim của lần sống mới. Không giữ khóa mob trong khi chờ khóa chủ nhân.

## Máy đo117

SQL `TYPE=0,range_move=0,speed=0`, HP2tỷ giữ nguyên; packet map cờ không di chuyển riêng117. `vsMap=5` để client tải lại template. Máy vẫn dùng `Mob` thường với resource `typeData=0`, không đổi thành NewBoss.

Đã sửa `Game1/Game2/Mob.cs` và `Controller.cs`: không random vị trí/hướng khi vào map, không đẩy lùi/đổi hướng khi trúng đòn, không nảy lúc chết. Tọa độ gốc, hướng1, frame đứng0 được giữ; HP và trạng thái chết/hồi sinh vẫn theo packet server. Chỉ áp dụng117; công thức đo điểm và hồi HP server không sửa.

## Kiểm chứng

- Full src Java17 compile;111 nhóm kiểm thử đạt trên candidate và JAR đã cài.
- Test biên toàn bộ roll0–99 trên các map/thành phần hành tinh; sai bộ/ô/quantity/hạn; Cỏ đúng thời điểm hết hạn; hai loại đệ ở cả ba vùng; concurrent death; hồi sinh; buff qua save/load; callback cũ qua hồi sinh và không replay.
- Full compile Assembly-CSharp gồm Game1/Game2 bằng Roslyn Unity; harness chạy hàm Mob thực tế qua Mono: đứng600tick, bị đánh từ hai phía, lethal injury/chết, packet HP reset, constructor/resource chưa tải, quái thường không đổi. Test đỏ trước sửa rồi xanh sau sửa.
- Unity Editor isolated bị lỗi licensing exit199. Harness chỉ cô lập static UI/texture trong DLL test, không sửa hàm Mob. Chưa kiểm tra hình ảnh trực tiếp hoặc build player EXE. Dừng Play và chạy lại client hiện có để dùng source mới.
-40 icon thức ăn/mảnh và4binary mob117 chứaPNG x1–x4 hợp lệ; đồng bộ4dump/seed.
- Probe runtime xác nhận SQL117,vsMap5 và SHA256 tất cả class thay đổi. JAR/backup đối chiếu hash, thư mục SRC chỉ có20.jar là JAR ứng dụng.

Chứng cứ: `.analysis/equipment-farm-verification.json`, `.analysis/equipment-farm-deployment-latest.json`, `.analysis/equipment-farm-resources.json`, `.analysis/power-meter-client-verification.json`, `.analysis/power-meter-standalone-green.log`.
