# Đã bổ sung res và sửa loader client — 03/10/2026

Đã sửa trực tiếp source tại `C:\Users\Admin\Downloads\PRJ_2Tab_550K`. Res bổ sung lấy từ project đầy đủ `C:\Users\Admin\Downloads\filefolder\PRJ 3in1 Goc\PRJ 3in1 Goc`.

## Kết quả

| Bộ res | PNG trước sửa | PNG sau sửa |
| --- | ---: | ---: |
| x1 | 0 | 1.038 |
| x2 | 0 | 475 |
| x3 | 0 | 441 |
| x4 | 426 | 533 |

- Bổ sung 2.223 file còn thiếu trong `res` và `fontsys`; tạo GUID riêng cho metadata mới để tránh trùng asset trong Unity.
- Đối chiếu SHA-256 xác nhận giữ nguyên toàn bộ 1.076 file res/metadata có trước lần bổ sung này.
- Giữ 381 đường dẫn ảnh mod ở x4. Các đường dẫn này tiếp tục ưu tiên ảnh mod x4 và thu về zoom đang dùng; ảnh thông thường ưu tiên bộ x1/x2/x3/x4 tương ứng, rồi mới thử đường dẫn chung và x4.
- Sáu ảnh mới `Small1085`, `Small1086` ở x1/x2/x3 đã đối chiếu và dùng ảnh từ res server để tránh hiển thị sai icon item. Bản tham chiếu trước thay được lưu trong `.analysis/client-native-res/reference-icons-before-alignment`.

## Source đã sửa

- `Assets/Scripts/Assembly-CSharp/ClientResourcePaths.cs`: quy tắc chọn đường dẫn res và danh sách ảnh mod cần giữ.
- `Game1/GameCanvas.cs`, `Game2/GameCanvas.cs`: nạp ảnh theo zoom với đường dẫn dự phòng. Khi thu ảnh x4, kích thước tối thiểu là 1 pixel; khôi phục render target và trả bộ đệm tạm sau khi resize.
- `Game1/mFont.cs`, `Game2/mFont.cs`: dùng lại atlas font bitmap x1 gốc; khởi tạo font dự phòng trước khi chọn bitmap. Font cam thiếu atlas dùng TrueType với cỡ phù hợp x1; các font số và alias cần thiết được khởi tạo.

Không thay JAR hoặc DB trong đợt sửa res client này. Các sửa trước về giao thức option/số lượng lớn được giữ lại.

## Kiểm chứng

- Biên dịch toàn bộ script client bằng thư viện Unity 2022.3.62f2: thành công, không thiếu reference.
- Kiểm tra trong Unity thực tế: **199 lượt đạt, 0 lỗi**, gồm cả hai tab ở zoom x1–x4, atlas/glyph font, ảnh mod, đường dẫn thiếu và resize ảnh nhỏ.
- Kiểm tra lựa chọn đường dẫn: đạt với toàn bộ 381 đường dẫn ảnh mod ở bốn zoom.
- Kiểm tra đọc gói tin Java → C# của cả hai tab: đạt với option và số lượng tới **99.999.999**, gồm ACK/reset và nhận item.
- Chạy màn hình chọn server và đăng nhập trong Unity: chữ và nút vẽ được, không ghi nhận lỗi vẽ font trong phiên kiểm tra. Ảnh kiểm chứng: `.analysis/client-native-res/native-menu.png`, `native-login.png`.

Phạm vi kiểm chứng là loader, font, màn hình khởi động và giao thức số lượng. Chưa kiểm tra toàn bộ NPC, map, mob và effect trong gameplay; bộ res bổ sung không bảo đảm mọi asset server đều có trong client tham chiếu.

## Mở lại trong Unity

Stop Play, chờ Unity import và biên dịch xong, rồi Play lại. Chưa xuất bản build client mới; source và res đã được cập nhật trực tiếp.

Quy tắc tự chọn zoom PC giữ nguyên: vùng render `width × height < 480000` dùng x1; từ ngưỡng đó dùng x2. Có đủ bộ res không làm cửa sổ nhỏ tự chuyển sang x2. Để kiểm tra x2, đặt Game View cố định **1024 × 600**. Thanh Scale 1.3x của Unity là phóng preview, không phải zoom res của game.

## Bản vá và dữ liệu khôi phục

- [Bản vá source và res bổ sung](client-native-res-patch.zip), khoảng 54,2 MiB. Giải nén vào gốc project client, gồm các asset mới và năm file source sửa trong đợt này.
- Backup source, SHA-256 và danh sách file: `.analysis/client-native-res/source-manifest.json`, `before-resource-hashes.json`, `copied-resources.json`.
- Kết quả kiểm chứng: `.analysis/client-native-res/verification.json`, `native-res-validation.log`, `embedded-icon-audit.json`.
- Gói `client-startup-font-scene-fix.zip` cũng đã cập nhật source hiện tại; gói nhỏ này không chứa đầy đủ res bổ sung, nên dùng gói native res phía trên khi cần chuyển sang project khác.
