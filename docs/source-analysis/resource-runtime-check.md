# Kiểm tra server và giao thức sau sửa res — 02/10/2026

Server đang chạy bằng `SRC/20.jar`, JVM PID **32**, nghe `0.0.0.0:14445`. Kết nối và kiểm thử giao thức thực hiện qua localhost; chưa kiểm tra truy cập từ Internet hay đăng nhập/chơi bằng client thật.

## Lỗi tìm thêm và đã sửa

Gói template dùng vị trí mảng làm ID, nhưng DB có khoảng trống ID:

- Map: 169 dòng, ID cao nhất 185; gói cũ chỉ có 169 slot, cần 186 slot.
- NPC: 93 dòng, ID cao nhất 110; gói cũ chỉ có 93 slot, cần 111 slot. Các ID 85–102 không có dòng.
- `NpcFactory` cũng lấy avatar bằng `NPC_TEMPLATES.get(tempId)`, dễ sai hoặc vượt mảng khi có khoảng trống ID.

Đã sửa đóng gói map/NPC/mob theo ID, giữ slot rỗng cho các ID không có dòng thay vì dịch chuyển những ID phía sau; không thêm template giả vào DB. Slot NPC rỗng sử dụng các part rỗng đã có. Writer từ chối ID trùng và số slot vượt giới hạn byte. Factory lấy NPC template bằng ID thật. Tăng phiên bản map **3→4** để client tải lại.

Tái hiện trước sửa: gói thực tế có 169/93 slot và test lookup NPC 110 gặp `IndexOutOfBoundsException`. Sau sửa: gói thực tế có đúng **186/111 slot**, từng tên và part NPC khớp ID trong DB; lookup ID thưa và từ chối ID chưa tồn tại đều pass.

## Kiểm thử trực tiếp server

- Bắt tay và tải sáu block update data ở **zoom 1–4**: nội dung khớp từng file server. Block part dài 88.125 byte, có đủ 2.099 part và khớp DB.
- Nhận tile map **21, 22, 23, 116**: nội dung khớp file gốc ở cả bốn zoom.
- Nhận icon Bình Kilis **15359**: nội dung PNG khớp file ở cả bốn zoom.
- Nhận số lượng res: **1.434 file/zoom**. Nhận mob template 0 ở cả bốn zoom.
- 74/74 kiểm thử hồi quy trên source/JAR đạt; thêm kiểm thử chỉ mục, lookup NPC và reader 180 file tile đạt.
- DB và bốn bảng template trong hai dump vẫn khớp. Kiểm tra lại SHA-256 và pixel của 3.203 ảnh mới đạt.
- Log khởi động và khoảng theo dõi sau probe không có exception. Luồng Network và Update boss còn chạy; thread dump không báo deadlock.
- Bảy map thiếu tile vẫn được báo unavailable và không khởi tạo; Heart vẫn ẩn theo yêu cầu. Các thiếu hụt res gốc trong [báo cáo sửa dữ liệu](resource-consistency-fix.md) vẫn cần bộ tài nguyên đúng phiên bản.

## Triển khai

Bảo trì bằng lệnh `bt`, chờ Java dừng trước khi thay JAR; đã sao lưu và đối chiếu SHA-256. Server được chạy lại trong cửa sổ ẩn; helper đếm ngược do lần bảo trì tạo ra đã được dừng để tránh khởi động trùng sau năm phút.

- JAR hiện tại SHA-256: `fdf48ab1643764092a0c3b9cf7dc03b74bcf7e16cb64a2cfa9fed5532cafcb2f`.
- Backup trước sửa template: `SRC/jar-backup/20-before-resource-repair-20261002-214925.jar`, SHA-256 `f5aaecf4e417a6e82412e2fcb16bc7b032e4022f2f1c382740ea5ef662529ee5`.
- Metadata: `.analysis/resource-repair/deployment.json` và `SRC/jar-backup/deployment-resource-repair-20261002-214925.json`.
- Bằng chứng: `.analysis/resource-repair/runtime-console.log`, `runtime-final-threads.txt`, `network-probe.json`, `template-index-probe.json`, `runtime-template-build.log`.

Các kiểm thử mạng không đăng nhập tài khoản và không sửa nhân vật. Log và PID là trạng thái tại lần kiểm tra này, không phải giám sát liên tục.
