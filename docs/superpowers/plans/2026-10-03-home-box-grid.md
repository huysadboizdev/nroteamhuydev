# Rương nhà dạng lưới giống hành trang — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rương nhà hiển thị lưới 5 cột, kéo/lướt dọc như hành trang hiện tại, chọn và lấy đúng đồ trong đủ 200 ô trên cả Game1/Game2.

**Architecture:** Thêm nhánh giao diện lưới riêng cho rương nhà vào hai `Panel.cs`. Dùng cùng một bộ công thức để vẽ, nhận tọa độ chọn và tính giới hạn cuộn; chỉ số lựa chọn là chỉ số thật trong `arrItemBox`. Giữ luồng gửi/nhận inventory 200 ô đã triển khai.

**Tech Stack:** Unity 2022.3.62f2, C#, giao diện `mGraphics`/`GameCanvas`; kiểm chứng bằng Roslyn, bộ kiểm thử giao thức hiện có và scene Unity tách riêng.

**Trạng thái:** Đã triển khai và kiểm chứng ngày 2026-10-04; build Windows64 thành công, 0 lỗi. Người dùng đã xác nhận: “giống như rương hành trang lưới là được mà”. Kết quả chi tiết ở mục 7 và báo cáo kiểm chứng.

---

## 1. Kết quả kiểm tra

Client nằm tại `C:/Users/Admin/Downloads/PRJ_2Tab_550K`.

Hai file `Assets/Scripts/Assembly-CSharp/Game1/Panel.cs` và `Game2/Panel.cs` giống nhau hoàn toàn sau chuẩn hóa tên namespace. Các điểm dưới đây áp dụng cho cả hai file, theo số dòng tại thời điểm kiểm tra:

| Vị trí | Hiện trạng | Việc cần sửa |
|---|---|---|
| `setTypeBox():1434`, `isTabBox():12223` | Rương dùng panel type 2, tab 0; màn hình rộng tạo thêm panel hành trang | Nhận diện đúng rương nhà và giữ hành trang bên cạnh |
| `paintInventory():6617`, phần túi `6741` | Hành trang hiện có lưới 5 cột, ô rộng 31/30, cao 21, cách nhau 3 đơn vị | Dùng kiểu ô, icon, màu chọn và hiển thị số lượng tương ứng cho rương |
| `paintBox():5775` | Rương vẫn vẽ mỗi món trên một dòng | Thêm vẽ lưới rương nhà |
| `paintBox222222():5641` | Chế độ còn lại chia 20 món/trang nhưng vẫn vẽ danh sách | Không dùng nhánh này để làm lưới |
| `setTabBox():4155` | Giới hạn cuộn tính theo số món nhân chiều cao dòng | Tính theo số hàng lưới |
| `updateKeyScrollView():3143` | Nhánh chọn tọa độ lưới chỉ nhận diện hành trang; rương tính ô theo dòng | Bổ sung hit-test rương cho cả giữ chạm và thả chạm |
| `GetInventorySelect_body():12213` | Ánh xạ có trừ dòng tiêu đề và cộng trang × 20 theo `ModFunc.isInventory` | Rương lưới dùng trực tiếp chỉ số ô, độc lập lựa chọn kiểu hành trang |
| `doFireBox():9974`, action `1000:10260` | Menu và lệnh lấy ra cùng dùng ánh xạ cũ | Chốt đúng ô lúc mở menu; kiểm tra lại trước khi gửi |
| `updateScroolMouse():2855` | Đã có cuộn bằng bánh xe; `GameCanvas.scrollMouse()` chuyển thao tác tới panel chính | Kiểm tra vùng cuộn, không làm ảnh hưởng panel hành trang phụ |

Ở nhánh danh sách chia trang, `setTabBox()` dùng `checkCurrentListLengthNew()` trả về toàn bộ số ô + 1, còn hàm vẽ dùng `checkCurrentListLength()` trả về tối đa 21 dòng. Hai công thức không đồng bộ, có thể tạo vùng cuộn trống. Nhánh rương lưới phải có công thức riêng thống nhất.

`ModFunc.isInventory` mặc định `true`: hành trang là lưới, nhưng rương vẫn là danh sách. Chỉ đổi cờ này không giải quyết yêu cầu.

Server `SRC/src/nro/models/services_func/UseItem.java:91` đọc chỉ số bằng `readUnsignedByte()`, loại 255 và kiểm tra phạm vi `itemsBox.size()` trước khi chuyển đồ. Chức năng hiển thị này dùng lại giao thức hiện tại, không cần sửa JAR hoặc migration SQL. Dữ liệu rương vẫn là 200 phần tử của `player.items_box`; vị trí ô trống phải được giữ nguyên.

## 2. Thiết kế đề xuất

Chọn **lưới 5 cột, cuộn dọc liên tục giống hành trang**. Với 200 ô có 40 hàng; người chơi kéo lên/xuống hoặc dùng bánh xe. Chạm một món mở thông tin và menu “Lấy ra” như hiện tại; ô trống vẫn có nền và không mở menu.

Không thêm trang ngang vì người dùng đã chọn giống hành trang hiện tại. Chia lưới thành trang ngang sẽ phải bổ sung cử chỉ và thanh đổi trang; chỉ vẽ icon trên danh sách cũ thì chưa tạo được lưới thực sự.

Phạm vi là rương nhà: `type == 2 && currentTabIndex == 0 && !isBoxClan && !Equals(GameCanvas.panel2)`. Rương bang dùng luồng hiện có. Tab hành trang, shop, đệ tử, giao dịch và bảng chuyển hóa giữ cơ chế riêng của chúng.

Đề xuất mặc định mở rương nhà là lưới, kể cả người chơi chuyển hành trang về chế độ danh sách bằng menu mod. Không thêm tùy chọn mới vì yêu cầu là dùng rương lưới.

## 3. Danh sách file

- Sửa: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game1/Panel.cs`.
- Sửa: `C:/Users/Admin/Downloads/PRJ_2Tab_550K/Assets/Scripts/Assembly-CSharp/Game2/Panel.cs`.
- Tạo bộ kiểm thử: `.analysis/home-box-grid/HomeBoxGridTest.cs` và `.analysis/home-box-grid/test_grid.py` trong workspace server, dùng compiler/reference như `.analysis/inventory200/test_client.py`.
- Tạo probe Unity: `.analysis/home-box-grid/HomeBoxGridVerification.cs`, chỉ cài trong project kiểm chứng tách riêng.
- Cập nhật báo cáo: `docs/source-analysis/home-box-grid-validation.md`.
- Xuất client và source patch mới trong `docs/releases`, không ghi đè ZIP inventory200 hiện có.

Không sửa `ModFunc`, `Service`, SQL, server hoặc giao thức nếu kiểm thử không phát hiện vấn đề trực tiếp liên quan.

## 4. Các bước triển khai

### Task 1: Sao lưu và khóa hành vi mong muốn

- [x] Sao lưu hai `Panel.cs` vào `.analysis/home-box-grid/source-backup` với đường dẫn gốc và SHA-256; giữ bản client inventory200 để khôi phục.
- [x] Dùng project Unity kiểm chứng tách riêng; không chạy thêm Unity trên project đang mở.
- [x] Viết kiểm thử hình học và ánh xạ cho cả Game1/Game2, gọi các helper qua reflection; chạy trước sửa để xác nhận thiếu helper và hành vi cũ không ánh xạ lưới.

Các giá trị kiểm thử bắt buộc:

```csharp
// Mỗi namespace chạy cùng tập dữ liệu; dùng AssertEqual thực hiện throw khi sai.
int[] slots = { 0, 4, 5, 19, 20, 127, 128, 199 };
foreach (int slot in slots)
{
    int col = slot % 5;
    int row = slot / 5;
    int x = 2 + col * 34 + 15;
    int y = 2 + row * 24 + 10 - scroll;
    // Với viewport đặt sao cho y nằm trong vùng hiển thị:
    // HitTest(x, y, scroll, 200) phải trả đúng slot.
}
// Click khe 3 đơn vị, x âm, y âm, ngoài viewport hoặc slot >= count: -1.
// count 0/1/4/5/6/199/200: ceil(count / 5) lần lượt 0/1/1/1/2/40/40.
```

### Task 2: Một bộ công thức cho rương

- [x] Thêm helper riêng vào mỗi `Panel.cs`, giữ hai bản đồng nhất ngoài namespace. Không thay helper ánh xạ dùng chung cho các giao diện khác.

```csharp
private const int HomeBoxColumns = 5;
private const int HomeBoxCellHeight = 21;
private const int HomeBoxGap = 3;
private const int HomeBoxTop = 2;
private const int HomeBoxBottom = 2;

private bool IsHomeBoxGrid()
{
    return type == 2 && currentTabIndex == 0 && !isBoxClan
        && !Equals(GameCanvas.panel2);
}

private int HomeBoxCellWidth() { return position == 0 ? 31 : 30; }

private int HomeBoxContentHeight(int count)
{
    int rows = (count + HomeBoxColumns - 1) / HomeBoxColumns;
    return rows == 0 ? 0 : HomeBoxTop
        + rows * (HomeBoxCellHeight + HomeBoxGap) - HomeBoxGap + HomeBoxBottom;
}

private int HomeBoxHitTest(int px, int py, int scrollY, int count)
{
    if (px < xScroll || px >= xScroll + wScroll
        || py < yScroll || py >= yScroll + hScroll) return -1;
    int x = px - xScroll - 2;
    int y = py - yScroll + scrollY - HomeBoxTop;
    if (x < 0 || y < 0) return -1;
    int width = HomeBoxCellWidth();
    int strideX = width + HomeBoxGap;
    int strideY = HomeBoxCellHeight + HomeBoxGap;
    int col = x / strideX;
    int row = y / strideY;
    if (col >= HomeBoxColumns || x % strideX >= width
        || y % strideY >= HomeBoxCellHeight) return -1;
    int index = row * HomeBoxColumns + col;
    return index < count ? index : -1;
}
```

- [x] Đầu `setTabBox()`, nếu `IsHomeBoxGrid()`, đặt `currentListLength = arrItemBox.Length`, `ITEM_HEIGHT = 24`, `newSelected = 0`, `size_tab = 0`, `cmyLim = Mathf.Max(0, HomeBoxContentHeight(currentListLength) - hScroll)`; clamp `cmyLast[currentTabIndex]` vào `[0,cmyLim]`, gán cả `cmy` và `cmtoY`, khởi tạo chọn `-1` trên touch hoặc `0` nếu có ô trên PC rồi return.
- [x] Kiểm thử chiều cao 200 ô là 961 đơn vị; giới hạn cuộn bằng `max(0,961-hScroll)`. Ô 199 nằm hàng 39/cột 4 và hiện đầy đủ khi cuộn xuống đáy.

### Task 3: Vẽ rương lưới

- [x] Thêm `paintHomeBoxGrid(mGraphics g)`; đầu `paintBox()` gọi nó rồi return nếu `IsHomeBoxGrid()`.
- [x] Vẽ mỗi ô ở `xScroll+2+(i%5)*(width+3)`, `yScroll+2+(i/5)*24-cmy`. Cắt theo `xScroll,yScroll,wScroll,hScroll`, chỉ vẽ hàng giao viewport; không nén ô trống hoặc sắp lại mảng.
- [x] Dùng nền ô và viền như phần túi trong `paintInventory()`: nền chọn `16318283`, nền thường `11837316`, viền trắng alpha 0.5; icon `SmallImage.drawSmallImage`, anchor 3 ở tâm ô.
- [x] Số lượng > 1 dùng `ItemOptionWire.FormatGridQuantity(item.quantity)` và font vàng như túi. Menu thông tin giữ các option, cấp, sao của món; không nhét toàn bộ dòng option vào ô nhỏ.
- [x] Khôi phục transform/clip đúng quy ước panel trước vẽ mũi tên cuộn; kiểm tra menu/tooltip không bị clip theo lưới.

### Task 4: Chọn ô, kéo, bánh xe và phím

- [x] Nhánh rương lưới chạy trước điều kiện `!ModFunc.isInventory` trong `updateKeyScrollView()` để không lọt vào xử lý danh sách cũ.
- [x] Ở cả nhánh giữ chạm và nhánh thả chạm, dùng `HomeBoxHitTest(GameCanvas.px,GameCanvas.py,cmy,arrItemBox.Length)`. Dùng vị trí đang vẽ `cmy`, không dùng đích cuộn `cmtoY` đang còn nội suy.
- [x] Khi kéo vượt ngưỡng cử chỉ của client, hủy `selected`, `currItem`, `waitToPerform` cho thao tác đó; kết thúc kéo chỉ cuộn, không mở menu lấy đồ. Nhấn giữ chỉ mở menu khi không có chuyển động kéo.
- [x] Phím lên/xuống chuyển ±5 ô; trái/phải chuyển ±1 trong cùng hàng, chặn ở biên hàng/mảng. Cuộn để hàng `selected/5` nằm trong viewport; không dùng công thức `selected*ITEM_HEIGHT`.
- [x] Bánh xe cuộn dọc vùng rương và clamp theo `cmyLim`; kiểm tra px/py nằm trong vùng panel đang nhận thao tác. Giữ thao tác cho panel hành trang bên cạnh theo cơ chế focus hiện tại.
- [x] Reset trạng thái kéo, menu chờ và clamp selection khi đóng/mở rương hoặc đổi tab. Rương và túi có vị trí cuộn riêng theo `currentTabIndex`; hai namespace không chia sẻ trạng thái.

### Task 5: Giữ đúng món khi lấy ra

- [x] Đầu nhánh rương trong `doFireBox()` dùng `index = IsHomeBoxGrid() ? selected : GetInventorySelect_body(selected,newSelected)`; kiểm tra `0 <= index < arrItemBox.Length` trước khi truy cập. Ô trống không tạo menu.
- [x] Khi tạo Command action 1000 cho rương lưới, lưu chỉ số và tham chiếu món của thời điểm mở menu trong tham số Command riêng; nhánh rương bang giữ tham số hiện tại.
- [x] Trước gửi action 1000, chỉ chấp nhận rương nhà còn mở, index hợp lệ và `ReferenceEquals(arrItemBox[index], itemAtMenuOpen)`. Nếu món đã thay/biến mất, đóng menu và yêu cầu chọn lại. Không suy ra index từ `selected` mới của một tab khác.
- [x] Gửi `Service.gI().getItem(BOX_BAG,index)` với index int. Không cộng offset trang × 20, không trừ dòng tiêu đề, không cast sbyte.
- [x] Kiểm thử gói gửi có chỉ số 127/128/199 đúng dạng unsigned; kéo và chạm ô trống không gửi gói chuyển đồ.

### Task 6: Kiểm chứng và xuất client

- [x] Chạy test helper trên cả hai namespace, gồm vị trí ô, khe giữa ô, mép viewport, scroll thực khác scroll đích, mảng ngắn và hàng cuối không đủ 5 ô.
- [x] Chạy `python .analysis/inventory200/compile_client.py`; mong đợi `CLIENT_FULL_COMPILE_EXIT=0`.
- [x] Chạy `python .analysis/inventory200/test_client.py`; mong đợi `CLIENT_PROTOCOL_TEST_EXIT=0`, giữ kiểm chứng collector/giao thức/index 200 ô.
- [x] Chạy Unity scene kiểm chứng với các fixture Java hiện có `box-0.bin`, `box-34.bin`, `box-200.bin`; mở rương thật trong scene, vẽ và thao tác pointer, không chỉ kiểm tra biến `cmyLim`.
- [x] Với cả Game1/Game2, kiểm tra màn hình hẹp chỉ một panel và màn hình rộng hai panel, kéo tới ô 199, bấm/giữ chọn, menu lấy ra, cập nhật ô sau khi nhận -36, tab hành trang, đóng/mở, bánh xe và phím. Chụp ảnh đầu/giữa/cuối lưới làm bằng chứng.
- [x] Kiểm tra `ModFunc.isInventory` cả true/false: rương nhà luôn lưới; hành trang và rương bang giữ cách hoạt động tương ứng. Không tự gộp phạm vi rương bang vào thay đổi.
- [x] Build Windows64 bằng Unity trong snapshot, yêu cầu 0 lỗi. Xuất ZIP mới kèm toàn bộ DLL/Data cần thiết và SHA-256; source patch chứa hai Panel và metadata cần thiết.
- [x] Ghi rõ thử nghiệm scene/packet và thử nghiệm đăng nhập người chơi thực tế là hai mức kiểm chứng khác nhau. Nếu chưa có phiên người chơi thực, báo chưa kiểm tra thủ công trên server.

## 5. Tiêu chí nghiệm thu

- [x] Rương mở ra thành 5 cột, 40 hàng với 200 ô; kéo tới đáy thấy và chọn được ô cuối.
- [x] Chạm ô nào mở thông tin đúng món ở ô đó; ô trống/khe giữa ô không chọn nhầm.
- [x] Kéo không kích hoạt menu hoặc chuyển đồ; lấy ra gửi đúng index ngay cả với ô 128–199.
- [x] Hiển thị số lượng như hành trang; option/cấp/sao của món không bị thay đổi.
- [x] Hai tab độc lập; panel hành trang cạnh rương, rương bang và các giao diện khác không bị đổi mapping.
- [x] Build thành công và có bằng chứng thao tác/vẽ trong Unity. Dữ liệu 200 ô và luồng SQL hiện có không cần migration.

## 6. Khôi phục

Nếu giao diện gặp lỗi, khôi phục hai Panel từ bản sao có SHA-256 rồi dùng lại client inventory200 đã xuất trước đó. Vì bản sửa chỉ thay client, không thay JAR hay dữ liệu SQL, việc khôi phục không cần hạ dung lượng rương hoặc phục hồi dữ liệu nhân vật.


## 7. Kết quả triển khai — 2026-10-04

Đã hoàn thành client, kiểm chứng và build Windows64 (0 lỗi). Báo cáo: `docs/source-analysis/home-box-grid-validation.md`.

Điều chỉnh sau kiểm chứng: nhấn giữ mở menu khi thả tại chỗ, vì mở khi còn chạm làm GameCanvas chuyển input sang Menu và không thể hủy khi kéo tiếp. Menu hết hiệu lực đóng trực tiếp để tránh hàm đóng thực thi lại lệnh. Panel phụ trả cử chỉ kéo đang thuộc rương về rương cho đến khi thả.

Các helper có tên triển khai HomeBoxGridHeight/HomeBoxGridHitTest/HomeBoxHit; giữ công thức chung và ánh xạ ô theo kế hoạch. Test integration chạy trong Unity vì GameCanvas khởi tạo texture native không chạy được trong .NET thuần.

Chưa đăng nhập người chơi để thử thủ công trên server thật; kiểm chứng bằng scene Unity, tài nguyên và packet fixture server, capture gói gửi. Không thay SQL/JAR.
