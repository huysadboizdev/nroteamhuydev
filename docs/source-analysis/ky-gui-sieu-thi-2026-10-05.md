# Kiểm tra ký gửi siêu thị — 05/10/2026

## Phạm vi và bằng chứng của bản cũ

Đã đọc `ConsignShopService`, `ConsignShopManager`, `ConsignItem`, `Trade`, NPC `KyGui`, SQL đang chạy. Probe offline gọi lớp ký gửi thực trong `SRC/20.jar`; chỉ dùng nhân vật giả, stub DB save/network, không thay đổi người chơi hoặc shop đang chạy.

Probe: `.analysis/ConsignAuditProbe.java`. Kết quả: `.analysis/consign-audit-20261005/runtime-findings.txt`. Classpath chạy chỉ gồm application JAR và class probe/fixture, không có class production rời.

Lệnh chạy lại:

```powershell
# Chỉ dùng JAR cũ đã backup để tái hiện lỗi; không chạy probe này như test PASS bản mới.
```

Kết quả mong đợi của probe kiểm tra lỗi hiện tại: `AUDIT COMPLETE: 5 defects reproduced offline on application JAR`. Đây là xác nhận lỗi còn tồn tại, không phải xác nhận siêu thị hoạt động đúng.

## Lỗi đã tái hiện trên JAR

| Mức | Lỗi | Bằng chứng và tác động |
| --- | --- | --- |
| Cao | Lệch đơn vị vàng | Mua giá 100 trừ 100 vàng thường, người bán nhận 90 thỏi vàng sau phí 10%. `buyItem` dùng inventory.gold nhưng `claimOrDel` tạo template 457. |
| Cao | Thu tiền dù giao đồ thất bại | Stub `addItemBag=false` nhưng người mua vẫn mất 10 ngọc, listing vẫn isBuy=true. Cả mua và hủy listing bỏ qua boolean trả về của addItemBag. |
| Yêu cầu mới chưa được đáp ứng | Ký gửi ngọc dùng ngọc xanh | Người mua gem 100→80, ruby giữ 100; người bán gem 100→118, ruby giữ 100. JAR chưa có thanh toán ruby ở nhánh này. |
| Vừa | Trang gửi 21 món | `getItemKyGui2` chạy vòng `i <= max`, khoảng 0..20 trả 21 thay vì 20. |
| Vừa | Trang thứ 7 lỗi chỉ số âm | page*20=140 ép byte thành -116; truy cập danh sách gây IndexOutOfBoundsException. |

## Vấn đề phát hiện khi đọc mã, chưa kiểm thử đồng thời/DB lỗi

- `ConsignShopManager.save` dùng TRUNCATE trước khi INSERT từng dòng; không dùng transaction phục hồi snapshot. Lỗi giữa chừng có thể làm mất dữ liệu ký gửi; TRUNCATE có implicit commit nên không thể khắc phục bằng chỉ bọc transaction quanh nó.
- Mua/hủy/nhận tiền không có khóa chung hoặc chuyển trạng thái nguyên tử. Hai yêu cầu cùng listing có thể vượt qua kiểm tra trước khi trạng thái thay đổi. Cần test đồng thời trước khi khẳng định kịch bản phát sinh thực tế.
- `claimOrDel` cộng tiền trước `listItem.remove`; nếu xung đột, tiền có thể đã cộng nhưng remove không thành công.
- `KiGui` chặn mọi món có option 30, trong khi yêu cầu mới là cấm giao dịch trực tiếp nhưng vẫn cho ký gửi ruby. Cần ngoại lệ có kiểm tra marker 34/35/36 và chính sách SQL, không bỏ chặn option 30 toàn hệ thống.
- `itemCanConsign`/đăng bán không dùng cùng một tập điều kiện: UI nhận option 86/87, endpoint đăng bán không xác thực itemCanConsign. Đồ khóa có thể xuất hiện trong UI nhưng gửi lên lại bị chặn.
- Hai hàm lấy trang lọc khác nhau: hàm đếm loại đồ của chính người xem, hàm gửi trang không loại. Số trang và dữ liệu có thể không khớp. Công thức số trang `n/20+1` cũng dư trang khi n chia hết 20.
- Up top hiển thị phí 5 ngọc xanh nhưng endpoint thu 2 thỏi vàng. Xác nhận up top không kiểm tra lại chủ sở hữu/trạng thái listing trước thu phí.
- Mã tạo item khi mua/hủy dùng `createNewItem` rồi `addAll(options)` mà không clear/deep-copy; phải kiểm tra giữ option chính xác và không nhân đôi default khi sửa.

## Dữ liệu SQL hiện tại

Tại thời điểm kiểm tra có 3 listing, đều chưa bán và đều giá vàng. Không có listing giá ngọc. Kiểm tra lượng <=0/>99, hai đồng tiền cùng bật/không có tiền và tab ngoài 0..3 đều cho 0 lỗi. Đây là kiểm tra cấu trúc hiện tại, không chứng minh luồng mua/bán an toàn.

Không chuyển các listing cũ sang ruby. Cột `gem` hiện mang ý nghĩa ngọc xanh theo JAR đang dùng; khi bổ sung ruby cần lưu currency riêng để không đổi nghĩa tiền của listing đã tồn tại.

## Chính sách đồ có ấn đã lưu SQL

`equipment_seal_market_rule`: 34/35/36 đều `direct_trade_allowed=0`, `trade_lock_option_id=30`, `consign_option_id=87`, `consign_currency='ruby'`. Áp dụng cả đồ thường và Thần Linh có ấn, không áp cho đồ không có ấn chỉ vì trùng template.

Khi sinh/chuẩn hóa đồ ấn: giữ option 30, chỉ hiển thị option ký gửi ngọc 87 và bỏ option ký gửi vàng 86. Java phải kiểm tra marker khi giao dịch trực tiếp và khi xác nhận giao dịch; ký gửi chỉ chấp nhận nhánh ngọc và thu/trả ruby. Không sửa tên option 87 toàn hệ thống vì sẽ ảnh hưởng đồ/listing không thuộc chính sách.

Migration: `SRC/sql/migrations/20261005_equipment_seal_market.sql`, đã áp lên team2026 và nối vào hai dump. Chạy lại vẫn đúng 3 dòng, 0 dòng cấu hình sai; shop_ky_gui vẫn có 3 listing. Snapshot SQL trước sửa, bản dump trước sửa và metadata/hash nằm ở `.analysis/seal-market-sql-20261005-094533-447/`.

**Trạng thái kiểm kê ban đầu:** Các lỗi phía trên được ghi nhận trên JAR cũ. Bản sửa đã triển khai theo mục cập nhật bên dưới; chưa thử mua bán bằng tài khoản người chơi thật.


## Cập nhật: bản sửa đã cài ngày 05/10/2026

- Toàn bộ ký gửi mới chỉ thanh toán bằng hồng ngọc (ruby). Đăng bán thu 1 ruby; up top thu 5 ruby; nhận tiền bán trừ phí `floor(giá * 10 / 100)`. Không trừ/cộng vàng hoặc ngọc xanh. NPC đã thông báo cùng chính sách này.
- Ba listing cũ giữ nguyên dữ liệu, đánh dấu `legacy`; không cho mua hoặc up top. Chủ sở hữu chỉ lấy lại món chưa bán để đăng lại bằng ruby. Không tự chuyển giá vàng/ngọc xanh thành giá ruby.
- Đồ có ấn type 0..4 mang marker 34/35/36 bị chặn giao dịch trực tiếp ở cả bước đưa đồ và xác nhận. Chuẩn hóa option 30/87, bỏ 86; ký gửi vẫn cho phép nhóm đồ này. Đồ khóa khác giữ điều kiện hạn chế cũ.
- Tiền, toàn bộ kho đồ và trạng thái listing ghi trong cùng transaction SQL InnoDB. `consign_receipt` giữ mã giao dịch chống thực hiện lại. Bỏ lưu shop bằng TRUNCATE. Lỗi SQL trước commit không đổi RAM; kết quả commit chưa rõ giữ khóa tài khoản cho tới khi đối soát xong.
- Mua/hủy kiểm tra sức chứa trên bản sao túi đồ trước commit; giữ nguyên option và thời gian tạo. Khóa và đối chiếu listing ngăn hai người mua cùng món, claim lặp, thu phí top lặp và request thay giá.
- Sửa phân trang 20 món, offset int, cùng điều kiện lọc khi đếm/gửi. Alias short theo phiên hỗ trợ ID SQL trên 32767 và từ chối ID chưa hiển thị/giá cũ. Phiên đã dùng hết 32767 alias cần đăng nhập lại; ID SQL không bị giới hạn ở mốc này.
- Luồng mất kết nối/đăng xuất giữ khóa cho tới khi đối soát và lưu đủ dữ liệu nhân vật; có dấu đã lưu để tránh tác vụ đăng xuất trễ ghi đè tài khoản vừa đăng nhập lại.

Migration `SRC/sql/migrations/20261005_consignment_ruby.sql` đã áp lên DB và nối vào cả hai dump. Giữ nguyên 3 listing cũ, bổ sung currency/create_time, auto_increment và receipt. Quy tắc thị trường marker trong Java hiện cố định theo chính sách đã chốt trong SQL; chưa có loader cho phép đổi chính sách bằng sửa bảng.

**Kiểm chứng:** 20 nhóm đạt trên đúng JAR cài đặt: 14 nhóm ký gửi (phân trang, tiền tệ, túi đầy, đăng, legacy, rollback, commit chưa rõ, đồng thời, top/tràn số, hai thứ tự đăng xuất, wire ID, hủy đầy túi, khóa đồ ấn); 4 hồi quy Trade/wire/túi/default option; 2 bộ DB thật cô lập cho ký gửi và RubyGift. Không thực hiện giao dịch thử trên DB người chơi thật. Log: `.analysis/consign-release-20261005-101431/verified-tests.log`.

Đã bảo trì có lưu dữ liệu, xác nhận JVM game dừng rồi mới thay JAR. Server PID 35836 mở cổng 14445; kết nối TCP localhost thành công, loader đọc đủ 3 listing. Chạy ẩn không có stdin khiến console lệnh cũ ghi `Scanner.nextLine: No line found`; server và cổng vẫn chạy. Các cảnh báo tile map có sẵn nằm ngoài phạm vi ký gửi.

JAR mới SHA-256: `adb03921a029864e4b611eba3d28c65abf341c19983d08007b54c29f14ebc5d5`.
Backup JAR: `SRC/jar-backup/20-before-consignment-ruby-20261005-101803.jar`.
Snapshot SQL trước migration: `.analysis/consign-deploy-20261005-101803/team2026-before-consignment.sql`.
Metadata/hash/cách khôi phục: `SRC/jar-backup/consignment-ruby-20261005-101803.json`.

Khôi phục cần bảo trì/lưu dữ liệu và giữ receipt; không chạy JAR vàng/ngọc xanh cũ trực tiếp trên listing ruby mới. Phần sinh đồ ấn và tính bonus random/bộ 5 món vẫn chỉ được cấu hình SQL, chưa triển khai engine trong lần sửa ký gửi này.
