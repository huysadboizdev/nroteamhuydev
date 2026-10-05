# Kiểm tra nhiệm vụ người chơi — 05/10/2026

Phạm vi: nhiệm vụ chính trong TaskService/Boss/Mob/NPC, ghi nhận tiến độ, chỉ dẫn SQL, nhiệm vụ ngày và nhiệm vụ bang. Đối chiếu 30 mẫu nhiệm vụ chính, 125 bước, 59 mẫu nhiệm vụ ngày và 59 mẫu nhiệm vụ bang. ID 29 là trạng thái đã hoàn thành, không phải nhiệm vụ có mục tiêu tiếp theo.

## Lỗi đã sửa

| Nhiệm vụ / luồng | Lỗi trước sửa | Sau sửa |
|---|---|---|
| ID 11, gia tăng sức mạnh | 40k đã hoàn thành bước yêu cầu 500k | Kiểm tra đúng 500k, 550k, 600k |
| ID 27, sức đánh gốc | Hiển thị 10k, code yêu cầu 35k; nói chuyện Thần Mèo có thể bỏ qua chỉ số | Yêu cầu đúng 10k, không hoàn thành bằng tương tác NPC |
| Các bước sức mạnh / bang / đi map | Đủ điều kiện trước khi nhận bước vẫn phải tạo sự kiện mới | Kiểm tra lại điều kiện hiện tại trong tick người chơi |
| ID 8, bước sức mạnh | Vào làng có thể bỏ qua điều kiện 40k | Chỉ tiến bước khi đạt sức mạnh |
| ID 8, ngọc rồng 7 sao | Ghi nhận ngay khi quái tạo drop, chưa cần nhặt | Ghi nhận sau nhặt thành công; trả nhiệm vụ phải có và nộp ngọc thật |
| ID 23, điểm hẹn Android | Chỉ ghi nhận map 93, không nhận ở 94/96; SQL chỉ sai map 97 | Nhận ở 93/94/96; chỉ dẫn SQL về map 93 |
| ID 25/26/27, đi map | Chỉ dẫn SQL để map -1 | SQL chỉ map 97/100/103 đúng với handler |
| Trả NPC | Danh sách hardcode chấp nhận NPC hành tinh khác, không đối chiếu địa điểm | Dùng NPC/map của bước hiện tại; khóa chung validation và advancement |
| ID 11, báo cáo về nhà | SQL trỏ NPC 105 ở map 42 | Trỏ ông ở nhà theo hành tinh |
| ID 13, gia nhập bang | SQL hướng nói chuyện Quy Lão thay vì điều kiện bang | Kiểm tra bang có ít nhất 2 thành viên; bỏ NPC của bước gia nhập |
| Capsule kì bí | Nhặt stack nhiều viên chỉ tăng một | Tăng đúng số lượng đã nhặt, giới hạn bằng mục tiêu |
| ID 28, Bui Bui / Drabura | Một lượt kill có thể tăng cả hai giai đoạn liên tiếp | Phân biệt ID boss giai đoạn 1 và 2 |
| Trả vật phẩm | Có thể tiến bước dù thiếu đùi gà/em bé/ngọc | Kiểm tra và trừ đủ vật phẩm trong cùng khóa người chơi |
| Thưởng vật phẩm nhiệm vụ chính | Túi đầy vẫn chuyển bước và mất món thưởng | Yêu cầu 1 ô trống trước tiến bước 7_2/10_2 |
| Trả nhiệm vụ ngày | Đòi 2 ô trống dù chỉ trao một món | Chấp nhận 1 ô trống |
| Nạp nhiệm vụ | SQL không ORDER BY nên thứ tự bước phụ thuộc kế hoạch truy vấn | Sắp theo ID nhiệm vụ và ducvupro |
| Chỉ dẫn map hành tinh | Placeholder MAP_500 chưa được chuyển, gửi map âm | Chuyển đúng map 2/9/16 |
| Lời thoại | Hướng dẫn 500 Capsule và 25/50 boss, SQL yêu cầu 50 Capsule và 10 boss | Đồng bộ lời thoại theo mục tiêu SQL hiện tại |

## SQL và dữ liệu nhân vật

Đã sửa 7 dòng metadata trong task_sub_template: ducvupro 38,41,45,89,99,105,113. Không reset hay tự next nhiệm vụ của nhân vật. Hai nhóm tiến độ đang lưu trong SQL đều có ID/index hợp lệ tại thời điểm kiểm tra.

Các cấu hình số lượng nhiệm vụ chính và 5 mức nhiệm vụ ngày/bang hợp lệ; không có bước phụ mồ côi. Hai bảng ngày/bang có đủ ID 0–58 cho các handler đang dùng.

Vẫn có mô tả thưởng SQL không khớp kinh tế SRC: ví dụ nhiệm vụ mộc nhân ghi 200 triệu vàng, công thức SRC hiện cộng 200 nghìn. Đây là sai lệch thông tin thưởng, không làm kẹt tiến độ; bản này chưa thay giá trị thưởng để tránh tự đổi kinh tế game. Phần mô tả thưởng cần chốt lại theo mức thưởng mong muốn.

## Kiểm chứng và triển khai

- Toàn bộ SRC biên dịch thành công; 53 nhóm kiểm thử đạt, gồm 13 trường hợp trong TaskProgressTest.
- Kiểm tra JVM sau restart đạt: đủ 30 nhiệm vụ/125 bước đúng thứ tự và 7 metadata đã sửa; PID 30944 lắng nghe cổng 14445. SHA-256 của JAR, backup và SQL snapshot đã khớp metadata.
- Đã xác nhận kiểm thử thất bại trước sửa và đạt sau sửa cho các lỗi ngưỡng, map, NPC, số lượng nhặt, giai đoạn boss, hành trang và tranh chấp tiến độ.
- Chưa chơi thử liên tục cả chuỗi nhiệm vụ bằng nhân vật trên client; các kiểm chứng là fixture server, SQL và catalog nạp trong JVM.
- JAR đang dùng: SRC/20.jar. Metadata: `.analysis/task-progress-deployment-latest.json`.
- JAR trước sửa: `SRC/jar-backup/20-before-task-progress-20261005-155606.jar`.
- SQL trước sửa: `.analysis/task-progress-deploy-20261005-155606/team2026-before-task-progress.sql`.
- Báo cáo dữ liệu trước sửa: `.analysis/task-audit/sql-catalog-before.json`; migration: `.analysis/task-progress-migration.sql`.
- Khi khôi phục cần bảo trì lưu dữ liệu và dừng server; đổi JAR và hoàn nguyên đúng 7 dòng metadata từ snapshot nếu cần. Không nhập ngược toàn bộ database làm mất tiến độ người chơi mới.
