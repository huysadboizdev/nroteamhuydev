# Khung PC đúng độ phân giải PRJ

Ảnh người dùng cho thấy canvas bị kéo đầy viewport desktop gần 1900 pixel ngang. Client đã nhận diện PC, nhưng HTML đặt CSS canvas 100% và buffer theo tỷ lệ viewport, làm UI 1024 pixel bị phóng lớn và thay đổi tỷ lệ cửa sổ native.

Đối chiếu Main.cs của PRJ_2Tab_550K: chế độ PC mặc định gọi Screen.SetResolution(1024,600,false). Bản web nay giữ buffer 1024x600 và khung CSS tối đa 1024x600; chỉ thu nhỏ đồng nhất khi cửa sổ nhỏ hơn. Android/iPhone vẫn có nhánh viewport mobile riêng. Sửa cả template và HTML đang phục vụ.

Kiểm thử PC trước sửa: 10 trường hợp fail, mobile pass. Sau sửa: 18 kiểm thử resolution đạt; suite Node tổng 36 đạt; 15 viewport responsive đạt.

Trong kiểm tra game thật phát hiện thêm trap chia cho 0 khi tải dữ liệu. Main của port xử lý packet trước khi gameCanvas cập nhật khai báo cấu hình/zoom. Đã chuyển xử lý packet sau gameCanvas.update trong WebGL, đọc count tài nguyên unsigned, reset counter và bảo vệ phần trăm khi tổng bằng 0; SplashScr cũng bảo vệ maxData. ResourceDownloadChecks tái hiện lỗi thứ tự trước sửa; sau sửa cả Game1/Game2 đạt zero/negative total, saturation và startup ordering. PRJ gốc không bị sửa.

Đã build và cài artifact WebGL-prj-pcfix-20261014: Succeeded, Errors=0, Warnings=70. Backup bản trước: Builds/WebGL-backup-20261014-085130; SHA-256 xác minh bằng .analysis/prj-webgl-deployed.json.

Runtime artifact mới PC/Android/iPhone và hai tab đạt. Game PC thật ở viewport 1920x926 có canvas hiển thị và buffer đều 1024x600; chọn server local mở menu PRJ, không có runtime error. Ảnh: Tests/evidence/prj-pcfix-20261014/pc-native-window.png. Không gửi đăng nhập tài khoản.

URL bản sửa: http://localhost:8086/?v=WebGL-prj-pcfix-20261014.
