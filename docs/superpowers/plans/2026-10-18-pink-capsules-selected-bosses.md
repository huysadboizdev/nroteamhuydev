# Hồng Ngọc Capsule cho nhóm boss mục tiêu — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Khi tiêu diệt từng boss trong danh sách, rơi capsule Hồng Ngọc theo cùng quy tắc hiện có của Ăn Trộm.

**Architecture:** Giữ cơ chế tạo vật phẩm trong `MiniBossPinkCapsuleDrop`, thêm policy chọn boss theo `BossID`, rồi gọi policy từ luồng thưởng trung tâm `BossAdminDropService`. Luồng này đã claim thưởng một lần mỗi vòng đời boss và được reset trong `Boss.resetBase()`, nên không cần sửa 19 lớp boss riêng lẻ.

**Tech Stack:** Java 21, server Java hiện có, test harness dạng `main()` trong `SRC/test`.

---

## Khảo sát và phạm vi

- Cơ chế Ăn Trộm đang dùng item `ConstItem.CAPSULE_HONG` (ID 722), rơi ngẫu nhiên 1–5 capsule riêng lẻ; mỗi capsule số lượng 1 và khóa quyền nhặt cho người kết liễu. Pet được tính về chủ. Cơ chế đã có kiểm tra vùng, tọa độ, hết hạn và thưởng một lần cho một đời boss.
- Các boss mục tiêu đều đi qua `Boss.reward()` và `BossAdminDropService.reward()` dù từng lớp có `rewardDefault()` riêng. Hook tại luồng này giữ nguyên phần thưởng nhiệm vụ/vàng/đồ sẵn có.
- `BossAdminDropService.CLAIMED` chặn thưởng lặp và `Boss.resetBase()` xóa claim khi boss tái sinh.
- Ăn Trộm đã tự gọi helper capsule riêng; policy mới không đưa Ăn Trộm vào danh sách để tránh rơi đôi.
- Danh sách 19 boss theo source hiện tại:
  - Fide: `BossID.FIDE` (lớp Fide có 3 giai đoạn, nhận capsule ở lần kết liễu cuối cùng).
  - Tiểu đội sát thủ bên Nappa: `KUKU`, `MAP_DAU_DINH`, `RAMBO`.
  - Android và nhóm liên quan: `ANDROID_19`, `DR_KORE`, `PIC`, `POC`, `KING_KONG`, `ANDROID_13`, `ANDROID_14`, `ANDROID_15`.
  - Xên con: `XEN_CON_1` đến `XEN_CON_7`.
- Repo không có `ANDROID_20`; kế hoạch ánh xạ Android 20 người dùng gọi tới lớp `Android/DrKore.java`, ID `BossID.DR_KORE`.

## Phương án

1. **Hook ở luồng thưởng trung tâm (đề xuất):** policy riêng chọn ID, `BossAdminDropService` gọi helper trước native reward. Chỉ thêm một điểm tích hợp; không phụ thuộc từng boss có override `rewardDefault()` ra sao.
2. **Sửa từng lớp boss:** thêm helper vào từng `rewardDefault()` và reset state từng lớp. Cách này chạm nhiều file, dễ thiếu boss hoặc bỏ sót luồng reward đặc biệt.
3. **Thêm cấu hình loot quản trị:** đưa capsule vào các cấu hình drop boss. Cách này làm thay đổi nghĩa của cấu hình append/replace và không tái sử dụng trọn vẹn quyền nhặt riêng hiện có.

Chọn phương án 1. Capsule là phần thưởng nền đã được yêu cầu, độc lập với chế độ thay thế loot quản trị; native reward và loot cấu hình hiện tại tiếp tục chạy nguyên trạng.

## Task 1: Tạo policy danh sách boss và test phạm vi

**Files:**
- Create: `SRC/src/nro/models/services/BossPinkCapsuleDropPolicy.java`
- Create: `SRC/test/nro/models/services/BossPinkCapsuleDropPolicyTest.java`

- [ ] Viết test policy trước: xác nhận đúng đủ 19 ID nêu trên được chọn; `AN_TROM`, `BROLY` và một boss bất kỳ ngoài danh sách không được chọn; xác nhận `DR_KORE` được coi là Android 20 theo source.
- [ ] Chạy test mới để xác nhận nó thất bại trước khi thêm policy.
- [ ] Thêm policy nhỏ với một hàm chọn theo `boss.questBossId()`; không thay đổi `BossAnnouncementPolicy` vì đó là policy thông báo.
- [ ] Chạy lại test policy và xác nhận đúng 19 ID, không có ID lặp.

## Task 2: Gắn capsule vào reward claim một lần

**Files:**
- Modify: `SRC/src/nro/models/services/BossAdminDropService.java`
- Modify: `SRC/test/nro/models/services/BossAdminDropServiceTest.java`
- Reuse unchanged: `SRC/src/nro/models/services/MiniBossPinkCapsuleDrop.java`
- Reuse unchanged: `SRC/src/nro/models/boss/Boss.java`

- [ ] Thêm test tích hợp trước: với một boss được policy chọn, reward tạo 1–5 item ID 722, mỗi item quantity 1, thuộc về killer và có `keepOwnerUntilDespawn`; pet kill phải cấp quyền cho master.
- [ ] Thêm assertions cùng test: callback native vẫn chạy; reward gọi lặp không tạo thêm capsule; `BossAdminDropService.reset(boss)` cho phép đời sau nhận lại; boss ngoài danh sách không rơi capsule; chế độ admin `replace` không xóa capsule nền.
- [ ] Chạy test tích hợp và xác nhận thất bại trước khi gắn hook.
- [ ] Trong `rewardClaimed()`, sau khi claim một lần thành công và trước khi gọi native callback, gọi `MiniBossPinkCapsuleDrop.drop(boss, killer, random)` khi policy chọn boss. Truyền cùng nguồn random injectable để test được số lượng 1–5 xác định.
- [ ] Không sửa `AnTrom.java`; không sửa reward riêng của các boss mục tiêu; giữ nguyên các phần thưởng gốc khác.
- [ ] Chạy lại test tích hợp; xác nhận mỗi boss chỉ có tối đa một đợt capsule trên một đời và native reward không đổi.

## Task 3: Chạy bộ kiểm tra liên quan

**Files:**
- Không thêm thay đổi sản phẩm trong task này.

- [ ] Từ thư mục `SRC`, biên dịch các test main cùng fixture bằng JDK hiện có:

```powershell
$cp = 'build/classes;20.jar;lib/*'
$out = 'build/test/classes'
New-Item -ItemType Directory -Force -Path $out | Out-Null
& 'C:\Program Files\Java\jdk-21.0.12\bin\javac.exe' -proc:none -encoding UTF-8 -cp $cp -sourcepath 'src;test' -d $out `
  'test/nro/models/services/PinkFixture.java' `
  'test/nro/models/services/BossPinkCapsuleDropPolicyTest.java' `
  'test/nro/models/services/BossAdminDropServiceTest.java' `
  'test/nro/models/services/MiniBossPinkCapsuleDropTest.java'
if ($LASTEXITCODE -ne 0) { throw 'Java test compilation failed' }
$testCp = "$out;$cp"
& 'C:\Program Files\Java\jdk-21.0.12\bin\java.exe' -cp $testCp nro.models.services.BossPinkCapsuleDropPolicyTest
& 'C:\Program Files\Java\jdk-21.0.12\bin\java.exe' -cp $testCp nro.models.services.BossAdminDropServiceTest
```

- [ ] Chạy cơ chế capsule hiện có với các case `bounds`, `pickup`, `expiry`, `guard`, `invalid`, `pet`, `concurrent` bằng `MiniBossPinkCapsuleDropTest <case>`; tất cả cần in `PASS`.
- [ ] Kiểm tra diff để chắc chắn chỉ policy, luồng thưởng trung tâm và test liên quan thay đổi; không build hay cài JAR cho đến khi người dùng yêu cầu rõ ràng.

## Rủi ro và giới hạn

- Fide có ba phase nhưng một boss identity; capsule dự kiến rơi ở lần reward cuối, không rơi ở từng phase.
- `DR_KORE` là tên/ID nội bộ trong repo cho Android 20; nếu người dùng muốn boss khác, cần thay mapping trước khi triển khai.
- Capsule rơi riêng trên map như Ăn Trộm; không cộng thẳng Hồng Ngọc vào ví.