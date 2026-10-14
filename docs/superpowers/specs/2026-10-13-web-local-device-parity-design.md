# Web local nhận diện thiết bị và đồng bộ gameplay

Người dùng đã chốt web tự chọn PC cho máy tính, mobile Android cho Android, mobile iOS cho iPhone/iPad; không có lựa chọn thủ công. Hai tài khoản phải dùng cùng chính sách gameplay với source native. Giữ các cầu nối WebSocket, lưu dữ liệu, bàn phím trình duyệt, coroutine và responsive riêng của WebGL.

Triển khai local được yêu cầu ngày 13/10. Sửa các config về TCP 127.0.0.1:14445, sao lưu trước khi sửa, khởi động lại đúng gateway đang phục vụ cổng 8086. Không dừng server game. Đồng bộ riêng tàn sát và appearance/hào quang từ native vào cả Game1/Game2, không chép đè project.

Device detection đã tồn tại: kiểm tra lại cả source, JS đang phục vụ và Unity runtime PC/Android/iOS. Chỉ sửa nếu có test chứng minh thiếu hành vi.

Không build game khi chưa có yêu cầu rõ ràng. Có thể test nhận diện thiết bị và sửa config trên WebGL ngày 11/10 hiện có; các sửa C# mới chỉ có trong source, chưa nằm trong wasm. Báo cáo phải phân biệt hai trạng thái này.
