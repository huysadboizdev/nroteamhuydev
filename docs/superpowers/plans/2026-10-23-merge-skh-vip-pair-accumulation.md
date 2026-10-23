# Thiết kế đề xuất: tích lũy chạy trong Nâng SKH VIP

Trạng thái: người dùng đã duyệt; đã triển khai, kiểm tra và cài JAR ngày 23/10/2026.

## Quy tắc đã chốt với người dùng

- Mỗi cặp Hủy Diệt + Thần Linh đã xử lý thành công về mặt giao dịch cộng 1 tiến độ cho mỗi bên, dù kết quả nâng là thành công hay xịt.
- Từ tiến độ 0, đủ 50 lượt nâng theo cặp sẽ đạt 50/50, trả tổng 50 tỷ vàng theo phí nâng hiện tại và nhận thêm 1 món Thần Linh kích hoạt chắc chắn.
- Cặp xịt vẫn giữ món Hủy Diệt theo công thức hiện tại. Món đó tham gia lần nâng tiếp theo vẫn được tính thêm một lượt; bộ đếm tính lượt nâng đã trả phí, không đếm các món Hủy Diệt khác nhau.
- Theo chỉnh sửa của người dùng: hành tinh phần thưởng mốc phụ thuộc món Hủy Diệt chính, không random hành tinh. Riêng rada Hủy Diệt dùng chung ba hành tinh thì lấy hành tinh người chơi, theo cùng quy tắc `NangSKHVip.outputPlanet` đang dùng cho kết quả nâng VIP.

## Trải nghiệm đề xuất

1. Bà Hạt Mít → Nâng SKH → Nâng SKH VIP mở thẳng màn ghép theo cặp.
2. Bỏ màn trung gian có hai nút `Nâng theo cặp` và `Tích lũy Thần Linh kích hoạt` như ảnh người dùng gửi.
3. Giữ thông tin phí, 50% thành công, vật phẩm tiêu hao và quy tắc giữ Hủy Diệt khi xịt trên màn xác nhận nâng.
4. Bộ đếm chạy nền, không có nút nộp nguyên liệu riêng hoặc bảng tiến độ cho người chơi. Có thể thêm một câu gợi ý nhẹ: “Càng kiên trì, con càng có cơ duyên nhận bảo vật.”
5. Đạt mốc thì trao thêm món Thần Linh kích hoạt cùng lượt nâng và hiện thông báo: “Bà Hạt Mít ban thưởng cho sự kiên trì của con: [tên món] Thần Linh kích hoạt!”
6. Món VIP từ lần nâng thành công vẫn được nhận đầy đủ. Phần thưởng mốc là một món thêm, không thay thế kết quả ghép.

## Công thức ghép và phần thưởng

- Nâng 1–5 cặp mỗi lần như hiện tại, phí 1 tỷ/cặp, xác suất thành công 50% mỗi cặp.
- Thành công tiêu hao Hủy Diệt và Thần Linh; xịt mất Thần Linh và giữ Hủy Diệt.
- Giữ cách random bậc SKH VIP, chỉ số tối đa theo bậc và cách chọn set theo hành tinh đang dùng.
- Phần thưởng mốc: một món Thần Linh kích hoạt thuộc hành tinh của món Hủy Diệt chính ở cặp chạm mốc; random loại trang bị và set hợp lệ của hành tinh đó. Riêng món chính là rada Hủy Diệt thì lấy hành tinh người chơi. Chỉ số Thần Linh gốc, khóa giao dịch, không thêm phí, không tự gắn 5 ô pha lê của hộp 1538.
- Ví dụ: món chính Hủy Diệt Trái Đất → thưởng Thần Linh kích hoạt Trái Đất; món chính Hủy Diệt Namek → thưởng Namek; món chính Hủy Diệt Xayda → thưởng Xayda. Không chọn hành tinh ngẫu nhiên khi tạo thưởng.
- Hủy xác nhận, chọn nguyên liệu sai, thiếu vàng, dữ liệu món thiếu, menu cũ hoặc giao dịch lưu thất bại: không cộng tiến độ, không thu vàng và không trừ đồ.

## Đếm mốc, lượt dư và tiến độ cũ

- Tính theo số cặp thực sự được xử lý, không theo số lần bấm nút. Ghép 5 cặp cộng 5.
- Kiểm tra mốc sau từng cặp trong cùng lượt ghép để giữ mọi lượt vượt ngưỡng. Ví dụ 48/50 ghép 5 cặp: 1 phần thưởng và tiến độ chu kỳ sau 3/50.
- Khi một lượt có nhiều cặp khác hành tinh, dùng món chính của đúng cặp vượt mốc để xác định hành tinh thưởng. Ví dụ đang 48 lượt thì cặp thứ hai trong lượt ghép tạo quyền thưởng; hành tinh của cặp thứ hai quyết định phần thưởng đó.
- Có thể lặp lại mốc nhiều chu kỳ.
- Giữ hai số đếm cũ để bảo toàn trường hợp trước đây đã nộp lệch Thần Linh/Hủy Diệt. Mỗi cặp mới tăng cả hai; khi cả hai đạt ít nhất 50 thì trừ 50 mỗi bên và ghi nhận một quyền thưởng. Không gán cả hai về 0 vì có thể mất số dư.
- Với số liệu cũ bị lệch, một bên có thể tạm lớn hơn 50. Cập nhật kiểm tra/đọc dữ liệu để không reset nhầm những giá trị hợp lệ đó. Ví dụ 40/20 thêm 30 cặp → nhận 1 thưởng, còn 20/0.
- Tiến độ cũ 50/50 đã đóng phí phải trở thành một quyền nhận thưởng, không thu lại tiền và không yêu cầu nâng thêm để được ghi nhận quyền đó. Dữ liệu cũ không ghi món chính; quyền thưởng chuyển đổi này lấy hành tinh người chơi tại thời điểm chuyển đổi và lưu cố định.
- Phí đã trả ở công thức cũ chỉ dùng để bảo toàn dữ liệu chuyển đổi; bỏ điều kiện thu phí tích lũy riêng trong cơ chế mới.
- Thêm danh sách quyền thưởng chưa nhận, mỗi quyền lưu hành tinh tương ứng, vào JSON `data_vip`, giữ nguyên các vị trí dữ liệu cũ; dữ liệu cũ thiếu trường mới được đọc là danh sách rỗng. Số thưởng chờ nhận tính từ danh sách này. Việc chuyển đổi phải dùng một lần và chạy lại an toàn khi đăng nhập.

## Lưu dữ liệu và nhận thưởng

- Tạo trước kết quả nâng, đồ thưởng có thể nhận và trạng thái tiến độ dự kiến.
- Lưu vàng, túi đồ, `data_vip` và receipt của lần ghép trong cùng một giao dịch database; chỉ công bố kết quả RAM sau khi lưu được xác nhận.
- Tái sử dụng cơ chế receipt/rollback/reconciliation hiện có. Mở rộng snapshot lưu tùy chọn `data_vip` và công bố tiến độ tương ứng, giữ tương thích các lời gọi mua hàng/mở hộp cũ.
- Trong lúc chưa xác định được commit có thành công hay không, chặn thao tác xung đột theo cơ chế pending hiện có; đối soát receipt rồi công bố đúng một lần.
- Nếu không đủ chỗ trao thưởng mốc, giữ quyền nhận thưởng đã lưu cùng hành tinh đã xác định của từng phần thưởng. Thử trao khi mở lại Nâng SKH VIP; không cần nâng/trả phí thêm để nhận thưởng đang chờ. Không lấy lại hành tinh từ món mới hoặc random hành tinh khi nhận sau.
- Không xóa quyền thưởng trước khi món thưởng được lưu vào túi. Thao tác nhận thưởng cũng phải lưu nguyên tử và chống bấm lặp.
- Lưu log quản trị cho mỗi lần nâng/mốc thưởng, có số cặp, kết quả, tiến độ trước/sau và phần thưởng; người chơi không thấy bảng đếm chạy nền.

## Thứ tự triển khai sau khi duyệt thiết kế

1. Cập nhật kiểm tra menu: bấm VIP mở ngay ghép cặp; callback menu tích lũy cũ không được phép nhận/trừ nguyên liệu.
2. Tạo kiểm tra tiến độ: thành công/xịt cùng cộng 1; lượt 5 cặp cộng 5; 49 + 1 và 48 + 5; lặp nhiều chu kỳ; cùng món Hủy Diệt được dùng lại sau xịt. Kiểm tra hành tinh thưởng theo món chính, rada theo người chơi, cặp vượt mốc trong lượt nhiều hành tinh và quyền thưởng nhận sau giữ đúng hành tinh.
3. Chuyển `NangSKHVipTichLuy` từ công thức nộp riêng sang quản lý đếm mốc/quyền thưởng; bỏ tuyến deposit khỏi `CombineService` và `BaHatMit`.
4. Tích hợp tính tiến độ vào giao dịch `NangSKHVip.execute`; cấp thêm món Thần Linh khi đủ mốc và có chỗ.
5. Cập nhật `Player`, `MrBlue`, `PlayerDAO` và snapshot/receipt để lưu, đọc, chuyển đổi dữ liệu cũ, khôi phục quyền thưởng và bảo toàn số dư.
6. Kiểm tra thiếu vàng, túi đầy, hủy menu, nguyên liệu đổi sau preview, bấm lặp/đồng thời, SQL rollback, mất phản hồi commit, đăng xuất/đăng nhập và bảo trì.
7. Kiểm tra hồi quy nâng SKH Thường, kết quả VIP, hộp 1538 và các thao tác dùng chung transaction/snapshot.
8. Khi được triển khai: dựng ứng viên, kiểm tra JAR, bảo trì lưu dữ liệu, backup JAR cũ có thời gian và SHA-256, cài mới rồi xác nhận server khởi động.

## Tệp dự kiến liên quan

- `SRC/src/nro/models/npc_list/BaHatMit.java`: menu VIP và nhận quyền thưởng đang chờ.
- `SRC/src/nro/models/combine/NangSKHVip.java`: tính kết quả nâng kèm tiến độ và phần thưởng.
- `SRC/src/nro/models/combine/NangSKHVipTichLuy.java`: đếm mốc chạy nền, chuyển đổi và trao thưởng.
- `SRC/src/nro/models/combine/Combine.java`, `CombineService.java`, `consts/ConstNpc.java`: loại bỏ nhánh nộp riêng và callback cũ.
- `SRC/src/nro/models/player/Player.java`, `database/MrBlue.java`, `database/PlayerDAO.java`: trường quyền thưởng và dữ liệu lưu/đọc.
- `SRC/src/nro/models/database/RubyGiftDAO.java`, `database/ConsignDAO.java`, `shop_ky_gui/ConsignTransactions.java`: snapshot tương thích ngược, cập nhật dữ liệu VIP cùng inventory, receipt và công bố RAM khi đối soát.
- Các kiểm tra của menu, VIP, tích lũy và transaction liên quan.

## Kết quả triển khai

- Nâng SKH VIP mở thẳng ghép cặp; loại bỏ đường nộp tích lũy riêng và vô hiệu callback cũ.
- Mỗi cặp đã trả phí cộng một lượt kể cả xịt. Nhận thêm Thần Linh kích hoạt khi đủ mốc; giữ phần dư và lịch sử lệch từ công thức cũ.
- Hành tinh thưởng cố định theo món chính của đúng cặp chạm mốc, rada theo người chơi; quyền thưởng chưa nhận lưu hành tinh trong `data_vip[11]`.
- Vàng, túi, tiến độ và receipt được ghi chung giao dịch. Cập nhật RAM sau commit xác nhận, khôi phục khi mất phản hồi; không tính trùng lần nâng/thưởng.
- PASS 10 nhóm kiểm tra VIP, kiểm tra tích hợp mốc/hành tinh/chuyển đổi dữ liệu, 9 trường hợp transaction VIP, database riêng kiểm tra rollback/replay/commit không rõ kết quả, 6 nhóm nâng thường, catalog VIP/900 đầu ra theo SQL và hồi quy hộp 1538/Thần Linh/vật liệu hồng ngọc.
- Rà soát độc lập hai bước (đúng đặc tả và chất lượng code): không có lỗi cần sửa.
- Full compile Java 17 thành công. Đối chiếu SHA-256 tất cả 893 class trong JAR ứng viên với class vừa biên dịch: khớp. Kiểm tra tích hợp chạy trực tiếp bằng ứng viên: PASS.
- Đã xác nhận server game dừng trước khi thay JAR. Không can thiệp tiến trình Java của VS Code.
- JAR đang dùng `SRC/20.jar`: SHA-256 `8A29ACD9D4455C4712481E7D23599BBC6CF4B0DAFC5CFE6560FE8A910946E2D8`.
- Backup trước khi thay: `SRC/jar-backup/merge-skh-vip-20261023-082543/20-before-merge-20261023-082713.jar`, SHA-256 `B9FE348BD763FF20CD6D54F7321E4EC0B32FD7DCAF6A7D5633B6CD977B2B6AE1`.
- Metadata triển khai: `SRC/jar-backup/merge-skh-vip-20261023-082543/metadata.json`. Server mới PID 22900; đã xác nhận log khởi tạo thành công và LISTEN cổng 14445.
