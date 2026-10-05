# Kiểm tra và sửa cộng dồn vật phẩm — 04/10/2026

Người dùng yêu cầu rà các item mua không cộng dồn, ví dụ bùa x2 đệ tử ở Santa, và nâng lượng cộng dồn đá/vật phẩm tương tự lên khoảng 1 triệu.

Đã đọc trực tiếp 2.001 template và 936 dòng item shop. Item 1628 (bùa x2 đệ tử) tắt cờ gộp, cùng Rađa kho báu, loa và một số nguyên liệu/hộp quà. Server đã hỗ trợ số lượng int và hơn 1 triệu; không hạ giới hạn hiện có. Shop chặn mua khi không có ô trống dù item có thể gộp vào stack cũ. Danh sách đầy đủ lưu tại `.analysis/item-stack/catalog-before.json` và `shop-items-audit.json`.

1. Ca hồi quy: gộp bùa/nguyên liệu lên 1 triệu; mua khi đầy nhưng gộp được; không đủ tiền/không gộp được không trừ tiền; chỉ số giống nhau khác thứ tự; bảo toàn HSD; cộng thời gian bùa sau khi cờ hết hạn chưa được dọn.
2. Bật gộp bằng danh sách ID rõ ràng cho vật phẩm tiêu hao/nguyên liệu. Trang bị, pet và món có trạng thái/chỉ số khác nhau tiếp tục tách ô.
3. Dùng cùng phép kiểm tra sức chứa với phép thêm thật; chuẩn bị item trước khi thu phí, khóa thao tác mua và kiểm tra thêm thành công. So sánh options theo nội dung, bảo toàn ngày hết hạn.
4. Kiểm tra 1 triệu trong bag/box và packet hiện có; bùa Santa dùng liên tiếp cộng thời gian, lưu/đọc lượng thời gian dài bằng long.
5. Chạy hồi quy, đóng gói, bảo trì lưu dữ liệu, backup JAR có timestamp/SHA-256, cập nhật server và kiểm tra cấu hình runtime.
