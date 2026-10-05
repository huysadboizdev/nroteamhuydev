# Kế hoạch xử lý số lượng vật phẩm giftcode

> **For agentic workers:** Dùng executing-plans trực tiếp khi được yêu cầu triển khai; không tạo subagent. Các bước là checklist; hiện chỉ kiểm tra và lập kế hoạch.

**Goal:** Số lượng cấu hình, thực nhận, hiển thị và dùng nâng cấp phải thống nhất; không mất vật phẩm hoặc lượt code khi trao thưởng thất bại.

**Architecture:** Phân loại phần thưởng trước khi cấp, chuẩn hóa nguyên liệu bông tai theo quantity để gửi được số lớn qua packet writeInt. Tính toàn bộ thay đổi túi trên bản sao và commit sau khi xác nhận đủ chỗ; code chỉ được đánh dấu đã dùng khi cấp đủ. Giữ tương thích nguyên liệu cũ dùng param31 qua lớp đọc/chuyển đổi có backup dữ liệu.

**Tech Stack:** Java17/JDK21, MySQL, test Java offline và packet client hiện có.

## Task 1 — Chốt và kiểm tra chính sách

- [ ] Theo yêu cầu tiếp theo của người dùng, nâng khả năng chứa số lượng lớn thay vì áp trần99.999. Quantity hiện là int và packet writeInt, có thể biểu diễn tới2.147.483.647 mỗi ô. Đặt giới hạn chung trong phạm vi int; phép cộng phải tính bằng long trước, điền đầy ô rồi chuyển phần dư sang ô khác, không cắt phần thưởng hoặc tràn thành số âm. Số100.000/1.000.000 không cần đổi quantity sang long. Trang bị không được gộp yêu cầu quantity1; item kích hoạt hành động chỉ chấp nhận quantity1.
- [ ] Định nghĩa quantity là số nguyên liệu cho933/934. Không dùng param31 lớn để thay quantity; không đổi packet option từ short sang int nếu chưa có hỗ trợ client. Trước khi chuyển dữ liệu cũ, đọc DB/options và kiểm tra client thật cách hiển thị933/934.
- [ ] Tạo `SRC/test/nro/models/services/GiftCodeQuantityTest.java` với ca100.000 khi túi trống/có1/thiếu ô;933/934 với quantity và param31; item hành động517/518; trang bị số lượng lớn; option không cùng nhau; bấm lặp/đồng thời; invalid ID/quantity âm/0 và tổng trànint. Tạo test packet quantity100.000 được đọc lại bằng readInt và option không bị cắt.

## Task 2 — Thống nhất cộng dồn và nguyên liệu

Files: `SRC/src/nro/models/services/InventoryService.java`, bốn lớp `NangCapBongTai.java`, `NangCapBongTai3.java`, `NangChiSoBongTai.java`, `NangChiSoBongTai3.java` trong `SRC/src/nro/models/combine/`; lớp mới `SRC/src/nro/models/item/PorataMaterialQuantity.java`.

- [ ] Viết lớp đọc số nguyên liệu tương thích item cũ: item có param31 dùng giá trị legacy; item đã chuẩn hóa dùng quantity. Không tự nhân param với quantity của dữ liệu cũ vì giftcode cũ có thể đã sai. Inventory và combine phải dùng cùng hàm đọc/trừ trên mọi ô, không chỉ ô đầu tiên.
- [ ] Khi nhận mới, lưu933/934 theo quantity; loại param31 đếm nguyên liệu khỏi item đã chuẩn hóa, giữ options khác. Chuyển legacy theo từng item với ghi log trước/sau và backup, không sửa hàng loạt DB chưa kiểm kê.
- [ ] Với vật phẩm thường được gộp: thay giới hạn99.999 và kiểm tra100.000.000 rời rạc bằng giới hạn chung trong phạm vi int, áp dụng cho cả ô cũ và ô trống. Phép cộng dùng long để kiểm tra trước khi chuyển int. Ngừng nhánh cộng param cho933/934 đã chuẩn hóa, giữ item khác để tránh thay đổi ngoài phạm vi.
- [ ] Tính trước tổng chỗ cần với cùng options. Nếu thiếu chỗ, toàn bộ túi giữ nguyên và thông báo số ô cần thêm. Test xác nhận không xảy ra nhận một phần rồi trả false.

## Task 3 — Trao giftcode an toàn

Files: `SRC/src/nro/models/services/GiftCodeService.java`, `SRC/src/nro/models/managers/GiftCodeManager.java`, `SRC/src/nro/models/player_system/GiftCode.java`, phần loader giftcode của `SRC/src/nro/models/server/Manager.java`.

- [ ] Kiểm tra thời gian hiện tại với ngày bắt đầu/hết hạn trước trao thưởng; validate ID có template, quantity dương, option tồn tại và param hợp lệ. Duplicate ID trong detail phải bị báo lỗi cấu hình, không ghi đè âm thầm.
- [ ] Tách xác nhận code hợp lệ khỏi đánh dấu đã dùng. Lập kế hoạch thưởng trên bản sao túi/tiền, kiểm tra đủ chỗ theo thực tế; tiền không yêu cầu ô túi. GiftCodeService không bỏ qua kết quả cấp.
- [ ] Khi kế hoạch hợp lệ, khóa theo code và người chơi, xác nhận lại lượt/số dư rồi commit. Cập nhật trạng thái dùng và lượt với giao dịch/lưu dữ liệu phù hợp schema hiện có; lỗi DB phải không tạo code dùng lại hoặc mất lượt/đồ. Kiểm kê schema lưu giftCode của player trước khi chọn transaction cụ thể.
- [ ] Thông báo số lượng thực nhận và lý do từ chối. Xử lý giới hạn tiền bằng long, không cắt phần thưởng âm thầm; kiểm thử sát cap.

## Task 4 — Kiểm chứng và triển khai

- [ ] Chạy test mới để thấy lỗi cũ trước khi sửa; sau sửa chạy `.analysis/verify_gold_economy.py`, bổ sung bộ test mới vào builder, rồi `.analysis/build_release_jar.py` với JDK trên PATH. Yêu cầu compile/archive/test đều đạt.
- [ ] Dùng tài khoản kiểm thử nhận100.000 item thường,933 và934, kiểm tra hiển thị/tiêu nguyên liệu nâng bông tai, đăng xuất/đăng nhập và túi thiếu chỗ. Không sửa vật phẩm người chơi thật để thử.
- [ ] Khi được yêu cầu triển khai: backup DB cho chuyển đổi legacy; bảo trì lưu người chơi, chờ server dừng; chuyển JAR cũ vào `SRC/jar-backup` tên có thời gian, xác minh SHA-256, cài bản mới và khởi động. Ghi metadata và đường khôi phục cả JAR lẫn dữ liệu đã chuyển.

## Trạng thái

Đã kiểm tra DB/source/JAR bằng probe offline ngày03/10/2026. Chưa sửa ứng dụng, DB hoặc JAR. Báo cáo: `docs/source-analysis/giftcode-quantity-audit.md`; danh sách từng ID: `docs/source-analysis/giftcode-item-quantity-catalog.csv`.
