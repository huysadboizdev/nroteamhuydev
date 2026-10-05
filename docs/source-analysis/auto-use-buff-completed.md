# Auto sử dụng item và buff 500 phút

Ngày triển khai: 2026-10-04. Kế hoạch được người dùng duyệt trong hội thoại và lưu ở `auto-use-buff-plan.md`.

## Client

- Thông tin vật phẩm luôn hiển thị `Số lượng: X`, kể cả số lượng 1 và trang bị đang mặc.
- Nút `Auto sử dụng` / `Dừng auto` có trong menu vật phẩm tiêu hao trong hành trang cũ/mới, cả Game1 và Game2. Rương nhà/trang bị không chạy auto.
- Nhập số giây không âm; `0` không thêm thời gian chờ. Auto chờ lượng vật phẩm giảm trong cập nhật hành trang từ server rồi mới gửi tiếp. Mỗi tab có một lệnh đang chờ; các item bật auto được phân lượt.
- Tìm lại ô theo loại và option khi ô thay đổi; dừng khi hết, đầy túi với hộp quà, mất kết nối, server từ chối hoặc không có phản hồi sau 3 giây.
- Khi vật phẩm mở menu/xác nhận/form nhập, dừng auto món đó để người chơi chọn tiếp. Không tự chọn phần thưởng.
- Đậu thần giữ nhịp tối thiểu 1 giây vì server có cooldown này; nhịp dùng chung cho các loại đậu và thao tác dùng thủ công.
- Cơ chế AutoItem cũ trong ModFunc được chuyển về cùng bộ điều phối để không gửi lệnh hai lần.

## Server

- Cuồng nộ, Bổ huyết, Bổ khí, Giáp xên, Ẩn danh và các bản cấp 2; đồ ăn 663–667, 880–882; nước mía 1614–1616; Commeson, nồi cơm, đuôi khỉ, cỏ bốn lá cộng thời gian tối đa 500 phút.
- Mỗi lần dùng cộng đúng thời lượng gốc của món. Giữ nguyên hệ số tăng chỉ số. Lần dùng làm vượt giới hạn bị từ chối trước khi tiêu hao.
- Buff thường/cấp 2 cùng nhóm loại trừ nhau. Đồ ăn khác trong cùng nhóm không đổi hiệu ứng của lượng thời gian đã tích. Cờ hết hạn không cản dùng buff mới.
- Dùng đậu từ hành trang tiêu hao đúng vật phẩm được chọn; thao tác hồi phục chung vẫn tìm ô đậu đầu tiên như trước.
- Bộ ghi/đọc thời gian dùng chung schema: giữ bố cục 32 trường của writer đang có, thêm dấu phiên bản cuối mảng; đọc được bố cục legacy 31 trường. Sửa lệch dữ liệu đồ ăn, bùa Santa, nước mía và khôi phục đúng mốc thời gian.
- Sửa bộ đếm hết hạn nước mía cấp 2/3 dùng đúng trường thời gian của từng cấp.

## Xác minh

- 124 chế độ kiểm thử mã nguồn và 124 chế độ trên JAR đóng gói: `.analysis/auto-use/build.log`.
- 16 tình huống client trên từng namespace, tổng 32 lượt: `.analysis/auto-use/client-tests/test.log`.
- Toàn bộ Assembly-CSharp biên dịch thành công bằng Roslyn của Unity 2022.3.62f2.
- Có kiểm thử red/green cho cộng thời gian, mốc 500 phút, đổi ô đậu, nhịp 0 giây và cooldown dùng chung.
- Kiểm thử codec và chỉ số dùng dữ liệu offline; chưa thực hiện thao tác mua/mở hộp hoặc đăng nhập bằng tài khoản thật trong Unity.
- Metadata cài đặt và hash backup/new JAR: `.analysis/auto-use/deployment.json`. Bản chạy: `SRC/20.jar`; backup: `SRC/jar-backup/20-before-auto-use-buff-<timestamp>.jar`.

Client đang chạy Play cần dừng và chạy Play lại để nạp mã C# mới.

## Sửa luồng xác nhận số giây

Sau phản hồi người dùng, xác nhận `ChatTextField.startChat(IChatable, string)` không gán `parentScreen`, khiến số 0 vào callback chat của GameScr. AutoItem ở cả hai tab chuyển sang `startChat2`, vốn gán đúng callback AutoItem trước khi mở nhập liệu.

Kiểm thử mới `.analysis/auto-use/test_chat_flow.py` biên dịch AutoItem và ChatTextField thật: 4 luồng PC/mobile ở Game1/Game2 đã pass; bấm xác nhận 0 bật auto bùa 1628, không gửi chat, đóng hộp nhập và dùng tiếp sau cập nhật túi. Bộ 32 lượt scheduler và biên dịch toàn Assembly-CSharp vẫn pass. Bộ trước chỉ gọi trực tiếp callback, nên bỏ sót lỗi nối giao diện này.

Kiểm tra chỉ đọc server đang chạy: nhân vật admin có 50 bùa 1628 ở slot 55, chưa bật buff; lưu ở `.analysis/auto-use/live-charm-state.json`. Không tiêu hao bùa trên tài khoản thật. Chỉ sửa client; JAR server không đổi.
