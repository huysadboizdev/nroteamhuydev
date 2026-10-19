# Thiết kế: Ẩn thông báo boss và làm mới bảng Boss

## Mục tiêu

Ẩn hoàn toàn thông báo của các nhóm boss đã chốt trên server, APK và Web. Thiết kế lại phần danh sách Boss trong tab **Thông báo** để chỉ tải khi người chơi chủ động vào mục Boss, hiển thị trạng thái boss ngắn gọn và luôn giữ dữ liệu được cập nhật ngầm sau lần mở đầu tiên.

## Phạm vi boss cần ẩn

- Boss Mabư 12h: `DRABURA`, `BUI_BUI`, `YA_CON`, `MABU_12H`, `DRABURA_2`, `BUI_BUI_2`, `DRABURA_3`, `GOKU`, `CADIC`.
- Boss Mabư 14h: `MABU` và `SUPERBU` thuộc map loại `MAP_MABU_14H`.
- Sói Hecquyn, Ăn Trộm, Mặt Trời và Ở Dơ: nhận diện bằng class/semantic ID phù hợp với boss thực tế.
- Các boss ngoài danh sách trên giữ hành vi hiện tại.

Phạm vi ẩn gồm thông báo xuất hiện/bị hạ, ticker, screen event, bảng Boss, danh sách hiện tại, lịch sử và chi tiết. Không xóa lịch sử cũ trong database hoặc outbox; chỉ ngăn phát sinh thông báo mới và lọc bản ghi mục tiêu khỏi mọi dữ liệu gửi tới client.

## Nguồn quyết định và lọc

Server có một quy tắc suppression dùng chung, dựa trên semantic ID và ngữ cảnh map khi cần. Áp dụng quy tắc trước khi ghi tracker/phát thông báo, đồng thời chặn các lời gọi broadcast trực tiếp trong lớp boss. Các endpoint/listener của bảng Boss lọc cả dữ liệu hiện tại lẫn lịch sử trước khi phân trang và phân loại thành **Boss nhiệm vụ** hoặc **Boss**.

Server không phát ticker command 93 hoặc screen event command 115 cho boss mục tiêu. APK và Web giữ bộ lọc client cho screen event có typed metadata như lớp bảo vệ phụ; Web cần ghép ticker cũ với metadata trước khi render để tránh thông báo mục tiêu lóe lên khi hai gói đến lệch thời điểm. Luồng spawn, chiến đấu, loot, phần thưởng và nhiệm vụ không đổi.

## Trải nghiệm bảng Boss

- Mở tab **Thông báo** không tự tải danh sách Boss. Chỉ khi người chơi chọn mục **Boss** hoặc **Boss nhiệm vụ** mới yêu cầu snapshot mới nhất.
- Sau lần chủ động mở đầu tiên, client giữ cache và nhận cập nhật server-push ngầm khi mục Boss đang đóng. Không dùng spinner cho cập nhật nền.
- Khi mở lại mục Boss, hiện cache mới nhất ngay rồi đồng bộ nền. Khi reconnect hoặc mất đồng bộ, lấy snapshot đầy đủ để sửa cache.
- Khi chưa có cache và đang chờ snapshot đầu tiên, dùng trạng thái chữ gọn, không spinner; không hiển thị dữ liệu giả hoặc thời gian ước đoán.
- Mỗi dòng ưu tiên tên boss và trạng thái dễ quét. Boss còn sống hiện map hiện tại, không hiện countdown sống còn/return. Boss đã chết hoặc nghỉ chỉ hiện countdown hồi sinh nếu server có ETA hợp lệ. Nếu hồi sinh phụ thuộc lịch hoặc điều kiện, hiển thị nhãn ngắn như **Chờ lịch** hoặc **Chờ điều kiện** thay vì tự suy ra số phút.
- Countdown được tính từ timestamp server/ETA đã nhận, không giảm dần sai theo thời gian thiết bị. Có nhãn thời điểm cập nhật ngắn để người chơi nhận biết độ mới của snapshot.
- Hành vi tải, render, hit-test và cache phải nhất quán giữa APK và Web, kể cả các bản Game1/Game2 nếu cùng chứa màn hình này.

## Hướng đồng bộ nền

Server-push bắt đầu sau lần người chơi mở mục Boss đầu tiên trong phiên. Cập nhật trạng thái boss cần cập nhật hoặc vô hiệu cache mà không yêu cầu client tải lại toàn bộ danh sách theo chu kỳ. Client nhận snapshot đầy đủ khi bắt đầu đăng ký và sau reconnect; các thay đổi tiếp theo áp dụng lên cache. Việc đóng mục Boss chỉ ẩn giao diện, không hủy đồng bộ nền trong phiên.

## Tiêu chí chấp nhận

- Mọi boss mục tiêu không tạo ticker, screen event hoặc dữ liệu bảng mới; các lịch sử cũ của chúng không xuất hiện ở bất kỳ trang, loại hoặc chi tiết nào trên APK/Web.
- Boss ngoài phạm vi tiếp tục phát và hiển thị thông báo đúng như trước.
- Mở **Thông báo** không tải danh sách Boss. Mở mục Boss có snapshot mới nhất; đóng mục vẫn nhận cập nhật ngầm sau lần mở đầu tiên; reconnect phục hồi bằng snapshot.
- Cập nhật nền không hiện spinner. Boss sống hiển thị map, không có countdown; boss chết/nghỉ có countdown chỉ khi ETA hợp lệ, nếu không thì ghi trạng thái chờ phù hợp.
- APK và Web có cách hiển thị, phân trang, chi tiết và đồng bộ nhất quán; không lóe ticker mục tiêu do thứ tự gói tin.
- Không build APK, EXE hoặc JAR nếu chưa được yêu cầu rõ ràng.

## Ngoài phạm vi

- Xóa dữ liệu lịch sử boss mục tiêu khỏi database hoặc outbox.
- Thay đổi lịch spawn, chiến đấu, loot, phần thưởng hoặc nhiệm vụ.
- Đặt countdown cho boss đang sống khi server không có khái niệm thời điểm return.
- Tự build/đóng gói hoặc cài APK/JAR.
