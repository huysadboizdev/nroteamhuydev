# Kế hoạch triển khai Nâng SKH VIP

> Đã triển khai source và thay SRC/20.jar ngày 2026-10-02, JAR cũ nằm trong SRC/jar-backup. Java compile và 28 lượt kiểm thử trên JAR mới đạt; chưa chạy server hoặc kiểm thử client thật. Chi tiết nghiệm thu: [skh-vip.md](../../source-analysis/skh-vip.md).

**Mục tiêu:** Đổi 1–5 món hủy diệt cùng số lượng phôi thần linh thành SKH, thu 1 tỉ vàng/cặp, thành công 50%; xịt giữ đồ chính và mất phôi; cấp 9–12 có tổng 5% trong đầu ra thành công.

**Thiết kế:** [Thiết kế và quy tắc đã chốt](../specs/2026-10-02-nang-skh-vip-design.md). Dùng catalog capsule chung nhưng giữ nguyên hồ sơ random của capsule cũ; VIP có danh sách chuẩn hóa đủ 12 cấp và tỷ lệ riêng. Chuẩn bị kết quả trước rồi thay slot đồ chính thành công, không cần thêm ô trống.

**Công nghệ:** Java 17, combine packet -81, test offline kiểu `main` đang dùng trong repo. Workspace không có Git repository nên không có bước commit tại đây. Không sửa SQL hoặc thay JAR đang chạy trong bước triển khai source.

## 1. Quy tắc nghiệp vụ để nghiệm thu

| Nội dung | Quy tắc |
| --- | --- |
| Số món chính | 1–5 món hủy diệt |
| Số phôi | Bằng số đồ chính; thần linh 555–567 |
| Thứ tự | N món hủy diệt đầu tiên, N món thần tiếp theo |
| Phôi | Tùy hành tinh, tùy loại so với đồ chính; không trùng loại với phôi khác |
| Loại/hành tinh kết quả | Theo đồ hủy diệt; riêng nhẫn chung 656 cho rada có set theo hành tinh người chơi |
| Phí mỗi cặp | 1.000.000.000 vàng, kể cả cặp xịt |
| Thành công | 50%, tiêu hao đồ chính + phôi, nhận một món SKH |
| Xịt | 50%, tiêu hao phôi, giữ nguyên đồ chính |
| Cấp trang bị đầu ra | Random cấp 1–12; 95% cho cấp 1–8, 5% cho cấp 9–12 trong món thành công |
| Loại set | Random đều 5 bộ của hành tinh, mỗi bộ 20% |
| Chỉ số | Max của đúng template được random, theo shop làng; không kế thừa nguyên liệu |
| Ràng buộc lượt 5 cặp | Có thể nhận 0–5 món; không có sàn 3 món như dự thảo nhánh Thường |

| Số cặp | Tổng nguyên liệu | Tổng phí |
| --- | --- | --- |
| 1 | 2 | 1 tỉ vàng |
| 2 | 4 | 2 tỉ vàng |
| 3 | 6 | 3 tỉ vàng |
| 4 | 8 | 4 tỉ vàng |
| 5 | 10 | 5 tỉ vàng |

Ví dụ: găng hủy diệt Trái Đất 657 + áo thần Namek 557 hợp lệ. Thành công ra găng SKH Trái Đất ở một cấp ngẫu nhiên; xịt giữ nguyên găng 657, mất áo 557. Với ba món hủy diệt đầu danh sách, ba phôi sau danh sách lần lượt phục vụ cặp 1/2/3; không ghép theo type phôi.

### ID nguyên liệu theo SQL

| Hành tinh | Áo hủy diệt | Quần hủy diệt | Găng hủy diệt | Giày hủy diệt | Nhẫn |
| --- | --- | --- | --- | --- | --- |
| Trái Đất | 650 | 651 | 657 | 658 | 656 dùng chung |
| Namek | 652 | 653 | 659 | 660 | 656 dùng chung |
| Xayda | 654 | 655 | 661 | 662 | 656 dùng chung |

| Hành tinh | Áo thần | Quần thần | Găng thần | Giày thần | Nhẫn |
| --- | --- | --- | --- | --- | --- |
| Trái Đất | 555 | 556 | 562 | 563 | 561 dùng chung |
| Namek | 557 | 558 | 564 | 565 | 561 dùng chung |
| Xayda | 559 | 560 | 566 | 567 | 561 dùng chung |

Không cấm đồ chính khác hành tinh nhân vật hoặc trùng loại với đồ chính khác. Quy tắc cấm trùng loại người dùng đã chốt là cho phôi thần. Các điều kiện phụ như từ chối đồ hạn sử dụng/đã có SKH/quantity khác 1 là đề xuất trong thiết kế, cần thể hiện rõ khi xem xét kế hoạch.

## 2. Các điểm source đã kiểm tra trước triển khai

- `SRC/src/nro/models/services_func/UseItem.java:2691`: CapsuleKichHoat mở menu chọn loại món.
- `SRC/src/nro/models/npc/NpcFactory.java:361`: CAPSULE_KICH_HOAT gọi OpenSKH với item 1655 và lựa chọn 0–4.
- `SRC/src/nro/models/services/ItemService.java:130`: OpenSKH chứa các mảng template và option set; random theo hành tinh nhân vật.
- `ItemService.createItemSKH`: tạo đồ mới, thêm option shop, option tên/hiệu ứng SKH và 30. `getListOptionItemShop` trả tham chiếu ItemOption dùng chung, không dùng trực tiếp để tăng chỉ số.
- `SRC/src/nro/models/item/ActivationSet.java`: bảng ID tên/hiệu ứng đã đồng bộ SQL; không tạo mapping riêng cho VIP.
- `SRC/src/nro/models/npc_list/BaHatMit.java`: map 5 chưa có menu Nâng SKH; các menu hiện có phải giữ nguyên index.
- `SRC/src/nro/models/combine/CombineService.java:82`: showInfoCombine hiện đọc slot chưa có kiểm tra bounds/duplicate trước khi lấy item. Cần thêm kiểm tra riêng cho VIP trước đoạn thu thập item.
- `SRC/src/nro/models/server/Controller.java:404`: packet -81 giữ thứ tự danh sách, chưa thấy giới hạn 5 món ở server; chưa có source client để xác nhận giao diện 10 món.
- `SRC/src/nro/models/combine/Combine.java`: goldCombine là int; không ghi tổng phí 5 tỉ vào field này.
- `SRC/src/nro/models/services/InventoryService.java`: addItemBag có thể trả false và copy/đặt quantity nguồn về 0; removeItem theo instance gọi dispose. Nhánh mới dùng thay slot đã được xác thực thay vì xóa trước rồi hy vọng addItemBag thành công.

## 3. Nhiệm vụ A — catalog và nguồn chỉ số tối đa

**Tạo:** `SRC/src/nro/models/item/SKHCapsuleCatalog.java`.

**Sửa:** `SRC/src/nro/models/services/ItemService.java`, giới hạn ở OpenSKH và các helper mới của VIP.

**Tạo test:** `SRC/test/nro/models/combine/NangSKHVipTest.java`, các chế độ `catalog`, `stats`.

- [x] Test hồ sơ capsule cũ có đúng 15 mảng gốc, giữ thứ tự, cả phần tử 174 lặp; option set giữ nguyên năm phần tử/hành tinh.
- [x] Test hồ sơ VIP có đúng 12 template khác nhau/nhóm, level 1–12, type đúng và gender đúng hoặc 3 với rada. Các mảng chính xác và option shop nằm trong [audit JSON](../../source-analysis/skh-vip-capsule-audit.json).
- [x] Chạy test và quan sát thất bại trước khi thêm catalog/helper.
- [x] Chuyển mảng gốc OpenSKH vào catalog; OpenSKH vẫn lấy hồ sơ capsule cũ, không đổi tỷ lệ, cách thu capsule hoặc phương thức OpenVeTangNgoc.
- [x] Tạo hồ sơ VIP chuẩn hóa: thêm 255 vào găng Trái Đất, 234 vào áo Namek, 258 vào găng Namek, thay phần lặp 174 bằng 175 trong quần Xayda. Sắp theo `ItemTemplate.level`, không theo độ lớn ID.
- [x] Thêm helper VIP tạo đồ max: chỉ xét shop tag BUNMA/DENDE/APPULE hoặc ID 1/2/3, cùng template ID; lấy max param của từng option nền và tạo ItemOption mới. Không sửa danh sách/option của shop.
- [x] Bắt buộc có đủ option nền đúng cấp: áo 47; quần 6/27; găng 0; giày 7/28; rada 14. Riêng quần/giày cấp 1 trong SQL chỉ có 6/7; không bắt buộc hoặc tự thêm hồi phục 27/28. Nếu thiếu dữ liệu bắt buộc, dừng lượt trước khi thu phí/tiêu hao.
- [x] Thêm đúng một option tên set, các option hiệu ứng từ ActivationSet và option 30; không thêm sao, reinforcement, HSD hoặc chỉ số nguyên liệu.
- [x] Test hai đồ tạo liên tiếp không chia sẻ ItemOption; thay một đồ không làm thay shop hoặc đồ còn lại. Chạy lại SKHOptionMappingTest.

Logic max param cho các option nền, áp dụng sau khi lọc đúng shop/template:

```java
java.util.Map<Integer, Integer> maxParams = new java.util.LinkedHashMap<>();
for (Item.ItemOption option : matchingVillageOptions) {
    maxParams.merge(option.optionTemplate.id, option.param, Math::max);
}
for (java.util.Map.Entry<Integer, Integer> option : maxParams.entrySet()) {
    result.itemOptions.add(new Item.ItemOption(option.getKey(), option.getValue()));
}
```

`matchingVillageOptions` là danh sách option nền của đúng template trong shop 1/2/3; `result` là item quantity=1 mới từ createItemSetKichHoat. Không lấy chỉ số từ một template khác cấp.

Ví dụ SQL: găng jean Calic 257 có option 0=2200; găng vàng Zealot 261 có 0=2150; găng lưỡng long 265 có 0=2250. Rada cấp 12 item 281 có option 14=12 ở cả ba shop. Các món cấp thấp vẫn lấy chỉ số của chính cấp thấp đã roll, không nhận chỉ số cấp 12.

## 4. Nhiệm vụ B — kiểm tra và chụp lựa chọn nguyên liệu

**Tạo:** `SRC/src/nro/models/combine/NangSKHVip.java` với `Selection`/`Pair` bất biến lưu slot và instance đồ chính/phôi.

**Sửa:** `SRC/src/nro/models/combine/Combine.java` để giữ preview VIP đang chờ và xóa preview khi clear/chuyển tab.

**Test:** chế độ `validation`.

- [x] Test chấp nhận N=1–5; găng 657 + áo thần 557; lượt phôi đủ năm loại, hành tinh bất kỳ; đồ chính trộn hành tinh; hai đồ chính cùng loại với hai phôi khác loại.
- [x] Test từ chối 0/1/3/11/12 item; thiếu phôi/thừa phôi; thần nằm trước nhóm hủy diệt; đồ không thuộc whitelist; hai phôi áo 555/557 dù khác hành tinh.
- [x] Test từ chối slot âm/quá chiều dài, slot lặp, cùng instance xuất hiện hai lần, item không ở túi, template rỗng hoặc số lượng bất thường.
- [x] Chạy test thất bại rồi viết validator: tổng phải chẵn trong 2–10, N=tổng/2, N item đầu thuộc 650–662, N item sau thuộc 555–567. Các phôi có type 0–4 không trùng nhau.
- [x] Chụp slot, instance và dữ liệu dùng để xác nhận; lúc thực hiện kiểm tra lại typeCombine, lựa chọn, membership, quantity và fingerprint item để không dùng preview cũ sau khi sửa/di chuyển đồ.
- [x] Quy tắc đề xuất từ chối HSD/đã có SKH dùng Item.isSKH đầy đủ 13 set và option 93; không dùng đoạn nhận diện chỉ 127–135 cũ.
- [x] Preview chỉ hiển thị thông tin; không roll thành công/cấp/set, không trừ tiền hoặc item.

Kiểm tra phôi không trùng bằng type:

```java
int seenTypes = 0;
for (Item fodder : fodders) {
    int type = fodder.template.type;
    if (type < 0 || type > 4 || (seenTypes & (1 << type)) != 0) {
        throw new IllegalArgumentException("Phôi thần không được trùng loại");
    }
    seenTypes |= 1 << type;
}
```

`fodders` chỉ chứa N item thần phía sau, sau khi đã xác thực template/membership; không áp dụng bitmask này cho đồ hủy diệt.

## 5. Nhiệm vụ C — random thành công, cấp đồ và set

**Sửa:** `NangSKHVip.java`, `SKHCapsuleCatalog.java`.

**Test:** chế độ `random`, dùng IntUnaryOperator để truyền roll xác định, không dựa vào test thống kê dễ dao động.

- [x] Test roll thành công 0/49 đạt, 50/99 xịt.
- [x] Test roll nhóm 0/4 thuộc VIP, 5/99 thuộc cấp 1–8; duyệt đủ index 0–3 và 0–7 để chứng minh mọi cấp đều có thể ra.
- [x] Test bộ set đủ năm lựa chọn từng hành tinh: Trái Đất 128/129/127/233/245; Namek 130/131/132/233/237; Xayda 133/135/134/233/241. Mỗi option tên có đúng các option hiệu ứng theo ActivationSet.
- [x] Chốt outputPlanet cho từng cặp: nhẫn hủy diệt 656 dùng player.gender, các món hủy diệt còn lại dùng main.template.gender. Chỉ nhận 0/1/2; từ chối giá trị khác trước khi thu phí/tiêu hao. Dùng cùng outputPlanet để chọn catalog và option set, không suy lại từ template rada gender=3 hoặc từ phôi. Lưu trong preview và kiểm tra lại khi xác nhận.
- [x] Test loại món/hành tinh đầu ra theo từng đồ chính, không theo phôi hoặc hành tinh nhân vật: người chơi Trái Đất + găng hủy diệt Namek 659 phải ra găng Namek; người chơi Xayda + găng hủy diệt Trái Đất 657 phải ra găng Trái Đất.
- [x] Test rada từ nhẫn 656 cho player.gender 0/1/2, ép lần lượt đủ năm lựa chọn set của mỗi hành tinh (15 trường hợp). Kiểm tra template đầu ra type=4 và option tên/hiệu ứng đúng hành tinh; không chỉ thử set Gohan 233 vì set này dùng chung và có thể che lỗi hành tinh. Phôi thần khác hành tinh không đổi kết quả.
- [x] Test nhẫn 656 với player.gender=-1/3 bị từ chối trước khi thu phí/tiêu hao; không mặc định thành Xayda. Test lượt có cả nhẫn chung và đồ chính khác hành tinh người chơi để bảo đảm từng cặp dùng đúng outputPlanet riêng. Test hành tinh người chơi thay đổi sau preview khiến lượt có nhẫn bị từ chối và yêu cầu chọn lại.
- [x] Roll độc lập từng cặp; xịt không roll template/set. Không gọi randomSKHId vì helper đó chỉ random ba set cơ bản.
- [x] Chạy test thất bại rồi triển khai các helper dưới đây và chạy lại.

Các mảng `normalIds` và `vipIds` lấy từ catalog đã xác thực: level 1–8 và 9–12 của đúng hành tinh/type.

```java
static boolean rollSuccess(java.util.function.IntUnaryOperator random) {
    return random.applyAsInt(100) < 50;
}

static int rollTemplate(int[] normalIds, int[] vipIds,
        java.util.function.IntUnaryOperator random) {
    int[] pool = random.applyAsInt(100) < 5 ? vipIds : normalIds;
    return pool[random.applyAsInt(pool.length)];
}

static int rollSet(int[] setIds, java.util.function.IntUnaryOperator random) {
    return setIds[random.applyAsInt(setIds.length)];
}
```

Production truyền `Util::nextInt`. Chỉ test boundary/helper và kết quả gameplay cần thiết, không thêm test đòi số lần roll ngẫu nhiên bằng một magic number ngoài hợp đồng.

## 6. Nhiệm vụ D — phí và thay đổi hành trang một lần

**Sửa:** `NangSKHVip.java`.

**Test:** `fees`, `success`, `failure`, `mixed`, `inventory`, `replay`.

- [x] Test phí 1/2/3/4/5 cặp bằng 1/2/3/4/5 tỉ; số dư thiếu một vàng bị từ chối và không mất bất kỳ item nào. N=5 với gold=5 tỉ được phép thực hiện.
- [x] Test toàn thành công: N phôi biến mất, N đồ chính được thay bằng đúng N output, vàng trừ đủ một lần.
- [x] Test toàn xịt: N phôi biến mất, N đồ chính vẫn chính instance ban đầu, mọi option/sao/cấp nâng giữ nguyên, vàng trừ đủ một lần.
- [x] Test hỗn hợp năm cặp với roll 49/50/0/99/10: 3 output, 2 đồ chính giữ nguyên, mất 5 phôi, tốn 5 tỉ. Không bắt tối thiểu 3 kết quả cho các chuỗi roll khác.
- [x] Test túi đầy vẫn đổi được bằng slot đồ chính/phôi đã chọn; không yêu cầu thêm ô trống. Không đè slot của món không chọn.
- [x] Test nguyên liệu bị đổi/chuyển/số dư giảm sau preview: kiểm tra lại và từ chối không thu phí/tiêu hao.
- [x] Test builder gặp dữ liệu shop thiếu: toàn lượt không bị mutation, không mất vàng hoặc nguyên liệu.
- [x] Test bấm xác nhận hai lần, hai thread cùng submit và thao tác ngay trong cooldown: chỉ một lần chuyển đổi/thu phí.
- [x] Ghi nhận các ca red → green thực tế: thiếu lớp VIP/catalog; quần/giày cấp 1 thiếu hồi phục hợp lệ; đồ nhận thêm ở ô khác bị ghi đè. Các mode còn lại kiểm chứng hành vi và hồi quy.
- [x] Dùng `synchronized(player)` cùng chuẩn pha lê hiện tại; bảo vệ cập nhật vàng/slot bằng inventory lock khi ghi. Rà tương tác với giao dịch để không coi lock riêng VIP là bảo đảm mọi luồng sửa túi khác đều bị khóa.
- [x] Trong vùng bảo vệ: kiểm tra lại toàn bộ Selection/fee; tạo tất cả output cho cặp thành công trước; chuẩn bị danh sách slot mới; sau đó ghi vàng và slot. Không gửi packet hoặc gọi removeItem/dispose giữa các bước ghi.
- [x] Thành công thay slot đồ chính bằng output; xịt không thay slot đồ chính. Mọi slot phôi được đặt Item rỗng. Dọn Selection/itemsCombine, đặt cooldown và xóa preview để xác nhận cũ không dùng lại.
- [x] Sau commit bộ nhớ: gửi tiền, hành trang, hiệu ứng và thông báo số món đạt/xịt. Dùng reOpenItemCombine khi danh sách đã dọn, không gửi số lượng item không khớp slot thực.

Phí phải là long, độc lập `Combine.goldCombine`:

```java
static long totalFee(int pairCount) {
    if (pairCount < 1 || pairCount > 5) {
        throw new IllegalArgumentException("Chỉ nhận từ 1 đến 5 cặp");
    }
    return 1_000_000_000L * pairCount;
}
```

Mỗi cặp giữ thông tin slot/instance đồ chính và phôi. Khung ghi slot sau khi đã kiểm tra và tạo đủ output:

```java
long remainingGold = player.inventory.gold - totalFee(selection.pairs().size());
Item[] emptyFodders = new Item[selection.pairs().size()];
for (int i = 0; i < emptyFodders.length; i++) emptyFodders[i] = new Item();
for (int i = 0; i < selection.pairs().size(); i++) {
    Pair pair = selection.pairs().get(i);
    Item output = outputs.get(i); // null chỉ có nghĩa là cặp đã roll xịt.
    if (output != null) player.inventory.itemsBag.set(pair.mainSlot(), output);
    player.inventory.itemsBag.set(pair.fodderSlot(), emptyFodders[i]);
}
player.inventory.gold = remainingGold;
```

`selection` là Selection đã xác thực lại; `outputs` có đúng N phần tử được tạo trước mọi mutation; lỗi dữ liệu không được chuyển thành null/xịt để vẫn thu phí. Khung này là commit bộ nhớ của luồng hiện tại, không tuyên bố thêm transaction ACID cho việc lưu database toàn server.

## 7. Nhiệm vụ E — menu Bà Hạt Mít và hướng dẫn

**Sửa:** `SRC/src/nro/models/npc_list/BaHatMit.java`, `SRC/src/nro/models/consts/ConstNpc.java`, `SRC/src/nro/models/combine/CombineService.java`.

- [x] Thêm mục Nâng SKH ở cuối menu gốc map 5, sau Tái tạo Capsule; không đổi lựa chọn 0–4 hiện tại.
- [x] Thêm submenu riêng bằng `ConstNpc.MENU_NANG_SKH`; bổ sung Nâng SKH VIP và Đóng. Nếu trình bày mục Thường theo thiết kế trước thì chỉ nối vào lớp Thường khi đã được triển khai; source hiện chưa có lớp này, không gán VIP vào nút Thường.
- [x] Thêm combine type `NANG_SKH_VIP` riêng, giá trị 998 chưa được khai báo trong CombineService hiện tại; kiểm tra lại collision khi triển khai.
- [x] Route showInfoCombine/startCombine sang lớp VIP; không gọi startCombineVip(player,10/100), vì tên hàm hiện tại là batch pha lê, không phải feature SKH VIP.
- [x] Trong showInfoCombine kiểm tra riêng VIP: độ dài 2–10, slot đúng bounds, không lặp, trước khi gọi itemsBag.get(index). Sau đó mở preview từ Selection hợp lệ.
- [x] Text tab: “Chọn 1–5 món hủy diệt trước, rồi chọn cùng số món thần phía sau. Đồ thần không trùng loại, được khác hành tinh.”
- [x] Preview liệt kê đồ chính/phôi theo cặp, hành tinh/loại kết quả, tổng phí long và 50% thành công từng cặp. Nêu rõ xịt mất thần, giữ hủy diệt; thành công đổi cả hai lấy đồ mới; phí không hoàn.
- [x] Preview nêu cấp 9–12 tổng 5% trong đầu ra thành công; tránh ghi “5% thành công đồ VIP” vì một cặp còn qua bước 50% trước đó.
- [x] Chỉ có nút “Nâng SKH VIP” và “Từ chối”. Handler chỉ xử lý select=0; select=1 phải đóng menu, không trở thành thử x10.
- [x] Test route/menu confirm/cancel bằng fixture NPC/network offline hiện có; xác nhận hủy không mất đồ hoặc vàng.

## 8. Nhiệm vụ F — kiểm chứng và tài liệu

- [x] Bổ sung NangSKHVipTest vào `.analysis/verify_gold_economy.py` với các mode validation, random, fees, success, failure, mixed, inventory, replay, menu; SKHVipCatalogTest cho catalog/stats; SKHVipSqlTest kiểm tra 900 đầu ra từ SQL thật.
- [x] Chạy `python .analysis/verify_gold_economy.py`: javac compile toàn bộ source/test Java 17; test VIP và 17 lượt hồi quy hiện có phải đạt. Khi test lỗi, phân biệt lỗi fixture và lỗi gameplay trước khi sửa.
- [x] Kiểm tra source/data bằng audit JSON: 15 nhóm × 12 cấp; các ID bổ sung có template/type/gender/option shop đúng; capsule cũ giữ mảng và phân bố riêng.
- [ ] Kiểm tra client thật: đặt lần lượt 2/4/6/8/10 món trong tab, thứ tự được giữ; xác nhận kết quả vừa thành công vừa xịt; đối chiếu tiền/đồ sau đăng nhập lại trên môi trường test.
- [ ] Nếu client không hỗ trợ 10 món, chuyển thiết kế UI sang hai bước lựa chọn có Selection lưu tạm và kiểm tra lại khi xác nhận. Giữ nguyên một lượt tối đa 5 cặp, thu phí một lần và các xác suất đã chốt; không âm thầm giảm tính năng xuống 2 cặp.
- [x] Review diff/luồng xử lý để tìm mất đồ, nhân đôi đồ/vàng, lệch hành tinh và chia sẻ ItemOption. Ghi kết quả thật vào docs/source-analysis; không ghi đã test client nếu mới chỉ test offline.
- [x] Báo rõ phiên bản source đã hoàn tất, test đã chạy và trạng thái JAR/server. Việc cập nhật JAR hoặc restart là bước riêng khi được yêu cầu.

## Tự rà kế hoạch

- Các quy tắc người dùng chốt có nhiệm vụ tương ứng: số lượng/thứ tự/phôi khác hành tinh ở B; phí và xịt ở D; random đủ cấp, chỉ số max và rare 5% ở A/C; menu ở E; kiểm chứng ở F.
- Giá trị 50% thành công, 5% rare và 1 tỉ/cặp đều tách riêng, tránh nhập nhằng xác suất và overflow int.
- Hủy diệt quyết định loại/hành tinh; phôi thần tùy loại/hành tinh nhưng không trùng loại; riêng rada từ nhẫn chung dùng hành tinh người chơi là các quy tắc đã chốt. Có kiểm thử riêng cho cả ba hành tinh và lượt trộn nhiều hành tinh.
- Những lựa chọn bổ sung được ghi là đề xuất: từ chối HSD/đã có SKH, chuẩn hóa riêng catalog VIP và UI dự phòng khi client không nhận 10 món. Không đánh dấu chúng thành yêu cầu đã được người dùng nêu.
- Dự thảo SKH Thường cũ có quy tắc khác và ghi chú lỗi đã được sửa; không sao chép chính sách của nhánh Thường sang VIP.
