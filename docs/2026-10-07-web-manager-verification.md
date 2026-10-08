# Kiểm tra và triển khai Web Control Panel

Hoàn tất ngày 07/10/2026, múi giờ Asia/Bangkok.

## Thay đổi

- Lệnh quản trị qua hàng đợi có kiểm tra quyền, CSRF, dữ liệu đầu vào và khóa yêu cầu để chống gửi lặp. Web theo dõi trạng thái hoàn tất hoặc thất bại.
- Server cập nhật SQL, ghi nhận thực thi và đồng bộ dữ liệu đang chạy; giữ chặn thao tác xung đột trong quá trình khôi phục. Bảo vệ thời điểm đăng nhập, đăng xuất và lưu nhân vật.
- Sửa xử lý tài khoản, nạp tiền, vật phẩm, ký gửi và tải lại danh mục. Duyệt thẻ có khóa giao dịch và không cộng tiền lần hai.
- Form đệ tử đọc đúng cấu trúc JSON, tên kỹ năng từ SQL, chiêu chưa mở khóa và chỉ cập nhật trường được sửa. Giữ các dữ liệu đệ tử khác.
- Sửa lỗi giữ kết nối SQL khi xin thêm kết nối: cấu hình một kết nối trước đây khiến lệnh treo. Đóng kết nối trước bước thực thi tiếp theo hoặc công bố dữ liệu; giữ thứ tự khóa danh mục nhất quán.

## Bằng chứng kiểm tra

- Biên dịch Java và 142 nhóm kiểm thử đều đạt trên lớp mới và JAR ứng viên.
- Kiểm thử hồi quy một kết nối SQL tái hiện lỗi ở JAR cũ và đạt ở bản sửa.
- 30 nhóm kiểm thử LiveSync đạt trên chính JAR đã cài.
- Kiểm tra trực tiếp JVM: hàng đợi chạy; năm bước đọc danh mục ALL/GIFTCODE/MOB/SHOP/NOTIFY đạt, giao dịch được rollback và không công bố dữ liệu thử.
- Lệnh thật số 16 UPDATE_STATS: completed, đúng một receipt. Tổng lịch sử: 15 completed, một failed cũ; không còn pending/processing tại thời điểm kiểm tra.
- Đọc chỉ kiểm tra nhân vật 1368: Mabu, sức mạnh 50000243, các chiêu Demon (2), Kamejoko (1), Tái tạo năng lượng (8); các chiêu còn lại chưa mở. Không thay đổi dữ liệu nhân vật để thử.
- Các bài kiểm tra PHP, hợp đồng API, giao dịch, đệ tử, giá shop, UI hàng đợi đều đạt. Kiểm tra cú pháp 22 tệp PHP và bảy script nội tuyến đạt.
- Server PID 11152 nghe cổng 14445 sau triển khai.

## Sao lưu và bản chạy

- Bảo trì có lưu dữ liệu, xác nhận JVM cũ dừng trước khi thay JAR.
- Bản cũ: `SRC/jar-backup/20-before-web-manager-20261007-165659.jar`.
- SHA-256 cũ: `60ef0ecca74c4df8145262007093279f9bd89a2815b4dbd9082188f6cdffe003`.
- Bản mới: `SRC/20.jar`.
- SHA-256 mới: `293c2be48af5a24f60c32653de745cd31cc6c1ea63b178a90707582f07fdb0e3`.
- SQL trước cập nhật và bản sao web nằm trong `.analysis/web-manager-deploy-20261007-165659/`.
- Metadata triển khai: `.analysis/web-manager-deployment-latest.json`; kết quả xác minh: `.analysis/web-manager-verification-latest.json`.
- Thư mục chạy SRC chỉ còn một JAR ứng dụng 20.jar. Không thay đổi thư viện.

## Giới hạn

Log khởi động vẫn có cảnh báo tile map thiếu hoặc không hợp lệ tại map 78, 95, 101, 121, 125, 130, 134. Đây là vấn đề tài nguyên map tồn tại trước đợt sửa web, cần xử lý riêng. Một lệnh failed cũ được giữ nguyên lịch sử, không tự phát lại.

Khôi phục JAR cần bảo trì có lưu dữ liệu rồi trả bản sao lưu về 20.jar; giữ nguyên dữ liệu người chơi hiện tại, không phục hồi SQL cũ tùy tiện.
