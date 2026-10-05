# Nâng hành trang và rương nhà lên 200 ô — 03/10/2026

Đã triển khai vào `SRC/20.jar`; server chạy cổng 14445. Nhân vật mới có sẵn 200 ô hành trang và 200 ô rương nhà. Hai nhân vật cũ cũng được bổ sung đủ 200/200. Không thu phí mở ô.

## Thay đổi server và SQL

- Trần, vòng khởi tạo DAO và loader nhân vật cùng dùng 200. Giữ đồ khởi đầu, 11 ô trang bị và rương vòng quay 110 ô.
- Đếm ô/chỉ số ô bằng int; đọc chỉ số inventory unsigned. Sentinel 255 chỉ dùng đúng nhánh tiền giao dịch hoặc dùng đồ theo template.
- Gửi đầy đủ dấu -1 cho mỗi ô hành trang trống; sửa thao tác ô 128–199 và kiểm tra phạm vi trước thay đổi đồ.
- Chặn yêu cầu ghép trùng/sai ô; ký gửi sai không mất phí; giao dịch số lượng 0 hoặc số dòng vượt khả năng biểu diễn bị từ chối trước lấy đồ.
- Hai cột `player.items_bag`, `player.items_box` đã chuyển từ TEXT sang MEDIUMTEXT. SQL không có cột sức chứa riêng: độ dài JSON array chính là số ô.
- Migration chỉ nối ô trống vào cuối. Đối chiếu từng chuỗi item cũ và `data_inventory` sau ghi; giữ nguyên đồ, số lượng, option trùng và tiền.
- Vật phẩm mở ô 517/518 bị từ chối khi đủ 200 và không bị tiêu hao.
- Giữ nguyên các quy tắc chuyển hóa đã triển khai, gồm cấm đồ kích hoạt ở cả nguồn/đích.

## Client và giao thức

Cả Game1/Game2 đã sửa số ô đăng nhập, chỉ số dùng/chuyển đồ, giao dịch, xác nhận sử dụng và auto-use. Giao thức `INV200` được xác nhận ngay ở luồng nhận trước khi đọc gói kế tiếp, độc lập ở Session_ME/Session_ME2 của mỗi tab; giữ hỗ trợ option int32 `IOP32`.

Gói -30/-35/-36/-86/-44 dùng chiều dài 3 byte khi đã thương lượng INV200. Tab cá nhân ký gửi dùng count unsigned short để không tràn khi có 200 món trong túi cộng thêm đồ đã đăng bán. Giao thức với server cũ vẫn dùng dạng cũ nếu không nhận xác nhận INV200.

Client cũ bị chặn đăng nhập trước khi nhận inventory 200 ô, với thông báo yêu cầu cập nhật. Vì vậy cần dùng bản client mới đi cùng JAR này.

## Kiểm chứng đã chạy

- 24 nhóm kiểm thử source: Java server, migration, SQL thật trên temporary table, biên dịch đầy đủ C#, bốn collector, API gửi thao tác, giao dịch và ký gửi; gồm hồi quy chuyển hóa với 768 tình huống đồ kích hoạt.
- 10 kiểm thử trực tiếp trên JAR đóng gói.
- DAO thật trên schema thử riêng: tạo nhân vật cả ba hệ, mỗi nhân vật 200/200, giữ đồ khởi đầu và các danh sách khác. Schema thử đã được dọn sau kiểm thử.
- Serializer thật gửi hành trang/rương: 200 ô trống, 34 món xen ô trống, 200 món đầy; decode hết đúng payload và giữ option.
- Gói lớn 65.535, 65.536, 100.000 byte; plaintext/XOR, gói kế tiếp không lệch; ACK và reset độc lập ở bốn luồng nhận.
- Ký gửi thật 256 dòng, mỗi dòng 64 option, payload hơn 64 KiB; count không tràn và frame không lệch.
- SQL temporary table: hai mảng 200 món, mỗi cột 312.401 byte; round-trip giống tuyệt đối, rollback đúng và không warning.
- Unity 2022.3.62f2 trong project tách riêng: cả hai tab đọc gói Java 200 ô rỗng/có đồ/đầy, cập nhật ô 199, tính cuộn và phân trang danh sách/lưới. Bản Windows 64-bit dựng thành công, 0 lỗi build.
- Server thực tế: nhận ACK đúng cho INV200+IOP32, INV200 riêng và client cũ; client cũ nhận thông báo cập nhật. Cổng 14445 hoạt động.

Chưa thực hiện đăng nhập tài khoản người chơi rồi chơi thủ công trên client mới. Kiểm tra Unity dùng gói tin thật do serializer Java tạo và trạng thái panel trong scene chạy; không phải kiểm thử thao tác chuột trên phiên người chơi thực tế.

## Bản để chạy

- Client Windows: `docs/releases/nro-client-inventory200-20261003.zip`. Giải nén toàn bộ rồi chạy `NRO200.exe`; giữ cùng thư mục `NRO200_Data`, `GameAssembly.dll`, `UnityPlayer.dll` và các DLL đi kèm.
- Source patch server/client: `docs/releases/inventory200-source-patch-20261003.zip`.
- JAR đang chạy: `SRC/20.jar`, SHA-256 `c86d84aeb1a4699698811b01b6540462267c85e2b6a2f228147e28d9c5a03696`.
- JAR cũ: `SRC/jar-backup/20-before-inventory200-20261003-224811.jar`; đã đối chiếu SHA-256 với bản trước thay.
- Backup DB: `.analysis/inventory200/team2026-before-inventory200-20261003-224811.sql`.
- Metadata triển khai và checksum: `.analysis/inventory200/deployment.json`; kết quả kiểm thử: `.analysis/inventory200/all-tests.log`.

Đã dừng server bằng bảo trì có lưu dữ liệu trước backup/migration/thay JAR; không thay khi server còn chạy. Thư mục chạy chỉ còn `20.jar`; thư viện và JAR công cụ bảo trì được giữ nguyên.

Nếu phục hồi, phải xử lý theo bộ client/JAR/database; không chỉ chạy lại JAR cũ với dữ liệu 200 ô. Chụp và đối chiếu dữ liệu phát sinh sau nâng trước khi khôi phục snapshot cũ.
