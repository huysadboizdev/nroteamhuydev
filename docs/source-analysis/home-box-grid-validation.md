# Kiểm chứng rương nhà dạng lưới — 2026-10-04

Đã sửa đồng bộ `Assets/Scripts/Assembly-CSharp/Game1/Panel.cs` và `Game2/Panel.cs` trong client `C:/Users/Admin/Downloads/PRJ_2Tab_550K`.

Rương nhà dùng lưới 5 cột, 200 ô/40 hàng, cuộn dọc. Các ô trống giữ nguyên vị trí; icon, số lượng và nền chọn dùng kiểu hành trang. Chiều cao nội dung là 961; giới hạn cuộn là `max(0, 961 - hScroll)`. Rương luôn dùng lưới dù hành trang đang ở chế độ danh sách.

## Thao tác và giới hạn phạm vi

- Hit-test dựa trên vị trí đang vẽ `cmy`; loại khe giữa ô, mép viewport và chỉ số ngoài mảng. Phím lên/xuống đi 5 ô; trái/phải đi 1 ô trong cùng hàng.
- Kéo hủy chọn và thao tác chờ. Nhấn giữ đánh dấu lựa chọn và mở menu khi thả tại chỗ, để người chơi vẫn có thể kéo sau khi giữ mà không mở nhầm menu.
- Khi kéo từ rương sang panel hành trang, panel rương giữ quyền xử lý cử chỉ cho tới khi thả; focus vẫn theo cơ chế hiện có. Bánh xe chỉ tác động vào viewport dưới con trỏ.
- Menu lưu chỉ số ô, tham chiếu món và phiên mở panel. Trước khi lấy ra, client kiểm tra rương còn mở, ô còn hợp lệ và món vẫn là cùng đối tượng. Menu hết hiệu lực được đóng trực tiếp, không gọi hàm đóng có thể thực thi lại lệnh.
- Chỉ số gửi đi là `int`, giữ đúng các ô 127/128/199; không trừ dòng tiêu đề hoặc cộng trang.
- Rương bang, tab hành trang và panel phụ được loại khỏi nhánh rương nhà. Không thay SQL, giao thức, server hoặc JAR.

## Kiểm chứng

- Đã sao lưu hai Panel gốc, kèm đường dẫn và SHA-256: `.analysis/home-box-grid/source-backup/20261004-063251-7722/manifest.json`.
- Test trước sửa thất bại do thiếu helper lưới. Test hồi quy giao thao tác giữa hai panel cũng thất bại đúng lỗi trước sửa: `.analysis/home-box-grid/unity-cross-panel-red.log`.
- Roslyn biên dịch toàn bộ client: `CLIENT_FULL_COMPILE_EXIT=0`. Các cảnh báo có sẵn được ghi trong `.analysis/inventory200/client-compile/compile.log`.
- Test hình học/hit-test/di chuyển của cả hai namespace: `HOME_GRID_TEST_EXIT=0`; gồm ô 0/4/5/19/20/127/128/199, rộng ô 30/31, khe, mép viewport, padding âm, mảng ngắn và hàng cuối chưa đủ 5 ô.
- Bộ kiểm thử giao thức inventory200 hiện có: `CLIENT_PROTOCOL_TEST_EXIT=0`; giữ thương lượng, plaintext/XOR, frame hơn 64 KiB và byte chỉ số unsigned.
- Unity 2022.3.62f2 chạy scene trong snapshot riêng `.analysis/client-startup/unity-verification`; project client đang mở không bị chạy thêm Unity.
- Scene chạy `HomeBoxGridTest.RunUnityTests()` với menu touch có lệnh thật, capture session gửi gói, menu thay món/đổi tab/đổi phiên, kéo/nhấn giữ, phím, bánh xe và public `panel2.updateKey()` cho thao tác vượt panel.
- Đọc các gói Java `box-0.bin`, `box-34.bin`, `box-200.bin`: 24 tổ hợp của hai namespace × rộng 320/600 × hành trang list/grid × ba fixture. Vẽ đầy đủ `panel.paint()` và panel hành trang bên cạnh, ở đầu/giữa/cuối: 72 mẫu vẽ. Pointer chọn được ô 199; kiểm tra scope rương bang/panel phụ, cuộn hai viewport độc lập và gói cập nhật -35/-36 ở ô 199.
- Scene nạp dữ liệu part và icon từ tài nguyên server cho nhân vật/món thử, không đăng nhập tài khoản người chơi. 12 ảnh đầu/giữa/cuối của hai layout và hai tab nằm trong `.analysis/home-box-grid/screenshots`.
- Rà soát độc lập về tuân thủ kế hoạch và chất lượng mã: không còn phát hiện chặn phát hành. Hai Panel giống nhau sau chuẩn hóa namespace.

Log scene cuối: `.analysis/home-box-grid/unity-runtime.log`. Probe và runner được lưu tại `.analysis/home-box-grid/HomeBoxGridVerification.cs`, `HomeBoxGridTest.cs`, `test_grid.py`; probe chỉ nằm trong Editor của snapshot kiểm chứng, không cài vào project client gốc.

Đây là kiểm chứng scene/gói tin và input được điều khiển trong probe. **Chưa đăng nhập tài khoản người chơi rồi kiểm tra thủ công trên server thật.**

## Bản phát hành và khôi phục

Client mới: `docs/releases/nro-client-home-box-grid-20261004.zip`. Giải nén toàn bộ, chạy `NROHomeBox.exe` cùng thư mục Data và các DLL đi kèm.

Source patch chỉ chứa hai Panel và metadata: `docs/releases/home-box-grid-source-patch-20261004.zip`. SHA-256 được lưu ở `docs/releases/home-box-grid-20261004-checksums.txt`; metadata build/phát hành ở `.analysis/home-box-grid/client-release.json`.

ZIP inventory200 trước đó được giữ nguyên. Nếu cần khôi phục, dùng lại client inventory200 hoặc phục hồi hai Panel từ backup đã đối chiếu SHA-256. Không cần phục hồi SQL/JAR cho thay đổi giao diện này.

Unity Windows64: **Succeeded; errors=0**. ZIP client: **79.986.438 byte**.

SHA-256 client ZIP: `b1e973c3edd6a00ab328fd7ddf6ce579646ba5ad7f24c243e50689d233926554`.

SHA-256 source patch: `b49597040e3809df7a64e361b7a473cbbe8c53cad88e34e87dd7a9e9c455beae`.
