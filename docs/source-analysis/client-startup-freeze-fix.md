# Client Unity chỉ hiện nền — 03/10/2026

> Đã triển khai sửa sau báo cáo này: xem [kết quả bổ sung res và loader](client-native-res-fix.md). Các số liệu/nguyên nhân dưới đây ghi nhận trạng thái trước sửa.

Source đã sửa: `C:\Users\Admin\Downloads\PRJ_2Tab_550K`.

## Nguyên nhân đã tái hiện

- Client vẫn chạy vòng cập nhật và kết nối TCP thành công. Màn hình hiện tại là `ServerListScreen`, zoom x1.
- `mFont.init()` chọn font bitmap ở x1, nhưng project chỉ có dữ liệu tọa độ glyph (`res/myfont/*.bytes`), thiếu toàn bộ ảnh atlas `res/x4/myfont/*.png`.
- Khi vẽ dòng phiên bản, `mFont.drawString(..., font)` truy cập `imgFont.texture` đang null; lỗi tại dòng 516 của source trước sửa. `GameCanvas.paint()` bắt lỗi rồi bỏ qua, vì vậy nền đã vẽ vẫn hiện nhưng menu và các nút phía sau không được vẽ.
- Log tái hiện: `.analysis/client-startup/runtime-before.txt`.
- Cấu hình build còn trỏ tới `Assets/Scenes/Nro1.unity` và `Assets/Scenes/NRO2.unity`, trong khi code đổi tab tải `NROL`, `NROL1`. Kiểm tra chuyển tab đã ghi lỗi scene không nằm trong Build Settings.

## Sửa

- Sửa `Game1/mFont.cs` và `Game2/mFont.cs`: chỉ chọn bộ font bitmap khi đủ các atlas. Nếu thiếu, dùng font TrueType có sẵn trong `fontsys/x2` ở zoom x1; x2–x4 dùng bộ tương ứng.
- Các hàm vẽ và đo chữ chỉ truy cập bitmap khi ảnh và bảng glyph đều đã nạp. Đường dẫn font dùng đúng tên thư mục `fontsys`.
- `ProjectSettings/EditorBuildSettings.asset`: thay hai scene cũ bằng `Assets/Scenes/NROL.unity` và `Assets/Scenes/NROL1.unity`, dùng GUID từ các file `.meta` thực tế.
- Backup các file và SHA-256: `.analysis/client-startup/font-fix-manifest.json`.
- Không thay JAR hoặc dữ liệu DB trong lần sửa này.

## Kiểm tra

- Biên dịch toàn bộ C# với tham chiếu Unity 2022.3.62f2: exit 0, không thiếu thư viện; còn cảnh báo cũ của project.
- Kiểm tra gói tin số lượng/option lớn ở cả hai namespace: đạt, không lệch dữ liệu.
- Chạy bản sao độc lập cùng source và res bằng Unity 2022.3.62f2: cả hai tab vẽ được giao diện, không còn lỗi font; cả hai đã chuyển tới `GameScr`. Ghi trạng thái và ảnh thực: `.analysis/client-startup/runtime.txt`, `tab1.png`, `tab2.png`, `gui-verification.log`, `tab2-verification.log`. Kết quả kiểm tra trước/sau: `.analysis/client-startup/verification.json`.
- Phạm vi kiểm tra là khởi động, kết nối và vẽ màn hình; chưa kiểm tra toàn bộ thao tác, nhân vật và tài nguyên trong game.

Bản vá: `docs/source-analysis/client-startup-font-scene-fix.zip`. Source tại đường dẫn client đã được cập nhật trực tiếp.

## Chỉnh lại tỉ lệ font và nút đổi tab

Ảnh đăng nhập tiếp theo cho thấy chữ quá lớn: bản font thay thế lấy font x2 nhưng dùng nguyên cỡ tại x1. Kiểm tra thực trên Unity ghi nhận chữ “Tài khoản” rộng 75–77 pixel ở x1, trong khi kích thước logic tại x2 là 37–38 pixel. Sáu trường hợp kiểm tra của hai tab đều thất bại trước sửa.

- Cỡ font x1 được giảm một nửa cỡ font x2; cùng cỡ được áp dụng khi đo chiều rộng, chiều cao và vẽ chữ.
- Cả hai `GameCanvas.cs` đưa nút đổi tab sang góc trái ở các màn hình trước khi vào game. Vùng nhấn được chuyển theo vị trí vẽ. Trong màn hình game vẫn dùng vị trí của bộ điều khiển hiện tại.
- Kiểm tra sau sửa: 6/6 đạt, chữ rộng 36–37 pixel tại x1; biên dịch C# và kiểm tra gói tin số lượng lớn đều đạt. Log: `.analysis/client-startup/font-scale-before.log`, `font-scale-after.log`.

Trong editor gốc, dừng Play nếu đang chạy, chờ Unity biên dịch lại rồi bấm Play. Khi xuất bản client, dùng đúng Unity 2022.3.62f2 và cả hai scene đã sửa.
