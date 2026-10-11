# Goku Day gifts — sửa và triển khai 10/10/2026

Đã sửa hai hộp1591/1594 và cài vào server bằng bảo trì có lưu dữ liệu.

- CT Goku SSJ God1593: giữ head1467; đổi body/leg từ1471/1472 (tuần lộc) sang bộ Goku1450/1451 đầy đủ animation. Không thay hoặc xóa artwork tuần lộc; các item God đã có dùng template sửa khi server gửi lại ngoại hình.
- Nhánh0,5% không thêm option93, nên phần thưởng mới là vĩnh viễn. 99,5% còn lại nhận option93=3 ngày. Giữ pool sáu cải trang và chỉ số cũ.
- Thông báo ghi đúng yêu cầu2 ô trống. Chỉ sửa handler chung của hai hộp; các hộp khác không đổi.
- Migration được thêm cuối bốn seed/dump, sau các migration cũ; INSERT lịch sử vẫn chứa mapping cũ nhưng migration cuối sửa thành1467/1450/1451 khi import.

Kiểm chứng: test trên JAR cũ thất bại ở phần thưởng vĩnh viễn có option93. Build670 source thành công. Mười nhóm test liên quan đạt trên classes và candidate, gồm thực thi handler trên đủ1000 roll (đúng5 kết quả vĩnh viễn), biên0/4/5/999 cho cả hai hộp, hạn sử dụng, bảo toàn chỉ số, tiêu hao đúng hộp, từ chối nhận thưởng, hành trang và giftcode. SQL chạy lặp trên database riêng, giữ nguyên item khác và từ chối part sai loại.

JAR chỉ khác đúng `nro/models/services_func/UseItem.class`. Đã kiểm tra hash JAR hiện tại và backup, snapshot SQL, cả bốn seed và bản backup; binary part và tất cả PNG trong audit trước sửa giữ nguyên. Resource audit sau sửa xác nhận SQL/binary/PNG hợp lệ cho bộ God mới. Không chỉnh dữ liệu túi hoặc hoàn lại các item hạn0 ngày đã nhận trước đây.

Server PID19432, listener14445; đọc trực tiếp JVM xác nhận item1593=1467/1450/1451. NPC làng0/7/14 khớp toàn bộ tọa độ trước cập nhật, gồm Daishinkan508/454/341. Visual Unity chưa thao tác kiểm tra; đăng nhập lại để nhận ngoại hình mới.

Một test ngoài phạm vi, `TimedBagCleanupTest menu`, đang kỳ vọng sáu nút Chi Chi và thất bại giống nhau trên JAR gốc. Không sửa menu/test này; các chế độ scope/stale/guards/failure/calendar/online/concurrent đều đạt trên candidate. Log nằm trong `.analysis/goku-day-fix`.

Metadata và rollback chính xác: `.analysis/goku-day-deployment-latest.json`. Backup JAR cũ trong `SRC/jar-backup/20-before-goku-day-fix-20261010-194900.jar`; snapshot SQL và backup seed trong `.analysis/goku-day-deploy-20261010-194900`. Đối chiếu SHA256 trước khi khôi phục. Rollback bằng bảo trì lưu dữ liệu, backup JAR đang dùng, khôi phục JAR cũ và chỉ body/leg item1593 cùng các seed liên quan; không restore toàn bộ SQL đè dữ liệu người chơi mới.
