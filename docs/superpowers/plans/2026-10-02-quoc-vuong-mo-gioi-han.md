# Kế hoạch mở giới hạn sức mạnh tại Quốc Vương

**Trạng thái:** Kế hoạch để người dùng duyệt; chưa sửa mã nguồn hoặc JAR.

**Mục tiêu:** Quốc Vương là NPC duy nhất mở giới hạn sức mạnh. Từ mốc 40 tỷ, mỗi lần mở tiêu hao một trang bị đúng cấp đang mặc và 500.000.000 vàng.

**Kiến trúc:** Quy tắc, kiểm tra và giao dịch tiêu hao nằm trong OpenPowerService, Quốc Vương chỉ tạo menu chọn món và xác nhận. NPoint cung cấp một bảng trần thống nhất cho giới hạn hiện tại và tiếp theo. ToSuKaio bỏ menu/handler mở giới hạn, giữ chức năng luyện tập.

**Công nghệ:** Java 17; hệ thống menu NPC, inventory và kiểm thử offline hiện có.

## 1. Quy tắc đã nêu và cách hiểu số

Hiểu các số “39 tỉ 9999”, “49 tỉ 9999”, … và “79 tỉ 999” là mốc ngay trước tròn chục tỷ, tức 39.999.999.999, 49.999.999.999, …, 79.999.999.999. Điều kiện là sức mạnh >= mốc, để người chơi đã vượt mốc theo dữ liệu cũ vẫn có thể mở.

| Bậc đang có | Sức mạnh tối thiểu để mở | Trần sau mở | Món đang mặc bị tiêu hao | Vàng |
| --- | ---: | ---: | --- | ---: |
| 4 | 39.999.999.999 | 49.999.999.999 | 1 thần linh | 500.000.000 |
| 5 | 49.999.999.999 | 59.999.999.999 | 1 thần linh | 500.000.000 |
| 6 | 59.999.999.999 | 69.999.999.999 | 1 hủy diệt | 500.000.000 |
| 7 | 69.999.999.999 | 79.999.999.999 | 1 hủy diệt | 500.000.000 |
| 8 | 79.999.999.999 | 90.010.000.000 (đề xuất giữ trần cuối cũ) | 1 thiên sứ | 500.000.000 |

Thần linh: ID 555–567; hủy diệt: 650–662; thiên sứ: 1048–1062. Dùng isDTL/isDHD/isDTS và chỉ chấp nhận trang bị hợp lệ ở năm ô áo/quần/găng/giày/rada đang mặc, số lượng một. Đồ trong túi hoặc rương không đáp ứng yêu cầu. Đồ đúng template có SKH/sao/cấp nâng cũng sẽ bị tiêu hao nguyên món; menu phải thể hiện rõ.

Từ bậc 4 trở đi chỉ còn một luồng mở: đủ sức mạnh, đủ vàng, đủ món đang mặc, xác nhận thì mở ngay thành công 100%. Không còn đường mở miễn phí bằng chờ thời gian hoặc mở nhanh 50 triệu để bỏ qua nguyên liệu.

## 2. Đề xuất và điểm cần duyệt

- Dưới mốc 40 tỷ (bậc 0–3), giữ cách mở cũ: bản thân mở thường khi đạt trần và chờ 2 giờ 24 phút, hoặc mở ngay 50 triệu vàng. Đệ tử giữ mở ngay 50 triệu ở các bậc này. Bổ sung kiểm tra đạt trần cho mở ngay để tránh bỏ qua yêu cầu sức mạnh.
- Giữ trần cuối 90.010.000.000 và trần HP/MP/sức đánh/giáp/chí mạng từng bậc hiện tại. Người dùng chỉ yêu cầu đổi điều kiện mở; nếu muốn trần cuối 89.999.999.999 hoặc 90.000.000.000 thì cần chọn trước triển khai.
- Người dùng đã chốt áp dụng cho cả bản thân và đệ tử; lấy món đang mặc của người được mở. Mở cho bản thân lấy đồ bản thân, mở cho đệ tử lấy đồ đệ tử. Vàng luôn lấy từ chủ/nhân vật người chơi.
- Người dùng chọn món sẽ mất bằng menu NPC, không tự lấy món đầu tiên. Nếu chỉ có một món vẫn hiện xác nhận trước khi tiêu hao.

## 3. Luồng menu

Quốc Vương → Bản thân/Đệ tử → xem trần hiện tại và trần kế tiếp → chọn một món đúng cấp đang mặc → xác nhận.

Ví dụ: “Mở giới hạn lên 49.999.999.999 sức mạnh. Tiêu hao: Áo thần linh đang mặc và 500.000.000 vàng. Món đồ sẽ mất vĩnh viễn.” Hai nút: “Mở giới hạn”, “Hủy”. Menu chọn món hiển thị tên, ô trang bị và thông tin sao/cấp/options giúp phân biệt các món.

Thiếu sức mạnh, thiếu vàng, không mặc món đúng cấp hoặc đã đạt bậc 9: báo lý do, không tiêu hao. Hủy/đóng menu không tiêu hao.

## 4. Các bước triển khai

- [x] Chốt phạm vi đệ tử: áp dụng cả hai, lấy đồ đang mặc của người được mở.
- [x] Duyệt cách hiểu mốc, trần cuối và các đề xuất trước khi triển khai.
- [x] Tạo `SRC/test/nro/models/player/OpenPowerRequirementTest.java`: kiểm thử dịch vụ thật với các mode thresholds, equipment, fees, confirmation, replay, legacy, pet, menu. Chạy từng mode trước sửa để xác nhận lỗi đúng hành vi cũ.
- [x] Sửa `SRC/src/nro/models/player/NPoint.java`: thống nhất trần hiện tại/tiếp theo; bậc 5–8 đổi thành 49.999.999.999/59.999.999.999/69.999.999.999/79.999.999.999; giữ các trần chỉ số gốc và xử lý bậc 9 theo lựa chọn đã duyệt.
- [x] Sửa `SRC/src/nro/models/services/OpenPowerService.java`: quy tắc theo bậc, kiểm tra sức mạnh/nguyên liệu/vàng; tạo preview giữ chủ thể, bậc, slot và instance món; khi xác nhận kiểm tra lại toàn bộ dữ liệu. Bịt đường gọi openPowerBasic/openPowerSpeed ở bậc mới để không bypass phí hoặc trang bị.
- [x] Sửa `SRC/src/nro/models/player/IDMark.java`: lưu/xóa preview mở giới hạn, dùng menu riêng tránh xung đột menu khác. Giao dịch đồng bộ trên đối tượng chung với thao tác chuyển/tháo đồ; kiểm tra instance và slot ngay trước ghi, không chỉ dựa vào preview.
- [x] Khi xác nhận hợp lệ: tiêu hao đúng một món, trừ 500 triệu bằng long, tăng đúng một bậc và xóa preview trong cùng vùng đồng bộ. Gửi lại body/money, tính lại chỉ số và ngoại hình; lưu theo cơ chế player hiện tại. Không nhận packet xác nhận lặp hoặc áp dụng lại preview cũ.
- [x] Sửa `SRC/src/nro/models/npc_list/QuocVuong.java`: bỏ MAX_LIMIT_CUSTOM=5, cho mở đến bậc 9, thêm chọn món/xác nhận, sửa mọi thông báo phí thành vàng đúng thực tế.
- [x] Sửa `SRC/src/nro/models/npc_list/ToSuKaio.java`: bỏ menu và handler mở giới hạn; giữ đăng ký/tập tự động và gọi boss tập luyện.
- [x] Kiểm tra `SRC/src/nro/models/item/ItemTime.java`: ngăn timer mở thường cũ tăng bậc từ bậc 4 trở đi sau cập nhật. Với timer đang chạy ở bậc 4 trở lên, hủy timer và hướng dẫn mở theo quy tắc mới; bậc 0–3 vẫn hoàn thành theo cách cũ.
- [x] Kiểm tra dữ liệu cũ vượt trần mới: giữ nguyên sức mạnh/chỉ số/bậc đã lưu, không hạ bậc hoặc xóa đồ; ngăn tăng sức mạnh vượt trần và cho mở khi power >= ngưỡng. Người đã bậc 9 không bị thu phí hồi tố.
- [x] Chạy kiểm thử source và JAR bằng `.analysis/verify_gold_economy.py` và `.analysis/build_release_jar.py`, bổ sung các mode mới vào hai script trước khi đóng gói.
- [x] Chỉ sau khi được yêu cầu triển khai: bảo trì bằng bt, xác nhận server dừng, sao lưu JAR, cài bản đã kiểm thử, đối chiếu checksum và xác nhận server chạy lại/lắng nghe.

## 5. Tiêu chí kiểm thử

- Mỗi mốc: thiếu 1 điểm từ chối; đúng mốc và vượt mốc được mở khi đủ các điều kiện còn lại.
- Mỗi cấp trang bị: đúng cấp đang mặc được nhận; sai cấp, chỉ có trong túi/rương, ô rỗng hoặc dữ liệu bất hợp lệ bị từ chối.
- Đủ 500 triệu mở còn đúng 0 vàng; thiếu 1 vàng không mất món; mỗi lần thành công mất đúng 500 triệu và đúng món đã chọn.
- Mặc nhiều món: chọn món nào mất món đó, các món còn lại giữ nguyên.
- Đổi/tháo/chuyển món sau preview, đổi bậc, tiêu vàng hoặc bắt đầu giao dịch: xác nhận thất bại, không tiêu hao.
- Xác nhận hai lần/đồng thời/preview cũ: chỉ một lần thành công; lần còn lại không bị trừ vàng hoặc mất thêm đồ.
- Sau mất đồ: chỉ số/ngoại hình phản ánh món đã tháo; lưu rồi đăng nhập lại vẫn đúng bậc, số vàng và body.
- Quốc Vương có đầy đủ các bậc; Kaio không còn mở qua menu hoặc gửi lại menu ID cũ.
- Không còn bypass qua mở thường, mở nhanh hoặc timer cũ ở bậc cần nguyên liệu.
- Hồi quy 36 mode hiện tại tiếp tục đạt; lưu/đăng nhập lại và menu trên client thật phải kiểm tra thực tế, không đánh dấu đạt chỉ từ test offline.

## 6. Tự kiểm tra kế hoạch

Đã bao phủ yêu cầu gom về Quốc Vương, năm mốc, hai thần linh/hai hủy diệt/một thiên sứ, món đang mặc, phí và tiêu hao; có xác nhận chọn món và ngăn thu phí lặp. Phạm vi đệ tử đã chốt; trần cuối và cách mở dưới 40 tỷ được tách rõ là đề xuất cần duyệt. Workspace hiện không có git repository nên không lên bước commit.


## Deployment status

Implemented and installed after approval. See ../../source-analysis/quoc-vuong-mo-gioi-han.md for final rules, 46/46 source and JAR checks, backup and startup verification. Live client and database save/login round-trip were not exercised.
