# Tách Mặt Trời theo khu

Theo yêu cầu người dùng, giữ 20 Mặt Trời và các map 0/5/7/14; mỗi khu tối đa một Mặt Trời. Loại boss khác được đứng chung khu. Quét khu theo thứ tự; chọn khu chưa có Mặt Trời, kể cả chờ boss chết rời hẳn map. Khóa chọn khu và vào khu để tránh hai lần hồi sinh đồng thời. Map hết khu phù hợp thì bỏ mục tiêu cũ, chọn lại khi RESPawn, không sinh trùng.

Kiểm thử: 20 con tách 20 khu, map đầy không sinh chồng, đứng cùng loại khác, vào đồng thời. Chạy toàn bộ hồi quy boss mini và 63 kiểm thử trước; kiểm tra runtime đủ 20 Mặt Trời và mỗi cặp map/khu không trùng. Bảo trì lưu dữ liệu, backup JAR timestamp/hash trước cập nhật, xác minh server mới.

Hoàn thành: 98 lượt kiểm thử PASS trên classes, candidate và JAR đã cài. Chỉ đổi MatTroi.class. Runtime PID 10320 mở cổng 14445, đủ 20 Mặt Trời ở 20 cặp map/khu riêng, tất cả đăng ký trong khu; số tối đa mỗi khu là 1. Các loại boss khác được phép cùng khu, không thay hồi sinh/hiệu ứng/thưởng. Backup `SRC/jar-backup/20-before-sun-zones-20261006-100950.jar`, metadata `.analysis/sun-zones-deployment-latest.json`, runtime `.analysis/mini-boss-audit/sun-zones-after.txt`. JAR SHA-256 `554b6f8bfd83618da7f2a474a118a77b60bd1c49ca655ec6557c3be63071d375`.
