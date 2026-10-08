# Sửa hướng dẫn Rồng Omega — 05/10/2026

Đã sửa `ConstNpc.HUONG_DAN_BLACK_BALL_WAR` theo hiệu ứng ngọc sao đen đã triển khai:

- 3 sao: +35% KI tối đa.
- 5 sao: Hút HP bằng 35% sát thương.
- 6 sao: +20% giáp, đúng cơ chế `tlGiap` hiện tại.

Biên dịch lại cả `ConstNpc` và `RongOmega` vì Java nhúng hằng chuỗi vào lớp gọi. Kiểm tra bản JAR mới: chỉ hai class này thay đổi, toàn bộ entry khác giống bản đang dùng. Không cần đổi client Unity hoặc SQL.

Đã bảo trì qua `Maintenance.startSeconds(5)` để lưu dữ liệu, xác nhận các JVM chạy JAR ứng dụng đã dừng, snapshot SQL, chuyển JAR cũ sang `SRC/jar-backup/20-before-black-ball-description-20261005-233139.jar`, đối chiếu SHA-256 rồi cài JAR mới. Thư mục chạy chỉ còn `20.jar`.

JAR mới SHA-256: `dc0e0d5f31c6700949865428c891b45120d21851a24853d0b5533ffaccac0dc7`.
Backup SHA-256: `bbe01782f637cd5237e7331a9842085951f95f4890ce01206c56abb5bb844d38`.

Server khởi động lại PID 38632, cổng 14445. Công cụ chỉ đọc xác nhận mô tả mới trong `ConstNpc` và bytecode `RongOmega` trên JVM thật. Chưa xác minh ảnh chụp client sau cập nhật; đóng bảng hướng dẫn cũ và mở lại để nhận nội dung mới. Các cảnh báo thiếu tile khi khởi động là những cảnh báo đã tồn tại trước bản sửa.

Metadata và bằng chứng: `.analysis/black-ball-description-deployment-latest.json`, `.analysis/black-ball-description-20261005-233105/runtime.txt`, metadata cạnh JAR backup. Muốn khôi phục: bảo trì lưu dữ liệu và dừng server trước khi phục hồi JAR backup; bản cập nhật này không chạy migration SQL. Chưa commit/push GitHub.
