# Kế hoạch sửa chuyển hóa trang bị

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Thực hiện trực tiếp; không tự triển khai lên server trong giai đoạn lập kế hoạch.

**Goal:** Cho trang bị bậc 11–12 đã đập tối thiểu +4 chuyển sang đồ Thần Linh; vàng chuyển cấp và mọi dòng %, ngọc chuyển thêm sao pha lê.

**Architecture:** Hai lớp Vàng/Ngọc dùng một bộ kiểm tra và tạo kết quả chung. Phần xác nhận tính lại điều kiện từ các món thật trong túi, tạo kết quả trước khi thay đổi tiền/đồ và thực hiện dưới khóa nhân vật. Menu xem trước dùng cùng thuật toán với kết quả cuối.

**Tech Stack:** Java 21, server NRO hiện có, MySQL/MariaDB team2026, JSON trong bảng player, các bài kiểm tra Java main theo SRC/test.

## 1. Yêu cầu đã chốt

- Người dùng xác nhận bậc 11, giữ cấp đập tối thiểu +4; không phải đập +11. Cấp đập tối đa hiện là +8.
- Người dùng xác nhận vàng giữ mọi dòng % của nguồn, kể cả % từ sao pha lê, chỉ không chuyển số sao.
- Nguồn là trang bị thường bậc 11 hoặc 12, type 0–4. Không dùng đồ Thần/Hủy Diệt/Thiên Sứ làm nguồn cho luồng này.
- Đích là đúng một món Thần Linh ID 555–567, cùng type và gender. Rada 280/281 và Nhẫn Thần Linh 561 đều type 4, gender 3 nên phải được hỗ trợ.
- Nguồn có sao được phép dùng cả hai nhánh. Không thêm yêu cầu nguồn bắt buộc có sao: món +4 có 0 sao vẫn được chuyển cấp/%.
- Đích chưa nâng cấp và chưa ép sao: 72 = 0, 102 = 0 hoặc không có các option đó; có lỗ trống 107 không phải đồ đã ép sao.
- Cần hai món khác nhau, còn tồn tại trong túi, số lượng hợp lệ. Chọn nguồn trước, đích sau; hướng dẫn ghi rõ thứ tự.
- Bổ sung đã triển khai theo yêu cầu người dùng: từ chối đồ kích hoạt ở cả nguồn và đích, cả vàng/ngọc. Dùng Item.isSKH()/ActivationSet, gồm dòng tên set và dòng hiệu ứng với param bất kỳ; kiểm tra lại khi xác nhận.
- Phí giữ như menu hiện có: vàng 2.000.000.000; ngọc 5.000 từ inventory.gem, không trừ vàng hoặc hồng ngọc.
- Thành công chắc chắn khi đủ điều kiện; tiêu hao hai món đầu vào, nhận một món đích.

| Thuộc tính | Vàng | Ngọc |
|---|---|---|
| Template và chỉ số nền | Đồ Thần Linh đích | Đồ Thần Linh đích |
| Cấp đập 72 | Lấy từ nguồn | Lấy từ nguồn |
| Dòng % thông thường | Chuyển toàn bộ | Chuyển toàn bộ |
| Bonus % do sao đã ép | Giữ | Giữ |
| Sao đã ép 102 | Không lấy từ nguồn | Lấy từ nguồn |
| Số lỗ sao 107 | Giữ lỗ của đích | Lấy cấu hình lỗ của nguồn |
| Set kích hoạt, Tinh/Nhật/Nguyệt Ấn, hạn dùng, yêu cầu sức mạnh | Không sao chép tùy tiện từ nguồn | Không sao chép tùy tiện từ nguồn |

Điểm cần thể hiện rõ trong menu: nhánh vàng giữ bonus %, nhưng các sao đã ép của nguồn sẽ mất. Đây là chính sách người dùng đã chọn. Sau đó người chơi có thể tiếp tục ép sao lên các lỗ của đích nếu còn lỗ; bonus cũ không bị trừ. Không tự áp đặt quy tắc chống cộng thêm bonus ngoài yêu cầu này.

## 2. Kết quả kiểm tra SQL và source ngày 2026-10-03

Đã truy vấn chỉ đọc database thực tế team2026 tại localhost:3306 và đối chiếu hai dump team2026.sql, SRC/sql/nro1.sql cho nhóm trang bị liên quan.

| Bậc | Trái Đất: áo/quần/găng/giày | Namếc | Xayda | Rada/nhẫn chung |
|---|---|---|---|---|
| 11 | 232/244/256/268 — da Calic | 236/248/260/272 — bạc Zealot | 240/252/264/276 — Kaio | 280 |
| 12 | 233/245/257/269 — jean Calic | 237/249/261/273 — vàng Zealot | 241/253/265/277 — lưỡng long | 281 |
| 13, Thần Linh | 555/556/562/563 | 557/558/564/565 | 559/560/566/567 | 561 |

- Các ID, type, gender, level này đúng trong DB thực tế và dump. Không cần đổi level 11 thành 12 hay sửa tên để chiều theo code.
- Đính chính thông tin ban đầu: Jean/Zealot vàng/Lưỡng Long thực tế là bậc 12, không phải bậc 11.
- Code hiện chặn đồ bậc 11 bằng danh sách nguồn cứng chỉ có bậc 12 và Thần; một số tên hàm Jean/Zealot còn gắn nhầm nhóm ID. Cần sửa logic, không sửa SQL cho khớp các tên hàm sai.
- Nhánh Ngọc chỉ nhận ID 555–563: bỏ sót 564–567 (găng/giày Namếc, Xayda). Các hàm chia dải theo hành tinh không đúng cách SQL xếp ID.
- Option 72 là cấp đập; 102 là số sao đã ép; 107 là số lỗ; 209 là số lần rớt cấp. Các ID này tồn tại trong DB.
- EpSaoTrangBi cộng bonus vào option của món đồ. Sao 1–7 có thể gộp vào dòng %, sao 8–9 có dòng riêng. Không có thông tin nguồn gốc bonus trong JSON hiện tại. Yêu cầu giữ toàn bộ % không cần tách bonus gốc và bonus sao.
- SQL option 14 là chí mạng %, đồng thời là chỉ số nền của rada/nhẫn. Không được vừa lấy chí mạng nền nguồn vừa đập lại chí mạng nền đích, gây cộng hai lần.
- DB thực tế có 2000 item_template ID 0–1999, 251 item_option_template ID 0–250, ID không thiếu/không trùng. Ba bảng player/item_template/item_option_template dùng InnoDB.
- Hai dòng player hiện có JSON ngoài items_bag và data_inventory hợp lệ. Đây không phải kiểm chứng mọi option bên trong hoặc thao tác chuyển hóa thực tế.
- Manager nạp template từ SQL, ItemService truy cập danh sách theo vị trí ID. Query hiện không có ORDER BY id: có rủi ro thứ tự dù dữ liệu hiện liền mạch. Cần kiểm chứng và thêm ORDER BY id vào các truy vấn template liên quan để bảo đảm ánh xạ; bài kiểm tra phải chứng minh index = ID.
- Chuyển hóa hiện chỉ sửa bộ nhớ. PlayerDAO.updatePlayer lưu tiền và items_bag bằng cùng câu UPDATE; MrBlue nạp lại các cặp [optionId,param]. Không có bảng chuyển hóa riêng hoặc migration bắt buộc cho quy tắc mới.
- Tính hợp lệ của JSON ngoài không bảo đảm chống mất dữ liệu khi server bị tắt đột ngột. Cần kiểm tra luồng lưu đăng xuất/bảo trì và thử lưu–nạp lại; không tuyên bố đã có giao dịch bền vững chỉ vì khóa Java hoặc InnoDB.

## 3. Các bước thực hiện

### A. Kiểm tra chung và ánh xạ trang bị

Files: tạo SRC/src/nro/models/combine/ChuyenHoaTrangBiRules.java; sửa ChuyenHoaTrangBi_Vang.java, ChuyenHoaTrangBi_Ngoc.java; tạo SRC/test/nro/models/combine/ChuyenHoaTrangBiRulesTest.java.

- [ ] Viết kiểm tra nguồn bậc 10 bị từ chối; đủ 13 món bậc 11 và 13 món bậc 12 được nhận ở +4; +3 bị từ chối; Thần/Hủy Diệt/Thiên Sứ bị từ chối làm nguồn.
- [ ] Kiểm tra đủ 13 đích Thần Linh, gồm 564–567 và Nhẫn 561; sai type/gender bị từ chối, cả hai nhánh cho kết quả giống nhau về điều kiện.
- [ ] Thay các hàm dò ID theo tên hành tinh bằng level/type cho nguồn và whitelist Thần + type/gender của template thật cho đích.

Quy tắc cụ thể:

```text
source != target
source.type in [0,4] && source.level in {11,12}
4 <= source.option(72) <= CombineService.MAX_LEVEL_ITEM
555 <= target.id <= 567
source.type == target.type && source.gender == target.gender
target.option(72) == 0 && target.option(102) == 0
```

### B. Tạo kết quả và menu xem trước chung

Files: ChuyenHoaTrangBiRules.java, ChuyenHoaTrangBi_Vang.java, ChuyenHoaTrangBi_Ngoc.java; tạo SRC/test/nro/models/combine/ChuyenHoaTrangBiOptionsTest.java.

- [ ] Sao chép đích thành kết quả độc lập, giữ yêu cầu sức mạnh, chỉ số nền và các thuộc tính hợp lệ của đích.
- [ ] Tìm chỉ số nền theo type và option ID, không dùng itemOptions.get(0). Nền: áo 47; quần 6 hoặc 22, hồi 27; găng 0; giày 7 hoặc 23, hồi 28; nhẫn 14. Tính riêng từng chỉ số.
- [ ] Xử lý option 14 như chỉ số nền, không sao chép raw chí mạng nguồn như bonus % thứ hai. Sao chép các dòng % còn lại theo danh mục option thông thường; loại option set/mô tả hiệu ứng bộ dù tên chứa ký tự %.
- [ ] Bảo toàn các dòng % riêng của sao 8–9. Không gộp mọi dòng cùng ID một cách mặc định: một số thuộc tính có cách cộng theo từng dòng. Không làm mất thuộc tính nền sẵn có ở đích.
- [ ] Vàng bỏ 102 nguồn và không lấy 107 nguồn; Ngọc lấy cả 102/107, kiểm tra 0 <= số sao <= số lỗ <= MAX_STAR_ITEM. Không sao chép số sao hoặc bonus hai lần.
- [ ] Chuyển đúng 72 và thông tin 209 nếu có. Trong phạm vi bản sửa này giữ hệ số chuyển hóa hiện có (1.1^cấp * 0.9^số lần rớt) nhưng áp dụng riêng trên mỗi chỉ số nền đích. Không tự thay đổi cân bằng đập đồ trong bản sửa này.
- [ ] Ghi kiểm tra riêng cho sai lệch công thức hiện có với NangCapVatPham (+10% làm tròn từng lần, rớt -11%): ghi rõ là quy tắc chuyển hóa kế thừa, không khẳng định kết quả tương đương lịch sử đập thật. Thay công thức lịch sử phải là thay đổi quy tắc riêng.
- [ ] Cả menu và thực thi gọi cùng hàm tạo kết quả; menu vàng ghi rõ giữ % nhưng mất số sao, menu ngọc ghi giữ cả cấp/%/sao.

### C. Thanh toán và đổi đồ an toàn

Files: hai lớp chuyển hóa, ChuyenHoaTrangBiRules.java; tạo SRC/test/nro/models/combine/ChuyenHoaTrangBiPaymentTest.java.

- [ ] Khi bấm xác nhận, khóa synchronized(player), lấy lại hai món trong túi, kiểm tra đủ tiền/đồ/điều kiện; không tin dữ liệu menu đã xem trước.
- [ ] Kiểm tra số ô, số lượng hai món và khả năng cấp kết quả trước khi thay đổi. Ưu tiên thay món đích ngay ở vị trí cũ và tiêu hao nguồn thay vì cần ô trống thứ ba; lưu snapshot để khôi phục khi lỗi trước khi hoàn tất.
- [ ] Vàng kiểm tra gold >= 2_000_000_000L; Ngọc kiểm tra gem >= 5000 và dùng inventory.subGem(5000). Không cho tiền âm.
- [ ] Một lần xác nhận chỉ xử lý một lần; sau thành công xóa trạng thái xác nhận. Thử nhấp lặp, chuyển đồ sau xem trước, hai index trùng, món bị bán/đổi, nguồn hoặc đích rỗng.
- [ ] Chỉ gửi hiệu ứng thành công sau khi đổi đủ tiền/đồ. Không phát hiệu ứng khi thao tác không thành công.
- [ ] Kiểm tra riêng nút Từ chối: BaHatMit hiện đưa select=1 vào startCombineVip dù menu này chỉ có Chuyển hóa/Từ chối. Từ chối phải đóng đúng menu, không thu phí và không đụng đồ.

### D. SQL, lưu–nạp và hướng dẫn

Files: SRC/src/nro/models/server/Manager.java, CombineService.java, BaHatMit.java; tạo SRC/test/nro/models/combine/ChuyenHoaTrangBiSqlTest.java; chỉ sửa PlayerDAO/MrBlue nếu bài kiểm tra phát hiện lỗi lưu–nạp cụ thể.

- [ ] Thêm ORDER BY id trước LIMIT/OFFSET khi nạp item_template; thêm ORDER BY id khi nạp item_option_template; kiểm tra index đúng ID và không phá các bài kiểm tra template hiện có.
- [ ] Dùng SELECT id,type,gender,name,level FROM item_template WHERE (level IN (11,12) AND type BETWEEN 0 AND 4) OR id BETWEEN 555 AND 567 ORDER BY level,type,gender,id để đối chiếu whitelist và các cặp chuyển hóa.
- [ ] Giữ schema JSON hiện có: không thêm cột hoặc đổi ID vật phẩm/option cho yêu cầu này. Kiểm tra round-trip giữ 72, 102, 107, 209 và nhiều dòng % cùng ID.
- [ ] Thử trên dữ liệu nhân vật kiểm thử: chuyển hóa, lưu theo DAO, đăng xuất/đăng nhập, đối chiếu chính xác tiền và túi đồ. Không sử dụng tài khoản thật để tiêu hao đồ thử.
- [ ] Sửa hướng dẫn “cao hơn 1 bậc” thành “đồ bậc 11 hoặc 12, từ +4, chuyển sang Thần Linh cùng loại/hành tinh”; thể hiện phí đúng từng nhánh.

### E. Kiểm thử và triển khai sau khi có yêu cầu thực hiện

- [ ] Biên dịch theo Java 21 và classpath SRC/lib; chạy các main test mới trong SRC/test với assertions/check hiện có. Chạy hồi quy ép sao, nâng cấp, ánh xạ option/template, gửi option qua mạng.
- [ ] Ca mẫu vàng: Găng Kaio 264 +4, 5 sao, SĐ +15% -> Găng Thần Xayda 566 +4, giữ SĐ +15%, không có sao đã ép của nguồn.
- [ ] Ca mẫu ngọc: cùng đầu vào -> Găng Thần Xayda 566 +4, giữ SĐ +15%, giữ 5 sao và cấu hình lỗ nguồn.
- [ ] Túi đầy vẫn chuyển được nếu thay thế hai ô đầu vào an toàn; các lỗi khác phải giữ nguyên tiền/đồ.
- [ ] Chỉ khi được yêu cầu triển khai: bảo trì có lưu dữ liệu, kiểm tra server đã dừng, chuyển SRC/20.jar cũ vào SRC/jar-backup với tên thời gian duy nhất; ghi SHA-256 và metadata bản cũ/mới trước khi đặt JAR mới.
- [ ] Không để JAR ứng dụng staging dư trong thư mục chạy; không di chuyển thư viện SRC/lib. Có đường khôi phục JAR cũ; việc khôi phục JAR không tự hoàn tác đồ đã chuyển hóa.

## Trạng thái

Đã triển khai source và cập nhật SRC/20.jar sau bảo trì có lưu dữ liệu. 13 lượt kiểm thử qua, gồm 768 ca chặn đồ kích hoạt; 8 lượt kiểm thử trực tiếp JAR qua. Server khởi động lại và nghe cổng 14445. SQL được thử trên bảng tạm, chưa thử đăng nhập/client game thật. Chi tiết: docs/source-analysis/chuyen-hoa-trang-bi-2026-10-03.md.
