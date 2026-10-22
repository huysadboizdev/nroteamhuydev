# Thiết kế: Tích lũy nguyên liệu trong Nâng SKH VIP

Ngày 22/10/2026. Trạng thái: bản thiết kế chờ người dùng rà soát.

## Mục tiêu

Bổ sung trong đường Bà Hạt Mít → Nâng SKH VIP một công thức chế tạo đồ Thần Linh có set kích hoạt bằng cách cộng dồn nguyên liệu qua nhiều lần nộp. Người chơi không cần giữ và chọn 50 món mỗi nhóm trong cùng một lượt.

Giữ nguyên công thức nâng SKH VIP hiện tại.

## Quy tắc đã chốt

1. Có hai bộ đếm riêng theo nhân vật: Thần Linh `0–50` và Hủy Diệt `0–50`. Các lần nộp cộng dồn từ đầu; chấp nhận nộp một nhóm hoặc cả hai nhóm trong một lượt.
2. Khi một lần nộp làm số cặp đã góp tăng, thu `1.000.000.000` vàng cho mỗi cặp mới được ghép. Số cặp đã tính phí là `min(số Thần Linh, số Hủy Diệt)`. Tổng phí khi đạt `50/50` là `50.000.000.000` vàng.
3. Nếu không đủ vàng để trả phí cho các cặp mới từ lần nộp, từ chối toàn bộ lần nộp đó trước khi tiêu hao đồ hoặc tăng tiến độ. Nộp phần chưa có cặp tương ứng không thu phí cho đến khi cặp được hoàn tất.
4. Chỉ nhận trang bị Thần Linh thường (ID `555–567`) và Hủy Diệt (ID `650–662`) còn hạn vô thời hạn, số lượng `1`, chưa có option kích hoạt, không có option hạn dùng. Cho phép lặp loại và khác hành tinh.
5. Vật phẩm được chọn bị tiêu hao khi lần nộp được chấp nhận; tiến độ cập nhật đồng thời với vàng. Phần nộp vượt số còn thiếu ở mỗi nhóm không bị tiêu hao.
6. Khi cả hai nhóm đạt `50/50` và phí đã trả đủ, người chơi xác nhận chế tạo để nhận đúng một món Thần Linh có set kích hoạt. Không có tỉ lệ thất bại.
7. Kết quả random đều một trong năm loại trang bị, một trong ba hành tinh, rồi một trong năm set kích hoạt hợp lệ của hành tinh đó. Rada dùng hành tinh đã random để chọn set tương ứng.
8. Món mới dùng chỉ số nền theo dữ liệu shop của đúng template và được tạo qua cơ chế gắn option kích hoạt hiện có. Nếu túi đầy lúc chế tạo, giữ nguyên trạng thái `50/50` để người chơi dọn chỗ rồi thử lại.
9. Sau khi tạo và trao món thành công, đặt hai bộ đếm và số cặp đã tính phí về `0` để bắt đầu lượt tích lũy kế tiếp.

## Luồng người chơi

1. Tại Bà Hạt Mít, chọn Nâng SKH → Nâng SKH VIP → tích lũy nguyên liệu.
2. Màn hình hiển thị tiến độ hiện tại, số vàng đang có và số đồ còn thiếu của mỗi nhóm.
3. Chọn một số món trong túi để nộp. Bản xem trước ghi số món hợp lệ sẽ tiêu hao, tiến độ sau nộp, số cặp mới và phí phát sinh.
4. Xác nhận. Server kiểm tra lại slot, instance, option, số lượng, số dư vàng và tiến độ dưới khóa người chơi/túi. Nếu dữ liệu đổi hoặc không đủ vàng, không thay đổi trạng thái.
5. Khi đủ `50/50`, hiện trạng thái sẵn sàng và nút chế tạo. Chế tạo dùng xác nhận một lần; kết quả được chuẩn bị và kiểm tra trước khi cập nhật túi/trạng thái.

## Thành phần và lưu dữ liệu

- Thêm chế độ tích lũy độc lập dưới mã/route Nâng SKH VIP, không thay đổi xử lý nâng theo cặp hiện hành.
- Dùng hai bộ đếm tiến độ cùng một trường số cặp đã tính phí trên Player. Lưu nối tiếp các giá trị vào cuối mảng JSON `data_vip` hiện có để tránh migration SQL; khi đọc dữ liệu cũ thiếu các phần tử mới, mặc định các giá trị bằng `0`.
- Đồng bộ thay đổi đồ, vàng và tiến độ qua cùng chu kỳ lưu nhân vật hiện có. Không lưu tham chiếu Item instance giữa các lần đăng nhập.
- Dùng catalog template Thần Linh `555–567`, mapping loại/hành tinh hiện có và danh sách set kích hoạt `SKHCapsuleCatalog`. Dùng builder `ItemService.createItemSKH` hoặc một builder tương đương đã xác thực template để đảm bảo chỉ số nền và các option hiệu ứng.
- Xác nhận giới hạn chọn nhiều món ở client. Thiết kế không yêu cầu chọn 100 món cùng lúc; mỗi lần nộp chỉ chứa số món người chơi muốn cộng thêm.

## Bảo toàn giao dịch

- Tính số lượng nhận thực tế theo phần còn thiếu đến `50` của mỗi nhóm; giữ nguyên item dư trong túi.
- Tính `newPaidPairs = min(nextDivine, nextDestroyer) - paidPairs`. Kiểm tra vàng đủ `newPaidPairs × 1 tỷ` trước khi tiêu item.
- Kiểm tra danh tính item/slot và trạng thái option lại ở lúc xác nhận; chống gửi lặp bằng khóa và trạng thái xác nhận một lần.
- Khi chế tạo, tạo output trước khi reset tiến độ; nếu tạo output lỗi hoặc túi không còn chỗ, giữ nguyên tiến độ và phí đã góp. Chỉ reset sau khi output đã được đặt vào túi.
- Không cho phép giao dịch tích lũy khi nhân vật đang trao đổi; kiểm tra cộng dồn không vượt 50; không nhận item đang khóa giao dịch, có hạn dùng, có quantity khác 1 hoặc đã kích hoạt.

## Kiểm chứng dự kiến khi triển khai

- Nộp riêng từng nhóm, nộp theo lô không cân bằng, ghép cặp qua nhiều lần và đạt đúng `50/50`.
- Thu phí đúng một lần cho mỗi cặp mới; tổng phí là 50 tỷ; thiếu vàng, item đổi slot hoặc lựa chọn lặp thì không tiêu hao.
- Chọn vượt số còn thiếu chỉ tiêu hao phần được nhận; phần dư ở lại.
- Đăng xuất/đăng nhập và khởi động lại server giữ đúng bộ đếm/phí; dữ liệu `data_vip` cũ đọc thành tiến độ 0.
- Đủ ngưỡng cho chế tạo chắc chắn đúng một món; đủ năm loại, ba hành tinh và năm set hợp lệ; Rada nhận set theo hành tinh đã random.
- Túi đầy hoặc lỗi tạo output không reset tiến độ; bấm xác nhận lặp không phát hai món; chế tạo thành công reset về `0/0`.
- Luồng Nâng SKH VIP nâng theo cặp hiện tại không đổi hành vi.

## Phạm vi

Thiết kế chỉ bao gồm logic server, trạng thái lưu tiến độ và menu/tương tác tại Bà Hạt Mít cho công thức mới. Không bao gồm build, đóng gói hoặc cập nhật JAR; các việc đó chỉ thực hiện khi người dùng yêu cầu rõ ràng.
