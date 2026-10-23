# Kiểm tra luồng Nâng SKH VIP — 22/10/2026

Phạm vi: đọc source và kiểm tra ngoại tuyến trên `SRC/20.jar` đã cài; không nâng đồ trên tài khoản thật hoặc thay đổi logic ứng dụng.

JAR SHA-256: `B9FE348BD763FF20CD6D54F7321E4EC0B32FD7DCAF6A7D5633B6CD977B2B6AE1`.
Đã đối chiếu SHA-256 bytecode trong JAR với class biên dịch từ source: NangSKHVip, NangSKHVipTichLuy, BaHatMit, PlayerDAO, MrBlue và ItemService đều khớp.

## Menu thực tế

Bà Hạt Mít → Nâng SKH → Nâng SKH VIP → hai lựa chọn `Nâng theo cặp` và `Tích lũy Thần Linh kích hoạt`.

## Nâng theo cặp

- Một lần 1–5 cặp, chọn nhóm Hủy Diệt trước rồi nhóm Thần Linh với số món bằng nhau. Đồ Thần Linh trong cùng lượt không trùng loại trang bị.
- Chỉ nhận Hủy Diệt 650–662 và Thần Linh 555–567, mỗi món số lượng 1, không hạn sử dụng, không đang giao dịch, chưa có SKH.
- Không bắt buộc phôi Thần Linh cùng loại/hành tinh với Hủy Diệt. Loại món và hành tinh đầu ra theo Hủy Diệt chính; riêng rada theo hành tinh người chơi.
- 1 tỷ vàng/cặp, thu cả khi xịt. Tỷ lệ thành công độc lập 50%.
- Thành công tiêu hao cả hai để tạo 1 món SKH VIP; thất bại chỉ mất Thần Linh, giữ Hủy Diệt.
- Trong kết quả thành công, tổng 95% bậc 1–8 và 5% bậc 9–12. Chỉ số gốc tối đa theo bậc, random set hợp lệ của hành tinh.
- Bộ đếm tích lũy 50/50 không thay đổi ở nhánh này, kể cả thành công hoặc thất bại.

## Tích lũy Thần Linh kích hoạt

- Nộp dần từng loại, không bắt buộc đủ cặp hoặc đưa 100 món vào một lần. Có thể nộp nhiều món cùng loại trang bị.
- Thần Linh và Hủy Diệt đếm riêng, mỗi bên tối đa 50. Món vượt ngưỡng giữ lại.
- Phí mỗi lần = `(min(tiến độ Thần Linh mới, tiến độ Hủy Diệt mới) - số cặp đã trả trước đó) × 1 tỷ vàng`.
- Ví dụ đã kiểm tra: 1/0 không thu phí; nộp thêm 2 Hủy Diệt thành 1/2 thu 1 tỷ; nộp thêm 2 Thần Linh thành 3/2 thu thêm 1 tỷ.
- Đủ 50/50 và trả đủ 50 cặp (50 tỷ) hiện nút Chế tạo; nhận chắc chắn 1 món Thần Linh kích hoạt, random loại món/hành tinh/set.
- Chưa bấm Chế tạo thì tiến độ giữ nguyên. Túi đầy giữ nguyên 50/50. Nhận thành công mới reset ba bộ đếm về 0.
- Món nhận có chỉ số Thần Linh gốc, khóa giao dịch. Không tự có 5 ô pha lê; quy tắc 5 ô trống thuộc riêng hộp 1538.
- Tiến độ được ghi/đọc trong JSON `player.data_vip` tại vị trí 8, 9, 10 bằng cơ chế lưu nhân vật hiện có. Các hàm deposit/craft cập nhật RAM và gửi lại túi/menu, không tự gọi lưu database ngay trong từng thao tác. Việc đọc/ghi được kiểm tra ở source; đợt audit này không thử đăng xuất/đăng nhập tài khoản thật.

## Kết quả kiểm tra

- `NangSKHVipTest`: PASS 8 nhóm validation, random, fees, success, failure, mixed, inventory, replay bằng JAR đang cài.
- `SkhVipFlowAudit`: PASS nộp lẻ, phí cặp mới, ngưỡng 50/50, giữ đồ dư, một món Thần Linh khóa giao dịch, reset sau nhận, chống nhận lặp và giữ tiến độ khi túi đầy.
- Probe xác nhận nâng theo cặp không cộng bộ đếm tích lũy. Đây là hai công thức độc lập trong cùng menu VIP. Nếu yêu cầu là tự cộng dồn từ những lần nâng VIP thông thường, cách triển khai hiện tại chưa đáp ứng điểm đó.
- Probe: `SRC/build/skh-vip-audit-20261022/SkhVipFlowAudit.java`. Không sửa source ứng dụng hoặc thay JAR trong lần kiểm tra này.
