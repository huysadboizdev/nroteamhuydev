# Super Broly loot và Mabu mập — kế hoạch dự thảo

**Trạng thái:** Người dùng đã duyệt và yêu cầu triển khai. Code, JAR server và client đã cập nhật; xem `2026-10-08-mabu-release-verification.md` để biết bằng chứng và giới hạn kiểm tra.

**Goal:** Thêm tỷ lệ trứng/bình Killis cho Super Broly và một Mabu mập săn công khai, có sát thương theo HP từng người, khiên hoạt động thật và thưởng đúng điều kiện.

**Architecture:** Boss Mabu mới có ID và vòng đời riêng; không sửa chuỗi Mabư hiện có. Dùng chung phần tạo trang bị với sáu boss săn, nhưng tách chính sách thưởng trứng/bình khỏi trang bị và khỏi cơ chế chiến đấu. Mỗi lượt chốt thưởng một lần, xác định chủ thưởng và điều kiện trước khi tạo vật phẩm trên map.

**Tech Stack:** Java 17, MySQL, Unity 2022.3.62f2; giữ giao thức item/skill và tài nguyên hiện có.

## 1. Yêu cầu đã chốt và lựa chọn đề xuất

| Nội dung | Cấu hình | Trạng thái |
|---|---|---|
| Super Broly: trứng | 5%, một trứng khi trúng roll | Người dùng yêu cầu |
| Super Broly: bình Killis | 3%, một bình khi đủ điều kiện và trúng roll | Người dùng yêu cầu |
| Mabu mập: HP | 300.000.000 | Người dùng yêu cầu |
| Mabu đánh người chơi | Mỗi đòn gây sát thương: 10% HP tối đa của chính người bị đánh, xuyên giáp/khiên | Người dùng chốt |
| Người chơi đánh Mabu | Damage thông thường không bị ép 1 hoặc ép theo % HP boss | Người dùng yêu cầu |
| Mabu: chiêu | Bộ chiêu Xên hoàn thiện ở võ đài và Khiên năng lượng; không Tái tạo năng lượng | Người dùng yêu cầu |
| Mabu: trứng / bình | 30% / 15%; bình cần có trứng Mabu hoặc đệ Mabu | Người dùng yêu cầu |
| Vị trí Mabu | Map 52, Đại hội võ thuật công khai | Đề xuất sau khảo sát |
| Trang bị Mabu | Chắc chắn một món: 60% shop cấp 9–12 có ấn, 30% Thần Linh thường, 10% Thần Linh có ấn | Đề xuất dùng tỷ lệ sáu boss đã chốt trước |
| Vòng đời Mabu | Một boss toàn server; tối đa 20 phút, ra sớm sau 5 phút khu trống; nghỉ 10 phút | Đề xuất, chưa áp dụng |
| Khiên Mabu | Cấp 7: 45 giây; hồi chiêu 100 giây | Đề xuất theo code skill hiện tại |

Map 129 cũng mang tên Đại hội võ thuật nhưng thuộc võ đài cá nhân: `Player.injured` có xử lý thua trận riêng ở map này. Chọn map 52 tránh đưa boss farm vào luồng thi đấu đó. Chọn khu hợp lệ còn sức chứa, vị trí nền đất cách NPC, dựa vào chiều rộng và `yPhysicInTop`; không cố định tọa độ ngoài biên. Chỉ một khu chứa Mabu mới tại một thời điểm.

## 2. Kết quả kiểm tra hiện trạng

- `SRC/src/nro/models/boss/Broly/SuperBroly.java`: `reward` hiện chống phát lặp bằng `rewardGiven` và tặng đệ thường nếu người hạ chưa có đệ; chưa rơi hai món theo tỷ lệ mới. Giữ phần đệ thường này trong dự thảo vì người dùng chưa yêu cầu bỏ.
- Trứng là item **568 — Quả Trứng**, icon 2164. `UseItem` đã gọi `MabuEgg.createMabuEgg` rồi trừ một trứng; trứng đang ấp là trạng thái `player.mabuEgg`, không còn là item hành trang.
- Bình là item **1795 — Bình hút năng lượng**, icon 15359. Bình cần option **250 = 0** khi mới rơi để cộng Killis về sau.
- Luồng dùng bình hiện yêu cầu đệ Mabu (`typePet == 1`), sức mạnh ít nhất 40 tỷ và option 250 ít nhất 3.000. Đây là điều kiện **dùng bình**, không thêm thành điều kiện rơi bình.
- Mabư cũ có outfit **297/298/299** và thuộc nội dung khác. Boss mới dùng outfit đó nhưng có ID riêng, không thay HP/map/lịch của boss cũ.
- `SkillUtil.getTimeShield(level)` trả `(level + 2) * 5000`, nên khiên cấp 7 kéo dài 45 giây.
- Đường `Boss.injured` chung không tự giảm damage theo khiên như `Player.injured`: chỉ thêm skill khiên sẽ chưa đủ. Phải xử lý nhận damage trong lớp Mabu mới.
- Audit `.analysis/mabu-superbroly-plan-audit.json`: 35 icon liên quan, 140 PNG ở x1–x4; không thiếu file hoặc lỗi kiểm tra PNG; tile map 52/129 có sẵn. Đây là kiểm tra tài nguyên, chưa phải chứng nhận chơi thực tế không crash.

## 3. Tỷ lệ và điều kiện thưởng

Trứng và bình dùng hai lần roll độc lập trong miền 0–99:

```text
eligible = snapshotOwnershipBeforeRewards(creditedPlayer)
egg = roll(100) < (SuperBroly ? 5 : 30)
bottle = eligible && roll(100) < (SuperBroly ? 3 : 15)
```

Không cộng hai tỷ lệ thành một bảng chọn loại quà. Người đã đủ điều kiện có thể nhận cả hai cùng một lần hạ. Người chưa đủ điều kiện không có roll bình; tỷ lệ trứng và trang bị không đổi. Trứng vừa rơi ở chính lượt đó chưa được sở hữu khi chốt thưởng nên không mở điều kiện bình cho lượt đó.

Định nghĩa sở hữu đề xuất: có item 568 trong hành trang hoặc rương đồ cá nhân, có trứng đang ấp (`mabuEgg != null`), hoặc có đệ hiện tại `typePet == 1`. Đệ Uub/Kid Beerus/Jiren không tính là đệ Mabu. Không tính trứng của người cùng khu, vật phẩm trên mặt đất hoặc item của tài khoản khác. Kiểm tra rương để xác định điều kiện không lấy/trừ vật phẩm trong rương.

Áp dụng điều kiện bình cho cả Super Broly và Mabu mới. Không tiêu hao trứng hoặc đệ để được rơi bình. Không dùng `receivedKilis` để chặn thưởng vĩnh viễn sau một lần nhận.

Nếu đệ kết liễu thì chủ đệ là chủ thưởng, dùng `MiniBossPinkCapsuleDrop.creditedPlayer`. Xử lý người hạ null, chủ không hợp lệ và map đã đóng trước khi chốt. Vật phẩm rơi có owner riêng, không chuyển thành quà công khai khi hết thời gian. Điều kiện được chốt lúc chết, không thay đổi quyền nhặt vì người chơi cất trứng sau đó. Túi đầy vẫn có thể thấy đồ dưới đất và cần tự dọn túi trước khi đồ hết hạn.

Tạo đầy đủ item/options/tọa độ trước khi công bố `ItemMap`; dùng constructor có options ban đầu. Khóa chốt thưởng từng boss/lượt; packet gửi thất bại sau khi item đã vào zone không được tạo bản sao để bù. Không gọi thưởng khi hết thời hạn hoặc boss bị gỡ.

## 4. Trang bị Mabu

Đề xuất giữ bảng trang bị của sáu boss săn: cấp 9–12 có ấn dùng pool 230–281 đúng hành tinh; Thần Linh dùng pool 555–567 đúng hành tinh và chỉ số gốc của đồ Thần Linh thường. Không lấy chỉ số shop đã tăng riêng làm chỉ số gốc rơi.

Một món có ấn chỉ có một trong các ấn **34 Tinh / 35 Nguyệt / 36 Nhật**; chuẩn hóa bằng `SealedEquipment.normalize`, không cộng thêm set kích hoạt 127–135 ngoài ý muốn. Theo bảng 60/30/10, mỗi lần hạ Mabu hợp lệ luôn có một trang bị; trứng và bình được tính riêng. Không tự thêm vàng/ngọc/capsule cho boss này trong phạm vi yêu cầu hiện tại.

## 5. Chiến đấu và khiên

| Skill cấp 7 | Hồi chiêu |
|---|---:|
| Kamejoko | 10 giây |
| Dịch chuyển tức thời | 20 giây |
| Galick | 1 giây |
| Thái dương hạ san | 50 giây |
| Khiên năng lượng | 100 giây, đề xuất |

Không thêm Tái tạo năng lượng. Khiên là chiêu dùng lên bản thân; Thái dương hạ san là hiệu ứng, không tự trừ 10% HP. Chỉ đòn gây damage mới lấy `hpMax * 10L / 100`, tính bằng long để tránh tràn số. Ví dụ người A có HP tối đa 1 tỷ nhận 100 triệu, B có 200 triệu nhận 20 triệu; không dùng HP của mục tiêu trước hoặc HP đang còn. Không nhân đôi phần 10% bằng chí mạng/hệ số skill sau đó.

Đường nhận damage của người chơi phải bỏ qua giáp/khiên/Giáp Xên theo yêu cầu. Giữ kiểm tra mục tiêu cùng khu, còn sống và miễn nhiễm sau hồi sinh; giữ quy tắc né hiện hành vì người dùng không yêu cầu bỏ né. Không sửa biến damage dùng chung của boss theo mục tiêu vì có thể ảnh hưởng người bị đánh tiếp theo.

Mabu nhận damage đầy đủ khi không có khiên, không kế thừa hạn chế 1 damage của sáu boss mới. Khi khiên đang bật, đề xuất dùng cơ chế khiên hiện tại: đòn không xuyên khiên giảm còn 1; đòn vượt HP tối đa phá khiên theo luật hiện tại; đòn xuyên khiên đi qua; khiên hết hạn hoặc bị phá thì nhận damage thông thường ngay. **Giảm còn 1 chỉ là tác dụng tạm thời của khiên**, không phải giới hạn thường trực lên boss. Không cho AI bật lại liên tục để thành bất tử.

## 6. Xuất hiện, đóng lượt và thông báo

Tách Mabu mới khỏi `BossID.MABU`/`MABU_12H` và các map/cửa sổ giờ của Mabư cũ. Boss mới có một lượt gồm: chờ khu → vào map → thoại ngắn trong trạng thái không bị đánh → chiến đấu → chết hoặc rút → nghỉ → lượt mới.

Đề xuất đồng hồ 20 phút bắt đầu khi chuyển sang chiến đấu, không gia hạn khi đổi mục tiêu, bật khiên hoặc có người vào. Khu không có người liên tục 5 phút thì rút sớm. Chết/rút xong chờ 10 phút, chỉ spawn mới khi boss cũ đã tháo khỏi zone và manager; không tìm được khu thì thử lại sau 10 giây. Rút do hết giờ không tạo quà, không giả chết; đồ đã rơi từ lần hạ hợp lệ giữ thời hạn riêng.

Ví dụ bắt đầu chiến đấu 14:00: hạ lúc 14:07 → lượt mới sớm nhất 14:17; không hạ và khu vẫn có người → rút 14:20, lượt mới sớm nhất 14:30. Đây là cấu hình đề xuất cho Mabu mới. Kế hoạch thời hạn các boss khác trong `2026-10-08-boss-turn-deadlines-and-hide-zone.md` vẫn là kế hoạch riêng chưa triển khai.

Đăng ký Mabu trong bảng **Boss**, không phải Boss nhiệm vụ. Thông báo xuất hiện chỉ tên boss + map, không số khu. Bảng chi tiết hiện còn dòng khu theo kế hoạch trước; khi triển khai phải thực hiện phần ẩn khu trên `Assets/Scripts/BossNoticeData.cs` cho cả hai tab, đồng thời giữ tương thích client/gói tin cũ. Không tuyên bố dòng khu đã được bỏ ở bản hiện tại.

## 7. Trình tự triển khai sau khi duyệt thiết kế

- [ ] Viết kiểm thử chính sách thưởng trong `SRC/test/nro/models/services/MabuRareLootTest.java`: roll biên 4/5, 2/3, 29/30, 14/15; không đủ điều kiện; trứng túi/rương/đang ấp; loại đệ 1 so với 2/3/4; hai roll độc lập; chủ đệ; không phát hai lần. Chạy thấy thiếu chính sách hoặc sai hành vi trước khi sửa.
- [ ] Tạo `SRC/src/nro/models/services/MabuRareLoot.java`: resolve chủ, chốt eligibility trước thưởng, chuẩn bị item 568/1795 và option 250, kiểm tra map và owner. Sửa `SuperBroly.reward` dùng helper bên trong khóa một-lần hiện có, giữ đệ thường và lease Broly/Super; chạy lại `BrolyLifecycleTest`.
- [ ] Tách phần dựng trang bị trong `HuntingTrioLoot.java` thành `SRC/src/nro/models/services/SealedBossEquipmentLoot.java`; không đổi bảng thưởng phụ của sáu boss. Kiểm thử giới tính/pool/ấn/chỉ số gốc và chạy lại `HuntingTrioLootTest`.
- [ ] Tạo `SRC/src/nro/models/boss/Mabu/FatMabuTournament.java`, thêm ID trong `BossID.java`, cấu hình trong `BossesData.java`, đăng ký trong `Boss_Manager/BossManager.java` và `notice/BossNoticeCatalog.java`. Dùng 300 triệu HP, map 52, bốn chiêu Cell và khiên. Khóa spawn/cleanup theo lượt; tuân thủ hợp đồng gán zone trước lần đổi map đầu của boss.
- [ ] Viết `SRC/test/nro/models/boss/FatMabuTournamentTest.java`: HP 300 triệu; damage người chơi không bị cap ngoài khiên; bật/phá/hết khiên; hồi chiêu; không regen; không spawn trùng; không thưởng khi timeout; hồi sinh sau cooldown; thiếu khu không crash; Mabư cũ không đổi.
- [ ] Sửa nhánh gây damage tại `SRC/src/nro/models/services/BossNoticeService.java` để nhận chính sách 10% riêng của Mabu; giữ chính sách 5% của sáu boss. Kiểm thử hai mục tiêu HP khác nhau, xuyên giáp/khiên, né/miễn nhiễm, mục tiêu đã rời zone, attacker/skill null và giá trị HP lớn.
- [ ] Kiểm thử danh sách hiện tại/lịch sử xếp Mabu vào Boss; áp dụng phần bỏ số khu theo kế hoạch trước cho nguồn thông báo và client dùng chung. Không thay bố cục/độ dài BOSS1 ngoài hợp đồng đã có.
- [ ] Chạy audit SQL/resource: item 568/1795 và option 250, outfit 297–299, avatar, icons và x1–x4; đối chiếu thêm part SQL với binary runtime và giới hạn mảng client. Không cần tạo item hoặc sprite mới nếu toàn bộ đối chiếu đạt.
- [ ] Biên dịch toàn bộ Java với `javac --release 17 -encoding UTF-8`, đăng ký test mới trong bộ build `.analysis/build_hunting_release.py` và chạy `python .analysis/build_hunting_release.py --db` sau khi bảo đảm schema test riêng. Fixture notice phải dùng outbox tạm và sink bộ nhớ, không ghi lịch sử boss lên database đang chạy.
- [ ] Đóng gói staging; chạy kiểm thử lại với classes lấy từ JAR staging thay vì classes build. Build Unity cả hai tab; chơi kiểm tra Mabu/khiên/trứng/bình với hai tài khoản, một trường hợp đệ kết liễu và một trường hợp túi đầy. Kiểm tra console, log server và packet item/skill sau khi chết hoặc đổi map.
- [ ] Chỉ cập nhật JAR khi được yêu cầu triển khai: bảo trì lưu dữ liệu, xác nhận JVM/listener đã dừng, backup `SRC/20.jar` tên thời gian riêng vào `SRC/jar-backup`, đối chiếu SHA-256, ghi metadata, rồi cài JAR mới. Gom app JAR dư vào backup, giữ thư viện/tool JAR. Sau khởi động xác nhận một Mabu, Broly/Super vẫn loại trừ và thông báo không có khu; khôi phục JAR backup nếu có lỗi, không ghi đè database người chơi bằng SQL cũ.

## 8. Tiêu chí nhận kết quả

Không lẫn tỷ lệ giữa hai boss; bình chỉ rơi cho chủ đủ điều kiện từ trước; trứng/bình không bị ăn mất để kiểm tra điều kiện. Mabu có đúng HP và damage, khiên có tác dụng thật rồi hết tác dụng, không tự hồi năng lượng hoặc bị giới hạn damage thường trực. Thưởng trang bị đúng một món, đúng pool và ấn. Không trùng boss, không phát thưởng khi rút, không trùng vật phẩm do gửi packet thất bại. Client hai tab hiển thị outfit/icon/skill đúng và không có managed exception trong các kịch bản kiểm tra. Báo cáo phải phân biệt audit tài nguyên, test tự động và trận đánh thực tế; không cam kết không có lỗi khi chưa chạy đủ các bước đó.
