# Sao đen: buff và sức chứa — 2026-10-05

Đã triển khai lúc 2026-10-05T22:31:01.029271+07:00 (Asia/Bangkok). Server PID 27240, cổng game 14445.

| Sao | Item | Map | Hiệu ứng | Khu | Người/khu |
|---|---:|---:|---|---:|---:|
| 1 | 372 | 85 | +21% sức đánh | 2 | 50 |
| 2 | 373 | 86 | +35% HP tối đa | 2 | 50 |
| 3 | 374 | 87 | +35% KI tối đa | 2 | 50 |
| 4 | 375 | 88 | +35% phản sát thương | 3 | 50 |
| 5 | 376 | 89 | +35% hút HP theo sát thương | 3 | 50 |
| 6 | 377 | 90 | +20 điểm phần trăm giáp giảm sát thương | 3 | 50 |
| 7 | 378 | 91 | +14 điểm phần trăm né đòn | 3 | 50 |

3 sao không còn hút HP; 5 sao không tăng sát thương chí mạng; 6 sao không tăng KI. Giáp dùng `tlGiap`, chịu xuyên giáp/đòn piercing và giới hạn tổng giáp 86% hiện có. Giữ lịch, điều kiện thắng và thời hạn buff.

Số khu và sức chứa được áp dụng trong code, đồng bộ SQL live, hai dump gốc và hai seed. Trần 50 được kiểm tra khi nhận người vào khu dưới khóa riêng; không tính boss/đệ tử, admin không được vượt sức chứa. Vòng chọn khu trả thất bại khi tất cả đầy. Danh sách người chơi sao đen an toàn cho thêm/xóa đồng thời; vòng xử lý bang dùng snapshot. Khóa nhận người tách khỏi khóa vật phẩm để tránh deadlock khi hai người giữ ngọc đổi khu ngược chiều. Đăng nhập lại ở map sao đen vẫn được đưa về nhà theo cơ chế cũ.

## Xác minh

- Biên dịch đầy đủ 594 tệp source, Java 17.
- 15 nhóm kiểm thử: 7 buff, tính lại/hết hạn, phối hợp buff, sát thương giáp; số khu, người thứ 50/51, 40 lượt vào đồng thời, đổi khu ngược chiều khi giữ ngọc, chọn khu đầy; 5 nhóm đệ tử hợp thể và 4 nhóm phó bản.
- Migration chạy hai lần trong database thử nghiệm riêng, 7 map và 7 mô tả đúng; database thử nghiệm đã xóa.
- Runtime JVM mới xác nhận 18 khu, tất cả 50 người/khu; KI 135.000 từ nền 100.000, hút HP 35, giáp 20, không tăng sát thương chí mạng từ 5 sao. SQL live và catalog runtime đã đối chiếu.
- Mọi class đóng gói trong JAR khớp class đã kiểm thử; hash source, JAR cũ/mới và snapshot đã xác minh.
- Chưa chơi nghiệm thu bằng client hoặc tạo tải 900 người thật. Giới hạn đồng thời đã kiểm thử bằng fixture và cấu hình thực tế đã đọc trong JVM.

## Triển khai và khôi phục

- Bảo trì qua hàm lưu dữ liệu của ứng dụng; xác nhận mọi JVM chạy `20.jar` dừng trước thay JAR.
- JAR hiện dùng: `SRC/20.jar`.
- SHA-256 mới: `9f549bc7726e3a5507d180b7ab544a1d6dc64c429ac3497a07a309bf20fd4764`.
- Backup: `SRC/jar-backup/20-before-black-ball-20261005-223047.jar`.
- SHA-256 cũ: `2a75364269992406b878f4f7f7c95677737988a484524c001bafdaffa3720262`.
- Snapshot database sau khi server lưu: `.analysis/black-ball-deploy-20261005-223047/team2026-before-black-ball.sql`.
- SQL rollback chỉ 7 map và 7 mô tả: `.analysis/black-ball-deploy-20261005-223047/black-ball-config-rollback.sql`.
- Metadata đầy đủ: `.analysis/black-ball-deployment-latest.json`, bản sao tại `SRC/jar-backup`.

Rollback: bảo trì/lưu dữ liệu và dừng server; phục hồi JAR backup vào `SRC/20.jar`, chạy SQL rollback cấu hình, khởi động và xác minh. Không import lại full snapshot lên dữ liệu người chơi đang sử dụng.

Source, migration, seed và tài liệu đang ở workspace; chưa commit/push GitHub.
