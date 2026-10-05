# Timed Bag Cleanup Implementation Plan

**Goal:** Dọn đồ có hạn trong hành trang qua NPC Chi Chi và giữ trang bị 0 ngày đến hết ngày.

**Architecture:** Service riêng kiểm soát preview/confirm và khóa player. PlayerDAO quan sát kết quả full save; ItemService tính ngày lịch cho trang bị. Tách chức năng dọn khỏi hết hạn tự động.

**Tech Stack:** Java 17, Java main offline tests, Python release tooling, Windows PowerShell.

- [x] Kiểm thử RED trên `SRC/20.jar` bằng `SRC/test/nro/models/services/TimedBagCleanupTest.java`: phạm vi, stale, guards, save failure, calendar, online, concurrent, menu.
- [x] Tạo `SRC/src/nro/models/services/TimedBagCleanupService.java` với prepare/confirm/cancel, snapshot và rollback, audit log; lưu preview trong `IdMark.java`.
- [x] Sửa `ChiChi.java` và `ConstNpc.java`: nút mới trước Đóng, menu xác nhận riêng, hủy và consume một lần.
- [x] Thêm `PlayerDAO.saveChecked` quan sát kết quả lưu thật, không bypass pending guards.
- [x] Sửa `ItemService.java`, `MrBlue.java`, `Player.java`: hết hạn đồ đang mặc theo ngày lịch, giữ 0 ngày, chạy online và lúc tải trang bị.
- [x] Chạy `python .analysis/build_release_jar.py`: biên dịch source, regression source và JAR; thêm tám mode mới vào cả hai runner.
- [x] Review source, xác nhận JAR không chứa tests/cache, class đúng source và các test đạt.
- [x] Bảo trì server có lưu dữ liệu, kiểm tra tiến trình đã dừng, backup và metadata SHA-256; cài JAR, gom staging cũ, khởi động ẩn và kiểm tra cổng thực tế.
- [x] Ghi báo cáo kết quả và giới hạn kiểm thử thực tế.
