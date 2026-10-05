# Kiểm tra số lượng vật phẩm giftcode — 03/10/2026

Đã đối chiếu source, đọc lại item_template từ DB bằng SELECT trong giao dịch chỉ đọc, và chạy probe offline với `SRC/20.jar` đang dùng. Không đổi source ứng dụng, DB, JAR hoặc tiền/vật phẩm người chơi.

## Kết quả với quantity = 100000

| Trường hợp | Kết quả hiện tại |
|---|---|
| Item thường, ô trống | Nhận nguyên 100.000 trong một ô; không cắt về 99.999. |
| Item được gộp, có sẵn 1 cùng option và có ô trống | Ô cũ thành 99.999, phần dư 2 sang ô mới; tổng 100.001. |
| Thỏi vàng 457, có sẵn 1 | Thành 100.001 trong một ô, theo nhánh cộng đặc biệt. |
| Trang bị không gộp, ví dụ áo ID 0 | Có thể tạo một ô quantity 100.000; code không tách thành 100.000 món riêng. Không nên cấu hình như vậy. |
| Mở rộng túi 517 hoặc rương 518 | Chỉ mở rộng một ô mỗi lần gọi; quantity 100.000 không mở 100.000 ô. Nếu đã đạt max, trả false. |
| Bùa type 13 | Cộng thời gian theo shop đang mở; quantity không quyết định số bùa hoặc số phút. |
| Tàu tennis 453 / đùi gà 74 / ngọc rồng đặc biệt | Chạy hành vi riêng, không thể áp dụng giới hạn ô thường. |

Nhánh gộp thông thường giới hạn ô có sẵn ở 99.999. Nhánh thêm ô trống không áp dụng cùng giới hạn. Một số item được cộng toàn bộ: 457, 610, 821, 2048, 2075, 1066–1070, 2050–2055, hoặc type 14, nếu DB đánh dấu được gộp. Điều kiện bỏ qua ô có quantity >=100.000.000 chỉ kiểm tra trước cộng, không phải trần sau cộng. Quantity là int, packet túi gửi bằng writeInt; chưa kiểm tra client thật.

Danh sách theo **2.000 bản ghi DB**, bao gồm tên rỗng/bản ghi giữ chỗ: [giftcode-item-quantity-catalog.csv](giftcode-item-quantity-catalog.csv). Đây là phân loại theo template và luồng source; options của từng giftcode có thể chuyển sang nhánh cộng param.

## Mảnh vỡ 933 và mảnh hồn 934

Cả hai là type 27, is_up_to_up=1. Các hàm nâng bông tai dùng **option 31 param** làm số mảnh: NangCapBongTai và NangChiSoBongTai gọi getParam/subParamItemsBag. Quantity không được chuyển thành param.

Probe trực tiếp trên JAR đã xác nhận:

- `quantity=100000, options=[{id:73,param:0}]`, chưa có item: nhận quantity 100.000 nhưng không có option31; không tương đương 100.000 nguyên liệu cho các hàm nâng cấp này.
- `quantity=100000, options=[{id:31,param:1}]`, chưa có item: quantity 100.000 nhưng số nguyên liệu dùng nâng cấp chỉ là param 1.
- Cũng cấu hình trên, nếu có sẵn item quantity1/param1: quantity vẫn 1, param thành 2; không thành 100.001 mảnh.
- `quantity=1, options=[{id:31,param:100000}]`, có sẵn param1: server giữ param100.001. Tuy nhiên packet option param dùng writeShort, không chứa đủ 100.000.

`writeShort(100000)` truyền 16 bit thấp: **34464** nếu giải mã unsigned, **-31072** nếu signed. Đây là lỗi biểu diễn packet đã kiểm chứng; chưa biết client hiện dùng đọc theo kiểu nào nên không khẳng định chính xác con số hiển thị. Không nên dùng param100000 làm giải pháp sản xuất trước khi sửa biểu diễn.

## Các rủi ro khác liên quan

- Option1/31 kích hoạt cộng param cho bất kỳ item có options này. Khi tìm được cùng ID, code cộng param một lần rồi bỏ toàn bộ quantity đầu vào; không nhân theo quantity. Việc tìm ô không so khớp toàn bộ options.
- GiftCodeService bỏ qua giá trị trả về của addItemBag, và thông báo theo số quantity cấu hình, không theo số nhận thực tế.
- GiftCodeManager yêu cầu số ô trống >= số ID phần thưởng (kể cả tiền), rồi đánh dấu đã dùng/trừ lượt trước khi service kiểm tra thời gian và trao thưởng. Quy tắc này thường giữ ít nhất một ô trống mỗi reward; trường hợp direct_full_bag dưới đây là kiểm tra riêng tầng inventory, không khẳng định giftcode hiện vượt qua precheck với túi đầy.
- Probe tầng inventory khi túi không còn ô trống: có sẵn 1, thêm 100.000 → ô thành99.999, trả false, còn dư2 trong item đầu vào. Service cần tránh partial grant và không bỏ qua thất bại.
- Hàm timeCode hiện so datecreate với dateexpired, chưa so ngày hiện tại. Đây là phát hiện source, chưa tái hiện với code thật.
- Tiền giftcode ID -2/-3 được giới hạn tổng 200.000.000 ngọc/ngọc khóa; phép cộng dùng int trước Math.min, có nguy cơ tràn với số lớn. Vàng -1 dùng Inventory.addGold, giới hạn 1.000.000.000.000.
- Loader dùng HashMap keyed by item ID: cùng một ID xuất hiện nhiều lần trong detail thì bản sau ghi đè bản trước, không tự cộng.

## Bằng chứng

Helper: `.analysis/giftcode-quantity-audit/GiftQuantityProbe.java`; kết quả `.analysis/giftcode-quantity-audit/probe.log`; snapshot DB `.analysis/giftcode-quantity-audit/live-templates.json`. Probe thay boundary DB/network bằng dữ liệu offline, giữ GiftCodeService và InventoryService thật từ JAR. Chưa xác minh giao diện hoặc nhận code bằng client thật.
