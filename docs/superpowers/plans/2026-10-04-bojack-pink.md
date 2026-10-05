# Đồng bộ thưởng Tiểu đội Bojack với Namek

Yêu cầu: Tiểu đội boss Trái đất Bojack tương tự Tiểu đội sát thủ Namek hiện tại.

Thiết kế:6 lớp BUJIN/KOGU/ZANGYA/BIDO/BOJACK/SUPER_BOJACK bỏ toàn bộ drop77, dùng PinkCapsuleService.dropShared để tạo5 capsule722 public. Giữ6 cải trang423–428, ngọc rồng19/20 và5 điểm sự kiện mỗi boss; cải trang/ngọc rồng giữ owner tới hết thời gian rơi bằng constructor private. Thêm guard đồng bộ một lần mỗi đời, reset qua resetBase. Hai form Bojack dùng cùng reward hiện có. Không thay spawn, chiến đấu hay DB.

- [x] Backup source, kiểm thử thưởng/respawn/pickup/invalid/concurrent và2 form; chạy RED trên JAR cũ.
- [x] Sửa6 lớp, compile; kiểm tra package chỉ6 class thay đổi.
- [x] Chạy67 regression cũ +5 nhóm Bojack mới.
- [x] Bảo trì lưu dữ liệu, dừng vòng restart gắn với server; kiểm tra JVM/cổng dừng trước backup/cài JAR.
- [x] Khởi động, kiểm tra hash/cổng/runtime và lưu metadata.

Kết quả:72/72 candidate PASS; JAR đã cài kiểm tra drops/concurrent PASS. Server tự khởi động sau khi cài; xác minh hash, PID, cổng14445 và runtime hồng ngọc. Chưa thao tác gameplay trên client.
