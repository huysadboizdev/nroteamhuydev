# Khôi phục hình Uub

Uub sử dụng part 946/947/948 và avatar đầu 946. SQL đang trỏ icon 11625–11655 là các bộ phận máy móc đang được dùng hợp lệ bởi trang phục khác. Bộ Uub chuẩn 8586–8616 tồn tại ở x1–x4, cùng chuỗi hai đầu, 16 thân, 13 chân; icon 8586 cũng là icon trang phục Uub 932.

Sửa ba dòng part: đổi icon 11625–11655 về 8586–8616, giữ nguyên thứ tự động tác, dx/dy và placeholder 2955. Sửa head_avatar 946 về 8586. Không ghi đè PNG vì icon máy móc được bộ khác dùng. Tăng vsData 10→11 để client tự lấy lại NR_part. Kiểm tra SQL, part binary được tạo khi khởi động, avatar, toàn bộ PNG ở bốn mức zoom và hash runtime DataGame. Không sửa chỉ số/trang bị/skill đệ.

Triển khai có bảo trì lưu dữ liệu, snapshot SQL, backup JAR cũ timestamp/hash, apply migration trước khởi động. Xác minh 98 hồi quy và dữ liệu thực tế; client cần kết nối lại để lấy dữ liệu phiên bản mới.

Đã hoàn thành: migration `SRC/sql/migrations/20261006_uub_sprite_mapping.sql` áp dụng trên team2026, đồng bộ hai seed. SQL và part binary khớp đủ 34 frame trong ba part, avatar 8586; 32 icon (31 hình Uub và placeholder) hợp lệ ở x1–x4, 128 PNG. Bộ frame đã được xem bằng contact sheet. Runtime PID 15748, cổng 14445, vsData=11 và hash DataGame khớp. 98 lượt hồi quy PASS trên classes/candidate/JAR đã cài. Chưa quan sát lại trực tiếp trong Unity; kết nối lại để client nhận NR_part mới.

Backup `SRC/jar-backup/20-before-uub-sprites-20261006-102522.jar`; metadata `.analysis/uub-sprites-deployment-latest.json`. SHA-256 JAR mới `20a0c0ae156df2d1f3bfa34e82973ffa17b5af72278cbe8285439188b7031b74`. Nếu rollback, bảo trì lưu dữ liệu rồi phục hồi JAR và riêng ba dòng part/avatar từ snapshot; không khôi phục toàn bộ dữ liệu người chơi.
