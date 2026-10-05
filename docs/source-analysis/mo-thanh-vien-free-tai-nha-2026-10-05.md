# Mở thành viên miễn phí ở nhà

Ở nhà cả ba hành tinh, nói chuyện với ông Gohan/Moori/Paragus rồi chọn **Mở thành viên / free**.

Điều kiện: hoàn thành nhiệm vụ Mộc Nhân (nhiệm vụ chính1), về nhà báo nhiệm vụ để sang nhiệm vụ2. Sau khi báo nhiệm vụ, nói chuyện lại với ông để mở menu hỗ trợ. Nhân vật đã qua nhiệm vụ này cũng mở được miễn phí. Chưa đủ điều kiện thì hiện thông báo, không thay đổi nhiệm vụ hoặc tài khoản.

Không trừ VND, hồng ngọc, ngọc xanh hoặc vàng; không phát thêm phần thưởng. Mở thành viên áp dụng cho tài khoản; SQL lưu account.active=1 và tiến độ nhiệm vụ trong cùng transaction. Session được cập nhật ngay sau khi lưu thành công, không cần đăng nhập lại. Bấm lại báo tài khoản đã mở, không phát sinh phí hay ghi đè tiến độ nhiệm vụ.

Giữ menu ngọc xanh/đệ tử/giftcode. Ưu tiên hội thoại trả nhiệm vụ như trước. Tạm chặn mở khi còn giao dịch chờ xử lý; lỗi SQL không báo thành công. Commit mất ACK thì đọc lại trạng thái dưới khóa tài khoản; thử lại an toàn vì thao tác chỉ đặt active=1.

Không cần migration schema, không tự kích hoạt các tài khoản đang có. Dùng FreeMembershipService/FreeMembershipDAO; OngMoori/Paragus kế thừa nhánh menu OngGohan.

Kiểm chứng: 43 nhóm đạt trên JAR ứng viên, gồm menu cả ba nhà, điều kiện nhiệm vụ/báo nhiệm vụ, tài khoản cũ đã qua nhiệm vụ, SQL lỗi, account ownership/ban, rollback, mất ACK, concurrent activation, bấm lặp, không mất tiền và hồi quy ký gửi/cửa hàng. Chưa thử bằng tài khoản thật trong client.

## Triển khai

Đã cài JAR sau bảo trì lưu dữ liệu và snapshot SQL. SHA-256 mới: `5718f496e321a3c08ee6bd1b86e83896fd818a531ea298aff1a6568995b39108`. Backup JAR cũ: `SRC/jar-backup/20-before-free-membership-20261005-123038.jar`. Server khởi động và kết nối TCP14445 đạt; schema thực tế có đủ account.active/ban và player.data_task. Metadata `.analysis/free-membership-deployment-latest.json`.
