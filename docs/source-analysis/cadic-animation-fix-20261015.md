# Sửa đứng quái và hiệu ứng tại Sa Mạc Hoang Vu

Ngày 15/10/2026, theo yêu cầu triển khai sửa của người dùng.

## Nguyên nhân đã tái hiện

File `SRC/data/mob/x4/95` khai báo `typeData=2` nhưng không chứa bảng animation. `readFrameBoss` gặp EOF và bỏ qua exception; `Mob.GetFrame()` tiếp tục truy cập bảng null. `GameScr.update()` nuốt lỗi ở catch chung, bỏ qua các quái phía sau, chữ sát thương và hiệu ứng. Nhân vật được cập nhật trước vòng quái nên vẫn hoạt động. PC chọn zoom1/2; mobile độ phân giải cao có thể chọn zoom4. Chưa xác nhận zoom runtime của điện thoại trong video.

## Thay đổi

- Resource x4 Cadíc đổi byte cuối từ 2 sang 0 để dùng animation thường đã có cho mob95. Không đổi ảnh, dữ liệu sprite, sát thương, HP, tỉ lệ Killis hoặc hàng đợi mạng. Resource có 15 sprite frame, đủ cho các sequence mob95 sử dụng frame 0–11.
- `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1/Mob.cs` và `Game2/Mob.cs`: kiểm tra bảng có đủ sáu sequence không rỗng; mọi index phải tham chiếu sprite frame hợp lệ. Khi thiếu/sai dữ liệu, giữ sequence mặc định và chờ lần cập nhật tiếp; chỉ thay toàn bộ sequence sau khi xác thực thành công.
- Regression: `PRJ_2Tab_550K/Tests/MobFrameRecoveryChecks.py`, có thể chỉ định server root qua `NRO_SERVER_ROOT`.

## Sao lưu và khôi phục

Metadata: `.analysis/desert-video-audit/repair-latest.json`.

- Backup: `.analysis/desert-video-audit/backup-20261015-135638-110839/mob-x4-95.bin`.
- SHA-256 cũ: `e96fe058648f41a78ae39d340a3833a34f774c3a311bcb1cf4643fb4f8272501`.
- SHA-256 mới: `c089bbd363563e8727c3d2a08e1cbc5300b2a3cf559b107e3f31e78528cee19a`.
- Chính xác một byte thay đổi, offset 213840. Có thể khôi phục file resource từ backup sau khi đối chiếu hash.

## Kiểm chứng

- RED: kiểm thử trước sửa thất bại ở Game1/Game2 do NullReferenceException; kiểm tra wire resource phát hiện byte 2.
- GREEN: `python Tests/MobFrameRecoveryChecks.py` qua cả hai test. Kiểm tra bảng thiếu/ngắn, sequence null/rỗng/index âm/vượt biên trên cả sáu vị trí, phục hồi khi bảng hợp lệ đến sau, chờ sprite data và animation thường. Hàm GetFrame/isTypeNewMod được trích trực tiếp từ source để chạy bằng harness C# trong bộ nhớ.
- `python Tests/NpcImageDecodeChecks.py`: qua kiểm tra giải mã của cả hai tab.
- `python Tests/android_compatibility_test.py`: cấu hình Android6/API23, ARMv7+ARM64 vẫn đạt.
- Scoped `git diff --check`: không có lỗi whitespace; rà soát độc lập không phát hiện lỗi correctness trong phạm vi thay đổi.

## Trạng thái áp dụng

Đã sửa source native hai tab và resource server tại workspace local. Chưa build APK/EXE/JAR, chưa thay JAR hoặc restart server, chưa đẩy resource lên VPS. APK v2 đang cài trên điện thoại chưa có guard mới. Chưa kiểm thử vòng update/render Unity đầy đủ hoặc trên Android thật. Khi resource sửa được đưa lên server thực sự phục vụ APK, cần khởi động lại client để loại bỏ template lỗi đã giữ trong bộ nhớ và yêu cầu lại dữ liệu quái.

## APK theo yêu cầu tiếp theo của người dùng

Sau khi người dùng yêu cầu rõ build APK mới, đã dựng `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Builds/Distribution/20261015-Cadic-AnimationFix-v3/Android/NRO.apk` từ source native đã sửa. Unity 2022.3.62f2: Succeeded, 0 errors, 82 warnings; các bộ kiểm tra prebuild Unity đã chạy. Regression MobFrameRecoveryChecks.py đạt trước build. Chưa kiểm thử gameplay trên Android thật.

Đã xác minh chữ ký hợp lệ và khớp v2, package không đổi, minAPI23/targetAPI34, IL2CPP ARMv7+ARM64, landscape, ZIP không lỗi và alignment đạt. APK 84.179.293 bytes, SHA-256 `fbaf22b76f3f4ab24b855cef54a1552691d38170b414f30144fe6ee51bb79073`. Metadata: thư mục `Android/verification.json` bên cạnh APK. Không build/thay JAR, không triển khai resource VPS.
