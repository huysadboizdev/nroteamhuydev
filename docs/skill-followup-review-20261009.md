# Rà lại source server và client: vòng đời skill

**Cập nhật:** lỗi nội tại và hai lỗi đồng thời đã được sửa thêm; JAR cuối đã cài và server đã chạy lại. Trạng thái hiện tại xem `skill-final-release-20261009.md`. Các phần bản ứng viên/chưa triển khai bên dưới là ghi nhận của lượt kiểm tra trước.

## Phạm vi

Rà các thay đổi trong SkillService, EffectSkillService, PlayerSkill, EffectSkill, ChangeMapService và luồng gồng/hồi chiêu client Game1/Game2. Có nhiều thay đổi khác trong workspace; báo cáo này không khẳng định đã kiểm tra toàn bộ tính năng của game. Git hiện chỉ có hai commit của Huydev, không đủ chứng cứ quy lỗi cho một người hay một công cụ cụ thể. Client không có lịch sử Git để đối chiếu; dùng bản source đã sao lưu và project Unity cô lập.

## Lỗi đã sửa thêm ở server

1. **Bom đến sớm bị hủy và bị tính hồi chiêu.** Giữ lượt gồng và bỏ qua lệnh phát sớm; không ghi cooldown. Giữ ngưỡng 1500 ms hiện có của server; client mới chờ đủ packet 2000 ms.
2. **Cầu kinh khí và Makankosappo phát trước thời gian gồng.** Với người chơi thường, chỉ hoàn tất sau 4000/3000 ms. Không áp thay đổi này lên cách boss/đệ phát hai skill đó.
3. **Biến khỉ đọc cấp skill đang chọn sau khi gồng.** Lưu cấp khỉ khi bắt đầu; hoàn tất lấy cấp đã lưu. Chặn bắt đầu lặp và hoàn tất lặp để không nhân HP thêm lần nữa. Giữ cách biến khỉ trực tiếp của boss/bot.
4. **Bom boss/đệ tạo executor cho mỗi lệnh và có thể xếp nhiều lần nổ.** Dùng scheduler daemon chung và một cờ sở hữu lượt bom đang chờ. Callback chỉ nhận trạng thái/skill/map/lượt gồng gốc, còn sống và chưa bị hủy. Callback cũ không xóa một lượt gồng mới; callback hoàn tất một lần.
5. **Đổi skill/rời map giữ cờ gồng cũ.** Xóa các cờ bom/cầu/laze khi đổi skill thực sự hoặc exitMap, giúp callback cũ bị loại bỏ kể cả rời rồi quay về cùng zone.
6. **Sát thương bom bị giảm dồn theo thứ tự mục tiêu.** Tính sát thương riêng cho từng mục tiêu, không ghi ngược vào sát thương cơ sở của cả lượt nổ.

Production sửa trong năm file server. Không rollback các thay đổi có sẵn trong workspace.

## Kiểm chứng

- Tái hiện RED bằng lời gọi service thật: bom sớm, cấp khỉ sau đổi skill, hoàn tất khỉ lặp; sau đó tái hiện RED cho cầu/laze phát sớm. Log ở `.analysis/skill-followup-20261009/red-*.log`.
- Biên dịch **toàn bộ SRC/src** bằng JDK 21: exit 0. Có ghi chú deprecated/unchecked và annotation processing, không có lỗi compile.
- **18 tình huống SkillCastLifecycleTest** PASS: bom sớm; cầu/laze sớm và đúng hạn; cấp khỉ; bắt đầu/hoàn tất khỉ lặp; bom trễ hợp lệ/lặp/map/chết/đổi skill/hủy/dispose/callback cũ; đổi skill; exitMap; thứ tự sát thương.
- Chạy trực tiếp trên **JAR ứng viên**, classpath chỉ có test fixture, JAR đó và lib. Không đưa thư mục chứa production class khác lên trước JAR.
- Regression có sẵn PASS: HighHpSkillTest (HP khỉ tới 2 tỷ), SuperBrolyDamagePolicyTest, SKHEffectTest `explosion` và `existing`.
- Client: SHA-256 của bảy file production khớp project Unity cô lập; chạy lại `HighSpeedSkillRegression.Run`: Game1/Game2 PASS, **169582 assertions**, tốc độ x1/x2/x5/x10/x20/x50/x100. Log `unity-regression.log`.
- Diff check server với cấu hình nhận CRLF `core.whitespace=cr-at-eol`: PASS.

## Bản dựng và trạng thái chạy

JAR ứng viên: `SRC/jar-backup/candidate-skill-followup-20261009-164645.jar`.

SHA-256: `5D0EEA7D8B540DE33D32B7AEEF5F5D4AE3D00886CFE6A3E2CE4DB662B3923670`.

JAR này được tạo từ bản ứng dụng hiện tại để giữ manifest/resource, cập nhật toàn bộ class biên dịch từ source mới. Test fixture không đóng vào JAR. Chưa thay `SRC/20.jar`, chưa dừng server; hash JAR đang dùng vẫn khớp trước kiểm tra. Source sửa mới cần được đưa vào JAR đang chạy để có hiệu lực ở server. Khi cập nhật phải bảo trì lưu dữ liệu, xác nhận server dừng, backup JAR cũ riêng có timestamp và ghi hash/metadata theo AGENTS.md.

Client giữ bản đã dựng: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Builds/PC-SkillSpeed-20261009-163432.zip`; không cần dựng lại vì bảy file production không thay đổi trong lượt rà này.

## Giới hạn

Chưa chơi thử trên server thật hoặc đo FPS bằng profiler. Các ca kiểm thử chứng minh thời gian gồng, packet/hồi chiêu và việc hủy lượt nổ trong fixture, không chứng minh mọi máy đều không tụt FPS. Protocol cũ không có cast token nên packet cùng skill từ lượt cũ vẫn có giới hạn phân biệt. Những vấn đề khác được ghi ở `docs/client-skill-server-review-20261009.md`, như bộ hẹn nội tại dùng một slot, chưa được thay cơ chế trong phạm vi bản sửa này.
