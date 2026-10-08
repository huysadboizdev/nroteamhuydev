# Kiểm tra Whis, sự kiện và tài nguyên — 08/10/2026

Đối chiếu SQL live `team2026`, source, JAR đang chạy và đường nhận tài nguyên của hai namespace Game1/Game2 trong Unity. Đây là kiểm tra chỉ đọc: không sửa dữ liệu ứng dụng, source, JAR, ảnh hoặc khởi động lại server.

## Whis cộng gì?

Thắng Whis cộng **1 điểm `thachdauwhis`** cho người kết liễu; bảng xếp hạng sắp theo điểm này. Chủ trận được lưu cấp Whis đã thắng và lần gọi tiếp theo gặp cấp cao hơn. Whis tăng HP/sức đánh theo cấp, sát thương người chơi lên Whis giảm theo cấp.

**Không có thưởng theo thứ hạng và không tự tăng HP, KI, sức đánh, sức mạnh/tiềm năng, vàng, ngọc hoặc hồng ngọc cho người chơi.** Dòng “1 lượt” trong giao diện chưa được kiểm soát ở luồng tạo trận.

Ba vấn đề có bằng chứng từ source và mô hình thứ tự thực thi: tạo nhiều trận cùng lúc có thể farm điểm và ghi lùi cấp; người khác kết liễu nhận điểm trong khi chủ trận nhận cấp; cập nhật top có thể đọc SQL trước khi lưu rồi xóa cờ, làm top chậm cập nhật. Các mô hình kiểm chứng không thay thế thử trận trong game. SQL tại thời điểm kiểm tra có hai nhân vật, đều 0 điểm Whis.

[Báo cáo Whis và vị trí code](whis-ranking-audit-20261008.md).

## Có bao nhiêu sự kiện?

| Phạm vi | Số lượng | Diễn giải |
|---|---:|---|
| Chế độ ConstEvent, bỏ mã 0 | 8 | Halloween, 20/11, Noel, Tết, Hùng Vương, Trung thu, Hè, Valentine |
| Nhóm EventManager | 7 | Tết, 8/3, Halloween, Noel, Hùng Vương, Trung thu, TopUp |
| Hợp các tên trên | **10** | Số tên được định nghĩa; không phải 10 sự kiện hoạt động đầy đủ |
| Lớp khởi tạo lúc startup | 3 | Default, Hùng Vương, TopUp; TopUp là lớp rỗng |
| NPC mùa có placement SQL trong nhóm kiểm tra | 2 loại / 6 placement | Vua Hùng và Xe nước mía |

Các hoạt động Rồng Băng, Hirudegarn, ngọc rồng đen được ghi riêng trong báo cáo. Đại hội võ thuật, đấu trường, siêu hạng, minigame và phó bản là các hệ thống khác, không cộng vào 10 tên sự kiện mùa ở trên. SQL không có bảng `event`; cấu hình runtime không có dòng nào.

Những lỗi cần ưu tiên:

- **Boss Hùng Vương thiếu vòng cập nhật:** JVM đang có 20 boss trong trạng thái REST, không ở zone nào; chưa có thread chạy manager này.
- **Top kem cập nhật nhầm bảng:** nhánh điểm sự kiện thứ hai lại cập nhật top thứ nhất; danh sách top kem trong JVM đang rỗng.
- **Rồng Băng dùng hai manager khác nhau:** luồng dùng ngọc thêm rồng vào manager không được chạy; thread startup chạy một hệ thống rồng cũ khác.
- **Đổi quà khi đầy túi có thể mất phí:** Xe nước mía/Vua Hùng trừ nguyên liệu hoặc vàng trước, không xử lý kết quả thêm vật phẩm thất bại.
- Nồi bánh chỉ kiểm tra có nguyên liệu, thiếu kiểm tra số lượng; NPC này hiện chưa đặt trên map. Trồng dưa có lỗi so sánh kiểu ID, giới hạn toàn server không được xóa đúng, thời gian thông báo không khớp thời gian thực tế.

[Danh sách 10 vấn đề sự kiện, mức độ và bằng chứng](events-audit-20261008.md).

## Boss, item, res và path

Đã đối chiếu **2.001 item, 2.099 part, 144 outfit boss khai báo trực tiếp**, cùng 128 template boss static đọc từ JAR. Outfit source/JAR khớp. File part gửi client giải mã đúng và khớp SQL. Đã kiểm tra cấu trúc và giải mã **75.368 PNG**, không có PNG giải mã lỗi; ảnh giải mã được chưa chứng minh mọi animation hiển thị đúng.

| Phát hiện | Kết quả / mức ảnh hưởng đã xác định |
|---|---|
| Icon catalog thiếu | **633 ID**, thiếu cả x1–x4, tương đương **2.532 path**; gồm dữ liệu chưa dùng, không phải 633 boss lỗi |
| Item thiếu icon | **12:** ID 673 và 1841–1851; không có dòng shop `is_sell=1` cho các item này |
| Costume thiếu frame | 11 item 1841–1851 còn thiếu icon frame bộ đồ |
| Boss sai vị trí part | **Nguyệt thần, Nhật thần, Virut** có part sai loại đầu/thân/chân; ID và file ảnh vẫn tồn tại |
| Item sai cấu trúc | Avatar 210 dùng head part loại chân; Pet Thỏ mập 1040 có leg sai trong SQL nhưng luồng pet thực tế dùng bộ part đúng |
| Transform | Hai head Halloween sai loại; Gogeta dùng part 2100–2102 chưa có trong SQL; socola có head khác loại nhưng là ảnh trống đủ frame, mức lo ngại thấp hơn |
| Ảnh đặt tên thiếu | `aura_999_999`, `mount_42_0`, `mount_42_1`; chưa xác nhận được gọi trong gameplay hiện tại |

Boss Rival lấy outfit động từ người chơi nên kế thừa vấn đề đồ/transform. Boss khai báo trong catalog chưa chắc được spawn. Chưa chạy Unity hoặc client sạch cache để xác nhận lỗi hiển thị thực tế.

Đường icon: client thử ảnh nhúng/cache rồi yêu cầu server; server đọc `SRC/data/icon/x{zoom}/{id}.png`. Đường part: SQL → `SRC/data/update_data/part` → client `GameScr.parts`. Ảnh đặt tên lấy từ `SRC/data/img_by_name/x{zoom}`; hiệu ứng lấy `ImgEffect_{id}.png`, không phải file binary tên trần. Chi tiết fallback của Unity và map/path nằm trong báo cáo tài nguyên.

[Báo cáo tài nguyên và đường nhận của client](boss-item-resources-audit-20261008.md). Các catalog để kiểm tra từng bản ghi: [item](../.analysis/resources-audit-20261008/full-item-catalog.csv), [boss](../.analysis/resources-audit-20261008/all-boss-outfit-configs.csv), [part](../.analysis/resources-audit-20261008/full-part-catalog.csv), [icon thiếu](../.analysis/resources-audit-20261008/missing-referenced-icons.csv).

## Kiểm chứng và giới hạn

Snapshot JVM ngày 08/10/2026 xác nhận trạng thái manager/top nêu trên. Toàn bộ 629 source Java khớp snapshot triển khai; JAR SHA-256 vẫn là `13d4a90762111edce5dc683c49655fcaaefa1c89bbf4bcfed98331c6d603a030`. Các lỗi nhận thưởng và hiển thị được đánh giá theo luồng code/catalog, chưa thực hiện giao dịch làm thay đổi tài khoản hoặc thử animation trong game.

Nên sửa luồng boss/top/rồng và nhận thưởng trước, sau đó xử lý mapping part đang dùng; chưa tự sửa hoặc xóa nội dung trong lần kiểm tra này.
