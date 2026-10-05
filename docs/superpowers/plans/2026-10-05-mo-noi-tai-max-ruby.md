# Kế hoạch mở nội tại max bằng hồng ngọc

Trạng thái: đã triển khai theo yêu cầu tiếp theo của người dùng;46 nhóm kiểm tra đạt. Chi tiết thực hiện trong `docs/source-analysis/mo-noi-tai-max-2026-10-05.md`.

## Hành vi người chơi

Menu Nội tại thêm **Mở max** giữa Mở VIP và Từ chối: Xem tất cả Nội Tại / Mở Nội Tại / Mở VIP / Mở max / Từ chối. Chọn Mở max hiện xác nhận: “Random nội tại đúng hành tinh, chỉ số tối đa. Giá 100 hồng ngọc/lần.” với nút Mở max và Từ chối. Hiển thị bằng menu server hiện có; không cần thay client chỉ để thêm nút.

Mỗi lượt hợp lệ trừ đúng100 ruby và nhận một nội tại ngẫu nhiên của hành tinh hiện tại. Loại được quay có thể giống nội tại đang sở hữu. Tất cả loại hợp lệ có xác suất ngang nhau. Nội tại mới thay nội tại cũ; không cộng dồn. param1=paramTo1 và param2=paramTo2, bao gồm cả hai thông số khi có hai dòng. Không có thông số thứ hai thì param2=0. Mức max lấy đúng bảng SQL, không dùng một mức chung cho mọi loại.

Giữ điều kiện sức mạnh tối thiểu10 tỷ giống hai lựa chọn hiện có. Người chơi cần online, đúng session, đủ100 hồng ngọc và không có giao dịch chờ xử lý. Không thêm yêu cầu thành viên riêng cho nhánh này. Giữ countOpen hiện tại vì yêu cầu không nói tái lập giá vàng; Mở VIP vẫn thực hiện hành vi hiện có.

## Danh sách quay đã đối chiếu

Nguồn `intrinsic` trong database team2026, kiểm tra 05/10/2026:

| Hành tinh | ID riêng | ID chung | Tổng loại hợp lệ |
| --- | --- | --- | --- |
| Trái Đất (gender0) | 1–7 | 23,24,25 | 10 |
| Namếc (gender1) | 8–15 | 23,24,25 | 11 |
| Xayda (gender2) | 16–22 | 23,24,25 | 10 |

Manager đang thêm gender3 vào danh sách cả ba hành tinh. ID0 là không có nội tại, tuyệt đối không quay ra. SQL hiện mỗi ID có8 bản ghi; không dùng vị trí index1 để loại “không có” vì thứ tự không đảm bảo và có nhiều ID0. Tạo pool riêng cho nhánh max: chọn gender đúng hoặc gender3, lọc id>0, gom theo ID. Nếu cấu hình các bản ghi cùng ID mâu thuẫn hoặc pool rỗng thì báo lỗi và không trừ tiền. Không xóa hàng SQL trùng hoặc sửa nhánh thường/VIP trong phạm vi tính năng này.

Ví dụ: nội tại chung ID23 có khoảng25–300%, Mở max quay trúng sẽ nhận300%; ID3 Trái Đất có hai khoảng10–35%, nhận35% ở cả hai thông số.

## Các bước triển khai

1. **Menu và xác nhận.** Sửa IntrinsicService.showMenu, thêm showConfirmOpenMax. Thêm hằng ConstNpc.CONFIRM_OPEN_INTRINSIC_MAX với ID chưa dùng và nhánh NpcFactory select3/xác nhận. Dịch Từ chối sang index4. Tạo token xác nhận gắn player/session, hạn ngắn, chỉ dùng một lần; kiểm tra lại sức mạnh, ruby và gender khi thực hiện. Xác nhận lại một lượt mới mới được tính là một lần quay mới.
2. **Tạo kết quả trước khi thu tiền.** Hàm tạo bản sao nội tại từ pool hợp lệ, uniform random theo ID; gán hai thông số tối đa. Không sửa template Manager và không sửa intrinsic hiện tại khi mới dựng kết quả. Không reroll để ép khác loại cũ.
3. **Giao dịch bền vững.** Mở rộng staging của ConsignTransactions để chứa intrinsic mới bên cạnh bag/ruby, dùng cùng khóa pending/id/account và đối soát đang có. Thêm action `intrinsic_max` vào ConsignDAO, inventory-only (không listing). Mở rộng payload DAO tùy chọn để ghi data_intrinsic cùng data_inventory và các container bằng một transaction, cùng receipt request UUID trong consign_receipt. Nhánh khác không truyền payload mới nên giữ tương thích. Không tạo bảng mới nếu schema receipt hiện có đáp ứng. Không ghi data_intrinsic thành một câu lệnh ngoài transaction.
4. **Giữ đủ dữ liệu nội tại.** Serialize data_intrinsic theo PlayerDAO: id,param1,param2,countOpen,isIntrinsic,skillID,cooldown,lastTimeUseSkill. Giữ countOpen và trạng thái/cooldown hiện có; cập nhật các chỉ số hiển thị/áp dụng bằng luồng hiện tại. Không làm mất cooldown qua đăng xuất. Chỉ publish ruby và intrinsic mới vào RAM sau khi SQL xác nhận commit; rollback giữ tiền/nội tại cũ. Commit chưa rõ khóa giao dịch, đối soát receipt rồi publish một lần, cùng cơ chế lưu đăng xuất hiện có. Các lượt mở thường/VIP cũng cần tôn trọng pending để không ghi đè một lượt max chưa xác nhận.
5. **Thông báo và client.** Gửi nội tại mới bằng packet112, làm mới tiền và chỉ số cần thiết; thông báo tên và số max nhận được. Kiểm tra nút thứ5 trên màn hình PC và màn hình hẹp. Sau commit, lỗi gửi packet không được thu tiền hay quay lại thêm lần nữa.
6. **Kiểm chứng và triển khai.** Viết kiểm tra trước thay đổi; chạy trên candidate JAR, review thay đổi. Khi được yêu cầu triển khai, bảo trì có lưu dữ liệu, kiểm tra server dừng, snapshot SQL, chuyển SRC/20.jar cũ vào SRC/jar-backup tên có thời gian và đối chiếu SHA-256, cài candidate đã kiểm tra rồi khởi động/TCP14445. Chỉ giữ JAR ứng dụng đang dùng ở thư mục chạy.

## Kiểm tra bắt buộc

- Ba hành tinh: đúng10/11/10 loại, gồm3 loại chung; không ID0, không nội tại riêng hành tinh khác. Duplicate SQL không tăng trọng số. Kiểm tra uniform bằng RNG fixture kiểm soát, không dùng test xác suất dễ chập chờn.
- Tất cả loại max đúng paramTo1/paramTo2; ID3 đạt35/35; ID23 đạt300. Template và nội tại cũ không bị sửa trong staging.
- Ruby99 từ chối, ruby100 thành công và còn0; vàng/ngọc xanh/VND giữ nguyên. Sức mạnh9.999.999.999 từ chối,10.000.000.000 được mở. countOpen không đổi.
- Người đang có cùng loại vẫn có thể quay trúng lại và nhận max. Pool rỗng/mâu thuẫn, session cũ, token hết hạn, bấm lặp hoặc pending không thu tiền.
- SQL rollback giữ tiền/nội tại cũ; commit mất ACK đối soát đúng, publish một lần; hai request đồng thời không dùng lại cùng token. Logout/relogin giữ cả ruby và nội tại mới; không bị lần lưu cũ ghi đè.
- Menu năm nút và đúng index/xác nhận; packet112/tiền đọc được bởi client; hồi quy Mở thường/Mở VIP/ký gửi/RubyGift.

## Phạm vi hiện tại

Đã sửa server và triển khai JAR cho Mở max. Không cần đổi các mức max trong intrinsic vì SQL đã có param_to_1/param_to_2; không cần thay client/schema. Metadata `.analysis/intrinsic-max-deployment-latest.json`.
