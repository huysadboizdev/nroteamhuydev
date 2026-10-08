# Kiểm tra vệ tinh — 05/10/2026

Các lỗi trong báo cáo này đã được sửa và triển khai sau đó; xem [bản sửa vệ tinh](satellite-fixes-2026-10-05.md). Nội dung dưới đây giữ kết quả kiểm tra trước khi sửa.

Đối chiếu SQL `team2026.item_template`, source và bytecode `SRC/20.jar`; kiểm thử chạy riêng, không thay đổi dữ liệu hoặc hiệu ứng của người chơi thật. Không sửa source ứng dụng, không thay JAR.

| ID | Vệ tinh | Tác dụng hiện tại |
|---|---|---|
| 342 | Trí lực | Hồi 10% KI tối đa mỗi lần hiệu ứng được kích hoạt |
| 343 | Trí tuệ | Cộng 20% lượng tiềm năng gốc trong `calSucManhTiemNang`; các hệ số khác vẫn áp dụng |
| 344 | Phòng thủ | Quái thường bỏ qua người có hiệu ứng khi chọn mục tiêu; đường đánh của `Mob` giảm 20% sát thương. Người đánh quái bị xóa cờ phòng thủ. Không phải giáp hay giảm sát thương chung cho PvP/boss; Hirudegarn dùng đường đánh riêng |
| 345 | Sinh lực | Hồi 10% HP tối đa mỗi lần kích hoạt |

Áp dụng cho người đặt hoặc cùng bang đã lưu lúc đặt, còn sống, cùng khu, khoảng cách dưới 200. Nhiều vệ tinh cùng loại không cộng dồn. Cờ hiệu ứng hết sau 3 giây; `Zone.update` chạy item trước player nên hồi thực tế có thể khoảng 4 giây theo nhịp khu 1 giây. Rời phạm vi có thể giữ cờ đến lần cập nhật hết hạn.

## Lỗi và nguy cơ

1. **Đã tái hiện vượt giới hạn:** `UseItem.useItemUnchecked` đếm và đặt riêng, không khóa chung cho khu. Hai người cùng đặt khi đang có 2 vệ tinh đều được qua; kết quả 4 vệ tinh. Giới hạn là 3 mỗi khu, dù thông báo ghi mỗi map. Kiểm thử dùng barrier ngay trước `Zone.addItem`, gọi trực tiếp đường sử dụng item thật.
2. **Đã tái hiện không hết hạn ở nhà:** vệ tinh quá 30 phút được xóa ở map thường, nhưng còn ở map 21, 22, 23 do điều kiện loại trừ map nhà của `ItemMap.update`.
3. **Nguy cơ packet khi đông:** mỗi người được hồi HP gọi `Service.Send_Info_NV`, gửi cập nhật cho người khác trong khu. Khi 50 người cùng bang cùng thiếu HP trong phạm vi, một lượt hồi tạo tối đa 2.450 lượt gửi cho người khác, cộng packet riêng. Đây là số tính theo đường broadcast, không phải lượng packet quan sát trên server hiện tại. KI chỉ cập nhật riêng cho người được hồi; trí tuệ và phòng thủ chủ yếu thay cờ.
4. **Nguy cơ truy cập danh sách đồng thời:** vòng quét người chơi của `ItemMap.satelliteUpdate` và stream đếm vệ tinh không khóa/snapshot ở các khu thông thường; người vào/ra hoặc item bị xóa đồng thời có thể làm lỗi vòng quét. Đây là nguy cơ từ source, chưa tái hiện lỗi này trên server thật trong lần kiểm tra. Map 126 đã có snapshot/COW từ thay đổi trước.

Hai lần `addItem` khi đặt không tạo vệ tinh trùng: `Zone.addItem` kiểm tra `contains`, fixture xác nhận chỉ có một item.

## Bằng chứng kiểm tra

- Fixture xác nhận điều kiện owner/bang/phạm vi/người chết, hồi 10% HP/KI, không cộng dồn, thứ tự cooldown, hai cờ khác, tăng tiềm năng 1.000 → 1.200, hết hạn map thường và giữ lại map nhà, đặt không trùng, race 2 → 4.
- Đo 3 vệ tinh với 50 người mô phỏng, 1.000 lượt: trung bình 0,009 ms/lượt, cao nhất 0,114 ms. Với 200 người: 0,036 ms/lượt, cao nhất 0,084 ms. Network được mô phỏng; đây không phải kiểm thử tải socket/client hay cam kết không lag. 200 người là trường hợp stress, không xác nhận sức chứa khu thật.
- Đọc JVM PID 46184 lúc **23:24:56 UTC+7**: 2.372 khu, 1 người chơi, **0 vệ tinh**, 0 khu vượt 3, tổng hàng đợi gửi 0, không phát hiện deadlock. Không có lỗi `ConcurrentModificationException` trong log khởi động đang dùng tại thời điểm kiểm tra. Số liệu chỉ là một ảnh chụp tức thời.
- Bytecode `ItemMap`, `Satellite`, `UseItem` trong JAR đang dùng khớp bản dựng đã xác minh.
- Công cụ và kết quả: `.analysis/SatelliteAudit.java`, `.analysis/SatelliteRuntimeAudit.java`, `.analysis/run_satellite_audit.py`, `.analysis/satellite-audit/fixtures.txt`, `.analysis/satellite-audit/runtime.txt`.

**Kết luận:** các vệ tinh có tác dụng. Chưa có bằng chứng vệ tinh đang gây lag trên server vì lúc kiểm tra không có vệ tinh hoạt động. Phần tính hiệu ứng nhẹ trong fixture; cần ưu tiên sửa giới hạn đặt đồng thời, hết hạn map nhà, an toàn vòng quét và giảm broadcast HP trước khi xác nhận vận hành ở khu đông.
