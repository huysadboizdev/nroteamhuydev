# Kế hoạch sửa nhận ngọc ở nhà

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Thực hiện trực tiếp trong workspace; không tự tạo subagent.

**Goal:** Người chơi có dưới 10.000.000 ngọc xanh được nhận thêm 10.000.000; tổng sau nhận có thể vượt 10.000.000.

**Architecture:** Sửa xử lý chung trong OngGohan, được OngMoori và OngParagus kế thừa. Giữ kiểm tra đúng nhà/menu, khóa synchronized(player), lưu PlayerDAO và gửi số tiền về client. Không đổi schema DB hoặc số dư có sẵn.

**Tech Stack:** Java 17, JDK 21, test Java offline hiện có, script Python build JAR.

---

## Bằng chứng và quy tắc

Đã đọc source và bytecode `SRC/20.jar`: hiện tại `gem != 0` thì từ chối; khi nhận dùng `gem = 10_000_000`. Menu yêu cầu dùng hết ngọc. Test hiện tại cũng khóa quy tắc chỉ nhận khi bằng 0.

Áp dụng tại Nhà Gohan (21), Nhà Moori (22), Nhà Broly/Paragus (23).

| Trước nhận | Được nhận? | Sau nhận |
|---:|:---:|---:|
| 0 | Có | 10.000.000 |
| 1 | Có | 10.000.001 |
| 5.000.000 | Có | 15.000.000 |
| 9.999.999 | Có | 19.999.999 |
| 10.000.000 | Không | 10.000.000 |
| 15.000.000 | Không | 15.000.000 |

Sau khi tiêu xuống dưới ngưỡng lại được nhận thêm. Không giới hạn theo ngày/lần. Số dư âm là dữ liệu bất thường: tiếp tục từ chối như hiện tại, không tự sửa số dư bằng tính năng này.

## Task 1: Cập nhật test để thể hiện yêu cầu

**Modify:** `SRC/test/nro/models/npc_list/HomeNpcSupportTest.java`.

- [x] Thay nội dung `gems()` bằng:

```java
private static void gems() throws Exception {
    for (int planet = 0; planet < 3; planet++) {
        Owner p = owner(21 + planet);
        OngGohan n = npc(planet);
        for (int before : new int[]{0, 1, 5_000_000, 9_999_999}) {
            p.inventory.gem = before;
            n.confirmMenu(p, 0);
            int expected = before + 10_000_000;
            check(p.inventory.gem == expected, "Add full reward below threshold: " + before);
            n.confirmMenu(p, 0);
            check(p.inventory.gem == expected, "Repeated click above threshold gives no reward");
        }
        for (int before : new int[]{10_000_000, 15_000_000, -1, Integer.MAX_VALUE}) {
            p.inventory.gem = before;
            n.confirmMenu(p, 0);
            check(p.inventory.gem == before, "Blocked balance preserved: " + before);
        }
        p.inventory.gem = 9_000_000;
        n.confirmMenu(p, 0);
        check(p.inventory.gem == 19_000_000, "Claim again after spending below threshold");
        check(p.inventory.gold == 123 && p.inventory.ruby == 456, "Other currencies preserved");
    }
}
```

- [x] Trong `menu()`, thay `in.readUTF();` sau kiểm tra template NPC bằng:

```java
String greeting = in.readUTF();
check(greeting.contains("dưới 10 triệu"), "Menu explains eligibility threshold");
check(!greeting.contains("Dùng hết"), "Menu does not require spending every gem");
```

- [x] Trong `concurrent()`, thay kiểm tra nhận ngọc hiện tại bằng:

```java
Owner p = owner(21);
OngGohan n = npc(0);
p.inventory.gem = 9_999_999;
race(() -> n.confirmMenu(p, 0));
check(p.inventory.gem == 19_999_999, "Concurrent clicks award full reward exactly once");
race(() -> n.confirmMenu(p, 0));
check(p.inventory.gem == 19_999_999, "Concurrent replay above threshold gives no reward");
```

Giữ kiểm tra nhận đệ tử và các guard phía sau.

- [x] Chạy test trước khi sửa xử lý để xác nhận thất bại đúng ở số dư 1, menu và concurrent. Thiết lập JDK trong phiên PowerShell:

```powershell
$env:PATH = 'C:\Program Files\Java\jdk-21.0.12\bin;' + $env:PATH
python .analysis/verify_gold_economy.py
```

Expected: test HomeNpcSupportTest chưa đạt yêu cầu mới, các test khác tiếp tục chạy. Script compile toàn bộ source/test và không khởi động server.

## Task 2: Sửa xử lý chung và thông báo

**Modify:** `SRC/src/nro/models/npc_list/OngGohan.java`.

- [x] Thay câu menu `Dùng hết ngọc xanh hiện có mới được nhận tiếp.` bằng `Khi có dưới 10 triệu ngọc xanh, con được nhận thêm 10 triệu.`
- [x] Thay đầu `case 0` đến phép gán ngọc bằng:

```java
if (player.inventory.gem < 0) {
    Service.gI().sendThongBao(player, "Số dư ngọc xanh không hợp lệ.");
    return;
}
if (player.inventory.gem >= 10_000_000) {
    Service.gI().sendThongBao(player,
            "Bạn chỉ được nhận thêm khi có dưới 10 triệu ngọc xanh.");
    return;
}
player.inventory.gem += 10_000_000;
```

Giữ nguyên `PlayerDAO.updatePlayer`, `sendMoney`, thông báo nhận thành công và khóa hiện tại. Không dùng phép gán bằng ngưỡng hoặc Math.min để chặn tổng.

- [x] Chạy lại `python .analysis/verify_gold_economy.py`; expected EXIT=0, toàn bộ test đạt. Ba nhà được kiểm tra trong cùng bộ test; số dư sau nhận tối đa 19.999.999 ở nhánh hợp lệ nên không tràn int.

## Task 3: Build, kiểm tra và triển khai đúng quy tắc JAR

**Tools:** `.analysis/build_release_jar.py`, `SRC/jar-backup`, `AGENTS.md`.

- [x] Trong cùng phiên đã thiết lập PATH, chạy:

```powershell
python .analysis/build_release_jar.py
```

Expected: `RELEASE_COMPILE_EXIT=0`, `JAR_CONTENT_CHECK=PASS`, `PACKAGED_JAR_TESTS=PASS`. Bản staging và SHA-256 ghi trong `.analysis/latest-release.json`; script không thay JAR đang dùng.

- [x] Kiểm tra Java/cổng 14445. Nếu server đang chạy, dùng bảo trì có lưu dữ liệu và đợi tiến trình thoát trước khi thay JAR.
- [x] Chuyển `SRC/20.jar` cũ vào `SRC/jar-backup/20-before-home-gem-claim-<timestamp>.jar` với thời gian duy nhất. So khớp SHA-256 backup với JAR cũ; cài candidate đã kiểm tra thành `SRC/20.jar`; so khớp SHA-256 với metadata build. Ghi đường dẫn backup, thời gian và hai hash vào metadata triển khai. Gom các JAR staging không còn dùng vào jar-backup theo AGENTS.md.
- [x] Khởi động bằng cách chạy launcher hiện có trong `SRC` với cửa sổ ẩn. Kiểm tra log khởi động và cổng 14445.
- [ ] Trên tài khoản kiểm thử, kiểm tra ở cả ba nhà: dưới ngưỡng nhận đủ phần thưởng, đúng ngưỡng từ chối, bấm lặp không cộng tiếp khi đã vượt ngưỡng; đăng xuất/đăng nhập xác nhận số dư đã lưu. Không tự chỉnh tiền của người chơi thật để kiểm thử. Nếu không có client/tài khoản kiểm thử, ghi rõ chưa xác minh tương tác client; test offline và JAR không thay thế kiểm tra này.


## Deployment status

Implemented on 2026-10-03 (Asia/Bangkok). Source tests and packaged JAR tests passed (74/74). Installed bytecode confirms threshold comparison and additive reward. Server PID 1648 listens on port 14445; local TCP connection succeeded. Backup SHA-256 and installed SHA-256 verified; metadata: `.analysis/home-gem-claim/deployment.json`.

Real-client claim/logout/login testing remains unverified; no real player balance was modified for testing. The existing database save call is retained.
