# Kiểm tra và sửa 7 pet rồng — 04/10/2026

> Bộ chỉ số mở trứng đã được cập nhật tiếp theo yêu cầu người dùng: xem [chỉ số và hiệu ứng hiện tại](dragon-pet-stats.md). Các phần chỉ số cũ bên dưới là kết quả trước lần cập nhật này.

Đã kiểm tra SQL gốc/đang duy trì (`team2026.sql`, `SRC/sql/nro1.sql`), database hiện tại, `SRC/data/update_data/part`, res server x1–x4 và luồng sử dụng/tải pet trong JAR ứng dụng.

## Kết quả sửa

- Bé Rồng Cute ID 1771: sửa `icon_id` từ **16187** (đầu lâu Ghost Rider) sang **15125** (rồng nhí đội mũ). Sửa database bằng giao dịch có kiểm tra giá trị cũ, cập nhật cả hai SQL dump. Ghost Rider ID 1900 giữ icon 16187.
- Pet ID 1765: sửa `Player.sendNewPet` từ part chân **1764** sang **1664**, khớp SQL và `UseItem`. Lỗi cũ tái hiện được khi tải pet sau đăng nhập.
- Tăng `DataGame.vsItem` **10 → 11** để client tự lấy lại danh mục item thay vì giữ icon cũ trong cache. Không thay PNG hoặc chỉ số pet.

## Bảng đủ res và đường dẫn

Tất cả cùng tên **Bé Rồng Cute**, type **27**, gender **3**, sức mạnh yêu cầu **0** trong database và SQL. Mô tả màu bên dưới dựa trên ảnh icon hiện có.

| ID pet | Ngoại hình icon | Icon đúng | Head / body / leg | Res x1–x4 | Code dùng / tải lại |
| --- | --- | --- | --- | --- | --- |
| 1765 | Xanh lá, râu vàng | 15077 | 1662 / 1663 / 1664 | Đủ | Khớp sau sửa |
| 1766 | Hồng, tai dài | 15085 | 1665 / 1666 / 1667 | Đủ | Khớp |
| 1767 | Xanh lam, miệng tím | 15093 | 1668 / 1669 / 1670 | Đủ | Khớp |
| 1768 | Nâu, miệng hồng | 15101 | 1671 / 1672 / 1673 | Đủ | Khớp |
| 1769 | Vàng, đeo kính | 15109 | 1674 / 1675 / 1676 | Đủ | Khớp |
| 1770 | Cam, miệng xanh | 15117 | 1677 / 1678 / 1679 | Đủ | Khớp |
| 1771 | Xanh lá, đội mũ trắng | 15125 | 1680 / 1681 / 1682 | Đủ | Khớp |

Đường dẫn runtime:

- Icon và frame: `SRC/data/icon/x1/<image_id>.png` đến `SRC/data/icon/x4/<image_id>.png`.
- Bảng part client nhận: `SRC/data/update_data/part`.
- SQL: `item_template.icon_id`, `item_template.part/head/body/leg`, bảng `part` với các ID 1662–1682.
- Code: `UseItem.useItem` trang bị rồi gọi `PetService.Pet2`; `Player.sendNewPet` khôi phục pet. Cả hai khớp SQL cho 7 pet.

Đã xác nhận đủ **21 part**, đúng type 0/1/2 và số frame 3/17/14; bảng part binary khớp database và hai SQL dump. Có **57 ID ảnh duy nhất** cần dùng, bao gồm ảnh trống 2955, kiểm tra đủ **228/228 PNG** ở bốn mức zoom, tất cả mở/verify được. Danh sách từng frame nằm trong `.analysis/dragon-pets/audit.json`. Các frame trống 2955 trong part là dữ liệu placeholder có chủ đích, không phải file thiếu.

## Chỉ số theo SQL và code hiện tại

**SQL không đặt bộ option chiến đấu mặc định cho 7 template này.** Trong database và cả hai SQL dump không có dòng `item_shop` cho ID 1765–1771. `item_template` chỉ cấu hình tên, loại, icon, part và các trường cơ bản; không lưu riêng bộ HP/KI/sức đánh mặc định. Kiểm thử thực tế `RewardService.initChiSoItem` với type 27 cho cả 7 ID không thêm option nào.

Nguồn code đang phát 7 pet là mở **Trứng vàng rồng nhí ID 1821** tại `UseItem.OpenTrungRongNhi`. Cả bảy pet có cùng cách random chỉ số, xác suất chọn mỗi ID khoảng 1/7:

| Option | Tên đúng trong SQL/database | Giá trị do code mở trứng cấp |
| --- | --- | --- |
| 50 | Sức đánh +#% | **10–18%** |
| 5 | +#% sức đánh chí mạng | **5–10%** |
| 14 | Chí mạng +#% | **5–10%** |
| 30 | Không thể giao dịch | Có; param random 0 hoặc 1 |
| 93 | Hạn sử dụng # ngày | **15 ngày** ở khoảng 99% lượt; **0 ngày** ở khoảng 1% lượt |

Các giá trị damage, crit được tính theo đúng option trong `NPoint`: option 5 cộng sức đánh chí mạng; option 14 cộng chí mạng. Biến `hp`, `ki`, `damage` trong hàm mở trứng đặt tên gây nhầm: thực tế gắn vào option 50, 5, 14; **không phải HP, KI, sức đánh** lần lượt.

Luồng mở trứng hiện **không thêm HP (77), KI (103), hoặc May mắn (236)**. Nhánh hạn 0 ngày vẫn thêm option 93, nên không thể ghi nhận là pet vĩnh viễn theo quy tắc kiểm tra thiếu option 93 của `Inventory.isPermanent`. Code dùng `randomOption` nguyên 0–99 và điều kiện `< 0.5`, chỉ giá trị 0 thỏa điều kiện, tương đương khoảng 1%, không phải 0,5%.

**Vật phẩm 1771 trong ảnh người dùng có bộ option riêng:** sức đánh +18%, HP +18%, sức đánh chí mạng +18%, chí mạng +10%, May mắn +15%; không thấy dòng hạn sử dụng. Đây không phải bộ chỉ số từ mở trứng hiện tại. Bộ option này khớp luồng cấp pet VIP 4 trước khi bị cắt ở lần sửa trước: 50=18, 77=18, 5=18, 14=10, 236=15. Tên May mắn đối chiếu theo `item_option_template`, dù comment cũ ở ToriBot từng ghi sai nghĩa option. Hiện VIP không còn cấp pet này.

## Kiểm thử và triển khai

- `DragonPetResourceTest`: tái hiện lỗi cũ ID 1765 (đăng nhập dùng chân 1764); bản sửa đạt **14/14** luồng trang bị/tải lại cho bảy pet. Xác nhận 7/7 không có option mặc định từ `initChiSoItem` và item version 11.
- Kiểm tra lại quà VIP cả bốn cấp để bảo đảm các quà đã cắt không quay lại.
- `DragonIconFix`: kiểm tra DB sau sửa 1771=15125, 1900=16187.
- `audit.py`: đủ part/PNG, đối chiếu hai SQL dump và đường dẫn code.
- Server được bảo trì lưu dữ liệu, xác nhận dừng trước thay JAR; chỉ thay `Player.class` và `DataGame.class` trong JAR. Các entry khác giữ nguyên byte.
- JAR cũ: `SRC/jar-backup/20-before-dragon-resource-20261004-075649.jar`; metadata SHA-256 nằm trong file JSON cùng tên và `.analysis/dragon-pets/deployment.json`.
- SQL dump đã có backup nguyên byte; vị trí/hash trong `.analysis/dragon-pets/sql-file-changes.json`. Snapshot DB trước sửa: `.analysis/vip-reward-check/dragon-before.json`; SQL hoàn tác: `.analysis/dragon-pets/rollback.sql`.
- Chưa kiểm thử đăng nhập và xem icon bằng client game thực tế. Cơ chế phiên bản item được kiểm tra từ code client cả luồng tải/cached catalog; đăng nhập lại để nhận danh mục mới.
