# Hộp quà Set kích hoạt 5 sao

Người dùng đã duyệt triển khai ngày 22/10/2026 sau khi xem thiết kế trong hội thoại.

**Mục tiêu:** Sử dụng hộp 1538 mở menu ba lựa chọn; mỗi lần tiêu thụ một hộp để nhận một món kích hoạt, 5 ô pha lê trống, khóa giao dịch.

**Quy tắc đã duyệt:** Random hành tinh, loại trang bị và set tương ứng với hành tinh. Thường dùng danh mục capsule legacy và chỉ số shop hiện tại; VIP dùng danh mục VIP đủ các bậc và chỉ số shop tối đa; Thần dùng đồ Thần Linh 555–567 với chỉ số gốc. Không thêm phí. Mỗi lần mở nhận một món. Option 107=5; không có option 102, 72 hoặc phần trăm ép đá; option 30=1. Hộp giữ nguyên nếu thiếu chỗ, lựa chọn cũ, mất hộp hoặc lỗi lưu.

**Kiến trúc:** Service riêng quản lý lựa chọn gắn với đúng hộp, session và thời hạn 120 giây. Tái sử dụng ItemService và SKHCapsuleCatalog để tạo món. Tái sử dụng cơ chế receipt/rollback/reconciliation trong ConsignTransactions để lưu trừ hộp và phần thưởng cùng lúc; thêm action box1538 trong ConsignDAO. Menu được gửi bằng NPC con mèo hiện có.

- [x] Viết và chạy kiểm tra đỏ: catalog đầu ra cả ba nhóm, 5 ô trống, khóa giao dịch, menu đóng/hết hạn/session thay đổi.
- [x] Viết và chạy kiểm tra đỏ: giao dịch trừ đúng một hộp, không phí, túi đầy, vật phẩm cũ, bấm đồng thời, lỗi SQL và mất phản hồi commit.
- [x] Tạo `SRC/src/nro/models/services/ActivationGiftBoxService.java`: tạo món, kiểm tra dữ liệu, quản lý lựa chọn dùng một lần và thông báo.
- [x] Thêm nhánh dùng 1538 vào `services_func/UseItem.java` và nhánh menu vào `npc/NpcFactory.java`.
- [x] Thêm `redeemActivationBox` vào `shop_ky_gui/ConsignTransactions.java`; chấp nhận action `box1538` trong `database/ConsignDAO.java`.
- [x] Chạy các kiểm tra mới, kiểm tra hồi quy hộp Thần Linh và catalog SKH; biên dịch source Java.
- [x] Theo yêu cầu update JAR trước đó: đóng gói ứng viên sau khi kiểm tra; chỉ cài khi server đã dừng có lưu dữ liệu. Backup JAR cũ với thời gian, SHA-256 và metadata trước khi thay.

Không sửa catalog, tỷ lệ nâng SKH, tiến độ tích lũy, SQL item template hoặc tài nguyên icon. Source kiểm thử dùng dữ liệu giả trong RAM và DAO giả, không cấp vật phẩm trên server thật.

## Kết quả thực hiện

- Full compile Java 17 thành công (892 class). Kiểm tra đỏ xác nhận thiếu service/thiếu redemption trước khi viết implementation.
- `ActivationGiftBoxTest`: PASS 1.855 tổ hợp, kiểm tra thêm dữ liệu shop SQL trích sẵn; chạy lại thành công trực tiếp trên JAR ứng viên.
- `ActivationBoxTransactionTest`: PASS ba nhóm, không thu phí, trừ một hộp, đầy túi, tham chiếu cũ, trade, SQL rollback, mất phản hồi commit và hai lời gọi đồng thời.
- `DivineEquipmentBoxTest`, `DivineBoxTransactionTest`, `SKHVipCatalogTest`: PASS.
- `ConsignDbTest`: PASS trên schema MySQL mới riêng; có kiểm tra action box1538, lưu túi và receipt, replay không ghi đè dữ liệu; không thay đổi DB game.
- Rà soát độc lập theo skill requesting-code-review: không phát hiện lỗi cần sửa.
- JAR ứng viên: `SRC/jar-backup/activation-gift-box-20261022-183133/20-candidate.jar`.
- SHA-256 ứng viên: `B9FE348BD763FF20CD6D54F7321E4EC0B32FD7DCAF6A7D5633B6CD977B2B6AE1`.
- Đối chiếu class service/menu/DAO/transaction trong JAR với class vừa biên dịch: trùng SHA-256. Tổng JAR: 978 entries.
- Đã cài vào `SRC/20.jar` ngày 22/10/2026 sau khi gọi `Maintenance.startSeconds(5)` để lưu dữ liệu và dừng JVM. Script restart tự bật lại ngay lần đầu; đã tạm ngăn restart, gọi bảo trì lần nữa và khôi phục nguyên trạng script sau khi cài.
- Backup JAR cũ: `SRC/jar-backup/deploy-activation-gift-box-20261022-184138/20-before-update-20261022-184225.jar`, SHA-256 `268A8FE8300868535451796D1270C4368F24FB777BD288D33C16C5119B81DC3C`. SHA-256 JAR đang dùng trùng bản ứng viên nêu trên.
- Đã khởi động lại server ẩn, PID 8816; xác nhận LISTEN cổng 14445 và log `Server initialized and listening on port 14445`. Metadata: `SRC/jar-backup/deploy-activation-gift-box-20261022-184138/deployment.json`.
- Đã chuyển hai JAR dựng cũ (candidate tích lũy SKH và compile-base) vào backup, ghi bảng đối chiếu SHA-256; thư mục chạy `SRC` chỉ có `20.jar`.
