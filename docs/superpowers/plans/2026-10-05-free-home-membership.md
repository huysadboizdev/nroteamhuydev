# Mở thành viên miễn phí tại nhà

Yêu cầu đã được người dùng cho phép triển khai: thêm lựa chọn mở thành viên miễn phí tại ông Gohan/Moori/Paragus; hoàn thành nhiệm vụ Mộc Nhân là điều kiện duy nhất về tiến độ. Xác định hoàn thành toàn bộ nhiệm vụ bằng taskMain.id >= 2 sau khi về báo nhiệm vụ1. Nhân vật có nhiệm vụ cao hơn cũng hợp lệ. Giữ mục ngọc xanh, đệ tử và giftcode.

1. Viết kiểm tra menu năm mục; kiểm tra trước nhiệm vụ, đang đánh Mộc Nhân, chưa báo, sau khi báo, nhiệm vụ cao hơn, đã kích hoạt, giao dịch pending, SQL lỗi và session mới.
2. Thêm FreeMembershipDAO ghi tiến độ nhiệm vụ và account.active=1 cùng transaction, kiểm tra player/account ownership và tài khoản không bị ban. Không cập nhật vnd, ruby/gem/gold. Account đã active thì trả thành công idempotent. Khi commit có thể đã xong nhưng mất ACK, đọc lại active dưới account lock; không có phần thưởng/tiền để lặp.
3. Thêm FreeMembershipService kiểm tra online và giao dịch pending, serialize đúng data_task hiện có, cập nhật session.actived sau SQL thành công; lỗi chưa rõ cho phép thử lại thao tác idempotent. Không tự mở khi kill mob; người dùng bấm nút ở nhà.
4. OngGohan thêm mục index3, chuyển Đóng sang index4; hai NPC hành tinh còn lại kế thừa. Giữ xử lý trả nhiệm vụ ưu tiên trước menu hỗ trợ.
5. Chạy kiểm tra service/menu, SQL thật trong database mới riêng, hồi quy cửa hàng/ký gửi. Review code trước cài JAR.
6. Bảo trì có lưu dữ liệu, kiểm tra server dừng, snapshot SQL, chuyển20.jar cũ vào jar-backup tên thời gian và hash; cài candidate đã kiểm tra, khởi động và kiểm tra TCP14445. Không cần migration schema hoặc sửa active các tài khoản sẵn có.
