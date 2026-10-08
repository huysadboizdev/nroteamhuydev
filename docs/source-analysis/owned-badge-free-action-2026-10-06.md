# Tab Sở hữu: Nhận miễn phí — 06/10/2026

Đã sửa `TabShopSoHuu` đặt `copy.cost=0` và `copy.typeSell=0` trên bản sao dành cho người mở shop. Catalog và tab Danh hiệu giữ giá cũ. Packetshop -44 gửi giá vàng/ngọc0/0; `Panel.doFireShop` Game1/Game2 đã có nhánh tạo một nút “Nhận miễn phí”, không thêm mua20/50/100lần. Không cần đổi Unity hay SQL.

Thao tác vẫn đi tab45 → `changeDanhHieu`, kích hoạt danh hiệu đã sở hữu còn hạn, không tạo thêm bản ghi hay gia hạn. Giữ guard sở hữu, thời hạn, điều kiện Mẹ Rồng, đã dùng và cooldown.

Regression trên JAR cũ thất bại đúng ở giá bản sao2ngọc và packet0/2; các kiểm tra hành vi kích hoạt đã qua trước sửa, xác nhận nguyên nhân nằm ở giá hiển thị. Bản mới qua24nhóm:6miễn phí,5Gõ đầu trẻ,8BadgeSystem,5DragonBadge. Fixture miễn phí kiểm tra packet thật, catalog/tabDanh hiệu giữ giá, không trừ vàng/ngọc/ruby, không gia hạn/tăng bản ghi, nhấn lặp và từ chối hết hạn/không sở hữu/đã dùng.

JAR chỉ thay `nro/models/shop/TabShopSoHuu.class`; mọi entry khác giống bản trước. Triển khai07:11:49 UTC+7 sau bảo trì lưu dữ liệu, xác nhận JVMdừng, snapshotSQL, backup và kiểm SHA256. Server PID9156 chạy cổng14445; probe chỉ đọc xác nhận class thật khớp bản kiểm thử.

Backup:`SRC/jar-backup/20-before-owned-badge-free-20261006-071137.jar`.
SHA256cũ:`fffcf718bb5a81d40014d9addb80d76800d5b5e90bad64555456606493b1fdfa`.
SHA256mới:`51019fb985e9c2db2fd14e65bec35d1ebab66c7a6cb3da88c7effb6ec23f38a6`.
Metadata cạnhbackup và `.analysis/owned-badge-free-deployment-latest.json`.

Chưa kiểm chứng hình ảnh bằng thao tác trên Unity sau triển khai. Cần đóng bảng shop cũ, đăng nhập lại và mở tabSở hữu để nhận packet mới. Không commit/pushGitHub. Khôi phục bằng bảo trì lưu dữ liệu, dừng server rồi phục hồi JARbackup; không có SQLmigration cần rollback.
