# NPC nhà: nhận ngọc xanh và đệ tử thường — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Ba NPC nhà cho nhận 10 triệu ngọc xanh khi số ngọc xanh bằng 0, và nhận một đệ tử thường ngẫu nhiên khi chưa có đệ tử.

**Architecture:** Dùng chung menu và xử lý tại OngGohan; OngMoori và OngParagus kế thừa, chỉ khác lời thoại. Kiểm tra điều kiện và cấp thưởng trong cùng khóa `synchronized (player)`. Tái sử dụng PetService và cơ chế lưu PlayerDAO, không đổi cấu trúc SQL hay tài nguyên client.

**Tech Stack:** Java 17, server hiện tại, kiểm thử Java trong bộ kiểm tra `.analysis`, PowerShell/Python để dựng và triển khai JAR.

## Yêu cầu đã chốt

| Nhà | Map | NPC |
| --- | --- | --- |
| Trái Đất | 21 | Ông Gohan, template 0 |
| Namek | 22 | Ông Moori, template 2 |
| Xayda | 23 (`NHA_BROLY`) | Ông Paragus, template 1 |

- Nút `Nhận 10 triệu\nngọc xanh`: chỉ nhận khi `inventory.gem == 0`, đặt thành `10_000_000`. Dùng hết về 0 được nhận lại, không hạn ngày/lần. Còn 1 ngọc cũng từ chối. Số âm không được xem là đã dùng hết.
- Ngọc hồng (`ruby`) không ảnh hưởng điều kiện; không thay vàng/ngọc hồng. Ngọc xanh có sẵn của tài khoản mới phải dùng hết trước.
- Nút `Nhận đệ tử\nthường`: chỉ khi `player.pet == null`. Người dùng đã xác nhận không đổi đệ đang có.
- Ba hệ đệ tử 0/1/2 có xác suất bằng nhau, độc lập hệ sư phụ và NPC đang nói chuyện.
- Dùng nguyên factory thường: `typePet = 0`, sức mạnh 2.000, bậc giới hạn 0, tiềm năng 0; chỉ số khởi tạo hiện hành. Khi luyện chỉ tăng HP/KI/sức đánh, không tăng giáp/chí mạng. Giáp khởi tạo hiện có vẫn giữ.
- Miễn phí cả hai thao tác; thêm nút `Đóng`.

## Kết quả kiểm tra nguồn

- `SRC/src/nro/models/npc_list/OngGohan.java`, `OngMoori.java`, `OngParagus.java`: hiện chỉ hiện tutorial khi không hoàn thành nhiệm vụ nói chuyện; confirm menu chưa có hành động cấp thưởng. Hai lớp sau kế thừa OngGohan nhưng đang ghi đè cả open/confirm.
- `SRC/src/nro/models/services/PetService.java`: `createNormalPet(player)` đã random giới tính bằng `Util.nextInt(0, 2)`, bao gồm cả hai đầu. Factory có thể thay đệ đang có, nên bắt buộc kiểm tra null dưới khóa bên ngoài rồi mới gọi factory; khóa Java có thể vào lại.
- `SRC/src/nro/models/player/Inventory.java`: `gem` và `ruby` là hai trường riêng, kiểu int; 10 triệu không vượt kiểu dữ liệu.
- `SRC/src/nro/models/services/Service.java`: `sendMoney(player)` gửi số tiền/ngọc hiện tại cho client.
- `SRC/src/nro/models/database/PlayerDAO.java`: `updatePlayer(player)` khóa trên player và lưu inventory cùng dữ liệu đệ tử. Không cần thêm cột SQL. Hàm hiện bắt lỗi nội bộ, không cung cấp kết quả commit cho bên gọi; cần kiểm tra log và đăng nhập lại khi thử tích hợp.
- `Npc.canOpenNpc` kiểm tra map NPC; menu mới vẫn dùng kiểm tra này. Không thay phạm vi tương tác toàn bộ NPC.

## Task 1: Menu dùng chung cho ba nhà

**Files:**
- Modify: `SRC/src/nro/models/consts/ConstNpc.java`
- Modify: `SRC/src/nro/models/npc_list/OngGohan.java`
- Modify: `SRC/src/nro/models/npc_list/OngMoori.java`
- Modify: `SRC/src/nro/models/npc_list/OngParagus.java`
- Test: `SRC/test/nro/models/npc_list/HomeNpcSupportTest.java`

- [x] Tạo kiểm thử ba lớp NPC: đúng map được mở, sai map bị từ chối; menu có đúng ba nút, giữ xử lý nhiệm vụ nói chuyện.
- [x] Thêm index riêng sau khi kiểm tra không trùng: `public static final int HOME_SUPPORT_MENU = 2026100201;`. Confirm chỉ xử lý khi index đúng, select 0/1 tương ứng hai thưởng; select 2 đóng, select ngoài phạm vi không cấp gì.
- [x] Gom open/confirm vào OngGohan. Cho hai lớp con ghi đè phương thức lời thoại `protected String getHomeGreeting()`, không ghi đè lại confirm. Giữ `TaskService.gI().checkDoneTaskTalkNpc(player, this)`; nếu hoàn thành nhiệm vụ thì giữ luồng nhiệm vụ, menu hỗ trợ có thể mở ở lần nói chuyện tiếp theo.
- [x] Giới hạn tính năng trong ba map 21/22/23; không vô tình đưa thưởng vào bản NPC cùng template ngoài nhà.

## Task 2: Cấp thưởng có kiểm tra lại điều kiện

**Files:**
- Modify: `SRC/src/nro/models/npc_list/OngGohan.java`
- Test: `SRC/test/nro/models/npc_list/HomeNpcSupportTest.java`

- [x] Thêm kiểm thử gem 0 nhận đúng 10 triệu; gem 1/10 triệu/âm bị từ chối, ruby/vàng giữ nguyên; giảm gem về 0 rồi nhận lại thành công.
- [x] Kiểm thử bấm lặp và hai request đồng thời: chỉ một lần cấp khi gem chưa tiêu; chỉ một đệ được tạo khi chưa có đệ. Kiểm thử đã có đệ thường/Mabu/Uub/Beer/Jiren: giữ đúng đối tượng, trang bị, bậc giới hạn.
- [x] Dùng hai đoạn xử lý dưới đây trong nhánh select tương ứng; không bỏ kiểm tra điều kiện vì menu trước đó từng cho nhận.

```java
// Select 0, sau khi kiểm tra NPC/map/menu.
synchronized (player) {
    if (player.inventory.gem != 0) {
        Service.gI().sendThongBao(player,
                "Hãy dùng hết ngọc xanh hiện có rồi nhận tiếp.");
        return;
    }
    player.inventory.gem = 10_000_000;
    PlayerDAO.updatePlayer(player);
    Service.gI().sendMoney(player);
    Service.gI().sendThongBao(player, "Bạn nhận được 10 triệu ngọc xanh.");
}

// Select 1, sau khi kiểm tra NPC/map/menu.
synchronized (player) {
    if (player.pet != null) {
        Service.gI().sendThongBao(player, "Bạn đã có đệ tử rồi.");
        return;
    }
    PetService.gI().createNormalPet(player);
    PlayerDAO.updatePlayer(player);
    Service.gI().sendThongBao(player, "Bạn nhận được một đệ tử thường.");
}
```

- [x] Kiểm tra callback lưu/gửi thông tin phù hợp session đã tải; không sửa điều kiện tạo pet chung hay các đường đổi pet khác.
- [x] Kiểm thử factory trả hệ trong 0/1/2, đúng type/sức mạnh/bậc. Dùng seed hoặc kiểm soát random trong fixture để kiểm tra đủ ba nhánh, không dùng assertion xác suất ngẫu nhiên dễ lỗi.

## Task 3: Kiểm tra và triển khai sau khi được yêu cầu thực hiện

**Files:**
- Modify: `.analysis/verify_gold_economy.py`
- Modify: `.analysis/build_release_jar.py`
- Create: `docs/source-analysis/home-npc-support.md`

- [x] Đăng ký các mode kiểm thử mới vào cả bộ source và candidate JAR; chạy `python .analysis/verify_gold_economy.py`. Mong đợi toàn bộ kiểm thử cũ và mới PASS, không dùng con số 69 cũ làm tổng cố định.
- [x] Chạy `python .analysis/build_release_jar.py`. Mong đợi compile và kiểm thử trực tiếp candidate JAR PASS; kiểm tra menu tiếng Việt/UTF-8.
- [ ] Thử client ở từng nhà: nhận ngọc, bấm lại khi còn ngọc, tiêu về 0 và nhận lại; nhận đệ khi trống và từ chối khi đã có. Đăng xuất/đăng nhập lại để xác nhận tiền và đệ được lưu; kiểm tra log SQL. Kiểm tra nhiệm vụ đầu không bị chặn. Nếu không có client/DB thử thì ghi rõ phần chưa xác minh.
- [x] Khi triển khai: kiểm tra server; bảo trì có lưu nếu đang chạy. Xác minh server đã dừng, chuyển `SRC/20.jar` cũ vào `SRC/jar-backup` tên thời gian riêng trước khi cài JAR mới; kiểm tra SHA-256 backup/candidate/installed và ghi metadata. Gom app JAR dư, giữ nguyên JAR thư viện/công cụ cần dùng. Khởi động lại, kiểm tra process/cổng/log.
- [x] Ghi kết quả thực tế và đường dẫn backup trong tài liệu nguồn. Workspace không có Git nên không thêm bước commit giả định.

## Trạng thái

Đã thực hiện và cập nhật JAR theo yêu cầu triển khai của người dùng; 74/74 source và 74/74 candidate JAR PASS, 10/10 kiểm thử bổ sung PASS, review độc lập không có lỗi chặn. Chưa có thử nghiệm client/đăng nhập lại DB thật. Chi tiết tại `docs/source-analysis/home-npc-support.md`.
