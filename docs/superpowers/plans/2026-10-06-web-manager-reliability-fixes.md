# Web manager: kế hoạch triển khai đã được duyệt

Phạm vi: `webgoc2026` và Java server, ưu tiên trang đệ tử của nhân vật 1368. Giữ các thay đổi gameplay đã triển khai trước đó.

1. Xác thực admin từ SQL, kiểm tra CSRF cho API, kiểm tra kiểu và giới hạn dữ liệu. Lệnh không hỗ trợ phải thất bại rõ ràng.
2. PHP chỉ xếp hàng thay đổi nhân vật/tài khoản. Java áp dụng cho cả online và offline; ghi tài nguyên và biên nhận trong cùng transaction. Request ID chống bấm lại; giao diện chờ trạng thái hoàn tất.
3. Đọc đúng JSON đệ tử và ID kỹ năng hiện có, kể cả ID 0. Chỉ gửi trường được sửa; giữ cấp kỹ năng, đồ mặc, hợp thể, giới hạn sức mạnh và các dữ liệu không sửa.
4. Khi kết quả COMMIT chưa rõ: giữ đối tượng và khóa theo nhân vật/tài khoản, chặn đăng nhập, lưu thoát và giao dịch; đọc biên nhận dưới khóa SQL để đối chiếu. Bảo trì không được thoát JVM khi chưa xử lý xong.
5. Chỉ một luồng được xử lý logout; đợi cleanup xong trước khi phục hồi, lưu hoặc dispose. Không cho đối tượng phiên cũ lưu sau khi hoàn tất logout.
6. Reload giftcode, quái, shop, thông báo bằng snapshot đã kiểm tra. Giữ số lần dùng giftcode và giá đặc biệt của shop; HP quái thay đổi ở lần hồi sinh. Hủy ký gửi chỉ áp dụng đồ chưa bán, trả đồ trong cùng transaction.
7. Duyệt thẻ dùng `user_nap`, khóa dòng thẻ và tài khoản, cộng tiền và cập nhật trạng thái cùng transaction. Lệnh đồng bộ chỉ refresh số dư đã được ghi.
8. Kiểm thử PHP/JavaScript; Java staging/runtime và chốt giao dịch; JDBC trên database riêng cho retry, rollback, quyền admin, online, concurrency và logout.
9. Trước thay JAR: bảo trì có lưu, kiểm tra JVM dừng, snapshot SQL/web, migration cộng thêm, lưu JAR cũ có timestamp và SHA-256. Khởi động bản đã kiểm thử, kiểm tra cổng, log và hash class đang chạy.

Không tự sửa dữ liệu thật của nhân vật 1368 để thử nghiệm, không chạy lại các lệnh đã hoàn tất trong lịch sử, không commit/push ngoài phạm vi yêu cầu hiện tại.
