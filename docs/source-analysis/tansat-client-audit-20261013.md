# Kiểm tra tàn sát client — 13/10/2026

## Phạm vi và kết luận

Kiểm tra source cả Game1/Game2 và đối chiếu server hiện hành. Chưa đạt tính nhất quán trong điều kiện dùng chiêu, gửi di chuyển và gửi đòn. Có đường dẫn mã phù hợp triệu chứng đánh không giảm HP, nhưng chưa xác định một nguyên nhân duy nhất cho tình huống người dùng vì chưa có map/chiêu/log gói tin cụ thể.

Không chạy Unity, compiler, build APK/EXE/JAR hoặc triển khai trong lượt này. Chỉ cập nhật quy tắc không tự build theo yêu cầu người dùng và lưu báo cáo. Hash JAR/APK hiện hành giữ nguyên.

## Các phát hiện

1. **Kaioken thiếu kiểm tra HP trong bộ chọn chiêu tàn sát.** `PickMobController.CanUseSkill` chỉ kiểm tra hồi chiêu và KI. Danh sách mặc định có ID 17, `IsSkillBetter` ưu tiên 17 so với 2. Server `SkillService.canUseSkillWithMana` lại từ chối Kaioken khi HP <= lượng HP tiêu hao (thường 10% HP tối đa, thay đổi theo bộ thần vũ trụ). Ví dụ HP tối đa 1.000, HP hiện tại 100, KI đủ: client đánh dấu chiêu dùng được, server từ chối; client vẫn đánh dấu hồi chiêu. Có thể lặp lại thay vì dùng chiêu đánh khác.

2. **Hai đường gửi đòn trong cùng nhánh chọn chiêu.** Tàn sát gọi `GameScr.doSelectSkill` khi đổi chiêu, rồi tiếp tục `ModFunc.AttackMob` ngay. Một số nhánh `doSelectSkill` gọi `doFire`, tạo skill animation; animation `Char.setAttack` có đường gửi đòn riêng. `AttackMob` không cập nhật `hasSendAttack` của animation. Khi cả hai nhánh xảy ra, server có thể nhận thêm đòn trong hồi chiêu và từ chối. Đây là khả năng được xác nhận từ call flow; chưa đo số gói tin trong gameplay.

3. **Vị trí client có thể khác server khi di chuyển bị chặn.** `ModFunc.MoveTo` sửa trực tiếp cx/cy rồi gọi `Service.charMove`. `charMove` có thể return khi đổi map, dừng đọc, teleport, cy <= 0 hoặc telePortSkill. Tàn sát không có guard tương ứng và chỉ đo khoảng cách theo cx/cy cục bộ. Với vị trí client cạnh quái nhưng server vẫn cách 200 px, chiêu đấm server có giới hạn 100 px sẽ trả damage 0. Server cũng bỏ qua di chuyển và từ chối chiêu khi có hiệu ứng khống chế, trong khi guard Update của tàn sát chỉ kiểm tra chết/HP và isCharge ở nhánh đánh.

4. **Hồi chiêu vẫn bị ghi sau lời gọi gửi thất bại.** `AttackMob`/`Service.sendPlayerAttack` bắt lỗi và không trả kết quả. Tàn sát luôn gọi `SetUsedSkill` ngay sau đó. Đánh dấu hồi chiêu dự đoán là bình thường, nhưng thiếu phân biệt gửi thất bại và thiếu điều kiện trước gửi làm triệu chứng đứng chờ rõ hơn. Không nên yêu cầu xác nhận server cho mỗi đòn nếu protocol hiện tại không hỗ trợ; sửa bằng một đường gửi chuẩn và kiểm tra điều kiện trước gửi.

5. **Tọa độ quái không phải nguyên nhân đã chứng minh.** Tàn sát ép mob.x/y về xFirst/yFirst và dùng khoảng cách 48 px. Server tính khoảng cách bằng mob.location. Source Mob server thông thường giữ tọa độ xuất hiện, nên không thể kết luận cứ quái bay/di chuyển là đánh hụt. Cần đối chiếu tọa độ runtime nếu lỗi xảy ra với loại quái đặc biệt. Ngưỡng client 48 nhỏ hơn ngưỡng server 100 khi tọa độ đồng bộ, tự nó không gây miss.

## Đối chiếu protocol

Client gửi packet 54 (mob ID và hướng), server case 54 đọc mob ID rồi `Service.attackMob` tìm mob trong zone hiện tại. Phần hướng ở cuối không bị đọc thành mob ID thứ hai. Packet di chuyển -7 đọc x và y tùy có dữ liệu. Vì vậy chưa thấy sai format của hai packet này trong luồng tàn sát.

Các điều kiện server từ chối chiêu: hết KI, chưa hồi chiêu, quái chết, khống chế; chiêu đấm quá khoảng cách 100 px gây damage 0. Cần phân biệt không có damage packet và có damage 0 trong lần tái hiện thực tế.

## Kiểm chứng không build

`python .analysis/audit_tansat_no_build.py` qua đối chiếu source cả hai client và ba kịch bản mô hình (di chuyển bị chặn, Kaioken HP thấp, hai đường gửi đòn). Đây là kiểm tra source và số học, không phải chạy C# hay test gameplay. Kết quả lưu `.analysis/tansat-source-audit-20261013.json`.

- JAR giữ SHA-256 `b59aeddb7e4c01ba8e8ffa8346611cd0fd73e5417f671b7b6aad864e11ee8d89`.
- APK giữ SHA-256 `44ae4f6c142db26d8d52fc0a9d1926cf8ee41665bd042b2a29ba26b565b33f0e`.

Hướng sửa source: kiểm tra HP Kaioken và các guard trạng thái; chọn một đường gửi đòn, tránh doSelectSkill tự đánh rồi đánh lại; bảo đảm di chuyển được gửi hợp lệ trước khi coi nhân vật đã vào tầm. Không bỏ kiểm tra khoảng cách/hồi chiêu của server.


## Th?ng tin b? sung t? ng??i d?ng

Ng??i d?ng x?c nh?n kh?ng hi?n s?t th??ng v? HP qu?i kh?ng gi?m. ?u ti?n ?i?u tra ??n ch?a t?i server ho?c `useSkill` return false tr??c khi g?i `playerAttackMob`, thay v? kh?ng ??nh ch? l?i kho?ng c?ch. Khi qu? t?m, lu?ng server th?ng th??ng v?n g?i `Mob.injured` v?i damage 0 v? g?i packet -9; client case -9 c? nh?nh hi?n th? miss. Do ch?a c? trace runtime, vi?c kh?ng th?y ch? miss ch? l? d?u hi?u ph?n bi?t, kh?ng ph?i ch?ng c? ?? lo?i tr? m?t/kh?ng ??c packet.

?? ??i chi?u ??nh d?ng packet tr? HP/s?t th??ng: server d?ng writeInt, client `Message.readLong` m?c ??nh ??c int v? `ModFunc.isReadInt=true` trong c? hai Game. Kh?ng th?y l?ch int/long ? c?u h?nh hi?n t?i. Client case -9 c?p nh?t HP tr??c khi v? ch? s?t th??ng.

`PickMobController.Update/CanUseSkill` thi?u c?c guard stone/sleep/blind/hold/loading m? lu?ng ??nh chu?n c?; direct AttackMob c? th? ?i ti?p d? lu?ng animation t? ch?i ho?c server ?ang kh?ng ch?/ch?a h?i chi?u. G?i packet khi m?t k?t n?i v?n c? th? ch? v?o queue Sender nh?ng kh?ng ???c g?i l?n m?ng. Ch?a ?o queue/g?i tin l?c x?y ra tri?u ch?ng.
