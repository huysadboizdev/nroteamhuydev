# Kiểm tra hồi sinh nhóm sói, Pilaf và Mabu mập

Đã kiểm tra source, JAR đang dùng và JVM PID 19236 bằng probe chỉ đọc. Không thay JAR, không restart, không ép spawn hay thay thời gian chờ/cấu hình boss.

Lúc 21:53, hai nhóm HuntingTrio và Mabu mập đều COOLDOWN; đối tượng cũ đã dọn sạch, các map ứng viên có khu trống, thread Update boss vẫn chạy. next của Mabu là 21:59:11.305; nextAction của hai nhóm là 21:59:33.886/900.

Theo dõi heartbeat tự nhiên từ 21:53:39 đến 21:59:40: cuối cùng đủ sáu HuntingTrio và một Mabu, không can thiệp thế giới game. Probe lúc 21:59:52 xác nhận:

| Nhóm | Trạng thái | Map:khu | Thành viên/HP |
|---|---|---|---|
| Sói | ACTIVE | 7:2 | Basil, Lavender, Bergamo; mỗi con HP 1000 |
| Pilaf | ACTIVE | 3:6 | Pi Láp, Mai, Su; mỗi con HP 1000 |
| Mabu mập | ACTIVE | 52:8 | HP 300.000.000 |

Luồng hiện tại: không người trong đúng khu liên tục 5 phút thì rời đi; sau đó chờ 10 phút, chọn khu mới và xuất hiện. Sói/Pilaf cũng chờ 10 phút khi cả ba bị hạ; còn sống một thành viên thì chưa kết thúc lượt. Mabu thêm giới hạn ACTIVE 20 phút, kể cả có người. Mabu 12h/14h là luồng khác; heartbeat thời điểm đầu kiểm tra ghi nhận 10 và 7 boss tương ứng.

Các test chạy trên SRC/20.jar thành công: HuntingTrioLifecycleTest, FatMabuLifecycleTest, HuntingTrioCleanupTest exitFailure/blockedDetach, FatMabuCleanupTest. Trường hợp cleanup cố tình gây exception kiểm tra khả năng tháo khu và retry; không phải lỗi trong server thật.

Kết luận: không tái hiện lỗi mất hồi sinh. Thời điểm user báo trùng khoảng chờ sau khi các khu vắng người. Dữ liệu đối chiếu: `.analysis/boss-respawn-probe/before-cooldown.txt`, `.analysis/boss-respawn-probe/snapshot.txt`, `.analysis/boss-respawn-observations.json`.
