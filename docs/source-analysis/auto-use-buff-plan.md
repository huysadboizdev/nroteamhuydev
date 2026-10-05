# Auto dùng item và buff 500 phút — kế hoạch đã được duyệt

Người dùng duyệt triển khai ngày 2026-10-04. Client Unity C# có hai namespace Game1/Game2; server Java.

1. Viết kiểm thử sử dụng thực tế UseItem: lần đầu, cộng dồn, 50 lần x10 phút, lần thứ 51 bị từ chối không mất item, buff hết hạn, loại trừ thường/cấp 2, đồ ăn khác cùng nhóm, lưu/khôi phục thời gian và hiệu lực hết hạn. Chạy trên JAR hiện tại để xác nhận lỗi.
2. Thêm SelfBuffRules quản lý duy nhất giới hạn 30.000.000 ms và cách cộng mốc thời gian. Giữ công thức hiệu lực, bộ đếm và schema lưu dữ liệu hiện có. UseItem kiểm tra trước khi tiêu hao, dùng cùng một khóa player cho giới hạn và tiêu hao. Cho phép dùng lại đồ ăn cùng loại.
3. Client Panel luôn hiện Số lượng, cả số lượng 1. Thay nút AutoItem cũ bằng Auto sử dụng / Dừng auto ở mọi item tiêu hao có thể dùng trong hành trang, cả giao diện hành trang cũ/mới.
4. Dùng chung AutoItem cho auto mới và chức năng cũ. Nhập số giây không âm; 0 bỏ nhịp chờ 20 tick. Mỗi tab chỉ gửi một lệnh đang chờ tại một thời điểm, phân lượt cho nhiều item. Chờ lượng trong ô giảm theo cập nhật túi server, dừng khi hết item, không có kết quả sau timeout, mất kết nối hoặc cần menu/xác nhận. Theo dõi lại đúng loại và option, tránh dùng nhầm sau đổi ô. Không tự chọn thưởng.
5. Viết kiểm thử C# offline có stub network: khoảng cách, 0s, chờ phản hồi, đổi ô, biến thể option, hết item, timeout, dừng/mất kết nối và hai tab độc lập. Compile toàn client bằng Unity Roslyn.
6. Chạy toàn bộ kiểm thử server, dựng JAR và chạy lại trên JAR. Bảo trì lưu dữ liệu, chờ server dừng; backup JAR cũ vào SRC/jar-backup có thời gian và SHA-256/metadata; cài bản mới, khởi động ẩn và kiểm tra cổng/danh mục.

Quy tắc: thời gian mỗi món giữ nguyên; chỉ cộng thời gian, không nhân hệ số buff. Không tiêu hao khi lần dùng mới vượt 500 phút. Buff đang hết hạn được xem là không còn hiệu lực; đồ ăn khác trong cùng nhóm không chuyển đổi lượng thời gian sang hiệu ứng khác. Auto chỉ dùng item trong hành trang. Các item cần lựa chọn tiếp tạm dừng để người chơi quyết định.
