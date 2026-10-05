# Ép trực tiếp sao pha lê 8 và 9 — 02/10/2026

Áp dụng quy trình Skill Ai → GitNexus, truy tìm CheckSlot rồi đối chiếu source.

Trong `EpSaoTrangBi.java`, bỏ kiểm tra cường hóa ở cả `showInfoCombine` và `epSaoTrangBi`. Người chơi đặt một trang bị còn lỗ trống và một đá pha lê để ép trực tiếp sao thứ 8 hoặc 9; không cần option 228, Hematite hay Dùi Đục để mở hai lỗ này.

Giữ phí hiện tại: 10 ngọc và một đá mỗi lần ép. Giữ cách lưu bonus riêng của sao 8/9 để tương thích đồ hiện có. Kiểm tra menu và hành động đều từ chối đồ hết lỗ hoặc có số lỗ vượt giới hạn 9 sao; không cho ép sao thứ 10 bằng hành động trực tiếp.

Không sửa chức năng cường hóa độc lập, nâng cấp đá pha lê hoặc dữ liệu trang bị cũ. Tỷ lệ pha lê hóa tạo lỗ 100%..5% được sửa ở lượt trước tiếp tục áp dụng.

## Kiểm thử

`DirectCrystalStarTest` chạy các hàm thật của ép sao, tạo player/item/template trong RAM; chỉ thay phần gửi thông báo mạng và menu NPC bằng bộ ghi nhận. Không chạy server hoặc mở DB.

- Source cũ: thất bại vì menu không cho ép trực tiếp sao thứ 8 khi thiếu option cường hóa.
- Source mới: kiểm tra 7/8 → 8/8, 7/9 → 8/9, 8/9 → 9/9 đều thành công không cần cường hóa.
- Đồ đủ sao, đồ 9 sao với dữ liệu 10 lỗ, thiếu ngọc: từ chối và không mất nguyên liệu.
- Kiểm tra bonus cũ không bị ghi đè và bonus sao 8/9 vẫn lưu riêng.
- Bộ kiểm thử tỷ lệ trước đó vẫn đạt, gồm 4.500 lượt random deterministic.

Lệnh: `python .analysis/verify_crystalization.py`. Biên dịch toàn bộ source target Java 17 và hai tệp test: exit 0; hai bộ test: exit 0. Còn các cảnh báo unchecked/unsafe có sẵn. Chưa kiểm thử trên client game.

Đã sửa source; chưa đóng gói/thay `SRC/20.jar` mà `run.bat` đang chạy.
