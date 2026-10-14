# Sửa source tàn sát — 13/10/2026

Đã áp dụng cho Game1 và Game2 trong `C:/Users/Admin/Downloads/PRJ_2Tab_550K`, theo yêu cầu sửa sau khi người dùng xác nhận đánh không có sát thương và HP quái không giảm.

## Thay đổi

- Tàn sát quái chọn chiêu qua helper mới của GameScr, gửi packet chọn chiêu mà không kích hoạt animation đánh phụ. Helper đồng bộ `myskill`, `lastSkill`, `selectedIndexSkill`, để thao tác chọn chiêu thủ công sau đó vẫn đúng.
- Mỗi lượt tự đánh quái chỉ có một đường gửi packet. `ModFunc.AutoAttack` nhường quyền khi tàn sát quái/người đang bật.
- Kiểm tra kết nối thực của session đang sử dụng, chết, chuyển map, loading, teleport, stone, sleep, blind, trói và các trạng thái gồng/biến hình. Khi hết trạng thái khống chế thì tự đánh tiếp.
- Kiểm tra mục tiêu còn thuộc danh sách quái trong map và có quyền đánh. Thay mục tiêu cũ đã rời danh sách bằng quái hiện tại.
- Chọn chiêu dùng được trước khi so sánh ưu tiên: kiểm tra dữ liệu null, hồi chiêu, KI thường/phần trăm, HP Kaioken ID 9. Khi Kaioken tùy chỉnh không đủ HP sẽ chọn chiêu hợp lệ khác. Dùng ngưỡng HP bảo thủ 10% như client gốc; với bộ giảm HP tiêu hao có thể chuyển sang Dragon sớm hơn mức server yêu cầu.
- Thêm `TryCharMove`, `TrySelectSkill`, `TrySendPlayerAttack` trả kết quả đưa gói vào session. Giữ API void cũ cho các caller hiện có. Chỉ ghi cxSend/cySend sau khi session nhận gói thành công. Khi di chuyển thất bại, tàn sát khôi phục cx/cy và Service giữ lại KI đã dự tính trừ cho bay.
- Di chuyển và đánh tách lượt, giữ thứ tự gói di chuyển trước đòn. Không ép mob.x/y về điểm xuất hiện; khoảng cách đánh vẫn dựa xFirst/yFirst vì quái thông thường trên server giữ location ở đó.
- Chỉ tính hồi chiêu tự đánh quái khi đưa gói tấn công vào session thành công. Protocol không có acknowledgement riêng cho mỗi đòn; thành công ở đây là queue thành công, không đảm bảo server chấp nhận chiêu hoặc socket đã phát đủ dữ liệu.

## Kiểm thử

Đã viết test trước khi sửa: hai controller cũ có 38 assertion thất bại đúng triệu chứng. Sau sửa, chạy các controller thật và phương thức thật được trích nguyên văn từ Service/GameScr/ModFunc trong C# harness chạy trong bộ nhớ. Mạng và trạng thái game được cô lập bằng fixture. Kiểm thử Transport thực phát hiện thêm lỗi trừ KI khi send di chuyển throw; đã sửa và chạy lại qua cả hai client.

Các ca gồm một đòn/không animation phụ, thứ tự chọn chiêu→đánh, các trạng thái khống chế/loading/teleport/gồng, mất kết nối, chiêu không dùng được, thiếu KI phần trăm, Kaioken HP thấp, mục tiêu cũ, hồi chiêu, gửi chọn chiêu/đòn/di chuyển thất bại, khôi phục vị trí/KI, không sửa tọa độ hình quái, selector thủ công, dừng trong animation và tự tiếp tục sau khống chế.

Lệnh kiểm chứng:

```powershell
python .analysis/prepare_tansat_harness.py
powershell -NoProfile -File .analysis/run_tansat_actual.ps1
python .analysis/verify_tansat_source.py
```

Kết quả: Game1 FAILURES=0; Game2 FAILURES=0; source verification PASS 8 files, chính sách hai client giống nhau, không có whitespace mới lỗi, hash APK/JAR giữ nguyên. Kiểm thử dùng biên dịch test C# trong bộ nhớ, không chạy Unity và không tạo bản phát hành.

## Phạm vi phát hành và khôi phục

Chưa build APK/EXE/JAR, chưa kiểm thử gameplay trên Unity/điện thoại và chưa thay bản client đang chạy. Source sửa sẽ được đưa vào bản phát hành khi người dùng yêu cầu build. Không sửa hoặc triển khai server trong lượt này.

Backup 8 file source: `.analysis/tansat-source-before/`; hash gốc `.analysis/tansat-source-before.json`; patch `.analysis/tansat-source.patch`; metadata kiểm chứng `.analysis/tansat-source-verification.json`. Backup source trước lượt này bao gồm các sửa hào quang trước đó, vì vậy khôi phục các file này không làm mất sửa hào quang.

APK hiện có giữ SHA-256 `44ae4f6c142db26d8d52fc0a9d1926cf8ee41665bd042b2a29ba26b565b33f0e`; `SRC/20.jar` giữ SHA-256 `b59aeddb7e4c01ba8e8ffa8346611cd0fd73e5417f671b7b6aad864e11ee8d89`.
