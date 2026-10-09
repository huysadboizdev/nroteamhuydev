# Boss Admin — Mở rộng tab Chỉ số (Giáp & Chí mạng) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
> **Lưu ý repo:** KHÔNG tự commit (quy tắc workspace: chỉ commit khi người dùng yêu cầu). Thay bước commit bằng kiểm tra `git diff --stat` sau mỗi task.

**Goal:** Thêm 2 ô chỉnh trong tab Chỉ số của `admin/bosses.php`: Giáp (giảm sát thương boss nhận vào) và Chí mạng (tỉ lệ boss crit lên người), áp dụng live không restart sau 1 lần cài JAR.

**Architecture:** Giữ nguyên pipeline hiện tại (web → `boss_admin_config` → RELOAD_BOSS → `BossAdminConfig.stage` → snapshot RAM). Giáp lưu vào `NPoint.defAdd`, chí mạng lưu vào `NPoint.critAdd` để `calPoint()` (`def = defg*4 + defAdd`, `crit = critg + critAdd`) tính lại cũng không mất giá trị admin.

**Tech Stack:** Java 17, MySQL 8 (CHECK), PHP 8.2, vanilla JS, Ant/javac build script `.analysis/build_boss_admin_candidate.py`, deploy script `.analysis/deploy_boss_admin.py`.

---

## Phạm vi chốt (duyệt trước khi làm)

| Chỉ số | Ý nghĩa combat (đã đối chiếu code) | Thêm? |
|---|---|---|
| Giáp (`defense`) | `NPoint.subDameInjureWithDeff` trừ thẳng `def` vào dame nhận, tối thiểu còn 1. Boss hiện `def=0`. Miền 0..2.147.483.647 | ✅ Task 1–8 |
| Chí mạng (`critical`) | `NPoint.setIsCrit` roll `Util.isTrue(crit,100)`. Boss hiện `crit=0`. Miền 0..100 | ✅ Task 1–8 |
| Né đòn (`tlNeDon`) | `Player` dòng 1124–1175, cap cứng 90. Có thể thêm nhưng dễ gây ức chế (boss né đòn người chơi) | ⏳ OPTIONAL Task 9 |
| MP | Boss `mpg` cố định 31.072.002 để không bao giờ hết skill. Chỉnh MP có thể khóa skill boss | ❌ KHÔNG làm, lý do đã rõ |
| Số lượng spawn, HP > INT | Giữ nguyên cam kết cũ | ❌ KHÔNG làm |

**Triển khai cần 1 lần bảo trì:** Java đổi (`Config` record + `apply()`) nên phải build JAR + restart 1 lần. Migration SQL chạy trong cùng lần bảo trì (không cần restart riêng cho DB). Rollback chỉ cần trả JAR cũ (code cũ SELECT liệt kê cột, thừa cột mới vẫn chạy).

---

## File map (chỉ chạm các file này)

- Tạo: `SRC/sql/migrations/20261008_boss_admin_defcrit.sql`
- Sửa Java: `SRC/src/nro/models/services/BossAdminConfig.java` (record, SELECT, validate, `captureDefaults`, `apply`), `SRC/src/nro/models/boss/Boss.java` (2 field default)
- Sửa test Java: `SRC/test/nro/models/services/BossAdminConfigTest.java` (thêm method mới, không sửa test cũ)
- Sửa web: `C:/xampp/htdocs/webgoc2026/admin/includes/boss_admin_helper.php`, `C:/xampp/htdocs/webgoc2026/admin/bosses.php`, `C:/xampp/htdocs/webgoc2026/admin/assets/js/boss-admin.js` (2 danh sách field)
- Sửa test web: `C:/xampp/htdocs/webgoc2026/tests/admin_boss_admin_test.php` (thêm case), `admin_boss_admin_ui_test.js` (thêm chuỗi kiểm tra)
- Sửa test SQL: `SRC/test/boss_admin_schema_test.py` (thêm 2 case invalid)
- Không chạm: catalog generator (giáp/crit mặc định 0 mọi boss), heartbeat, drop service, seeds (merge sau khi live ổn), `BossLiveReload` (legacy chỉ có hp/damage, stat mới mặc định = giá trị runtime hiện tại).

---

### Task 1: Migration SQL thêm 2 cột

**Files:**
- Create: `SRC/sql/migrations/20261008_boss_admin_defcrit.sql`

- [ ] **Step 1: Viết file migration**

```sql
-- Additive only. Single-apply with the deploy script (no IF NOT EXISTS for columns on MySQL 8).
ALTER TABLE boss_admin_config
  ADD COLUMN defense INT NULL,
  ADD COLUMN critical INT NULL,
  ADD CONSTRAINT boss_admin_defense CHECK (defense IS NULL OR defense >= 0),
  ADD CONSTRAINT boss_admin_critical CHECK (critical IS NULL OR critical BETWEEN 0 AND 100);
```

- [ ] **Step 2: Chạy schema test hiện tại (đỏ ở case mới sau Task 6, hiện tại chỉ đảm bảo file đọc được)**

Run: `python SRC/test/boss_admin_schema_test.py`
Expected: PASS (migration mới chưa được test này nạp; Task 6 nối vào)

- [ ] **Step 3: Kiểm tra diff**

Run: `git diff --stat`
Expected: chỉ 1 file mới `20261008_boss_admin_defcrit.sql`

---

### Task 2: Java — Config record + validation + apply (TDD)

**Files:**
- Modify: `SRC/src/nro/models/services/BossAdminConfig.java`
- Modify: `SRC/src/nro/models/boss/Boss.java`
- Test: `SRC/test/nro/models/services/BossAdminConfigTest.java`

- [ ] **Step 1: Thêm field default vào Boss.java** (dòng `public int adminDefaultHp, adminDefaultDamage;`)

```java
public int adminDefaultHp, adminDefaultDamage, adminDefaultDef, adminDefaultCrit;
```

- [ ] **Step 2: Viết failing test mới** (thêm method vào `BossAdminConfigTest.java`, dùng lại `db()`, `row()`, `install()`, `blank()`, `player()`, `check()` có sẵn). Row cần thêm cột `defense`/`critical` nên tự dựng map:

```java
static Map<String,Object> defRow(long rev,int defense,int critical){Map<String,Object> m=row(rev,"next",200);m.put("defense",defense);m.put("critical",critical);return m;}
public static void defenseCrit()throws Exception {
  install();var api=Class.forName("nro.models.services.BossAdminConfig");
  var stage=api.getMethod("stage",Connection.class);var spawn=api.getMethod("applyAtSpawn",Boss.class);
  Boss b=blank(Boss.class);b.id=BossID.FIDE;b.currentLevel=0;b.isBoss=true;b.bossStatus=nro.models.consts.BossStatus.ACTIVE;b.nPoint=new nro.models.player.NPoint(b);b.nPoint.hpg=b.nPoint.hpMax=100;b.nPoint.hp=100;b.nPoint.dameg=b.nPoint.dame=10;
  ((Runnable)stage.invoke(null,db(7,List.of(defRow(7,1000,15))))).run();spawn.invoke(null,b);
   check(b.nPoint.defAdd==1000&&b.nPoint.def==1000,"armor lands in defAdd and effective def");
   check(b.nPoint.critAdd==15&&b.nPoint.crit==15,"crit lands in critAdd and effective crit");
   // defAdd/critAdd survive NPoint.calPoint by construction (def=defg*4+defAdd, crit=critg+critAdd); blank fixtures cannot run full calPoint (cần gear/player thật), nên không gọi calPoint trong test.
   ((Runnable)stage.invoke(null,db(8,List.of(row(8,"next",200))))).run();spawn.invoke(null,b);
   check(b.nPoint.defAdd==0&&b.nPoint.critAdd==0,"nullable reset restores native armor/crit");
  var bad=defRow(8,-1,0);
  try{stage.invoke(null,db(8,List.of(bad)));throw new AssertionError("negative defense accepted");}catch(InvocationTargetException e){check(e.getCause() instanceof IllegalArgumentException,"defense bound");}
  bad=defRow(8,0,101);
  try{stage.invoke(null,db(8,List.of(bad)));throw new AssertionError("crit>100 accepted");}catch(InvocationTargetException e){check(e.getCause() instanceof IllegalArgumentException,"crit bound");}
  System.out.println("Boss admin defense/crit PASS");
}
```

Và thêm `"defenseCrit"` vào danh sách test chạy (hàm `main` của file test nếu liệt kê theo tên; nếu `main` gọi từng hàm thì thêm lời gọi `defenseCrit();`).

- [ ] **Step 3: Chạy test, xác nhận ĐỎ**

Run: `C:/"Program Files"/Java/jdk-21.0.12/bin/java.exe -cp .analysis/boss-admin-build/tests;.analysis/boss-admin-build/classes;SRC/lib/* nro.models.services.BossAdminConfigTest` (biên dịch test trước bằng `javac --release 17 -proc:none ...` như `build_boss_admin_candidate.py` đã làm)
Expected: FAIL (chưa có cột defense trong SELECT/validate → `IllegalArgumentException` hoặc `NullPointerException` ở `defRow` vì `row()` không có key — đỏ vì lý do đúng: stat mới chưa được hỗ trợ)

- [ ] **Step 4: Implement tối thiểu trong `BossAdminConfig.java`**
  - Record (dòng 18 cũ): thêm `Integer defense,Integer critical` trước `String applyMode`:

```java
public record Config(Integer hp,Integer damage,Integer capDamage,BigDecimal capPercent,Integer defense,Integer critical,String applyMode,String dropMode,List<Rule> drops,long revision) { public Config { drops=List.copyOf(drops); } }
```

  - SELECT (dòng 40 cũ): `SELECT boss_key,phase,hp,damage,cap_damage,cap_percent,defense,critical,apply_mode,drop_mode,drops,revision ...`; đọc thêm:

```java
Integer def=nullable(r,"defense"),crit=nullable(r,"critical");
```

  - Validate (sau dòng check numeric cũ):

```java
valid((def==null||def>=0)&&(crit==null||crit>=0&&crit<=100),"Invalid boss defense/crit");
```

  - `captureDefaults` (dòng 66 cũ), thêm:

```java
b.adminDefaultDef=b.nPoint.defAdd;b.adminDefaultCrit=b.nPoint.critAdd;
```

  - Chống sót giá trị admin giữa 2 mạng sống (phát hiện khi test reset): `nPoint` tái dùng mỗi lần spawn, nên reset 2 field admin sở hữu TRƯỚC khi capture. Native không bao giờ set 2 field này (grep `defAdd|critAdd` trong `models/boss` chỉ trúng `XenBoHung critg++` — đó là `critg`, không phải `critAdd`):

```java
/** Native boss code never sets defAdd/critAdd (only XenBoHung critg); admin owns them, so clear leftovers before capturing per-life defaults. */
private static void resetAdminOwnedAdds(Boss b){b.nPoint.defAdd=0;b.nPoint.critAdd=0;}
```

Gọi `resetAdminOwnedAdds(b);` ngay trước `captureDefaults(b);` trong cả `applyAtSpawn` và `applyAfterNativeSpawn` (bao phủ cả boss spawn qua `joinMap` như Whis).

  - `apply` (sau dòng set damage cũ), thêm:

```java
int def=c.defense()==null?b.adminDefaultDef:c.defense();b.nPoint.defAdd=def;b.nPoint.def=b.nPoint.defg*4+def;
int cr=c.critical()==null?b.adminDefaultCrit:c.critical();b.nPoint.critAdd=cr;b.nPoint.crit=b.nPoint.critg+cr;
```

  - Không cần `nativeDef/nativeCrit` (không boss nào override def/crit theo công thức; `applyAfterNativeSpawn` đã capture sau reset gốc).

- [ ] **Step 5: Chạy lại test, xác nhận XANH**

Run: như Step 3 (biên dịch lại test + chạy)
Expected: `Boss admin defense/crit PASS`, các test cũ trong file vẫn PASS

- [ ] **Step 6: Kiểm tra diff**

Run: `git diff --stat`
Expected: 3 file Java (2 source + 1 test)

---

### Task 3: PHP helper — validate + lưu + đọc + rollback

**Files:**
- Modify: `C:/xampp/htdocs/webgoc2026/admin/includes/boss_admin_helper.php`
- Test: `C:/xampp/htdocs/webgoc2026/tests/admin_boss_admin_test.php`

- [ ] **Step 1: Thêm failing case vào test** (gọi `boss_admin_validate($p,$catalog)` với catalog stub `[['key'=>'id:-32','phases'=>[['phase'=>0]]]]`, theo đúng pattern các case có sẵn trong file):

```php
// defense phải >= 0
try{boss_admin_validate(['boss_key'=>'id:-32','phase'=>0,'expected_revision'=>0,'defense'=>-1,'drops_json'=>'[]'],$catalog);throw new RuntimeException('defense âm lọt');}catch(InvalidArgumentException $e){}
// critical phải 0..100
try{boss_admin_validate(['boss_key'=>'id:-32','phase'=>0,'expected_revision'=>0,'critical'=>101,'drops_json'=>'[]'],$catalog);throw new RuntimeException('crit>100 lọt');}catch(InvalidArgumentException $e){}
// hợp lệ đi qua
$v=boss_admin_validate(['boss_key'=>'id:-32','phase'=>0,'expected_revision'=>0,'defense'=>1000,'critical'=>15,'drops_json'=>'[]'],$catalog);
if($v['defense']!==1000||$v['critical']!==15)throw new RuntimeException('defense/critical không giữ giá trị');
```

- [ ] **Step 2: Chạy test, xác nhận ĐỎ**

Run: `C:/xampp/php/php.exe C:/xampp/htdocs/webgoc2026/tests/admin_boss_admin_test.php`
Expected: FAIL (validate chưa biết 2 field → bỏ qua hoặc lỗi thiếu)

- [ ] **Step 3: Implement tối thiểu trong helper**
  - `boss_admin_defaults()` (dòng 4): thêm `'defense'=>null,'critical'=>null`.
  - Sau vòng `foreach(['hp','damage','cap_damage']...)` (dòng 18), thêm:

```php
$out['defense']=$p['defense']??'';$out['defense']=$out['defense']===''||$out['defense']===null?null:admin_integer($out['defense'],0,2147483647);
$out['critical']=$p['critical']??'';$out['critical']=$out['critical']===''||$out['critical']===null?null:admin_integer($out['critical'],0,100);
```

  - `boss_admin_row_config()` (dòng 47–52): đổi `foreach(['hp','damage','cap_damage'] as $k)` thành `foreach(['hp','damage','cap_damage','defense','critical'] as $k)`.
  - Dòng reset (dòng 59): thêm `'defense'=>'','critical'=>''` vào `array_merge`.
  - INSERT dòng 98: thêm 2 cột (giữ đúng thứ tự binds):

```php
admin_execute_statement('INSERT INTO boss_admin_config(boss_key,phase,hp,damage,cap_damage,cap_percent,defense,critical,apply_mode,drop_mode,drops,revision) VALUES(?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE hp=VALUES(hp),damage=VALUES(damage),cap_damage=VALUES(cap_damage),cap_percent=VALUES(cap_percent),defense=VALUES(defense),critical=VALUES(critical),apply_mode=VALUES(apply_mode),drop_mode=VALUES(drop_mode),drops=VALUES(drops),revision=VALUES(revision)','siiiisiisssi',[$v['boss_key'],$v['phase'],$after['hp'],$after['damage'],$after['cap_damage'],$after['cap_percent'],$after['defense'],$after['critical'],$after['apply_mode'],$after['drop_mode'],$drops,$revision]);
```

(Rollback/reset/audit tự theo vì dùng chung `boss_admin_validate` + `boss_admin_row_config` + `boss_admin_defaults`.)

- [ ] **Step 4: Chạy test, xác nhận XANH**

Run: `C:/xampp/php/php.exe C:/xampp/htdocs/webgoc2026/tests/admin_boss_admin_test.php`
Expected: `Boss admin validation tests passed`

- [ ] **Step 5: Lint helper**

Run: `C:/xampp/php/php.exe -l C:/xampp/htdocs/webgoc2026/admin/includes/boss_admin_helper.php`
Expected: `No syntax errors detected`

---

### Task 4: UI tab Chỉ số + JS nối field

**Files:**
- Modify: `C:/xampp/htdocs/webgoc2026/admin/bosses.php`, `C:/xampp/htdocs/webgoc2026/admin/assets/js/boss-admin.js`

- [ ] **Step 1: Thêm 2 ô nhập vào thẻ "HP và sức đánh" trong `bosses.php`** (giữ nguyên class/placeholder pattern):

```html
<div class="boss-grid"><label>Giáp tùy chỉnh (trừ thẳng vào dame nhận)<input name="defense" type="number" min="0" max="2147483647" class="form-control" placeholder="Để trống = mặc định"></label><label>Chí mạng tùy chỉnh %<input name="critical" type="number" min="0" max="100" class="form-control" placeholder="Để trống = mặc định"></label></div>
```

- [ ] **Step 2: Nối field trong `boss-admin.js`** — 2 chỗ danh sách cố định:
  - `snapshot()`: thêm `'defense','critical'` vào mảng field.
  - `loadPhase()`: thêm `'defense','critical'` vào vòng `for(const k of [...])`.
  - Dòng restore trong `render()` cuối file: thêm `'defense','critical'` vào mảng copy `['hp','damage',...]`.

- [ ] **Step 3: Lint + node check**

Run: `C:/xampp/php/php.exe -l C:/xampp/htdocs/webgoc2026/admin/bosses.php` → `No syntax errors detected`; `node --check .../boss-admin.js` → exit 0

- [ ] **Step 4: Mở rộng smoke render** (`C:/Users/Admin/AppData/Local/Temp/opencode/boss-render-smoke.js` là file tạm — viết assertion mới kiểm tra `bosses.php` chứa `name="defense"` và `name="critical"`; hoặc thêm 2 chuỗi này vào `admin_boss_admin_ui_test.js` mảng feature):

```js
for(const feature of [...,'name="defense"','name="critical"'])assert.ok(php.includes(feature),feature);
```

Run: `node C:/xampp/htdocs/webgoc2026/tests/admin_boss_admin_ui_test.js` → `Boss admin UI contract tests passed`

---

### Task 5: Test SQL schema cho cột mới

**Files:**
- Modify: `SRC/test/boss_admin_schema_test.py`

- [ ] **Step 1: Thêm 2 case invalid** theo đúng pattern case `invalid-settings` có sẵn: 1 row `defense=-1` và 1 row `critical=101`, cả hai phải bị CHECK từ chối khi replay migration mới trên DB disposable.
- [ ] **Step 2: Nối migration mới vào luồng replay của test** (test hiện nạp migration live; thêm file `20261008_boss_admin_defcrit.sql` vào sau file live theo đúng cách test đang nạp file cũ — đọc đầu file test để copy pattern, không đoán).
- [ ] **Step 3: Chạy**

Run: `python SRC/test/boss_admin_schema_test.py`
Expected: PASS kèm 2 case mới (output `BOSS_ADMIN_SCHEMA PASS ...` vẫn exit 0)

---

### Task 6: Build candidate + full suite

**Files:** không sửa source, chỉ chạy.

- [ ] **Step 1: Chạy build candidate**

Run: `python .analysis/build_boss_admin_candidate.py` (timeout 420000)
Expected: `BOSS_ADMIN_CANDIDATE PASS`, metadata `.analysis/boss-admin-build-latest.json` mới (`verified:true`, `database_verified:true`)

- [ ] **Step 2: Chạy full suite boss-admin** (theo `.analysis/build_boss_admin.py`: Broly, FatMabu×2, HuntingTrio×2/Combat, MiniBoss×4, Bojack×5, Namek×5, Loot×9, Death, Full — mỗi lệnh `java -cp ... <TestClass> [mode]`, timeout 60000–120000 riêng; `BossFullLifecycleTest` in PASS nhưng JVM không thoát — coi dòng `PASS` là đạt, kill tiến trình sau 120s)
Expected: tất cả in PASS, exit 0 (trừ FullLifecycle: PASS trên log)

- [ ] **Step 3: Chạy test web + catalog**

Run: `node .../admin_boss_admin_ui_test.js`, `php .../admin_boss_admin_test.php`, `php .../admin_boss_admin_db_test.php SRC/sql/migrations/20261008_boss_admin_live.sql SRC/sql/migrations/20261008_boss_admin_defcrit.sql`, `python SRC/test/boss_admin_catalog_test.py`
Expected: 4/4 PASS (db test nhận migration file; db test đã mở rộng nhận NHIỀU migration qua `array_slice($argv,1)` + payload save/reset/rollback assert luôn `defense`/`critical` — lệch nhỏ so với bản plan đầu, đã cập nhật file test tương ứng)

---

### Task 7: Deploy (1 lần bảo trì) + smoke live

- [ ] **Step 1: Chạy deploy**

Run: `python .analysis/deploy_boss_admin.py` (timeout 300000; script tự đọc `boss-admin-build-latest.json` mới, assert hash source/test/web/migration, maintenance lưu dữ liệu, snapshot SQL, apply migration, đổi JAR, verify hash)
Expected: `DEPLOYMENT PASS; saved server stopped, hashed SQL/JAR backups verified`

- [ ] **Step 2: Khởi động server** bằng tay (`run.bat`, KHÔNG khởi từ tool — tool kill tiến trình con khi xong việc, đã ghi nhận 2026-10-08). Kiểm tra listener `Get-NetTCPConnection -State Listen -LocalPort 14445` đúng PID mới, 1 instance duy nhất.
- [ ] **Step 3: Smoke live**: heartbeat `SELECT COUNT(*), MAX(updated_at) FROM boss_admin_runtime` (>0, mới); queue 1 lệnh RELOAD_BOSS test (config toàn NULL = không đổi gameplay) → `status=completed`; xóa row test.
- [ ] **Step 4: Mở `admin/bosses.php`, sửa Giáp/Chí mạng 1 boss thử** (vd HP giữ nguyên, defense=500) → chờ ACK `Đã nạp` → vào game kiểm tra dame nhận giảm. Giữ hay reset tùy bạn.

---

### Task 9 (OPTIONAL, tách riêng): Né đòn `tlNeDon`

Chỉ làm khi bạn duyệt Task 1–8 xong. Nội dung giống hệt Task 2–4 với field `nedon`, miền 0..90 (code cap cứng 90 ở `Player` dòng 1157), set trực tiếp `nPoint.tlNeDon` (không có công thức recalPoint). Cảnh báo gameplay: boss né đòn gây ức chế — khuyến nghị để sau, hoặc chỉ bật cho boss sự kiện.

---

## Không làm (đã chốt lý do)

- MP: `mpg` cố định 31.072.002 để boss không kẹt skill; chỉnh MP có thể khóa skill boss.
- Catalog generator: giáp/crit mặc định 0 mọi boss → không cần cột mới; web hiển thị "gốc" khi NULL đã đúng.
- Heartbeat thêm cột def/crit: YAGNI (cột Đang sống đã đủ; thêm thì phải đổi query + JS, để khi nào cần).
- Seeds: merge migration vào seed sau khi live ổn định (đúng quy ước cũ).

## Rủi ro & rollback

1. Lần bảo trì duy nhất là bắt buộc (JAR mới). Giảm rủi ro: build candidate đã chạy full suite trước; deploy assert hash mọi file; backup JAR+SQL có SHA-256.
2. Rollback: bảo trì lưu dữ liệu → trả JAR backup → start lại. Không cần rollback DB (code cũ SELECT liệt kê cột, thừa 2 cột mới vẫn chạy).
3. Hai admin sửa cùng lúc: giữ nguyên optimistic revision hiện tại (không đổi).

---

## Self-review (đã tự soát)

1. **Spec coverage:** Giáp ✅ Task 2–4; Chí mạng ✅ Task 2–4; "tự chỉnh trên bảng" ✅ Task 4+7; không restart sau cài ✅ Task 7 (1 lần cài); Né đòn ⏳ Task 9; MP ❌ có lý do.
2. **Placeholder scan:** không có TBD/TODO/"tương tự task N" — mọi bước có code/lệnh/expected cụ thể; tên field `defense`/`critical` thống nhất PHP↔JS↔Java↔SQL.
3. **Type consistency:** Java `Integer` nullable ↔ SQL NULL ↔ PHP null ↔ JS `??''`; INSERT 12 binds khớp 12 placeholder (`siiii...` đã đếm: s,i,i,i,i,s,i,i,s,s,s,i = `siiiisiisssi` — kiểm tra lại khi implement: boss_key(s),phase(i),hp(i),damage(i),cap_damage(i),cap_percent(s),defense(i),critical(i),apply_mode(s),drop_mode(s),drops(s),revision(i)).

---

**Plan complete and saved to `docs/superpowers/plans/2026-10-08-boss-admin-extra-stats.md`. Hai cách triển khai:**

**1. Subagent-Driven (khuyến nghị nhưng hiện lỗi)** — subagent team-coder/team-integration đang lỗi kết nối API (3 lần 2026-10-08); nếu chọn, tôi thử lại từng task.

**2. Inline Execution** — tôi làm trực tiếp từng task trong session này, checkpoint sau mỗi task để bạn duyệt.

**Bạn chọn cách nào? (Và có duyệt phạm vi Giáp + Chí mạng, Né đòn để sau / làm luôn / bỏ không?)**
