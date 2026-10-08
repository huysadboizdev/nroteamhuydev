# Hỗ trợ nhiệm vụ — triển khai 2026-10-07

Đã cài JAR hỗ trợ nhiệm vụ và khởi động lại server. Thời gian cài: 2026-10-07T21:17:25.369385+07:00. JVM PID 10584, cổng 14445.

## Hành vi

- Hàng ngày 14:00–17:00 theo Asia/Bangkok (UTC+7).
- Sáu nhóm: Tiểu đội sát thủ Nappa, Fide, Android 19/20, Android 15/14/13, Póc/Píc/King Kong và Xên Bọ Hung thị trấn Ginder. Nhiệm vụ chính 20–26; không tăng boss nhiệm vụ 19, võ đài Sên 27 hoặc Mabu 28.
- Mỗi nhóm có một chuỗi gốc và hai chuỗi bổ sung: tối đa 12 chuỗi bổ sung/30 boss thành viên. Hồi 120 giây sau khi cả nhóm hoàn tất; chờ thành viên cuối cùng, giữ nguyên chuyển dạng/gọi thành viên.
- Giữ HP/sức đánh, cả chỉnh chỉ số live, yêu cầu sức mạnh và các bước cốt truyện/quái hiện tại.
- Boss bổ sung dùng ID mạng riêng và loại nhiệm vụ riêng. Chỉ cộng nhiệm vụ đúng bước, mỗi lần chết/dạng một lần; không có đồ rơi, điểm sự kiện, huy hiệu, achievement, KOL hoặc thông báo boss xuất hiện/tiêu diệt đại trà. Boss gốc giữ thưởng hiện có.
- Chọn các khu hợp lệ trong chính map gốc, ưu tiên người đang ở bước boss tương ứng; tránh khu đã có chuỗi cùng nhóm.
- 17:00 ngừng hồi mới, thu hồi chuỗi chưa giao chiến. Chuỗi đã giao chiến được hoàn tất tối đa đến 17:05; thông báo khi mở, đóng và thu hồi cuối. Không đưa người chơi ra khỏi map thường.
- Boss gốc trở lại thời gian hồi thường ngoài giờ. Bản hỗ trợ được dọn khi bảo trì; lỗi dọn được retry và không tạo thêm khi còn đối tượng chờ dọn.
- Thoát phó bản, capsule và phần thưởng phó bản giữ nguyên theo yêu cầu người dùng: hash 10 file trong phạm vi này không đổi.

## Kiểm chứng

- Java 17 compile đầy đủ, 226 nhóm kiểm thử qua trên classes, JAR staging cuối và JAR đã cài.
- Ba bộ kiểm thử QuestSupport dùng constructor và TaskService thật, kiểm tra các boss/dạng, power gate, callback trùng/muộn, thưởng gốc, live HP/sức đánh và lựa chọn khu thật.
- Mô phỏng khung 3 giờ ở tick 1,5 giây, 100 tick đồng thời, giữ tối đa 12 chuỗi/30 thành viên, 119999/120000 ms, khởi động giữa giờ, đóng 17:00 và thu hồi 17:05; rollback khi thiếu khu hoặc constructor lỗi, retry khi dọn lỗi.
- Hai lỗi rà soát được tái hiện trước sửa rồi kiểm tra qua sau sửa: thông báo boss bổ sung và cuộc đua vào map/thu hồi. Vào map của bản hỗ trợ khóa cùng monitor với thu hồi, kiểm tra lại đã thu hồi ngay trong khóa; không khóa toàn update/rest để tránh đảo thứ tự khóa với service.
- Rà soát độc lập yêu cầu và chất lượng đã chấp thuận sau sửa.
- Kiểm tra chỉ đọc trong JVM thật:

```text
QUEST_SUPPORT_RUNTIME_AUDIT PASS
time_bangkok=2026-10-07T21:18:11.499701200+07:00[Asia/Bangkok]
inside_window=false
extra_chains=0
extra_members=0
manager_extra_bosses=0
```

Hiện ngoài giờ hỗ trợ, pool bằng 0 đúng lịch. Chưa thử gameplay trực tiếp trong Unity hoặc đo bộ nhớ/tick thực trên server trong đủ 3 giờ. Mô phỏng 3 giờ không thay thế kiểm tra tải/gameplay đó. Các map của chức năng đều có tile; các cảnh báo thiếu tile cũ ngoài phạm vi còn nguyên.

Log khởi động còn `NoSuchElementException: No line found` ở `activeCommandLine` khi chạy ẩn không có stdin, giống log bản trước `dragon-cleanup-20261007-203156.out.log`. Luồng game vẫn khởi động và lắng nghe cổng 14445; không phát hiện lỗi mới của hỗ trợ nhiệm vụ. Không sửa console trong tác vụ này.

## Backup và khôi phục

Đã bảo trì lưu dữ liệu, xác nhận server dừng trước khi đổi JAR và chụp SQL sau khi lưu. Không chạy migration hoặc sửa dữ liệu nhiệm vụ/shop cho chức năng này.

- Backup JAR: `C:\Users\Admin\Downloads\nroteam\Teamobi2026\SRC\jar-backup\20-before-quest-support-20261007-211710.jar`.
- SHA-256 cũ: `0b5021ec6996d7cf319661a034816fe17d74c7fd7d3dfbd31032c6a430a3e3dd`.
- SHA-256 mới: `f06c7bc977378926f01591265690b16b6a0b03c135ac54599d2080727ab7a8cc`.
- Snapshot SQL: `C:\Users\Admin\Downloads\nroteam\Teamobi2026\.analysis\quest-support-deploy-20261007-211710\team2026-before-quest-support.sql`.
- Metadata: `.analysis/quest-support-deployment-latest.json` và metadata có thời gian trong `SRC/jar-backup`.
- Chỉ `SRC/20.jar` là JAR ứng dụng trong thư mục chạy. Bản staging dư ở backup; thư viện và tool JAR còn cần dùng giữ nguyên.

Nếu cần khôi phục: bảo trì lưu dữ liệu, xác nhận JVM đã dừng, backup JAR hiện tại bằng tên mới rồi khôi phục JAR được chỉ định ở trên, đối chiếu SHA-256 trước khi khởi động. Không khôi phục SQL cũ đè lên tiến độ người chơi mới.
