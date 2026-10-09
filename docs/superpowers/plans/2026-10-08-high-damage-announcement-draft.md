# Thông báo sát thương trên 20 triệu — kế hoạch đề xuất

**Trạng thái:** Đã triển khai, server khởi động lại thành công. Chi tiết kiểm chứng/backup tại `docs/high-damage-announcement-deployment-20261008.md`.

**Goal:** Khi người chơi gây sát thương thực tế >20.000.000 lên boss, người hoặc quái, phát cùng một nội dung trên kênh thế giới và dòng thông báo phía dưới cho người chơi online.

**Architecture:** Ghi nhận kết quả đòn đánh tại nơi sát thương đã áp dụng, kèm skill đang thực thi; chuyển bản ghi bất biến tới service thông báo riêng. Dùng các packet hiện có, không đổi chỉ số hoặc quy tắc chiến đấu.

**Tech Stack:** Java server, packet92 và -25 hiện có; dự kiến không migration SQL, không sửa client.

## Bằng chứng source

- `SRC/src/nro/models/services/ChatGlobalService.java`: sendSystemNotice/systemNoticeMessage gửi packet92 miễn phí, không phụ thuộc chat người chơi; giới hạn nội dung100 ký tự.
- `SRC/src/nro/models/services/Service.java:1264`: sendThongBaoAllPlayer gửi packet-25 cho toàn bộ người chơi.
- Client `PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1/Controller.cs:2653`: packet-25 gọi GameScr.info1.addInfo. Game2 có handler tương ứng. Vị trí hình ảnh thực tế vẫn cần xem trên client thật khi nghiệm thu.
- `SRC/src/nro/models/services/SkillService.java`: playerAttackPlayer đã có executingSkillId, gọi BossNoticeService.injure; đánh quái qua playerAttackMob và các nhánh AOE/Tự sát trực tiếp gọi Mob.injured.
- `SRC/src/nro/models/services/BossNoticeService.java`: wrapper có skill context và đảm bảo lifecycle boss. Không thay/loại bỏ cơ chế lịch sử boss hiện có.
- `SRC/src/nro/models/mob/Mob.java:131`: injured trả void; damage tiếp tục bị giảm/giới hạn trước trừ HP và còn bị gán lại sau đó. Cần ghi nhận ngay điểm áp dụng HP, không suy đoán từ dame gửi vào.
- `SRC/src/nro/models/player/Player.java:1077`: injured có giảm/chặn dame trước subHP và trả int. Boss có thể override; phải đối chiếu các override và skill đặc biệt trước chọn điểm hook cuối cùng.

## Hành vi đề xuất

- Tin: `Người chơi <tên> dùng chiêu <tên chiêu> với mức sát thương là <số>.`
- Ví dụ: `Người chơi abc dùng chiêu Kamejoko với mức sát thương là 25.000.000.`
- Lấy tên chiêu từ catalog bằng skillId của đòn đang thực thi. Chụp ID/tên trước đòn đánh; không đọc skillSelect lúc phát tin vì người chơi có thể đổi chiêu hoặc skill có hiệu ứng trễ.
- Tính từng hit lên từng mục tiêu, không cộng nhiều quái thành20 triệu. Đòn được tính sau né/giáp/khiên/miễn nhiễm và giới hạn HP thực sự mất; đòn giết không lấy overkill. Không dùng HP trước/sau ngoài vùng xử lý hit vì đòn khác/hồi HP có thể xen vào.
- Chỉ người chơi thật trực tiếp gây dame; loại boss, bot, đệ tử, phản sát thương, độc và hiệu ứng không có skill trực tiếp. Không gán dame đệ tử cho chủ. Bao gồm skill trực tiếp thông thường, skill đặc biệt và Tự sát nếu sát thương hợp lệ.
- Cùng một sự kiện gửi đúng một tin ở mỗi kênh; không trừ ngọc, không thay cooldown chat thế giới của người chơi.
- Tin kênh thế giới dưới tên Hệ thống; tên người gây dame nằm trong nội dung. Làm sạch ký tự điều khiển/mã màu từ tên để không giả mạo dòng tin. Giữ nguyên giá trị dame; rút gọn phần tên khi cần để tin cả hai kênh giống nhau và <=100 ký tự.
- Thông báo lỗi không được ảnh hưởng kết quả chiến đấu. Không ghi SQL cho mỗi đòn; không tạo hàng đợi hoặc thread không giới hạn. Nếu dùng worker, snapshot dữ liệu và queue có giới hạn, giải phóng đúng lúc.

## Lựa chọn chống spam cần chốt

1. **Đề xuất:** mỗi người tối đa1 thông báo/5 giây, toàn server tối đa1 thông báo/giây; trong cửa sổ cooldown bỏ qua hit mới, không xếp hàng tin cũ. Một skill AOE chỉ có tối đa một tin được chấp nhận. Tin đã chấp nhận phát cả hai kênh.
2. **Mọi hit >20 triệu:** đúng điều kiện mỗi hit nhưng AOE/liên hoàn có thể liên tục đẩy tin ra toàn server.
3. **Chỉ kỷ lục mới:** ít tin nhưng thay đổi yêu cầu thành báo kỷ lục, không đề xuất cho phạm vi này.

Người dùng đã yêu cầu triển khai; áp dụng mặc định phương án1 đã đề xuất. Khi chọn phương án1, test phải kiểm tra hit bị bỏ qua không gây tích backlog và hit dưới ngưỡng không chiếm cooldown.

## File và trình tự triển khai sau chấp thuận

- [x] Tạo `SRC/src/nro/models/services/HighDamageNoticeService.java`: ngưỡng, kiểm tra attacker/skill, format, chống trùng/spam và gửi hai kênh. Tách policy/clock/sink để test không gửi người chơi thật.
- [x] Tạo `SRC/test/nro/models/services/HighDamageNoticeTest.java`: RED trước implementation cho19.999.999/20.000.000/20.000.001; exact format; tên dài/ký tự điều khiển; clock/cooldown/parallel hit; gửi một lần mỗi kênh; không tính bot/pet/null/miss.
- [x] Truy vết tất cả injured override của boss, skill đặc biệt và đánh quái. Thiết lập context đòn đánh theo attacker/skillId, khôi phục context sau nested call; tránh skill ID bị ghi nhầm khi phản dame/đổi chiêu.
- [x] Nối hook trong SkillService/BossNoticeService và Mob.injured tại điểm có kết quả dame cuối. Chỉ sửa Player.injured nếu cần để đủ coverage đã xác minh; tránh double-report giữa wrapper và target. Giữ lifecycle/death/drop boss/quái hiện có.
- [x] Tạo `SRC/test/nro/models/services/HighDamageNoticeCombatTest.java`: hit boss/PvP/mob, giáp/khiên/miss/immune, HP mục tiêu thấp hơn dame đầu vào, AOE/nested reflection, skill thay đổi và skill trễ. Kiểm tra HP/death/drop/PK kết quả không đổi khi bật thông báo hoặc khi sender lỗi.
- [x] Test packet92/-25, giới hạn100 ký tự và nội dung giống nhau. Regression BossNotice/HighHpSkill/combat phù hợp; build toàn source và test candidate JAR.
- [x] Khi được giao triển khai: bảo trì có lưu dữ liệu, chuyển JAR cũ vào SRC/jar-backup tên timestamp, đối chiếu SHA-256 và metadata; cài candidate khi server đã dừng, khởi động lại và kiểm tra startup/listener.
- [ ] Nghiệm thu client thử (chưa thao tác game thật): hit >20 triệu hiện cả kênh thế giới và info phía dưới, đúng skill/dame, đúng chính sách cooldown; không thử giao chiến trên tài khoản người chơi thật.

## Cần người dùng chốt trước code

Đã áp dụng phương án1: mỗi người5 giây/toàn server1 giây. Phạm vi mặc định là cả hai kênh cho toàn server, chỉ dame trực tiếp của người chơi và dame thực tế sau giảm trừ.
