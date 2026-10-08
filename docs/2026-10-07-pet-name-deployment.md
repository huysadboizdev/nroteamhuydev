# Tên đệ tử chỉ đọc trong admin — đã triển khai

Pet Studio hiển thị tên mặc định theo loại đệ tử: Đệ tử, Mabư, Uub, Kid Beer, Kid Jiren. Ô tên chỉ đọc. Khi chọn loại khác, tên xem trước thay đổi theo; tên và loại thật được server cập nhật sau khi lệnh hoàn tất. Khi người chơi đổi đệ tử trong game, tải lại trang để đọc dữ liệu mới.

API và server từ chối pet_name. Form không gửi tên, chỉ gửi pet_type khi admin đổi loại. Lưu riêng chỉ số giữ nguyên tên đang lưu và không can thiệp chức năng đổi tên bằng vật phẩm trong game. Không chạy cập nhật hàng loạt SQL, không thay đổi tài nguyên client.

## Kiểm tra

- Kiểm thử trước sửa tái hiện lỗi: PHP vẫn nhận tên tùy ý, helper hiện tên rỗng, Java vẫn cho sửa tên.
- Sau sửa: PHP kiểm thử tên mặc định cả năm loại, API từ chối sửa tên đều đạt.
- Node chạy hàm savePet thực tế: không gửi tên, sửa riêng chỉ số không gửi loại, đổi loại gửi đúng loại mới. Kiểm tra cú pháp bảy script và 22 tệp PHP đạt.
- 142 nhóm kiểm thử Java đạt trên lớp biên dịch và JAR ứng viên; 30 nhóm LiveSync đạt trên JAR đã cài.
- Kiểm tra trực tiếp JVM xác nhận hai lớp PetService/LiveSyncPlayerPatch đúng hash bản cài; hàng đợi đang chạy và năm danh mục SQL đọc được.
- Đọc chỉ kiểm tra nhân vật 1368: loại 4, tên hiển thị Kid Jiren, sức mạnh 64250000243. Không thay đổi nhân vật để thử.
- Tại thời điểm xác minh: hàng đợi 23 completed, một failed cũ; không còn pending/processing. Server PID 2852 nghe cổng 14445.

## Bản sao lưu

- JAR cũ: `SRC/jar-backup/20-before-web-manager-20261007-174249.jar`.
- SHA-256 cũ: `293c2be48af5a24f60c32653de745cd31cc6c1ea63b178a90707582f07fdb0e3`.
- JAR đang dùng: `SRC/20.jar`.
- SHA-256 mới: `f902d3d718cde8905b01a61cf3040db89e53fea987b7caced6635e0c1612365c`.
- SQL và web backup: `.analysis/web-manager-deploy-20261007-174249/`.
- Đã bảo trì lưu dữ liệu và xác nhận JVM dừng trước khi thay JAR. Metadata/hash nằm trong `.analysis/web-manager-deployment-latest.json` và `.analysis/web-manager-verification-latest.json`.
