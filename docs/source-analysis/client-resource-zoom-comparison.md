# So sánh res và zoom hai client — 03/10/2026

> Đã triển khai sửa sau báo cáo này: xem [kết quả bổ sung res và loader](client-native-res-fix.md). Các số liệu/nguyên nhân dưới đây ghi nhận trạng thái trước sửa.

Client đang sửa: `C:\Users\Admin\Downloads\PRJ_2Tab_550K`.

Project tham chiếu đầy đủ: `C:\Users\Admin\Downloads\filefolder\PRJ 3in1 Goc\PRJ 3in1 Goc`. Thư mục cha cũng có `Assets/Resources`, nhưng bản đó chỉ có x1 và x2; các số liệu dưới đây dùng project con có cả `Assets`, `Scripts`, `Packages`, `ProjectSettings` và x1–x4.

## Số ảnh được đóng gói trong Assets/Resources

Không tính `.meta`; số dưới đây là PNG/JPG/JPEG trong `res/xN`.

| Bộ res | PRJ_2Tab_550K | PRJ 3in1 Goc |
| --- | ---: | ---: |
| x1 | 0 | 1.038 |
| x2 | 0 | 475 |
| x3 | 0 | 441 |
| x4 | 426 | 513 |

Client 2 tab có font TrueType trong `fontsys/x2`, `fontsys/x3`, `fontsys/x4`: mỗi bộ ba file, tương tự bản tham chiếu. Cả hai không có `fontsys/x1`; bản tham chiếu dùng 26 ảnh font bitmap trong `res/x1/myfont` cho x1. Client 2 tab chỉ có bốn bảng glyph `.bytes`, thiếu các ảnh font này.

## Vì sao đang chạy x1

`MotherCanvas.checkZoomLevel()` của hai client dùng cùng quy tắc PC:

- Vùng render có `width × height < 480000`: zoom x1.
- Từ 480000 trở lên: zoom x2.
- Nhánh điện thoại còn có ngưỡng x3 và x4; nhánh PC hiện không tự chọn x3/x4.

Ví dụ vùng game 791×463 là 366233 pixel nên chọn x1. Cửa sổ Unity lớn không đồng nghĩa vùng render game lớn; ô `Scale 1.3x` của Game View là mức phóng preview trong editor, riêng với `mGraphics.zoomLevel` của game.

## Khác biệt trong cách đọc ảnh

- Client 2 tab: `GameCanvas.loadImage()` luôn đọc `res/x4`, rồi `resizeImage()` thu xuống kích thước của zoom hiện tại. Do đó code hỗ trợ các zoom dù project chỉ giữ một bộ ảnh giao diện x4.
- Bản Gốc: ưu tiên `res/x{zoom}`, sau đó thử ảnh ở thư mục gốc và fallback về x4 có resize.
- Ảnh tải từ server và cache RMS đi theo `x{zoom}` ở cả hai bộ source. Các số lượng trong bảng chỉ nói về tài nguyên đóng gói tại client, không bao gồm res server/cache.

## Thiếu và khác biệt đã xác định

- So với x4 của bản Gốc, client 2 tab thiếu 107 ảnh cùng đường dẫn, gồm ảnh bụi `e/d*.png`, `bg/fog1.png`, `bg/lacay6.png`, `board/title*.png`, một số ảnh menu và ảnh khác. Các đường dẫn này là chênh lệch giữa hai project; cần đối chiếu từng chỗ gọi để xác định phần bắt buộc với bản 2 tab.
- Cả hai có 406 ảnh x4 cùng đường dẫn: 45 ảnh giống pixel, 361 ảnh khác pixel hoặc kích thước. Đây là lý do cần giữ các ảnh mod hiện tại khi bổ sung, không thay toàn bộ bộ res.
- Bản tham chiếu cung cấp 24/25 tên atlas mà `mFont.cs` của client 2 tab yêu cầu; riêng `/myfont/tahoma_7_orange.png` cũng không có ở x1 tham chiếu. Khi khôi phục font gốc cần xử lý riêng tên này.
- 2229 đường dẫn có trong toàn bộ `Assets/Resources` của bản Gốc mà client 2 tab không có. Con số này bao gồm các bản zoom của cùng một tài nguyên, không phải 2229 hình độc lập cần tải từ server.

## Hướng đồng bộ

1. Bổ sung các tài nguyên tương ứng từ project con bản Gốc, giữ ảnh riêng và ảnh khác của bản mod hiện tại.
2. Cho loader ưu tiên bộ ảnh đúng zoom, fallback về x4 khi thiếu; đồng bộ cả hai tab.
3. Khôi phục font bitmap x1 từ tài nguyên gốc, đối chiếu bảng glyph và xử lý tên font cam; giữ font thay thế để tránh lỗi khi thiếu tài nguyên.
4. Kiểm tra menu, đăng nhập, map, NPC, item và chuyển tab ở x1–x4 trước khi kết luận bộ res đầy đủ với server này.

Lần đối chiếu này chưa chép res từ bản Gốc vào client, chưa thay cách chọn zoom và chưa thay JAR/DB.

## Dữ liệu chi tiết

- `.analysis/client-resource-comparison/summary.json`: số lượng, các tên font và chênh lệch.
- `.analysis/client-resource-comparison/missing-from-current.csv`: toàn bộ đường dẫn thiếu so với bản tham chiếu.
- `.analysis/client-resource-comparison/x4-missing-from-current.csv`: 107 ảnh x4 thiếu.
- `.analysis/client-resource-comparison/x4-image-differences.csv`: các ảnh cùng đường dẫn nhưng khác pixel/kích thước.
