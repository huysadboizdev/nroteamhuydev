# Reset khi đổi đệ tử

Ghi chú cập nhật: đệ đặc biệt40tỷ hiện bắt đầu bậc4 thay vì0; xem [pet-start-tier-four.md](pet-start-tier-four.md). Phần bên dưới ghi lại bản reset trước thay đổi này.

Đã triển khai ngày 02/10/2026. PetService tạo đệ mới đầy đủ dưới khóa sư phụ, rồi bỏ hợp thể, thoát map và dispose đệ cũ trước khi công bố đệ mới. Không còn khoảng trống player.pet=null trong giao dịch. Lưu dữ liệu và mở giới hạn dùng cùng khóa.

Mọi đệ mới có limitPower=0 (trần 17.999.999.999) và tiềm năng=0, chỉ số/kỹ năng/trang bị/tên theo mặc định loại được chọn. Không kế thừa bậc đã mở; người chơi phải mở lại tại Quốc Vương theo phí 500 triệu vàng/bậc và điều kiện trang bị hiện hành.

| Loại | typePet | Sức mạnh khởi tạo | HP/KI gốc | Sức đánh gốc |
| --- | ---: | ---: | --- | --- |
| Thường | 0 | 2.000 | Mỗi loại 800–2.100, bước 20 | 20–45 |
| Mabư | 1 | 1.500.000 | Mỗi loại 800–2.100, bước 20 | 50–120 |
| Uub | 2 | 40 tỷ | 400.000 / 400.000 | 20.000 |
| Kid Beer / Beerus | 3 | 40 tỷ | 400.000 / 400.000 | 20.000 |
| Kid Jiren | 4 | 40 tỷ | 400.000 / 400.000 | 20.000 |

Giữ sức mạnh/chỉ số khởi tạo sẵn của từng loại. Đệ đặc biệt vẫn có 40 tỷ dù bậc mở trở về 0; không tự tăng sức mạnh/chỉ số vượt trần hiện tại, phải mở lại các bậc để tiếp tục. Không thay đệ đang có trong DB nếu người chơi chưa đổi. Đồ trên đệ cũ không được chuyển hoặc hoàn lại, như luồng đổi đệ trước đây.

Đã sửa changeUubPet/changeKidBeerPet đọc đệ sau khi xoá và changeJirenPet gọi nhầm Beerus. API legacy limitPower được giữ chữ ký nhưng không còn cho kế thừa bậc. Bình 1795 kiểm tra item còn trong túi, còn số lượng, đủ 3000 Kilis và Mabư 40 tỷ trong khóa sư phụ; dùng luồng thay đúng và chỉ tiêu hao một lần. HP/KI đệ mới được khởi tạo đầy. Chat trễ bắt đúng đối tượng, bỏ qua đệ đã đổi tiếp. Cập nhật chỉ số sư phụ chỉ áp dụng người chơi để giữ chỉ số combat tuỳ chỉnh của Super Broly.

## Kiểm chứng

Năm chế độ PetReplacementTest đã xác nhận RED trên JAR cũ; kiểm thử boss cũng bắt lỗi trước khi thêm guard. Tổng 61/61 lượt source và 61/61 lượt trên JAR cuối đạt. Bao gồm mọi loại, bỏ override bậc cũ, default mới/cleanup, đầy HP/KI, invalid/replay/concurrent bình, chat đúng đệ và bảo vệ boss. Hai vòng review source đã chấp thuận. Class sản xuất/JAR đã đối chiếu, không có test/cache/entry trùng.

Logs: `.analysis/pet-replacement-red/red.log`, `.analysis/pet-reset-build.log`, `.analysis/gold-economy-check/test.log`, `.analysis/release-20261002-163537/jar-tests.log`. Chưa kiểm tra bằng client game thật hoặc đăng nhập/lưu-tải DB sau đổi; không khẳng định các kiểm thử đó đã thực hiện.

## Triển khai

Server đã dừng sẵn khi kiểm tra trước thay JAR, nên không cần gửi lệnh bảo trì. JAR cũ đã được chuyển vào backup trước khi cài. Sau cài đã khởi động run.bat và xác nhận Java PID 15364 nghe cổng 14445.

- Hiện tại: `SRC/20.jar`, SHA-256 `c19de0aecc144b01c15cfdf2638049ce524bba78ddbd68771043b26e5fa3359f`.
- Backup: `SRC/jar-backup/20-before-pet-reset-20261002-163645.jar`, SHA-256 `9ba1ce642c691b9f140e7d22711f3f29f212ef1ec6319f7aefc8ce20690e9454`.
- Metadata: `.analysis/latest-deployment.json`, `SRC/jar-backup/deployment-20261002-163645.json`.
- Không còn JAR ứng dụng dư trong SRC hoặc các thư mục release.

Đối chiếu SQL/res/path: [pet-special-assets.md](pet-special-assets.md). Hình ba đệ đầy đủ x1–x4; còn thiếu icon bình Kilis x1/15359.png. Không sửa SQL hoặc hình bình trong thay đổi này.
