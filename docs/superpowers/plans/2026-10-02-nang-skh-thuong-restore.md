# Bổ sung Nâng SKH Thường và sửa menu thiếu lựa chọn

Người dùng đã làm rõ ngày 2026-10-02: mọi lượt 1–5 món có 50% thành công, 50% xịt độc lập cho từng món. Năm món có thể nhận từ 0 đến 5 SKH; không cố định nhận ba món khi xịt. Quy tắc này thay phân bố 3/5 đã dùng trong lần triển khai trước.

## Thiết kế đã xác định

- Menu Bà Hạt Mít map 5 → Nâng SKH có Thường, VIP, Đóng; route và preview độc lập. Không đưa Thường vào luồng VIP hoặc nút pha lê x10/x100.
- Thường nhận 1–5 món thần linh 555–567, không trùng loại. Mỗi món ra trang bị cấp 1 đúng loại và hành tinh nguyên liệu; nhẫn chung 561 → rada 12 với set theo hành tinh nhân vật, kiểm tra nghiêm ngặt 0/1/2.
- Thu 500 triệu vàng/món, tối đa 2,5 tỉ, dùng long. Mọi nguyên liệu đều tiêu hao, kể cả các món không ra SKH. Mỗi slot được roll độc lập 50/50, kể cả khi chọn đủ năm món; không bảo đảm số kết quả tối thiểu.
- Set random độc lập theo bảng ưu tiên đã ghi: Trái Đất 233/127/128/129/245; Namek 233/132/237/130/131; Xayda 134/233/133/135/241, tỷ lệ 30/30/30/5/5. Giữ chỉ số shop của đúng template cấp 1, option mới và đủ hiệu ứng.
- Revalidate preview, số dư, nguyên liệu, hành tinh rada, cooldown và giao dịch; dựng đủ output trước khi ghi. Chỉ ghi slot nguyên liệu được chọn để tránh mất đồ nhận thêm ở ô khác.
- Theo phạm vi nhánh Thường trong dự thảo trước: tắt riêng rơi trực tiếp SKH ở chín map 1/2/3/8/9/11/15/16/17. Giữ phân loại map, rơi capsule, đồ lỗ sao và SKH tại map riêng 164.

## Công việc

- [x] Test tái hiện menu thiếu Thường, lớp Thường chưa tồn tại và nhánh rơi trực tiếp map 1 (seed 285).
- [x] Thêm NangSKHThuong và kiểm thử chọn đồ, random, phí, kết quả và chống lặp.
- [x] Nối Combine/CombineService/BaHatMit, sửa kiểm thử menu VIP theo thứ tự mới.
- [x] Xóa riêng nhánh rơi SKH ở chín map; kiểm thử đường reward thật và bảo toàn map riêng.
- [x] Compile, kiểm thử source; review yêu cầu rồi review chất lượng.
- [x] Build lại JAR, chạy kiểm thử trên JAR; backup 20.jar đang dùng trước khi thay. Nếu server đang chạy, chỉ thay sau khi dừng an toàn.
- [x] Ghi kết quả và hướng dẫn chạy/quay về JAR cũ.
