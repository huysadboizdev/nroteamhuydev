# Hủy Diệt/Thiên Sứ: yêu cầu sức mạnh và khóa giao dịch — 06/10/2026

Áp dụng cho 13ID Hủy Diệt650–662 và15ID Thiên Sứ1048–1062, cả đồ mới và đồ đã lưu. Option30=0 hiển thị Không thể giao dịch; server trade/ký gửi chặn trực tiếp bằngID, gồm tin ký gửi cũ và đồ có optionấn/87.

| Bộ | Áo | Quần | Găng | Giày | Nhẫn/rada |
|---|---:|---:|---:|---:|---:|
| Hủy Diệt |40tỷ|44tỷ|50tỷ|43tỷ|47tỷ|
| Thiên Sứ |80tỷ|80tỷ|80tỷ|80tỷ|80tỷ|

Option21 dùng đơn vị tỷ, được chuẩn hóa tại factory/grant/login cùngoption30; không đưa số >INT_MAX vào SQL/templatepower_require. Server kiểm tra mặc đồ theo cùng mapping, không tin optionthiếu/cũ/giả. Giữ chỉ số nền, bonus, thời hạn và đồ đang mặc; không tự tháo đồ cũ. ĐiểmsetDHD đếm bằngoption21==80 là counter hiện không có consumer, không thay đổi cơ chế set ngoài yêu cầu.

Hai file Panel.cs Game1/Game2 đã sửa lấyoption21 bằnglong, fallbacktemplate, hiển thị một dòng yêu cầu sức mạnh màu đen ởtooltip và header; bỏ dòngoption21 lặp cho trangbị0–4. FullUnityRoslyn compile PASS sangartifactisolated `.analysis/equipment-client/Assembly-CSharp.dll`; chưa build bản phát hành client hoặc quan sát UnityUItrực tiếp. Người dùng cần Unitybiêndịch lại source và chạy lạiPlay để thấy thay đổi.

Migration `SRC/sql/migrations/20261006_high_tier_equipment_power_lock.sql` đồng bộ toàn bộ16shoprow chứa cácIDnày, giữ giá/options nền, và4dump/seed. ĐồThiênSứ hiện chưa cóshoprow; áp dụngbằngruntimepolicy khi chế tạo/grant/load.

61 lượt kiểm thử PASS trên classes/candidate/JARđãcài: Beerus, cácshopruby, kýgửi, points; thêm28IDlock/power, mặcđúng5slot vàđườngrương, dướingưỡng/đúngngưỡng. JAR thay5class, cácclassboss/mapgiữbytecũ.

Server PID13408, cổng14445. JAR SHA-256 `60034aec9f591565e93400ed3e463e465da00540c38aa9db3259cd6d6d46d085`. Backup `SRC/jar-backup/20-before-equipment-20261006-092045.jar`.

Bảo trìcólưudữliệu, snapshotSQL, backuphashvàmetadata `.analysis/equipment-deployment-latest.json`. Rollback bằngbảotrì/JARbackup/rollback-options.sql; khôngrestoretoànbộplayersnapshot.
