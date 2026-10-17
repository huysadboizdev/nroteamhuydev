# Kiểm tra capsule chuyển map bị đứng trên APK

Ngày kiểm tra: 17/10/2026 (Asia/Bangkok).

## Cập nhật triển khai theo yêu cầu người dùng

Người dùng sau đó yêu cầu "build luôn apk đi". Đã build thành công APK `C:\Users\Admin\Downloads\PRJ_2Tab_550K\Builds\Distribution\20261017-CapsuleReceiveFix-01\Android\NRO.apk`, 84.272.762 byte; Unity exit 0, build errors 0. Regression hàng đợi nhận và XMAP chạy qua trong Unity Editor. Chữ ký v1/v2, ZIP integrity, ARMv7/ARM64 và hash source đều kiểm tra qua; certificate trùng APK cục bộ trước. SHA-256 APK: `410f25192ccf9030987557c54529ea39a82decbe9d4e0046f1a94a0ff6f3f39e`. Chưa kiểm thử trên điện thoại; các ghi chú "chưa build" bên dưới mô tả giai đoạn sửa source trước khi người dùng yêu cầu build.

Sau yêu cầu "triển khai sửa tôi xem", đã sửa source client ở cả 4 session Game1/Game2. Hàng đợi nhận dùng Queue<Message> có khóa; handler chạy ngoài khóa, kiểm tra cờ dừng trước từng gói, giữ nguyên mọi gói chưa xử lý và thứ tự gói mới đến. Main-thread arrivals cũng đi qua hàng đợi. Mỗi session xử lý tối đa 64 gói hoặc khoảng 2 ms mỗi Unity frame; gọi OnGUI lặp trong cùng frame không cấp lại ngân sách. Một handler riêng lẻ vẫn có thể vượt 2 ms vì chỉ nhường giữa các handler.

Đã thấy regression mới thất bại trên source cũ ở cả 4 session, sau đó qua 6 kịch bản/session (24 lượt kiểm tra) trên source sửa. Kiểm tra hàng đợi gửi hiện có qua 160.000 tin đúng một lần. Đây là kiểm tra C# in-memory, không phải kiểm thử trên Android; chưa build APK/EXE/JAR và chưa thay server.

Kiểm tra hiện hành nằm ở `C:\Users\Admin\Downloads\PRJ_2Tab_550K\Tests\CapsuleReceiveChecks.ps1`. Kịch bản chẩn đoán ban đầu bên dưới là bằng chứng trước sửa, được thay bằng regression kỳ vọng hành vi đã sửa.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File 'C:\Users\Admin\Downloads\PRJ_2Tab_550K\Tests\CapsuleReceiveChecks.ps1'
```

## Phạm vi và kết luận

Đã xem video `C:\Users\Admin\Downloads\video6240247959628293595.mp4` (15,50 giây), đối chiếu server trong Teamobi2026 và client Unity trong `C:\Users\Admin\Downloads\PRJ_2Tab_550K`.

Phát hiện lỗi thứ tự xử lý gói tin ở client có thể tái hiện bằng hàm `update()` lấy nguyên văn từ source. Cơ chế phù hợp với video: tên map chuyển từ Làng Mori sang Vách núi đen, xuất hiện Xin chờ, rồi hiện lại nền Làng Mori với NPC biến mất và nhân vật đứng yên.

Tại thời điểm chẩn đoán ban đầu chưa có log điện thoại hoặc xác nhận phiên bản APK trong video, nên chưa xác nhận trực tiếp lỗi runtime trên thiết bị. Phần chẩn đoán ban đầu không sửa source ứng dụng, không build APK/EXE/JAR, không thay JAR hoặc dừng server. Trạng thái triển khai sau đó được ghi riêng ở trên.

## Thông tin người dùng bổ sung

Người dùng xác nhận: bị đứng lúc chuyển map, sau đó map vẫn lag/không mượt. Vì vậy không được coi mô hình kẹt vĩnh viễn bên dưới là lời giải đầy đủ cho toàn bộ triệu chứng. Mô hình xác nhận một lỗi điều phối nhận tin tồn tại trong source; lag sau khi map thực sự tải hoàn tất cần được đo riêng.

Rà thêm source cho thấy `Main.OnGUI()` của cả Game1/Game2 đều gọi hai hàm nhận tin trước điều kiện chọn tab và Repaint. Mỗi hàm nhận tin xử lý toàn bộ snapshot hàng đợi, không có giới hạn số gói hoặc thời gian cho một lượt. Các handler có thể tải/giải mã ảnh và dữ liệu map trên main thread. Đây là một đường có khả năng gây khựng khi dữ liệu dồn về, kể cả sau khi map đã hiện; chưa có số liệu thời gian handler, độ dài hàng đợi hoặc FPS thiết bị để quy kết đây là nguyên nhân lag kéo dài.

Các điểm cần đo trên đúng APK: thời gian xử lý từng gói trong lúc đổi map và vài giây sau đó, số gói chờ của từng session/tab, frame time, GC và thời gian tải/giải mã ảnh. Video được ghi khoảng 16,77 frame/giây; không thể dùng thông số của video để suy ra FPS thực của game.

## Chuỗi gây lỗi

1. `Controller.cs`, gói `-65`: capsule bật `isStopReadMessage = true`, đặt `GameScr.lockTick = 500`, tạo Teleport rời map.
2. `Session_ME.update()` / `Session_ME2.update()` chỉ kiểm tra cờ dừng ở đầu hàm, lấy toàn bộ hàng đợi vào một mảng rồi gọi handler cho hết mảng. Nếu `-65`, `-22`, `-24` cùng đợt nhận, handler vẫn xử lý gói đổi map sau khi cờ dừng đã bật.
3. `Controller`, gói `-24`, gọi `GameScr.resetAllvector()`, xóa Teleport vừa tạo; đặt `GameCanvas.isLoading = true` và cập nhật tên/map ID.
4. `Assets/Resources/res/mymap` không có `20.bytes` (Vách núi đen). Client gửi `requestMaptemplate(20)`, giữ `messWait`, rồi return trước `loadInfoMap()`.
5. Server có dữ liệu `SRC/data/map/tile_map_data/20` (1.472 byte); `DataGame.sendMapTemp()` trả qua `-28`, subcommand `10`. Tuy nhiên, khi phản hồi vào hàng đợi ở lượt tiếp theo, `isStopReadMessage` chặn xử lý.
6. `GameCanvas.update()` bỏ qua `currentScreen.update()` khi `isLoading` còn true. Vì vậy watchdog `lockTick--` trong `GameScr.update()` cũng không chạy; Teleport mở khóa đã bị xóa. Client kẹt chờ dữ liệu mà chính nó đang chặn.
7. Sau timeout 3 giây trong `GameCanvas.paint()`, `GameScr.switchToMe()` xóa `Char.isLoadingMap` và gửi `finishLoadMap`, nhưng không xóa `GameCanvas.isLoading` / cờ dừng nhận tin, cũng không tải địa hình mới. Do đó màn hình Xin chờ mất đi và lộ cảnh cũ bị xử lý dở dang.

Nếu gói rời map được xử lý riêng và Teleport kết thúc trước khi nhận gói đổi map, luồng có thể chạy bình thường. Điều này giải thích khả năng lỗi phụ thuộc thời điểm nhận gói.

## Vị trí đối chiếu

Các đường dẫn dưới đây tương đối với client PRJ_2Tab_550K:

- `Assets/Scripts/Assembly-CSharp/Game1/Session_ME.cs:457`: xử lý cả mảng sau một lần kiểm tra cờ dừng.
- `Assets/Scripts/Assembly-CSharp/Game1/Session_ME2.cs:452`: cùng cơ chế; Game2 có cả hai bản tương ứng.
- `Assets/Scripts/Assembly-CSharp/Game1/Controller.cs:1315`: bật cờ dừng khi capsule xuất phát.
- `Assets/Scripts/Assembly-CSharp/Game1/Controller.cs:2661`: nhận map, xóa vector, xin map template.
- `Assets/Scripts/Assembly-CSharp/Game1/Controller.cs:5290`: phản hồi map template.
- `Assets/Scripts/Assembly-CSharp/Game1/GameCanvas.cs:658`: không update màn chơi khi đang loading.
- `Assets/Scripts/Assembly-CSharp/Game1/GameCanvas.cs:2397`: timeout loading gọi switchToMe.
- `Assets/Scripts/Assembly-CSharp/Game1/GameScr.cs:1009`: switchToMe.
- `Assets/Scripts/Assembly-CSharp/Game1/GameScr.cs:1044`: xóa Teleport.
- `Assets/Scripts/Assembly-CSharp/Game1/GameScr.cs:4367`: watchdog nằm trong update màn chơi.
- `Assets/Scripts/Assembly-CSharp/Game1/Teleport.cs:372`: Teleport xuất phát hoàn tất mới bỏ cờ dừng.

Server: `SRC/src/nro/models/map/service/ChangeMapService.java` gửi gói capsule rồi clearMap/mapInfo; `SRC/src/nro/models/data/DataGame.java:374` trả dữ liệu địa hình.

## Kiểm chứng

Script `.analysis/capsule-video/Check-CapsuleReceive.ps1` trích nguyên hàm update từ cả bốn source nhận tin, chạy trong C# in-memory bằng Add-Type. Handler và vòng đời loading là mô hình tối giản theo source, không phải chạy Unity hoặc thiết bị Android.

Kết quả cả 4 trường hợp:

- Gói xuất phát và đổi map tách lượt: tải map hoàn tất.
- Hai gói chung lượt: tái hiện cờ dừng còn bật, Teleport bằng 0, phản hồi địa hình nằm trong hàng đợi sau 1.000 tick, lockTick vẫn 500, loading còn true dù overlay đã tắt.

Chạy bằng PowerShell:

```powershell
Get-Content -LiteralPath '.analysis\capsule-video\Check-CapsuleReceive.ps1' -Raw | Invoke-Expression
```

Bản APK cục bộ đã kiểm tra thông tin: `Builds/Distribution/20261016-233405-GcTransitionFix/Android/NRO.apk`, SHA-256 `8469e91134e375e6e10aaf8641b80e95b448b431658ae1b54ada50a99d1c4a01`. Đây là IL2CPP; không suy diễn rằng source hiện tại chắc chắn trùng binary hoặc đây là APK trong video.

## Hướng sửa đề xuất

Sửa luồng nhận tin để kiểm tra cờ dừng sau từng handler; giữ nguyên thứ tự và giữ lại mọi gói chưa xử lý trước các gói mới đến. Áp dụng nhất quán Session_ME và Session_ME2 ở cả hai tab. Không chỉ thêm break vào vòng lặp hiện tại vì cả hàng đợi đã bị lấy ra và xóa, làm mất các gói còn lại.

Sau sửa cần kiểm tra gói cùng đợt/tách đợt, thứ tự gói mới đến trong lúc dừng, map có/không có resource cục bộ, và capsule cả hai tab. Cuối cùng xác nhận trên APK đúng phiên bản và log thiết bị. Các thay đổi BFS/giảm GC đã có trong workspace không khắc phục trực tiếp cơ chế kẹt nhận tin này.
