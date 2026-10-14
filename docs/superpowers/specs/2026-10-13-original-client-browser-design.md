# Client gốc trong trình duyệt

Người dùng chọn cách2: xuất từ source client gốc sang WebGL, giữ giao diện và gameplay gốc, tự nhận diện nền tảng. Bỏ thanh DOM tài khoản/fullscreen/device bên ngoài. Dùng nút đổi tab có sẵn trong Unity client. Canvas chiếm vùng màn hình browser, giữ tỉ lệ điểm ảnh và giới hạn backing buffer; không cố ép khung PC1024:600 cho mọi viewport.

Giữ WebSocket và nhập liệu HTML tối thiểu để browser/iOS keyboard hoạt động. Mobile dọc yêu cầu xoay ngang; xoay màn hình không khởi tạo lại game. Không đổi gameplay hoặc build game trong bước sửa shell. Source tàn sát/hào quang đã đồng bộ ở bước trước; artifact game vẫn cũ đến khi người dùng yêu cầu build.
