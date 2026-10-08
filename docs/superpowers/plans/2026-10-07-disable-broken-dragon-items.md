# Tạm gỡ vật phẩm và điều ước lỗi

Mục tiêu: giữ hai điều ước Namek (ngọc 3 sao và Hổ vàng), gỡ bột mỳ 2053, tránh mất thưởng khi túi đầy; gỡ chức năng/phát mới Siêu Cấp 1015 vì server/client chưa hỗ trợ. Giữ catalog ID để dữ liệu đồ đang có vẫn đọc được.

- [x] Rà SQL, source, resource và packet -83 của hai client.
- [x] Kiểm thử tạm ngưng 1015: creation, use, shop, gift; restore vẫn đọc được đồ cũ.
- [x] Kiểm thử Hổ 942 trang bị, chỉ số, pet, tháo đồ, khôi phục, parts và ảnh.
- [x] Namek: chỉ hai lựa chọn, owner/session/timeout/index guards; chuẩn bị mọi túi trước khi nhận, khóa theo ID, giữ wish khi capacity/pending lỗi. Offline dùng giao dịch cập nhật riêng items_bag và receipt chống thưởng lại khi retry.
- [x] Dựng toàn bộ, hồi quy và review.
- [x] Bảo trì lưu dữ liệu, snapshot SQL, migration, backup JAR với timestamp/hash, cài và xác minh server.