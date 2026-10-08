# Santa KOL VIP: SQL và tài nguyên

Migration: `SRC/sql/migrations/20261006_santa_kol_vip_rewards.sql`.
Đồng bộ seed: chạy `python .analysis/prepare_santa_kol_sql.py`; script cập nhật dòng template 1825 và chèn migration một lần ở cuối cả `team2026.sql` và `SRC/sql/nro1.sql`. Chạy lại không nhân bản migration hoặc dòng vé.

Vé 1825 bán tại tab 17 của Santa, `type_sell=1`, giá 1.000 ngọc xanh. Migration sửa tên/mô tả vé, xóa dòng shop trùng cùng option của dòng trùng nếu có, rồi tạo/cập nhật đúng một dòng bán. Template vẫn giữ type/icon/ID đang dùng. Quà VIP được xử lý trong source Java, không tạo item mới trong SQL.

Đã kiểm tra các cặp item/icon:

| Item | Icon |
|---:|---:|
| 1825 | 15293 |
| 722 | 6782 |
| 1283 | 11569 |
| 1284 | 11573 |
| 1794 | 15412 |
| 1204 | 11048 |
| 1791 | 14247 |
| 1792 | 14248 |
| 1793 | 14249 |

Toàn bộ 36 PNG x1–x4 tồn tại, giải mã PNG hợp lệ. Cả chín item là vật phẩm hành trang, `head/body/leg=-1`, không cần body part. Bốn mảnh radar giữ nguyên type 33 và ID vì UseCard tra radar bằng ID template. Không thay bằng ID ở bộ res khác.

Cả `Game1/SmallImage.cs` và `Game2/SmallImage.cs` thử ảnh local `/SmallImage/Small{id}.png`, sau đó gọi `requestIcon(id)` nếu thiếu. `DataGame.sendIcon` đọc `SRC/data/icon/x{zoom}/{id}.png`. Vì res server đã đủ, không cần sao chép/ghi đè ảnh hoặc sửa loader client. Template đổi tên cần tăng `vsItem` để client cập nhật dữ liệu item.

Xác minh đọc: `python .analysis/verify_santa_kol_resources.py` (SQL template, 36 PNG, loader hai client, đường dẫn server, hai seed).
Sau triển khai SQL: `python .analysis/verify_santa_kol_resources.py --after-migration` (thêm tên vé, đúng một dòng shop và giá/currency). Báo cáo máy đọc: `.analysis/santa-kol-resources-verification.json`.

Chưa thay SQL live hoặc JAR trong phần công việc này. Kiểm thử sau migration live ban đầu báo lỗi đúng dự kiến ở tên vé cũ; cần chạy lại sau triển khai.
