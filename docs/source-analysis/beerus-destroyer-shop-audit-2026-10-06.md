# Kiểm tra shop Hủy Diệt Beerus — 06/10/2026

Chỉ kiểm tra và lập kế hoạch. Không thay source ứng dụng, SQL, JAR hoặc dữ liệu người chơi.

## Kết quả SQL và server

Runtime lúc 08:53:37 giờ Việt Nam, PID 5936: shop `BILL`, ID 11, NPC 55, loại shop 0. SQL và cache cùng có 15 dòng shop, tương ứng 13 loại đồ Hủy Diệt; Nhẫn 656 xuất hiện ở cả ba tab 21/22/23.

Giá hiện tại đều bằng vàng: áo 800 triệu; quần 1 tỷ; găng/nhẫn 2 tỷ; giày 720 triệu. Chưa có giá 100 hồng ngọc.

`ShopService`, `Bill`, `InventoryService` đang chạy khớp class trong JAR cài; Bill/Inventory còn khớp lần full compile source gần nhất. Bằng chứng: `.analysis/beerus-shop-audit/runtime.txt`.

## Lỗi và điểm chưa đúng yêu cầu

1. `ShopService.buyItemHD()` trừ tiền trước khi kiểm tra thức ăn và đồ Thần. Thiếu nguyên liệu có thể mất tiền; sau trừ thức ăn nếu thiếu đồ Thần không hoàn lại.
2. Thức ăn hiện cần một ô của một trong các ID 663–667 có số lượng >=99. Không cộng tổng nhiều ô cùng loại.
3. Code chỉ tìm bất kỳ món level 13 đang mặc, không yêu cầu cùng loại/hệ với đồ Hủy Diệt mua, **không tiêu hao món Thần**.
4. `canOpenBillShop()` yêu cầu đủ 5 món Thần đang mặc + 99 thức ăn trong hành trang: **đúng điều kiện người dùng yêu cầu giữ**. Helper đủ bộ hiện chỉ kiểm tra level13, cần kiểm tra thêm đúng ô/loại/hệ, quantity và hiệu lực. Đường mua phải kiểm tra lại toàn bộ điều kiện, không chỉ dựa vào lần mở shop.
5. Menu Bill map 48 đặt `indexMenu=100` nhưng nhánh map 48 không xử lý index 100. Đoạn mở shop nằm ở map 2 và kiểm tra `select==100`, khác hành vi bấm OK bình thường. Fixture chạy trên JAR hiện tại xác nhận map48/index100/select0 không mở shop: `.analysis/BeerusMenuAudit.java`.
6. Bonus hiện tại là nâng chỉ số nền 0–15%, áp dụng vào các option được `optionCanUpgrade` chấp nhận, không phải một dòng HP/KI/SD 1–5%. Ngay cả giáp/chí mạng có thể được tăng nền.
7. Đường mua này chưa giữ khóa player bao trùm kiểm tra/trừ/người nhận; bỏ qua kết quả `addItemBag`, không rollback khi cấp vật phẩm thất bại.
8. Code cuối cùng thêm option 30=0 (`Không thể giao dịch`); không phù hợp việc coi phần thưởng là đồ thường nếu không có yêu cầu khóa giao dịch riêng.

## Cơ chế ấn đã đối chiếu

SQL `equipment_seal_rule`: đồ thường có khoảng bonus 1–5%; đồ Thần có 1–7%; option bonus lần lượt 50=sức đánh, 103=KI, 77=HP. Option 34/35/36 là dấu ấn, hiệu ứng đủ bộ là cơ chế riêng.

Mã nguồn hiện chỉ có helper nhận diện/hạn chế giao dịch `SealedEquipment`; chưa tìm thấy bộ load/roll các bảng seal này trong source. Không có bảng trọng số xác suất trong `equipment_seal_rule`. Vì vậy không khẳng định đã có một phân phối xác suất runtime của đồ ấn để sao chép. Kế hoạch đề xuất chọn đều một trong ba dòng, sau đó chọn đều 1–5%, giống khoảng random của ấn thường trong SQL.

`NPoint` đã hỗ trợ option 50/77/103 khi mặc đồ, không cần thêm effect ấn mới vào NPoint.

## Yêu cầu đã chốt

- Mỗi lượt: 99 thức ăn + tiêu hao 1 món Thần Linh tương ứng + 100 hồng ngọc.
- Người dùng xác nhận **chỉ một dòng HP/KI/sức đánh ngẫu nhiên**; không thêm cả ba dòng.
- Bonus đề xuất 1–5%, tối đa 5%, hiển thị thành dòng riêng. Giữ nền gốc của món, bỏ roll nền cũ 0–15%.
- Không có option ấn 34/35/36; không cộng hiệu ứng đủ bộ ấn; không tự gắn cờ giao dịch kiểu đồ ấn.
- Người dùng chốt: phải mặc đủ 5 món Thần và có 99 thức ăn trong hành trang mới mở shop; kiểm tra lại khi bấm OK và ở mỗi lần mua.
- Tiêu hao đúng món Thần **đang mặc** tương ứng với đồ Hủy Diệt mua. Không lấy đồ trong túi/rương thay thế và không tự mặc phần thưởng.
- Sau lần đầu còn 4 món, lần mua tiếp phải báo **“Thiếu 1 món Thần trên người, không được mua.”**, không trừ thêm tiền/thức ăn. Shop cũ đang mở, mua nhiều lần, packet gửi lại và giao dịch đồng thời đều phải tuân theo kiểm tra này. Mặc bổ sung đủ bộ mới được mua tiếp.
- Khi giao dịch thất bại phải hoàn tác cả trang bị, hành trang và ví; không sửa hồi tố đồ đã mua.

Kế hoạch: `docs/superpowers/plans/2026-10-06-beerus-destroyer-ruby-trade.md`.
