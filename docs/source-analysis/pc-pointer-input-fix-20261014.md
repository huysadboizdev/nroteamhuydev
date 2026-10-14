# Sửa nút menu không nhận chuột PC

Nguyên nhân: port web đổi GameCanvas.initGame từ `isTouch=true` của PRJ sang kiểm tra BrowserPlatform.IsMobile. Cờ `isTouch` thực tế bật đường nhận pointer chung của PRJ, bao gồm cả chuột desktop. ServerListScreen.updateKey chỉ kiểm tra từng nút trong nhánh này, nên bản PC đã bỏ qua hit test.

Khôi phục `isTouch=true` trong Game1/Game2; Main.isPC và nhận diện PC/mobile giữ nguyên. Sửa generator Tools/integrate_storage.py để không lặp lại thay đổi sai.

Xác minh trước sửa:

- Fixture dùng các method thật Command.isPointerPressInside/ServerListScreen.updateKey thất bại: button 0 trên PC bị bỏ qua.
- Browser pointer fixture không chọn được server bằng chuột, timeout.

Xác minh sau sửa:

- Fixture actual method: cả năm action menu và nút download phản hồi, hai tab; desktop role vẫn PC.
- Compiler source browser/editor 409/410 file không lỗi.
- Build WebGL-prj-inputfix-20261014 Succeeded, Errors=0, Warnings=70.
- Game thật qua fixture TCP riêng: chuột chọn server, mở Đổi tài khoản, focus ô nhập, gõ và bấm lưu thành công trong cả hai tab. Kiểm tra viewport PC 1280x800, 1920x926 và 800x480 đạt.
- Android Chromium emulation: chạm chọn server, mở bàn phím HTML, Xong và lưu từ form game đều đạt cho hai tab, không có errors/dialogs.

Tài khoản `click-test1/2` chỉ dùng trong browser context/test TCP riêng. Không đăng nhập hoặc tạo tài khoản trên server Java thật bằng các fixture này. Đã cài artifact vào Builds/WebGL; backup cũ Builds/WebGL-backup-20261014-090245. Metadata/hash: .analysis/prj-webgl-deployed.json.

URL mới: http://localhost:8086/?v=WebGL-prj-inputfix-20261014.

Evidence: .analysis/pc_pointer_regression.py, .analysis/pc-pointer-regression.ps1, Gateway/browser-pointer.cjs, Gateway/browser-mobile-input.cjs, Tests/evidence/browser-pointer.json và browser-mobile-input-android.json.
