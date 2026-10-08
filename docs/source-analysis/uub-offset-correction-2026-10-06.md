# Chỉnh vị trí đầu Uub

Ảnh sau sửa icon cho thấy đúng Uub nhưng đầu rời thân. Nguyên nhân: giữ dy=-4 của part máy móc khi đổi icon. Theo công thức Char.paintCharBody, đầu dùng cy-CharInfo[cf][0][2]+dy, thân dùng cy-CharInfo[cf][2][2]+dy. Đã dựng lại 33 frame theo đúng CharInfo của client; hạ đầu 7 pixel đưa hai frame đầu dy=-4→3, giữ dx, thân/chân và icon.

Ảnh so sánh đứng: `.analysis/uub-audit/idle-offset-compare.png`. Toàn bộ động tác sau chỉnh: `.analysis/uub-audit/all-frames-head-plus7.png`. Không ghi đè ảnh hoặc sửa chỉ số/skill đệ. SQL migration `SRC/sql/migrations/20261006_uub_head_offset.sql`; phiên bản dữ liệu 12 buộc tải lại NR_part khi kết nối lại.

Xác minh dự kiến: SQL và binary part khớp, avatar/icon chuẩn; tất cả 128 PNG ở bốn zoom hợp lệ; full build và hồi quy; hash/phiên bản runtime. JAR/SQL backup sau bảo trì lưu dữ liệu.

Đã triển khai và xác minh: SQL/binary head 946 `[[8586,1,3],[8587,-2,3],[2955,0,0]]`; runtime vsData=12, hash DataGame khớp. 98 lượt kiểm thử PASS trên classes, candidate và JAR đã cài. Server PID 9728 mở cổng 14445. Backup `SRC/jar-backup/20-before-uub-offset-20261006-105102.jar`; metadata `.analysis/uub-offset-deployment-latest.json`. Đã xem bản dựng 33 frame theo CharInfo giống nhau ở hai tab; chưa quan sát trực tiếp lại Unity sau cập nhật.
