# Kế hoạch cập nhật 7 nhiệm vụ KOL nhận VND và điểm

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thay hai chuỗi nhiệm vụ KOL ở Quy Lão Kame bằng 7 nhiệm vụ người dùng cung cấp, nhận VND vào ví ToriBot và điểm sự kiện, mỗi nhánh một lần, tiến độ tích lũy bền vững.

**Architecture:** Tách catalog, state, event tracking và transactional reward service khỏi NPC. KOL v2 lưu trong JSON `player.nhiem_vu_kol`, giữ dữ liệu legacy; ledger chống nhận trùng. Tận dụng đường hạ quái thật, boss đúng dạng và nhặt item thành công, không tạo drop Capsule trong lần này.

**Tech Stack:** Java 21, MySQL/InnoDB, Gson hiện có, giao thức menu NPC hiện có.

**Spec:** [Thiết kế và yêu cầu đã chốt](../specs/2026-10-04-kol-money-point-design.md). Các mức thưởng và quy tắc trong spec là tiêu chí nghiệm thu. Workspace không có Git; dùng backup source/SQL có thời gian và SHA-256, không tạo commit giả.

## Trạng thái triển khai — 2026-10-04 11:49

- [x] Tasks 1–7: đã triển khai source/state/catalog/tracking/claim/ví/menu; spec và quality review độc lập chấp thuận.
- [x] Task 8: candidate và JAR cài đặt đạt 44/44 nhóm kiểm thử, migration live đối chiếu không đổi số dư/JSON, bảo trì lưu dữ liệu, chuyển JAR cũ vào backup SHA-256, server mới PID15512 nạp class v2 và nghe cổng14445.
- [x] Đã ghi metadata/rollback và [báo cáo nghiệm thu](../../source-analysis/kol-money-point-quests.md).
- [ ] Gameplay client thực tế: chưa thực hiện, không đánh dấu nghiệm thu các tương tác chơi thật.

Các test dự kiến ở từng task được gom thành `KolQuestBehaviorTest` (9 mode) và `KolQuestRewardDbTest` (schema riêng); manifest và log trong `.analysis/kol-plan`. Bằng chứng RED gồm thiếu class ban đầu và các biến thể cố ý làm sai hành vi chạy riêng, GREEN là suite đóng gói; không khẳng định mọi checkbox test-before-code phía dưới đã được thực hiện đúng thứ tự nguyên văn. Checklist chi tiết ban đầu được giữ để đối chiếu yêu cầu. Đã bổ sung biên nhận UUID thanh toán theo điều chỉnh rà soát bên dưới.

---

## Task 1: Snapshot, schema ledger và cấu trúc dữ liệu v2

**Create:** `SRC/src/nro/models/player/KolQuestState.java`, `SRC/sql/migrations/20261004_kol_money_point_quests.sql`, `SRC/test/nro/models/player/KolQuestStateTest.java`.
**Modify:** `KOLProgressData.java`, `Player.java`, `MrBlue.java`, `PlayerDAO.java`, `team2026.sql`, `SRC/sql/nro1.sql`.

- [ ] Backup source sẽ sửa và hai SQL dump có tên thời gian/SHA-256. Chụp read-only schema engine, tổng ví/điểm và JSON KOL trong DB live vào `.analysis/kol-plan/database-before.json`; không in mật khẩu/số dư cá nhân ra log công khai.
- [ ] Viết test trước: JSON cũ không có v2 → v2 stage 1/1, mọi counter 0; JSON v2 relog giữ nguyên stage/counter; malformed data không cấp thưởng; legacy fields không bị mất; counter không âm/overflow; ghi nhận thất bại đúng lý do trước khi viết class.
- [ ] Tạo state với `version=2`, `normalStage=1`, `vipStage=1`; 9 bộ đếm `long` tên `woodenDummyKills`, `birdDemonKills`, `fide1Kills`, `fide2Kills`, `fide3Kills`, `perfectCellKills`, `pinkCapsulesPicked`, `blackGokuKills`, `superBlackGokuKills`. Dùng `Math.addExact` hoặc cộng bão hòa `Long.MAX_VALUE` để không wrap âm. Thêm `KolQuestState v2` trong `KOLProgressData`, `KolQuestState kolQuestState` trong Player.
- [ ] Nạp JSON ở MrBlue: nếu v2 thiếu, tạo mới zero; nếu tồn tại chỉ chấp nhận version/stage hợp lệ (stage 1–8). Không lấy lại các stage/bộ đếm legacy để suy luận thành tích mới. Bản save luôn giữ cả legacy + v2; chụp state dưới `synchronized(player)` đã có trong PlayerDAO.
- [ ] Tạo migration chỉ bổ sung ledger, không cộng tiền, không reset dữ liệu player, không đổi ID item hoặc số dư.

```sql
CREATE TABLE IF NOT EXISTS kol_reward_claim (
  player_id BIGINT NOT NULL,
  quest_version INT NOT NULL,
  branch TINYINT NOT NULL,
  quest_id TINYINT NOT NULL,
  account_id INT NOT NULL,
  reward_vnd INT NOT NULL,
  reward_points INT NOT NULL,
  claimed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (player_id, quest_version, branch, quest_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] Append cùng DDL vào hai dump; kiểm tra mọi row item/map/boss config giữ nguyên. Không sửa tên vé sai chính tả nếu chưa có yêu cầu riêng. Không tăng vsItem vì không đổi template.
- [ ] Chạy test state và test SQL schema trên MySQL test riêng; xác nhận unique key từ chối cùng player/version/branch/quest nhưng cho phép hai nhánh khác nhau.

## Task 2: Catalog và cách tính tiến độ

**Create:** `SRC/src/nro/models/task/KolQuestCatalog.java`, `SRC/test/nro/models/task/KolQuestCatalogTest.java`.

- [ ] Viết test bảng 7 nhiệm vụ/14 phần thưởng, tổng thường 2.800.000/71.000, VIP 3.150.000/124.000; NV7 chính xác 700.000/50.000 và 750.000/100.000. Test fail trước triển khai.
- [ ] Catalog immutable chứa ID1–7, mô tả đúng spec, ngưỡng 10.000/100.000/10/3/200/10/10, cặp reward thường/VIP. API `reward(int questId, boolean vip)` và `progress(KolQuestState state, int questId)`; dùng switch theo ID.
- [ ] Hàm tiến độ NV3 phải trả `Math.min(s.fide1Kills, Math.min(s.fide2Kills, s.fide3Kills))`. Test 30/0/0 → 0, 12/10/11 → 10, đổi thứ tự sự kiện vẫn tính đúng, số lẻ không tạo turn.
- [ ] Test các counter của nhiệm vụ khác không làm tăng nhầm nhau; catalog không trả item reward cũ. Chạy tests catalog PASS.

## Task 3: Tracking hạ quái và Tự động luyện tập

**Create:** `SRC/src/nro/models/services/KolQuestService.java`, `SRC/test/nro/models/task/KolQuestTrackingTest.java`.
**Modify:** `SRC/src/nro/models/mob/Mob.java`.

- [ ] Viết tests hạ Mộc nhân đúng/sai tempID; TDLT bật/tắt/hết hạn; Quỷ chim đúng map77/81 và sai map8/76; đệ tử/bot/boss/newPet loại trừ; đòn chưa chết không tăng; gọi tạo drop không tăng; một lần chết thật chỉ tăng một lần. Ghi nhận FAIL trước hook.
- [ ] Service có `onMobKilled(Player killer, Mob mob)` kiểm tra primary, state, template/map, TDLT còn hiệu lực. Dùng khóa player chỉ khi sửa counters, không gửi packet/SQL mỗi kill.
- [ ] Hook ngay sau khi `injured` xác lập chết thật, trước khi tạo drop. Bảo đảm nhận diện một sự kiện sống→chết: có guard theo lần sống, reset tại hồi sinh. Không đặt hook mới trong `mobReward` hoặc `hutItem`. Không cần xóa bộ đếm legacy trong lần này.
- [ ] Không làm thay đổi damage/hp/respawn, đặc biệt cơ chế giới hạn damage lên Mộc nhân. Chạy mô phỏng nhiều hit và hồi sinh để có 2 mạng chết → đúng 2 counter.
- [ ] Chạy test tracking modes `mob`, `tdlt`, `maps`, `actors`, `respawn`. Kiểm tra save/relog với state thật giữ counts.

## Task 4: Tracking đúng từng dạng boss

**Modify:** `SRC/src/nro/models/boss/Boss.java`, `KolQuestService.java`.
**Test:** `SRC/test/nro/models/task/KolQuestBossTest.java`.

- [ ] Test Fide ID-28 level0/1/2 cộng đúng từng counter; chưa đủ ba loại không có turn; 10 mỗi loại → 10 turn; relog giữa turn giữ phần lẻ. Test Black ID-203 level0/1 không lẫn. Test Xên ID-101 level0 map103 đúng, ID-100 level2 map100 và Siêu Bọ Hung level1 sai. Test killer null/nonprimary không ghi.
- [ ] Thêm `onBossKilled(Player killer, Boss boss)` vào service, dùng ID/currentLevel/map tại thời điểm death, không dùng name. Hook một lần trong `Boss.die`, trước khi `currentLevel` thay đổi; không thêm hook trùng trong từng subclass reward.
- [ ] Guard nhận diện death của mỗi dạng/lượt sống, reset trong `resetBase` (đã được gọi khi `initBase`/respawn). Giữ nguyên synchronized combat hiện có; hook chỉ sửa RAM dưới khóa player, không gọi SQL trong đường boss death. Không thêm đường khóa player → boss ngược thứ tự. Giữ nguyên reward/drop/task cũ của boss. Xem điều chỉnh kỹ thuật bên dưới.
- [ ] Chạy tests `fide`, `cell`, `black`, `duplicate`, `relogin`; kiểm tra damage và chuyển dạng boss không thay đổi. Chính sách mặc định chỉ final killer primary như spec; không tự thêm chia credit cho đội/đệ tử.

## Task 5: Tracking Capsule hồng đã nhặt thành công

**Modify:** `SRC/src/nro/models/map/Zone.java`, `KolQuestService.java`.
**Test:** `SRC/test/nro/models/task/KolQuestPickupTest.java`.

- [ ] Test ID722 nhặt stack20 → +20; ID861 không tăng; bag đầy/không đúng người sở hữu không tăng; một itemMap gửi lại packet không tăng; giftcode/shop/trade/box move không tăng; dùng capsule không làm mất counter.
- [ ] Trong Zone.pickItem, dùng `pickupQuantity` đã lưu và template ID trước khi addItem sửa quantity. Chỉ gọi `onItemPicked(Player player, int itemId, int quantity)` **sau** `addItemBag` thành công và sau khi itemMap đã được đánh dấu tiêu thụ/gỡ khỏi đất; hook không phụ thuộc việc gửi packet thành công. Giữ scope synchronized(itemMap) cho nhận một lần.
- [ ] Nếu cần sắp lại đoạn gửi packet/mark picked để sự kiện có commit rõ ràng, bảo toàn special item74/78 và quantity protocol hiện có; không đếm ở InventoryService.addItemBag chung.
- [ ] Không thêm drop ID722 ở Mob/boss/SQL; test dùng fixture tạo itemMap hợp lệ. Khi người dùng thêm drop sau này, tracking tự hoạt động.
- [ ] Chạy pickup test và hồi quy `LargeItemQuantityTest`, `HomeBoxTransferTest`, `InventoryCapacityTest` theo các mode có trong source.

## Task 6: Giao dịch nhận thưởng một lần và ví VND

**Create:** `SRC/src/nro/models/database/KolRewardDAO.java`, `SRC/test/nro/models/task/KolQuestRewardTest.java`, `SRC/test/nro/models/task/KolQuestRewardDbTest.java`.
**Modify:** `KolQuestService.java`, `SRC/src/nro/models/player/PlayerEvent.java`, `SRC/src/nro/models/npc_list/ToriBot.java`, `SRC/src/nro/models/database/PlayerDAO.java`.

- [ ] Tests trước: thiếu vé VIP bị từ chối nhưng thường vẫn nhận; vé chỉ nằm box bị từ chối; stage8 không thưởng; thiếu tiến độ bị từ chối; hai nhánh nhận cùng hoạt động; nhận thường không làm mất progress/VIP; không trừ vé; 14 lần nhận tổng đúng 5.950.000/195.000; bấm lại không thưởng.
- [ ] `claim(Player player, boolean vip)` khóa player, kiểm tra primary/session/state/stage/progress/vé. Snapshot JSON mới chỉ tăng stage của nhánh đang nhận. Không mutate RAM trước transaction.
- [ ] DAO mở connection `setAutoCommit(false)`, khóa account bằng `SELECT ... FOR UPDATE` rồi khóa player; xác thực player.account_id đúng session.userId; đọc ledger unique key; kiểm tra VND/điểm là int không âm, cộng bằng long không vượt Integer.MAX_VALUE; không clamp làm người chơi mất thưởng.
- [ ] Trong cùng transaction: INSERT ledger, UPDATE account.vnd bằng phép cộng, UPDATE player.event_point và nhiem_vu_kol snapshot mới. Nếu update không đúng một row hoặc ghi ledger trùng, ROLLBACK và không phát thưởng. COMMIT rồi mới sync session/vnd, eventPoint và stage vào RAM. Trường hợp mất kết nối quanh COMMIT phải đối chiếu ledger bằng khóa unique để biết đã commit, không retry blind.
- [ ] Nếu server crash sau COMMIT trước sync RAM, relog đọc DB/ledger vẫn thấy thưởng và stage mới; không phát lại. Chỉ gọi thông báo thành công sau xác nhận đã commit.
- [ ] Nếu COMMIT và lần đối chiếu đều mất kết nối, đánh dấu pending; không cho autosave ghi RAM cũ hoặc cấp thưởng mới. Claim và autosave cùng gọi helper đối chiếu dưới khóa player, dùng khóa account DB để chờ transaction cũ settle trước kết luận. Khi đã rõ kết quả, giữ counter/điểm kiếm thêm trong thời gian pending và đồng bộ stage/ví đúng; DB còn lỗi thì giữ guard và chưa báo thành công.
- [ ] Đồng bộ `PlayerEvent.addEventPoint/subEventPoint/setEventPoint/getEventPoint` với monitor player để không mất điểm khi boss cho +5/autosave/shop đổi điểm chạy cùng claim. PlayerDAO autosave đã dùng cùng monitor; không tự tạo lock đảo thứ tự.
- [ ] Sửa ToriBot debit ở BuyVip từ chỉ trừ RAM sang SQL debit có điều kiện `vnd >= cost`, cập nhật session bằng số dư DB sau debit thành công rồi mới cấp VIP/thưởng. Giữ điều kiện mùa/4 lượt/giá hiện có; đồng bộ cùng khóa player. Kiểm tra `PlayerDAO.subvnd` và luồng Input đổi vàng/ngọc tránh debit/cộng ghi đè session cũ; chỉ thay phần liên quan ví nếu cần.
- [ ] DB tests chạy trên schema test riêng có account/player fixture và ledger thật: hai yêu cầu đồng thời cùng reward chỉ commit1; thường+VIP commit cả2; fail sau ledger/credit VND/credit điểm đều rollback; cộng VND không thay tongnap; DB lỗi/overflow không mất lượt nhận; nhận rồi mua VIP/relog không hồi tiền; autosave không ghi đè state/điểm mới. Không chạy tests phát thưởng trên tài khoản thật.

## Task 7: Menu Quy Lão Kame

**Modify:** `SRC/src/nro/models/npc_list/QuyLaoKame.java`.
**Test:** `SRC/test/nro/models/npc_list/KolQuestMenuTest.java`.

- [ ] Test hai menu KOL thường/VIP tồn tại; hiển thị stage, current/required, VND và điểm đúng từng nhánh; NV3 có từng dạng Fide; đủ điều kiện mới hiện nhận thưởng; stage8 chỉ hiện hoàn thành/đóng; thiếu vé thông báo rõ.
- [ ] Thay KOL_QUESTS/KOL_VIP_QUESTS và vòng cấp item cũ bằng catalog/state/service mới. Giữ menu constants/routing hiện có và các menu đổi điểm/rùa/lân/nhiệm vụ nói chuyện.
- [ ] Nội dung NV2 ghi đúng Rừng đá Fide/Hang quỷ chim; NV4 đúng Võ đài Xên; NV5 **Capsule hồng**, không “capsule hồng ngọc”; NV7 dùng tên source **Super Black Goku**. Thông báo nhận thưởng nêu số VND/điểm và nhánh; refresh menu sau commit.
- [ ] Xác minh lại server-side khi nhận, không tin stage/percent từ menu. Lưu quest ID/nhánh đang hiển thị trong context menu phía server; claim kiểm tra expectedQuestId khớp stage hiện tại. Sau thành công xóa context nhận thưởng và mở menu gốc, không tự mở ngay nút nhận của nhiệm vụ kế tiếp; khóa/ledger ngăn nhận trùng. Test bấm hai lần không tự nhận luôn bước kế tiếp dù đã có đủ tiến độ. Không tuyên bố chống replay bằng nonce client vì giao thức menu hiện tại không có nonce.
- [ ] Không sửa client Unity nếu menu hiện có hiển thị vừa; kiểm tra Game1/Game2 xuống dòng trên màn hình hẹp, ví tại ToriBot và số điểm tại Quy Lão cập nhật sau nhận.

## Task 8: Đóng gói, triển khai, nghiệm thu và rollback

**Evidence:** `.analysis/kol-plan/`, `docs/source-analysis/kol-money-point-quests.md` khi triển khai.

- [ ] Compile Java21 các class thay đổi với classpath active20.jar + SRC/lib; test classes đặt trước application JAR. Không dùng các class cũ cache làm bằng chứng mới.

```powershell
# Từ SRC, sau khi compile classes/test-classes vào .analysis/kol-plan.
& 'C:/Program Files/Java/jdk-21.0.12/bin/java.exe' '-Dfile.encoding=UTF-8' -cp '../.analysis/kol-plan/test-classes;../.analysis/kol-plan/classes;20.jar;lib/*' nro.models.task.KolQuestTrackingTest mob
& 'C:/Program Files/Java/jdk-21.0.12/bin/java.exe' '-Dfile.encoding=UTF-8' -cp '../.analysis/kol-plan/test-classes;../.analysis/kol-plan/classes;20.jar;lib/*' nro.models.task.KolQuestBossTest fide
```

- [ ] Chạy toàn bộ state/catalog/tracking/boss/pickup/menu/claim tests và DB transactions trên test schema; tests fail→pass có log. Chạy hồi quy 32 nhóm của lần dragon collection, thêm VIP purchase persistence/menu/shop điểm nếu có thay đổi.
- [ ] Package candidate bằng cách thay đúng class trong active JAR, kiểm tra entry không trùng/thiếu, SHA-256, chạy lại suite trên candidate **không đưa source classes vào classpath**.
- [ ] Bảo trì lưu dữ liệu theo Maintenance, xác định đúng PID/workdir, kiểm tra tiến trình dừng và cổng14445 đóng. Backup DB/SQL, áp dụng migration ledger; kiểm tra engine/unique key và không đổi balances/player JSON do migration.
- [ ] Chuyển SRC/20.jar cũ vào SRC/jar-backup tên riêng có thời gian, so SHA-256, sau đó cài candidate, ghi metadata + schema version + counters baseline + rollback. Gom application JAR dư SRC/.analysis vào backup, không di chuyển SRC/lib hoặc tool JAR còn cần.
- [ ] Khởi động server ẩn, kiểm tra DB loaded/port/PID, ledger schema v2 và menu. Chỉ dùng nhân vật test riêng để nghiệm thu thực tế, không cấp thưởng hay sửa counters cho người chơi thật.
- [ ] Nghiệm thu: TDLT thật có hiệu lực; đúng quái/map; Fide cộng dồn từng dạng/relog; Xên đúng dạng/map; Black/Super riêng; Capsule nhặt không nguồn mới; nhận cả hai nhánh; không trừ vé; đủ14 thưởng tổng đúng; bấm lại/relog không cấp lại; nhận tiền→mua VIP→relog giữ số dư đúng.
- [ ] Rollback bảo trì và backup bản hiện tại trước khi phục hồi JAR. Giữ ledger để triển khai lại cùng version không cấp thưởng trùng; giữ dữ liệu tiền/điểm đã commit, không tự hoàn tiền đã tiêu/thu hồi thưởng. Nếu cần rollback source/dump dùng backup có hash; SQL ledger mới có thể để lại, không DROP lịch sử khi đã có claim.
- [ ] Ghi rõ kiểm thử nào thực hiện, trường hợp nào chỉ kiểm tra source; không tuyên bố đã test client nếu chưa chơi thật. Đánh dấu task hoàn tất theo bằng chứng.

## Phạm vi lần lập kế hoạch

### Điều chỉnh sau rà soát thanh toán

Bổ sung bảng `wallet_debit_receipt` lưu UUID, account, loại giao dịch và số tiền trong cùng transaction trừ VND. Khi kết nối mất quanh COMMIT, đối chiếu biên nhận dưới khóa account trước khi kết luận; nếu chưa đối chiếu được, giữ yêu cầu mua và phần cấp thưởng gốc trong RAM, chặn giao dịch mới, thử đối chiếu lại khi mua hoặc autosave. Kiểm thử COMMIT thành công nhưng trả lỗi, mất kết nối đối chiếu và phục hồi chỉ cấp một lần. Không dùng chênh lệch số dư để suy đoán vì tài khoản có thể được nạp thêm tiền.

Phạm vi này xử lý mất kết nối khi tiến trình vẫn chạy. Việc phục hồi toàn bộ vật phẩm/gói VIP nếu tiến trình bị crash giữa trừ tiền và lưu phần cấp thưởng vẫn là giới hạn của hệ thống mua hiện có, cần cơ chế lưu đơn hàng và cấp vật phẩm bền vững riêng.

Chỉ đọc source/SQL, tạo tài liệu và snapshot kiểm tra dump. **Chưa sửa source ứng dụng, hai SQL dump, database live, JAR hoặc server.** Không có Git trong workspace nên không commit tài liệu. Người dùng đã xác nhận các lựa chọn trong spec và muốn Capsule tracking trước/drop sau; chưa yêu cầu triển khai kế hoạch này.

## Điều chỉnh kỹ thuật trong lúc triển khai đã được duyệt

Người dùng yêu cầu triển khai kế hoạch vào 04/10/2026. Ghi chú “chỉ lập kế hoạch/chưa yêu cầu triển khai” ở trên là trạng thái trước yêu cầu này.

Rà soát phát hiện `Boss.injured`, `BlackGoku.injured` và `SieuBoHung.injured` vốn synchronized toàn phương thức, gọi die/reward bên trong. Không tháo khóa combat hoặc chuyển reward sang executor vì sẽ thay hành vi chiến đấu ngoài phạm vi. Giữ khóa hiện có và hook RAM ngắn theo thứ tự boss → player; audit các luồng đang khóa player (claim, điểm, ví, menu, autosave, inventory/combine/pet) không có đường lấy monitor boss ngược lại. Tracking không gọi JDBC trong boss death; giao dịch JDBC chạy từ NPC/autosave, không giữ monitor boss. Đây là điều chỉnh giới hạn của bước Task4 nhằm giữ đúng đồng bộ combat đã có.

Thêm xác minh JSON số nguyên chính xác trước Gson để không nhận version/stage/counter null, chuỗi, số lẻ hoặc tràn kiểu; thêm guard đối chiếu COMMIT không rõ kết quả trong Task6. Hai bổ sung này thực hiện yêu cầu fail-closed và không cấp trùng của kế hoạch.
