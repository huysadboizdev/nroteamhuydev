# Tiểu đội Bojack dùng thưởng tương tự Namek — đã triển khai

Áp dụng BUJIN, KOGU, ZANGYA, BIDO, BOJACK (cả2 form), SUPER_BOJACK. Mỗi boss rơi5 capsule722 public quantity1, ai cũng nhặt được và được tính số capsule nhặt như cơ chế hiện tại. Bỏ ngọc xanh ID77. Giữ cải trang423–428 và ngọc rồng19/20, đặt owner người hạ boss tới khi hết thời gian rơi như Namek. Giữ5 điểm sự kiện hiện có; không thay spawn/chiến đấu. Guard synchronized phát thưởng một lần mỗi đời, reset khi initBase/resetBase; không sửa DB hoặc số dư người chơi.

Kiểm thử RED trước sửa thất bại vì Bujin chưa rơi capsule. Candidate72/72 nhóm PASS, gồm67 regression và5 nhóm Bojack: drops, duplicate/reset, pickup, invalid, concurrent/hai form. Xác minh chỉ6 class boss thay đổi so với JAR cũ. JAR đã cài kiểm thử drops/concurrent PASS. Runtime capsule, vé, phí PASS; cổng14445 mở; tiến trình mới bắt đầu sau thời điểm cài. Chưa kiểm tra trực tiếp gameplay bằng client.

JAR cũ đã MOVE vào `SRC/jar-backup/20-before-bojack-pink-20261004-140734.jar` sau bảo trì lưu dữ liệu và xác minh JVM/cổng dừng. Thư viện và JAR công cụ được giữ; staging đã chuyển thành SRC/20.jar.

SHA-256 cũ: `016d54f85ade9e51e9c32dc94f227d96208f3829f487e1da6bc4e1e06e5069b7`.

SHA-256 mới: `52be7070c78f78e9ffb8fe04ddb2909ed99b575a7043ce02c65fa3623b87b0ed`.

Metadata: `.analysis/bojack-pink/deployment.json` và JSON cạnh backup. Source backup/log: `.analysis/bojack-pink/`. Khôi phục cần bảo trì có lưu, backup bản hiện tại trước khi thay và kiểm tra SHA; thay đổi này không cần rollback DB. JAR cũ giữ nguyên các tính năng hồng ngọc đã cài trước đó.
