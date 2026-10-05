# Nâng SKH Thường — bổ sung menu và chức năng

Ngày 2026-10-02, menu Nâng SKH chỉ có VIP vì đợt triển khai trước chưa bổ sung lớp Thường. Đã sửa submenu thành **Nâng SKH Thường / Nâng SKH VIP / Đóng**, thêm luồng Thường riêng (combine type 999), giữ VIP type 998. Kiểm thử menu đi qua BaHatMit và CombineService thật, kiểm tra mở đúng tab, xác nhận/hủy và không trừ phí nhầm nhánh.

## Quy tắc Thường

Nhận 1–5 món thần linh 555–567 khác loại. Mỗi món tiêu hao 500.000.000 vàng; năm món thu 2.500.000.000 vàng, tính bằng long. Tất cả nguyên liệu đã chọn bị tiêu hao, kể cả món không ra SKH.

- Mọi lượt 1–5 món: mỗi món thành công 50%, xịt 50%, roll độc lập. Lượt đủ 5 món có thể nhận 0/1/2/3/4/5 SKH; xịt mất từ một đến cả năm món. Không có sàn kết quả và không roll một lần chung cho cả lượt.
- Đầu ra là trang bị cấp 1 đúng loại/hành tinh của từng đồ thần. Nhẫn chung 561 ra rada 12, set theo hành tinh nhân vật 0/1/2. Cho phép nguyên liệu khác hành tinh nhân vật.
- Lấy chỉ số nền của đúng template shop làng bằng builder đã kiểm tra SQL, tạo option mới, thêm đầy đủ option tên/hiệu ứng set. Không kế thừa sao/cấp nâng của nguyên liệu.

| Hành tinh | Set theo thứ tự ưu tiên | Tỷ lệ |
| --- | --- | --- |
| Trái Đất | Gohan 233, Thiên Xin Hăng 127, Kirin 128, Sôngôku 129, Kaio 245 | 30/30/30/5/5 |
| Namek | Gohan 233, Pikkoro Daimao 132, Nail 237, Picolo 130, Ốc tiêu 131 | 30/30/30/5/5 |
| Xayda | Ca Đíc 134, Gohan 233, Kakarot 133, Nappa 135, Cađic M 241 | 30/30/30/5/5 |

Preview Thường/VIP được xóa khi đổi tab. Kiểm tra lại instance, slot, option, quantity, hành tinh rada, số dư, giao dịch và cooldown trước khi tạo output. Dựng mọi output trước khi ghi/trừ vàng; chỉ ghi các slot nguyên liệu đã chọn. Xác nhận lặp hoặc đồng thời không thu phí lần hai.

Theo phạm vi nhánh Thường đã ghi trong dự thảo, bỏ riêng khối rơi trực tiếp SKH ở map 1/2/3/8/9/11/15/16/17. Giữ `isMapUpSKH`, capsule, đồ lỗ sao và SKH tại map riêng 164. Test đường reward thật phát hiện lỗi cũ ở map 1/seed 285; sau sửa kiểm tra chín map và xác nhận map 164 vẫn ra SKH.

## Kiểm chứng và JAR

Source compile exit 0; **36/36 lượt kiểm thử đạt**. Gồm sáu mode Thường selection/random/outcomes/fees/inventory/replay, test menu Thường/VIP, test rơi SKH và toàn bộ hồi quy VIP/SQL/combat/vàng/pha lê. Test random duyệt đủ 100 giá trị chọn set mỗi hành tinh và 32 tổ hợp thành công/xịt của năm món. Test execute kiểm tra thực tế đủ sáu số kết quả 0–5, tiêu hao mọi nguyên liệu và phí 2,5 tỉ. Đã kiểm tra rada Thường đủ năm set × ba hành tinh. Quy tắc nhận cố định 3/5 trong lần trước đã được thay theo giải thích mới nhất của người dùng.

Compile riêng 552 file production, đóng gói lại và chạy **36/36 lượt trên chính JAR**. Kiểm tra archive không trùng entry, không chứa cache công cụ, mọi class production khớp kết quả compile. Hai review riêng yêu cầu/chất lượng không còn lỗi xác nhận được.

Lần bổ sung menu đã thay `SRC/20.jar`; bản trước (chỉ VIP) chuyển nguyên vẹn vào `SRC/jar-backup/20-before-skh-thuong-20261002-135708.jar`, SHA-256 đã đối chiếu. Bản trước VIP ban đầu vẫn giữ trong cùng folder. [Hướng dẫn quay về JAR cũ](../../SRC/jar-backup/README.md).

Ở lần bổ sung menu không có tiến trình Java chạy lúc thay JAR; chưa sửa database. Giao diện client thật và lưu/đọc sau đăng nhập lại cần kiểm tra trong game; không đánh dấu các bước này đã được chạy offline.

Cập nhật 50/50 độc lập: source và JAR mới đã qua 36/36 test, gồm mọi kết quả 0–5. Sau khi xác minh server đã dừng, đã thay `SRC/20.jar` bằng bản mới và kiểm tra SHA-256. Backup bản quy tắc 3/5 là `SRC/jar-backup/20-before-independent-skh-20261002-141309.jar`. `.analysis/pending-release-install.json` có trạng thái installed; `.analysis/latest-deployment.json` ghi đường dẫn và checksum. Chưa khởi động server. Lệnh `bt` ở console gọi bảo trì, lưu dữ liệu và dừng server.
