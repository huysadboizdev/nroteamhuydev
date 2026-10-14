# Ôsin: bình hút năng lượng và Android 6+

## Nguyên nhân và bản sửa server

- Luồng NPC Ôsin tại map52 → Bình hút năng lượng → OK dẫn đến map165.
- Database thực tế và seed gán map165 `type=9` (`MAP_MABU_14H`). `Map.initZone` đăng ký các khu vào `MajinBuu14HService`; `MajinBuu14H.finish` đưa người chơi về nhà ngoài 14:00–15:00 (UTC+7). Đây là nguyên nhân trực tiếp của việc vào map bình hút rồi bị đưa về nhà.
- Chuẩn hóa map165 thành `MAP_NORMAL` khi tải; chỉ cho phép map127/128 đăng ký vào bộ điều khiển sự kiện 14h. Migration sửa đúng hàng165 từ type9 sang0, seed đồng bộ.
- Cộng Killis cho chủ khi đệ tử đánh quái, yêu cầu chủ và đệ cùng khu; chỉ ghi điểm cho lượng HP thực sự mất. Không ghi điểm khi đánh quái đã chết hoặc đánh không gây sát thương; giới hạn đúng map165. Khởi tạo option250 nếu thiếu và tránh tràn số.
- Bản sửa đầu tiên giữ tỷ lệ cũ1/333 mỗi đòn hợp lệ,10/333 khi có bùa. Sau đó người dùng chốt tăng tỷ lệ lên30% thường/60% có bùa; mức mới đã triển khai như ghi ở mục bên dưới.
- Hết hạn bùa dùng `timeLengthKilis` đã mua/cộng dồn, có fallback cho dữ liệu cũ.

## Kiểm chứng

- Tái hiện thất bại trên JAR cũ: map165 nhận loại sự kiện9; bùa mua10 phút vẫn chưa hết sau11 phút.
- JAR ứng viên chỉ thay các class thuộc sáu nhóm: Map, MapService, MajinBuu14HService, Mob, ItemTime, KilisAbsorptionService. Các class khác giữ nguyên byte của JAR đang dùng.
- Năm nhóm kiểm thử đã qua cả source build và JAR ứng viên: map/khu độc lập; thời hạn bùa; cộng điểm cho người chơi/đệ và các trường hợp không được cộng; DungeonTimingContractTest; DungeonTimeoutRegressionTest.
- Metadata, SHA-256, log: `.analysis/oshin-build-latest.json`, `.analysis/oshin-build/`.
- Đã triển khai sau khi người dùng xác nhận cho phép: sao lưu database, chuyển JAR cũ vào `SRC/jar-backup/20-before-oshin-20261013-193200.jar`, đối chiếu SHA-256, áp dụng migration và cài `SRC/20.jar`. Metadata: `.analysis/oshin-deployment-latest.json`. Server ban đầu đã dừng; sau triển khai đã khởi động JVM PID18924, cổng14445.
- Kiểm tra JAR đã cài: cả năm nhóm kiểm thử PASS; SHA-256 JAR/backup/database snapshot khớp metadata. Kiểm tra JVM thật: map165 loại0,10 khu, mỗi khu5 quái95, NPC44 tồn tại, map165 không nằm trong các vùng sự kiện14h; item1795 và option250 tồn tại. Log: `.analysis/oshin-runtime/runtime.txt`.

## Android

- Bản dựng Unity mới nhất trước sửa (`20261011-MonoARMv7`) chỉ có ABI armeabi-v7a. Máy chỉ chạy64-bit không cài được bản này.
- Chuyển bản Android sang IL2CPP, ARMv7+ARM64, minimum API23 (Android6), target API34; giữ hai scene và các kiểm thử build hiện có.
- Bản phát hành mới: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Builds/Distribution/20261013-Android6Universal/Android/NRO.apk`.
- APK dựng thành công, không có lỗi build. Đã kiểm tra bằng aapt, apksigner, zipalign và phân tích ELF: minAPI23,targetAPI34,ARMv7+ARM64; chữ ký hợp lệ; mọi LOAD segment của ba thư viện ARM64 căn chỉnh16384; thư viện native trong APK được nén nên không yêu cầu offset ZIP16KB. Kết quả: `Android/verification.json`. SHA-256 APK: `0e1e13509f889334e47a10de504b08f8b8e6185a1dc90d582b95f69178a793a5`.
- Không có thiết bị Android kết nối để thử cài/chạy thực tế. Không thể bảo đảm mọi model điện thoại chỉ từ kiểm tra tĩnh.
- APK web `C:/xampp/htdocs/webgoc2026/down/NRO.apk` thuộc package khác (`online.srcnrofree.n8`, minAPI23, targetAPI36, ARMv7+ARM64). Cấu hình `settings.Android` hiện NULL. Chưa thay APK hay link tải web khi chưa xác định link người dùng tải.

## Nguồn Android

- Android Developers: https://developer.android.com/games/optimize/64-bit
- Android Developers: https://developer.android.com/guide/practices/page-sizes
- Unity hỗ trợ16KB từ2022.3.56f1: https://unity.com/releases/editor/whats-new/2022.3.56f1

## Cập nhật tỷ lệ Killis theo yêu cầu người dùng

- Đã triển khai ngày13/10/2026:30% thường,60% khi bùa còn hiệu lực, mỗi lần trúng nhận+1 Killis. Áp dụng cho mỗi đòn thực sự gây sát thương ở map165, gồm đòn của đệ tử cộng vào bình chủ.
- Chỉ thay `nro/models/services/KilisAbsorptionService.class` trong JAR; đối chiếu mọi entry khác giữ nguyên byte.
- TDD: kiểm thử trên bản cũ thất bại vì dùng333 kết quả quay. Bản mới duyệt đủ100 giá trị xác nhận chính xác30 lần thường,60 lần có bùa,30 lần bùa hết hạn và30 lần đệ đánh không bùa.
- Sáu nhóm kiểm thử PASS trên source build, JAR ứng viên và JAR đã cài. Runtime thật xác nhận tỷ lệ30/60, map165 không đăng ký vào sự kiện Mabư.
- Bảo trì có lưu dữ liệu, xác nhận server dừng trước khi thay; backup JAR/database và đối chiếu SHA-256. Metadata `.analysis/kilis-rate-deployment-latest.json`; runtime `.analysis/kilis-rate-runtime/runtime.txt`.
- Server khởi động lại thành công, PID20152, cổng14445. Mô tả bùa hút nhanh gấp đôi hiện khớp30%/60%.
