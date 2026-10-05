# Đệ đặc biệt 40 tỷ bắt đầu bậc 4

Đã triển khai ngày 02/10/2026 theo kế hoạch đã duyệt. Uub, Kid Beer/Beerus, Kid Jiren mới khởi tạo 40 tỷ sức mạnh và bậc 4, kể cả đổi từ đệ bậc 9. Không kế thừa bậc đã mở của đệ cũ. Đệ thường/Mabư vẫn khởi tạo bậc 0 và giữ chỉ số mặc định riêng.

Pet.restoreMinimumPowerLimit chỉ đưa type2/3/4, power>=40 tỷ, limitPower<4 lên4. PetService gọi trên đối tượng mới trước tính chỉ số/công bố; MrBlue gọi sau đọc type, bậc và power từ DB, trước công bố đệ đã tải. Đệ cũ đủ40tỷ bị bậc0–3 được hiệu chỉnh khi đăng nhập; bậc5–9 đã trả phí không bị hạ. Không thêm lệnh gọi vào update/auto/hợp thể. PlayerDAO vẫn lưu bậc thực tế dưới khóa sư phụ.

## Mở tiếp bắt buộc tại Quốc Vương

| Chuyển bậc | Đồ đang mặc bị tiêu hao | Vàng | Trần sau mở |
| --- | --- | ---: | ---: |
| 4→5 | 1 món thần linh | 500 triệu | 49.999.999.999 |
| 5→6 | 1 món thần linh | 500 triệu | 59.999.999.999 |
| 6→7 | 1 món hủy diệt | 500 triệu | 69.999.999.999 |
| 7→8 | 1 món hủy diệt | 500 triệu | 79.999.999.999 |
| 8→9 | 1 món thiên sứ | 500 triệu | 90.010.000.000 |

Giữ quy tắc sư phụ >=17.999.999.999, đồ đang mặc trên sư phụ hoặc đệ, xác nhận từng lần, không chờ đệ luyện tới từng trần. Tổng5lần là2,5tỷvàng và5món. Thiếu đồ/vàng hoặc chưa xác nhận không mở. Bậc4 HP/KI gốc giới hạn400.000, sức đánh20.000; sức mạnh khởi tạo40tỷ hơn trần39.999.999.999 một điểm được giữ nguyên, nhưng tăng sức mạnh/chỉ số bị chặn tới khi mở bậc5. Không sửa max9 hoặc bonus hợp thể20% đã triển khai.

## Kiểm chứng

PetReplacementTest đã cập nhật để xác nhận bậc4 trên cả3 loại/legacy override/bình1795 và vẫn kiểm tra xử lý đệ cũ, boss, chat, yêu cầu đồng thời. PetStartingLimitTest gồm restore/unlock/growth: hiệu chỉnh bậc thấp và giữ5–9, loại khác và thấp40tỷ không đổi, cả3loại mở đủ5lần bằng đồ sư phụ/đệ xen kẽ, thiếu điều kiện/replay không tiêu hao thêm, auto chỉ số/power không mở miễn phí, trả phí xong mới tăng tới trần tiếp.

Đã xác nhận RED trên JAR cũ; toàn bộ69/69 lượt source và69/69 trên JAR cuối đạt. Đối chiếu class sản xuất và cấu trúc JAR; review độc lập chấp thuận. Kiểm thử load dùng trực tiếp hàm hiệu chỉnh trên dữ liệu fixture; vị trí đọc/lưu DB đã rà soát source. Chưa thực hiện đăng nhập/client thật hoặc save/load round trip với live DB, không khẳng định đã thử các bước đó.

Logs: .analysis/pet-tier-four-red/red.log, .analysis/pet-tier-four-build.log, .analysis/gold-economy-check/test.log, .analysis/release-20261002-172816/jar-tests.log.

## Triển khai

Server đã dừng sẵn trước khi thay, không gửi bảo trì lần này. Đã chuyển JAR cũ trước cài mới, khởi động run.bat ẩn và xác nhận JavaPID7712 nghe cổng14445.

- SRC/20.jar SHA-256 dd2af8e9f6ea08a510de9af44e6eb571956868cc25659771a0598cb0bdcb4e3f.
- Backup SRC/jar-backup/20-before-pet-tier-four-20261002-192251.jar SHA-256 aad0030e7594e9b0ad5098d90aa11f47372a8eb302d2b256f574adab7fd66b33.
- Metadata .analysis/latest-deployment.json và SRC/jar-backup/deployment-20261002-192251.json.
- Không còn JAR ứng dụng dư trong SRC hoặc các thư mục release; thư viện SRC/lib giữ nguyên.
