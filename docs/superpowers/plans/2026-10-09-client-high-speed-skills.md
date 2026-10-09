# Kế hoạch sửa skill client khi tăng tốc game

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Đây là kế hoạch đề xuất sau kiểm tra source; chưa triển khai sửa client.

**Goal:** Tự nổ, biến khỉ, quả cầu kênh khí và Makankosappo dùng được ở tốc độ x5 trở lên, kết thúc đúng trạng thái, hiện hồi chiêu và không gây khựng do gồng.

**Architecture:** Dùng thời gian thực và trạng thái từng lần thi triển để điều khiển skill; tách việc phát hiệu ứng khỏi tốc độ update. Cùng một bộ quy tắc áp dụng cho Game1 và Game2; server giữ quyền quyết định biến hình, sát thương và cooldown.

**Tech Stack:** Unity 2022.3.62f2, C#, client hai tab; Java server dùng giao thức TCP hiện có.

---

## 1. Phạm vi và dữ kiện

- Người dùng xác nhận: từ cheat x5, vẫn hiện hiệu ứng gồng nhưng bị lag, chiêu không hiện hồi. Kiểm tra đủ bốn skill, dù yêu cầu ban đầu gọi chung là ba skill.
- Client: `C:/Users/Admin/Downloads/PRJ_2Tab_550K`.
- Server: `C:/Users/Admin/Downloads/nroteam/Teamobi2026`.
- Client không có Git tại thư mục hiện tại; không thể quy trách nhiệm bản sửa cho một commit/tác giả. Backup source kèm SHA-256 trước khi triển khai.
- GitNexus không có tool callable trong phiên này và không thấy `.gitnexus` tại server; đã truy luồng trực tiếp bằng source/rg.
- Đã kiểm tra mã và tính thời gian bộ đếm; chưa tái hiện gameplay hoặc đo Unity Profiler. Không tuyên bố đã xác nhận toàn bộ nguyên nhân FPS/cooldown trên bản exe người dùng đang chạy.
- Các build có sẵn dùng snapshot riêng trong `.analysis`; phải xác định exe đang chạy và Assembly-CSharp.dll trước khi kiểm thử. Source hiện tại chưa chắc là bản được phát cho người chơi.

## 2. Lỗi đã thấy trong source

Đường dẫn dưới đây tính từ `Assets/Scripts/Assembly-CSharp/` của client, trừ khi ghi rõ server.

### 2.1. Game1 dùng số lượt update làm thời hạn gồng

- `Game1/ModFunc.cs:1850`: `ChangeGameSpeed` gán `Time.timeScale`, cho phép 1..100.
- `Game1/Main.cs:294`: toàn bộ cập nhật canvas chạy trong `FixedUpdate`.
- `ProjectSettings/TimeManager.asset`: fixed timestep 0.02 giây.
- `Game1/Char.cs:1639`: nhánh gồng đứng tăng `chargeCount` hai lần/update; tới 500 gọi `stopUseChargeSkill`, bỏ qua nhánh phát chiêu nếu thời gian chưa đủ.
- `Game1/Char.cs:1714`: gồng bay tăng bộ đếm và hủy tại 500.
- `Game1/Char.cs:1621`: chờ khỉ cũng dùng ngưỡng 500.
- Thời gian gồng lại được tính bằng `mSystem.currentTimeMillis()` (`DateTime.UtcNow`, `Game1/mSystem.cs:226`). Như vậy bộ đếm tăng theo cheat nhưng thời gian gồng vẫn là thời gian thật.

Mô hình danh nghĩa 50 update/giây, chưa tính CPU thiếu thời gian hoặc Unity giới hạn catch-up:

| Speed | Hủy gồng đứng | Hủy gồng bay/chờ khỉ |
|---|---:|---:|
| x1 | 5 s | 10 s |
| x2 | 2.5 s | 5 s |
| x5 | 1 s | 2 s |
| x10 | 0.5 s | 1 s |
| x20 | 0.25 s | 0.5 s |
| x100 | 0.05 s | 0.1 s |

Server `SRC/src/nro/models/services/SkillService.java` gửi QCKK 4000 ms (456), Makankosappo 3000 ms (495), tự nổ 2000 ms (742). Client hiện trừ 1000 ms ở gói chuẩn bị QCKK/Makankosappo (`Game1/Controller.cs:2044`). Ở x5, tự nổ bị hủy khoảng 1 s trước mốc 2 s; QCKK có thể bị hủy khoảng 2 s trước thời gian gồng client khoảng 3 s. Đây là đường lỗi mất chiêu cụ thể, độc lập với FPS.

### 2.2. Game2 có bản sửa thời gian chưa đầy đủ

- `Game2/Char.cs:1632,1704,1747`: giới hạn cứng 3000 ms cho khỉ, gồng đứng và gồng bay.
- `Game2/Char.cs:4480`: nhánh bắt đầu biến khỉ chỉ reset `chargeCount` và bật `isWaitMonkey`; không khởi tạo `chargeStartTime`. Mốc này chỉ được gán trong `useChargeSkill` (4602,4617). Một lần biến khỉ đầu tiên với mốc 0 có thể bị thoát ngay; các lần khác có thể dùng mốc của skill cũ.
- `Game2/Char.cs:1626`: phát hiệu ứng khỉ dùng `cur` nhưng nhánh này không cập nhật `cur` trước khi dùng. Có thể dùng thời gian cũ.
- `Game2/Char.cs:1722`: sau 20 lượt bay lại đặt `last`. Tại x5, 20 lượt là khoảng 80 ms; nếu gói chuẩn bị đã đặt mốc trước đó, việc reset này đẩy thời điểm hoàn thành QCKK ra sau 3 s. Giới hạn 3 s tính từ đầu có thể hủy trước khi hoàn thành. Với nhân vật khác, gói server chứa 4 s càng không phù hợp ngưỡng 3 s.
- Game1 và Game2 không tương đương; không sao chép nguyên bản sửa Game2 sang Game1.

### 2.3. Nguy cơ hiệu ứng, gửi chiêu và hồi chiêu cần đo

- Game1 phát hiệu ứng gồng bằng `gameTick % N`, nên tăng tốc tăng số lần tạo đối tượng mỗi giây. `ServerEffect.addServerEffect` tạo mới object và thêm vào `Effect2.vEffect2`. Tuổi một số hiệu ứng cũng giảm theo lượt update, vì vậy chưa đủ bằng chứng để nói danh sách chắc chắn tích tụ; cần đo allocation, số effect sống và GC.
- Game2 đã giảm tần suất một số hiệu ứng theo ms nhưng chưa xử lý thời gian/trạng thái đầy đủ.
- `Char.setSkillPaint` kiểm tra/trừ MP và ghi cooldown dựa trên `myskill` hiện tại (Game1:4374..4429), trong khi giai đoạn kết thúc gồng lại gọi hàm này. Cần kiểm thử đổi skill giữa lúc gồng và chống xử lý cooldown/MP hai lần.
- `Char.setAttack` gửi attack có cờ `hasSendAttack` (Game1:4782). Phải chứng minh gói release được gửi đúng một lần và đúng skill trước khi thay đường gửi.
- `Controller` gói -94 cập nhật hồi chiêu (Game1:543..555) nhưng chỉ chấp nhận `remaining < coolDown`; giá trị bằng cooldown đầy đủ hiện bị bỏ qua. Trace server trước khi sửa điều kiện.
- Server tự nổ từ chối release trước 1500 ms và xóa trạng thái chuẩn bị (SkillService:749..752). Không bỏ điều kiện server để che lỗi client.
- Server biến khỉ sau 1500 ms và gửi cải trang; client nhận -90 để mở khóa (EffectSkillService:270..305, Game1/Controller:594..615).

## 3. Các phương án

1. **Khuyến nghị: sửa thời gian + trạng thái skill + nhịp hiệu ứng.** Giữ tốc độ người chơi chọn, sửa đúng luồng hủy/phát chiêu và hồi chiêu. Kiểm thử hai tab với cùng kịch bản.
2. Tạm hạ speed trong lúc gồng: giảm triệu chứng nhưng `Time.timeScale` là trạng thái Unity toàn cục, ảnh hưởng cả hai tab và hành vi tốc độ. Không đáp ứng đầy đủ yêu cầu.
3. Chỉ giảm/tắt hiệu ứng: ít thay đổi nhưng không sửa các đường hủy chiêu đã tìm thấy. Không dùng làm giải pháp chính.

## 4. Thiết kế đề xuất

### Thời gian và trạng thái

- Mỗi nhân vật có trạng thái một lần thi triển: Idle, WaitingPrepare, Charging, Released, WaitingResult, Cancelled.
- Lưu skillId, templateId, targetId/type và đối tượng Skill tại lúc bắt đầu; không lấy `myskill` vừa được người chơi đổi để quyết định chiêu đang gồng.
- Thời gian elapsed dùng nguồn monotonic (`System.Diagnostics.Stopwatch`); giữ timestamp epoch hiện có cho các giao thức/UI khác. Không thay toàn bộ `mSystem` trong đợt sửa này.
- Gói chuẩn bị cập nhật deadline theo duration server, chỉ áp dụng cho lần thi triển đang phù hợp. Giữ nguyên giá trị gốc để trace; khoản trừ 1000 ms phải được kiểm thử x1/đối chiếu giao thức trước khi giữ hoặc bỏ.
- Hoàn thành theo deadline, không theo 500 update hoặc 3000 ms chung. Bay 20 lượt chỉ thay hoạt ảnh/vị trí, không đặt lại deadline đã nhận từ server.
- Tự nổ/QCKK/Makankosappo dùng duration riêng; khỉ chờ kết quả server. Watchdog chỉ giải phóng trạng thái kẹt, không phát chiêu thay server. Đề xuất chờ prepare tối đa 10 s; chờ result thêm tối đa 10 s sau deadline, điều chỉnh từ trace trước phát hành.
- Gói TCP không chứa cast token riêng: token nội bộ không phân biệt được mọi gói cũ cùng skill. Không tự động retry cast khi timeout; khóa lần thi triển mới khi cast còn pending, dọn pending khi disconnect/map change; kiểm tra trường hợp same-skill packet trễ trước phát hành.
- Cleanup xóa cờ gồng/khỉ, trạng thái gửi và khóa do cast này sở hữu; không mở khóa do choáng/ngủ/chết. Nhận cải trang khỉ vẫn áp dụng trạng thái server kể cả animation chờ đã timeout.

### Gửi chiêu và cooldown

- Chuẩn bị một lần, release tối đa một lần theo giao thức từng skill. Nhân vật khác chỉ hiển thị, không gửi attack.
- Trước release kiểm tra còn sống, map/zone đúng và target hợp lệ; mất target thì hủy sạch, không gửi attack rỗng.
- Tách khởi tạo animation khỏi thao tác trừ MP/ghi cooldown để kết thúc gồng không tiêu hao hai lần.
- UI dùng Skill đã chụp và thời gian thật; gói cooldown server hiệu chỉnh có xử lý 0, bằng cooldown, và giá trị bất hợp lệ. Không biến timeout thành giả lập damage hoặc hồi chiêu server.

### Nhịp hiệu ứng và hiệu năng

- Mỗi lần update lấy `now` một lần rồi dùng chung cho gồng và effect của nhân vật đó.
- Phát gồng theo thời gian thật; bắt đầu với 100 ms/lần và tối đa một batch/cast/update. Khi mất frame không phát bù hàng chục batch.
- Chỉ throttle effect trang trí của các skill này; giữ effect đánh trúng và kết quả server.
- Đo fixed updates/frame ở x20..x100. Nếu vẫn vượt ngân sách sau sửa skill, dùng bước riêng có đo trước/sau để xử lý vòng update; không tùy tiện đổi `fixedDeltaTime` toàn cục vì ảnh hưởng chuyển động, auto và mạng của hai tab.

## 5. Các bước thực hiện và kiểm chứng

### Task 1: Xác định bản chạy và tạo ca tái hiện

**Files:** tạo `.analysis/client-skill-speed-20261009/manifest.json`, `trace.csv`, `repro.md`; snapshot Game1/Game2 Char, Controller, GameScr, Main, ModFunc, Service, ServerEffect. Snapshot server SkillService chỉ để đối chiếu.

- [ ] Ghi đường dẫn exe, hash Assembly-CSharp.dll, hash source, Unity version và fixed timestep. Dùng `Get-FileHash -Algorithm SHA256 -LiteralPath <đường-dẫn-đã-xác-định>` cho từng artifact; không ghi đè backup.
- [ ] Ghi log development vào các điểm begin, receive prepare, timeout, release, receive result/cooldown, cleanup. Mỗi dòng gồm tab/char/skill/phase/time/duration/count/target/sendCount; không log mỗi update.
- [ ] Chạy riêng mỗi tab ở x1/x2/x5/x10; ghi FPS/frame time, effect allocation, GC và các gói chuẩn bị/release/cooldown. Kỳ vọng bản lỗi có đường timeout sớm hoặc mất release; nếu không, đối chiếu source với DLL rồi điều tra tiếp.
- [ ] Dùng cùng target và cùng máy cho so sánh; ghi kết quả từng skill thay vì gom tất cả thành một nhãn “lag”.

### Task 2: Viết regression và mô hình thời gian trước khi sửa

**Files:** tạo `Assets/Editor/HighSpeedSkillRegression.cs` và `Assets/Scripts/Assembly-CSharp/SkillCastTiming.cs` trong bản triển khai cô lập; test chạy cả hai namespace thực tế.

- [ ] Tạo test gọi adapter thực tế của Char/Controller bằng fake clock và packet sink; adapter không có socket thật. Tạo seam nội bộ cho clock/sink thay vì dùng sleep.
- [ ] Kịch bản tự nổ: prepare 2000 ms; update 250 lần/s ở x5; tới 1000 ms vẫn Charging và sendCount=0; tới 2000 ms release đúng một lần. Bản Game1 hiện tại phải thất bại do hủy ở 500 count.
- [ ] Kịch bản QCKK: prepare 4000 ms; qua chuyển bay 20 lượt không đổi deadline; trước deadline vẫn Charging. Test cả local và remote; remote sendCount luôn 0.
- [ ] Kịch bản khỉ Game2: lần đầu và sau skill khác; không thoát chờ ngay do mốc 0/cũ; nhận -90 sau 1500 ms cập nhật hình dạng và mở đúng khóa cast.
- [ ] Test Makankosappo, packet prepare đến trễ 500 ms, gói lặp, đổi skill giữa gồng, mất target, chết, disconnect và map change. Mỗi ca kiểm tra phase, gói gửi, MP, cooldown và khóa di chuyển.
- [ ] Chạy Unity batch bằng runner `HighSpeedSkillRegression.Run`; phải lưu được log ca lỗi trước sửa. Test chỉ mô phỏng công thức bộ đếm không thay được test tích hợp Char/Controller.

### Task 3: Sửa thời gian/trạng thái cho cả hai tab

**Files:** `SkillCastTiming.cs`, `Game1/Char.cs`, `Game2/Char.cs`, `Game1/Controller.cs`, `Game2/Controller.cs`; `Game1/Service.cs`, `Game2/Service.cs` nếu cần packet sink cho test.

- [ ] Thêm cast context theo thiết kế mục 4, fake-clock seam và hàm xử lý receive duration. Khởi tạo mọi mốc ở mỗi cast, bao gồm khỉ.
- [ ] Thay các nhánh 500 count/3000 ms bằng deadline hợp lệ và watchdog riêng. Hủy cờ charge không thay cho hoàn thành cast. Đặt release latch trước khi chạy đường animation/gửi để tránh reentry.
- [ ] Không reset mốc nhận prepare khi hết 20 lượt bay; không dùng `cur` cũ trong nhánh khỉ.
- [ ] Chuyển các điểm kết thúc sang skill đã chụp; cleanup áp dụng cả timeout, mất target, chết, đổi map và ngắt mạng.
- [ ] Chạy lại Task 2; chỉ chuyển bước khi tất cả regression hai namespace pass và xác minh packet sink không gửi trùng.

### Task 4: Hiệu ứng và hồi chiêu

**Files:** `Game1/Char.cs`, `Game2/Char.cs`, `Game1/GameScr.cs`, `Game2/GameScr.cs`, hai `Controller.cs`; hai `ServerEffect.cs` chỉ sửa nếu Profiler chứng minh cần.

- [ ] Đổi spawn effect gồng sang nhịp elapsed; lấy now mới trước mọi nhánh. Test 10 s ở x1/x5/x20/x100 có số batch giới hạn bởi 100 ms, không tỷ lệ thuận với speed và không burst sau frame dài.
- [ ] Tách animation kết thúc khỏi đường tiêu hao đầu cast; dùng trace xác định mốc cooldown server. Test MP và cooldown không bị ghi hai lần.
- [ ] Test packet -94 với remaining=0, remaining=coolDown, 0<remaining<coolDown; chấp nhận thông tin server hợp lệ, loại giá trị âm/vượt giới hạn theo hợp đồng đã trace.
- [ ] Dùng Unity Profiler so sánh cùng map/cùng mật độ nhân vật ở x1/x5/x10/x20. Ghi p50/p95 frame time, max stall, allocation/s và effect peak. Nếu chỉ có speed cao gây tải nền, tách kết luận này khỏi tải tăng thêm khi cast.

### Task 5: Build và nghiệm thu gameplay

**Files:** tạo `Assets/Editor/HighSpeedSkillBuild.cs`, report trong `.analysis/client-skill-speed-20261009/`; build mới vào `Builds/PC-SkillSpeed-20261009-<timestamp>`.

- [ ] Runner build gọi regression trước `BuildPipeline.BuildPlayer`, build StandaloneWindows64 với scene enabled hiện có; thất bại test hoặc `BuildResult != Succeeded` trả exit khác 0.
- [ ] Chạy trên bản project cô lập, không project Unity đang mở. Command PowerShell:

```powershell
& 'C:/Program Files/Unity/Hub/Editor/2022.3.62f2/Editor/Unity.exe' `
  -batchmode -nographics -quit `
  -projectPath 'C:/Users/Admin/Downloads/nroteam/Teamobi2026/.analysis/client-skill-speed-20261009/project' `
  -executeMethod HighSpeedSkillRegression.Run `
  -logFile 'C:/Users/Admin/Downloads/nroteam/Teamobi2026/.analysis/client-skill-speed-20261009/regression.log'
```

Kỳ vọng sau sửa: exit 0, log liệt kê PASS riêng Game1/Game2 và các ca trên; không compiler errors. Runner/build cần được tạo ở Task 2/5 trước khi chạy command tương ứng.

- [ ] Gameplay bốn skill trên cả hai tab: x1/x2/x5/x10/x20; x50/x100 stress vì menu đang cho phép tới 100. Cast liên tiếp 20 lần/skill khi cooldown cho phép; khỉ kiểm tra vòng lên/xuống đủ điều kiện hoặc dùng môi trường test có reset server hợp lệ.
- [ ] Thử hai tab cùng hoạt động, chuyển tab khi gồng, đổi x1→x20→x5 giữa cast, PvE/PvP, trên không/đứng đất, nhiều người cùng gồng, latency 0/200/500 ms, chết/mất target/đổi map/ngắt mạng.
- [ ] Nghiệm thu chức năng: 0 cast bị hủy bởi speed; mỗi cast hợp lệ có đúng kết quả server; cooldown hiện và giảm theo thời gian thật; hết pending thì không kẹt khóa; không duplicate attack/MP.
- [ ] Nghiệm thu hiệu năng trên máy tham chiếu đạt baseline 60 FPS: ở x5/x10/x20 mục tiêu p95 frame ≤25 ms, không stall ≥200 ms do cast; nếu máy không đạt baseline, báo delta so với cùng speed không cast. x50/x100 phải báo số đo và giới hạn thực tế, không cam kết mượt vô hạn trên mọi máy.
- [ ] Ghi manifest source/DLL/ZIP SHA-256 và đường dẫn rollback. Chỉ phát bản đạt gameplay nghiệm thu; build thành công không đồng nghĩa hết lag.

## 6. Giới hạn thay đổi và nhánh server

Ưu tiên client. Chưa có bằng chứng cần đổi server để khắc phục ngưỡng x5, và chưa xác nhận bản server đang chạy khớp source hiện tại.

Nếu trace chứng minh server không phát kết quả/cooldown đúng khi client đã gửi chuẩn, điều tra riêng SkillService/PlayerSkill/EffectSkillService; giữ luật thời gian, damage và MP. Các executor tự nổ của boss/pet là nhánh khác, không dùng để giải thích lỗi người chơi khi chưa có trace.

Nếu cần cập nhật JAR: dừng bằng bảo trì lưu dữ liệu, xác nhận server đã dừng; chuyển `SRC/20.jar` cũ vào `SRC/jar-backup` với tên timestamp không trùng, đối chiếu SHA-256 và ghi metadata trước khi cài bản mới. Gom staging/app JAR dư vào backup; không di chuyển `SRC/lib` hoặc tool JAR. Sau triển khai chỉ để JAR ứng dụng đang dùng tại thư mục chạy.

## 7. Trạng thái bàn giao

Đã hoàn tất kiểm tra tĩnh, xác định khác biệt hai tab và lập kế hoạch. Chưa sửa source, chưa build client, chưa đổi JAR, chưa chứng minh runtime hết lag. Bước triển khai đầu tiên là trace bản exe thực tế và làm regression thất bại trước sửa.
