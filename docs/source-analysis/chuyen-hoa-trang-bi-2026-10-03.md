# Chuyển hóa trang bị — bản sửa 2026-10-03

## Cách thử trong game

Đến Bà Hạt Mít ở map 5, chọn Chuyển hóa Trang bị. Chọn đồ nguồn trước, đồ Thần Linh nhận sau. Xem kết quả dự kiến và chọn Chuyển hóa hoặc Từ chối.

Nguồn: đồ thường bậc 11 hoặc 12, từ +4 đến +8; áo/quần/găng/giày/rada. Đích: đồ Thần Linh chưa nâng cấp, chưa ép sao, cùng loại và hành tinh. Rada cấp 11/12 chuyển sang Nhẫn Thần Linh. Đồ có option cấp/sao bằng 0 và lỗ sao còn trống được nhận.

**Bổ sung theo yêu cầu:** Không nhận đồ kích hoạt ở cả nguồn và đích, cả vàng lẫn ngọc. Nhận diện bằng Item.isSKH()/ActivationSet của server: chỉ cần có dòng tên set hoặc dòng hiệu ứng, kể cả param = 0. Kiểm tra trước menu và lại khi xác nhận; không thể thêm option kích hoạt sau menu rồi chuyển hóa. Bản đầu đã lọc option nguồn khỏi kết quả nhưng thiếu chặn nguyên liệu kích hoạt; lỗi này được tái hiện trước khi bổ sung kiểm tra.

| Nhánh | Phí | Thuộc tính chuyển |
|---|---|---|
| Vàng | 2 tỷ vàng | Cấp đập và mọi dòng % thông thường, gồm bonus % từ sao; không chuyển số sao đã ép, giữ số lỗ của đồ đích |
| Ngọc | 5.000 ngọc xanh | Cấp đập, các dòng % và số sao/số lỗ của đồ nguồn |

Chỉ số nền dùng đồ Thần Linh, áp dụng riêng từng chỉ số với hệ số chuyển hóa kế thừa `1.1^cấp * 0.9^số lần rớt`. Không lấy yêu cầu sức mạnh ở option đầu làm chỉ số nền. Chí mạng của nhẫn là chỉ số nền, không cộng lại chí mạng nguồn lần thứ hai. Các dòng % riêng của sao 8/9 vẫn riêng. Không tự mang set kích hoạt, Tinh/Nhật/Nguyệt Ấn hoặc hạn dùng từ đồ nguồn sang.

Ví dụ: Găng Kaio +4, 5 sao, sức đánh +15% -> Găng Thần Xayda +4 và +15%. Dùng vàng không giữ 5 sao; dùng ngọc giữ cả 5 sao. Nhánh vàng giữ bonus % cũ và có thể ép thêm sao vào lỗ của đích theo chính sách người dùng đã chọn.

## An toàn và SQL

- Hai nhánh dùng chung điều kiện, đủ 13 món Thần Linh; không còn bỏ sót găng/giày Thần Namếc và Xayda.
- Xác nhận dùng một lần và kiểm tra lại trạng thái đồ, vị trí trong túi, tiền và giao dịch đang mở. Món thay đổi sau menu phải chọn lại.
- Túi đầy vẫn xử lý được bằng thay đồ trong hai ô nguyên liệu. Không cần cấp đồ vào ô thứ ba rồi bỏ qua kết quả cấp.
- Hai món bị tiêu hao khi thành công. Thiếu tiền, chọn sai, hủy hoặc xác nhận lặp không mất thêm đồ/tiền.
- Không đổi schema, ID hoặc level SQL. Nạp item_template/item_option_template có ORDER BY id để giữ ánh xạ vị trí danh sách = ID.
- PlayerDAO tiếp tục lưu data_inventory và items_bag; định dạng JSON giữ được nhiều dòng cùng option ID. Không bổ sung bảo đảm lưu tức thời khi mất điện hoặc server bị kill.

## Kiểm chứng

- Biên dịch toàn bộ production source và test source thành công bằng JDK 21, target Java 17 tương thích bản chạy hiện có.
- 13 lượt kiểm thử qua: vàng, ngọc, toàn bộ 26 nguồn/13 đích từ catalog SQL thật, điều kiện từ chối, chỉ số riêng/chí mạng, SQL lưu–nạp, xác nhận đồng thời/hủy, 768 ca chặn kích hoạt, ba bài hồi quy sao, template index, option packet.
- SQL lưu–nạp dùng bảng TEMPORARY tạo theo ba cột id/data_inventory/items_bag của bảng player. Không sửa dữ liệu player thật; kiểm tra JSON hợp lệ và tiền/đồ đọc lại khớp chính xác. Kiểm tra format nạp lại theo MrBlue giữ cả option trùng ID.
- JAR đóng gói qua 8 lượt kiểm thử chuyển hóa trực tiếp, gồm 768 ca chặn kích hoạt, kiểm tra ZIP và manifest.
- Server cũ được bảo trì bằng lệnh bt để lưu dữ liệu rồi dừng; JAR cũ chuyển vào SRC/jar-backup trước khi cài bản mới, đối chiếu SHA-256. Metadata: .analysis/conversion-check/deployment.json và metadata cạnh backup JAR.
- Server mới đã nạp database và nghe cổng 14445. Chưa kiểm thử thao tác bằng client game thật hoặc đăng nhập nhân vật sau chuyển hóa; không coi kiểm thử bảng tạm là kiểm thử đăng nhập trực tiếp.

Source: ChuyenHoaTrangBiRules.java, hai wrapper ChuyenHoaTrangBi_Vang/Ngoc.java, Combine.java, CombineService.java, BaHatMit.java, Manager.java. Test: SRC/test/nro/models/combine/ChuyenHoaTrangBiTest.java. Script tái lập: .analysis/verify_conversion.py.
