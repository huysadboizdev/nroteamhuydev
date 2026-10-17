# Kiểm tra Dịch chuyển tức thời Trái Đất bị lag

Ngày 17/10/2026. Phạm vi: kiểm tra source client `C:\Users\Admin\Downloads\PRJ_2Tab_550K` và server Teamobi2026. Không sửa source ứng dụng, không build thêm APK/JAR, không thay server.

## Kết luận có bằng chứng

Người dùng bổ sung: "dịch chuyển xong đánh cứ báo hụt, qua map cũng không được". Đối chiếu thêm cho thấy cờ `telePortSkill` giữ nguyên chặn gói di chuyển `-7`, khiến vị trí client có thể tiếp tục thay đổi nhưng vị trí server còn cũ. Client yêu cầu chuyển map bằng gói `-23` không kèm tọa độ (`Service.cs:814`); trước đó `Char.cs:1859` gọi charMove nhưng bỏ qua việc gửi có thành công hay không. Server dùng vị trí đã lưu để kiểm tra cổng (`MapService.getWaypointPlayerIn`, dòng 31; `ChangeMapService.changeMapWaypoint`, dòng 482). Nếu vị trí server nằm ngoài cổng thì chuyển map thất bại. Đây là đường giải thích được triệu chứng không qua map khi vị trí bị lệch.

Các chiêu đấm thường sau đó cũng kiểm tra khoảng cách theo vị trí server (`SkillService.useSkillForMob`, dòng 105); đứng sát quái ở client chưa đủ nếu server vẫn ở xa. Đây là khả năng liên quan tới việc người dùng thấy hụt. Chưa có packet/log từ điện thoại để xác định từng chữ "hụt" là gói damage 0, né đòn hay một đường hiển thị khác; không coi mọi đòn hụt đều có cùng nguyên nhân. Skill DCTT ID 20 không nằm trong danh sách chiêu đấm bị giới hạn cự ly này.

Skill Dịch chuyển tức thời (template ID 20) có một đường bị kẹt logic phía client khi dùng qua hoạt ảnh đánh thông thường. Lỗi xuất hiện ở cả hai tab, không có điều kiện chỉ áp dụng Android. Nó có thể gây cảm giác dịch chuyển xong khựng/không đánh hoặc không di chuyển tiếp. Chưa có log hoặc số đo frame time trên điện thoại nên không kết luận đây là nguyên nhân duy nhất của tình trạng lag người dùng báo.

## Chuỗi gây lỗi

1. `Game1/Char.cs:4438` (Game2:4449), nhánh skillPaint 128–134, đổi tọa độ nhân vật về tọa độ mục tiêu trước khi gửi đòn; sau đó đặt `telePortSkill = true`. Tọa độ đã báo server `cxSend/cySend` vẫn ở vị trí cũ.
2. `Game1/Char.cs:4869` (Game2:4880), `setAttack()` yêu cầu `TryCharMove()` thành công trước khi gọi `TrySendPlayerAttack()` nếu tọa độ hiện tại khác tọa độ đã báo.
3. `Game1/Service.cs:876` và bản Game2 tương ứng từ chối `TryCharMove()` khi `telePortSkill` đang true. Vì vậy `setAttack()` return trước khi gửi đòn Dịch chuyển tức thời.
4. Server chỉ thực hiện nhánh DCTT sau khi nhận đòn. `SRC/src/nro/models/services/SkillService.java:636` gọi `Service.setPos()` rồi gây sát thương/choáng; `Service.java:1727` phát gói 123, type 1.
5. Client nhận gói này tại `Game1/Assets/src/f/Controller2.cs:956`, gọi `Char.setPos()`. Trong update vị trí type 1 (`Game1/Char.cs:1614`, Game2 tương ứng), client mới xóa `telePortSkill`.
6. Đòn bị chặn nên không có phản hồi vị trí tương ứng để mở cờ. Khi hoạt ảnh kết thúc, hai nhánh dọn skillPaint trong `updateSkillPaint()` cũng không xóa cờ này; di chuyển và một số luồng tự đánh/đánh lại tiếp tục bị chặn.

Trường hợp đã đứng đúng tọa độ mục tiêu có thể gửi đòn vì điều kiện đồng bộ di chuyển không chạy. Vì vậy khoảng cách/vị trí mục tiêu làm triệu chứng khác nhau.

## Kiểm chứng

Chạy:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File '.analysis\dctt-audit\Check-InstantTransmission.ps1'
```

Script trích nguyên nhánh bắt đầu teleport, điều kiện chặn trước đòn, guard của TryCharMove và nhánh mở cờ khi nhận vị trí từ source thật. Phần gửi mạng được thay bằng bộ đếm; đây là kiểm tra logic in-memory, không phải chạy Unity/Android.

Kết quả cho từng tab:

- 14 trường hợp kẹt: 7 cấp hoạt ảnh, mục tiêu quái/người chơi; mỗi trường hợp thử 100 lần vẫn không phát gói di chuyển hoặc đòn, cờ teleport còn true.
- 29 đối chứng qua: cùng tọa độ vẫn gửi đòn; phản hồi vị trí type 1 giả lập mở được cờ; di chuyển và đánh thường vẫn hoạt động.

Tổng hai tab: 28 trường hợp tái hiện kẹt và 58 đối chứng. Tất cả kiểm tra qua với kỳ vọng xác nhận lỗi hiện tại.

`git blame` xác định điều kiện bắt buộc TryCharMove trước đòn được thêm ở commit `993a8ef32` ngày 16/10/2026. Không sửa điều kiện này trong lần sửa hàng đợi capsule trước đó.

## Giới hạn và phạm vi sửa phù hợp

Luồng AutoAttack/tàn sát có đường gửi đòn riêng nên không mặc định quy mọi cách dùng skill cho cùng lỗi. Luồng bắt đầu từ hoạt ảnh thông thường và các luồng đi vào nó đã được xác nhận có xung đột. Các bài ManualMobAttackChecks hiện có giả lập TryCharMove luôn true, nên không bắt được trường hợp này.

## Bản sửa client và APK (2026-10-17)

Đã sửa cả Game1 và Game2 tại client `PRJ_2Tab_550K`. Với nhân vật cục bộ, skill paint 128–134 không còn nhảy tọa độ hoặc bật `telePortSkill` theo dự đoán trước khi server trả packet123. Người chơi ở xa vẫn giữ hiệu ứng dự đoán cũ. Đòn DCTT và đổi map tiếp tục dùng tọa độ server có thẩm quyền; guard đồng bộ di chuyển bình thường không bị nới lỏng. Không đổi server hoặc JAR.

`Tests/InstantTransmissionChecks.ps1` qua 86 ca mỗi tab (172 tổng). `ManualMobAttackChecks.ps1` và `CapsuleReceiveChecks.ps1` cũng qua. Regression chạy tự động khi phân phối ghi `HIGH_SPEED_SKILL_REGRESSION_PASS checks=169672`, cùng kết quả đạt cho receive-queue và xmap.

APK Android IL2CPP đã build thành công, không lỗi, ARMv7 và ARM64, Android API tối thiểu 23 / target 34: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Builds/Distribution/20261017-InstantTransmissionFix-01/Android/NRO.apk`. Chữ ký v1/v2 hợp lệ và certificate SHA-256 trùng APK capsule trước đó, nên có thể cập nhật cùng package nếu thiết bị đang dùng bản cùng certificate. APK ZIP nguyên vẹn; cả hai thư viện `libil2cpp.so` đều có mặt. SHA-256: `6C7F8F388D208391487B2D5F280C5103654AD0F368F8E28D00D0DFE9557A6365`.

Unity Editor đang giữ khóa dự án và không có cửa sổ điều khiển, nên build thực hiện từ bản sao tạm của source; tiến trình Editor ban đầu được giữ nguyên. Chưa kiểm thử trực tiếp trên điện thoại hoặc server đang chạy.
