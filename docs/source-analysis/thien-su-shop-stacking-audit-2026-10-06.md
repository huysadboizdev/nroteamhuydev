# Kiểm tra cộng dồn khi mua nhiều Thiên Sứ — 06/10/2026

Chỉ kiểm tra; không thay JAR, không sửa source ứng dụng, không sửa túi/số dư người chơi.

## Kết quả

- Dùng 16 row trong cache shop thực tế tạo hai vật phẩm mới vào danh sách thử riêng: tất cả được gộp thành **một ô, số lượng 2**. Cả 16 template runtime đều có `isUpToUp=true`.
- SQL còn bốn công thức có cờ cũ false (1072/1073/1085/1086), nhưng `ItemStackRules.CONSUMABLE_IDS` và `normalizeCatalog()` đã bổ sung. Không kết luận lỗi từ cờ SQL này khi runtime và addItemList đều xác nhận cộng dồn.
- Chạy đúng đường `ShopService.takeItem(player,3,id)` trong fixture với options shop `30:1`, lần lượt 20/50/100 lần cho từng ID 1071–1086. **48 trường hợp PASS:** một ô, số lượng đúng 20/50/100, ví hồng ngọc trừ đúng số lượng nhân 10.
- Client Game1/Game2 dùng `ModFunc.AutoBuyItem`: gửi từng yêu cầu mua một cái, chờ 200ms giữa các lần. Vì vậy 20 lần khoảng 4 giây, 50 lần khoảng 10 giây, 100 lần khoảng 20 giây cộng thời gian mạng; số lượng tăng dần, không nhận cả lô ngay.
- `Controller` đang bỏ qua quantity bổ sung của packet 6; đường AutoBuy hiện tại gửi quantity 0 từng lần nên vẫn đúng. Chuyển sang mua cả lô trong một request sẽ là thay đổi khác cần xử lý cả protocol, kiểm tra sức chứa và thanh toán tổng.

Chưa tái hiện lỗi không cộng dồn ở đường mua mới. Các ô cũ đã tách sẵn không tự compact toàn bộ khi mua thêm; thuật toán cộng vào ô phù hợp đầu tiên. Vật phẩm khác ID dù cùng tên, khác thuộc tính ở nhóm cần so khớp options, hoặc có thời hạn khác không được tự gộp.

Mẫu đọc trực tiếp túi của người online tại lúc audit không có vật phẩm Thiên Sứ để đối chiếu trường hợp đang nhìn trên client. Không suy luận rằng người dùng không gặp lỗi từ mẫu này. Chưa quan sát UI Unity trực tiếp.

Bằng chứng: `.analysis/thien-su-stack-audit/runtime.txt`; kiểm tra nhiều lần `.analysis/ThienSuBulkAudit.java`, kết quả console `THIEN_SU_BULK_AUDIT PASS groups=48`. Client: `Assets/Scripts/Assembly-CSharp/Game1/ModFunc.cs:1791`, Game2 cùng chức năng. Không phát triển chức năng mua cả lô trong lượt kiểm tra này.
