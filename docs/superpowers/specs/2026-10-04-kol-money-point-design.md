# Thiết kế cập nhật 7 nhiệm vụ KOL thường và KOL VIP

Ngày lập: 04/10/2026, UTC+7. Phạm vi hiện tại: kiểm tra source/SQL và lập kế hoạch; chưa triển khai.

## Yêu cầu đã được người dùng chốt

- Giữ hai nhánh KOL thường và KOL VIP tại Quy Lão Kame. Có vé ID 1825 trong hành trang thì được nhận cả hai nhánh; mỗi nhánh có tiến độ nhận thưởng riêng.
- Vé không tiêu hao; kiểm tra vé ngay khi nhận thưởng VIP. Không có vé vẫn tích lũy hoạt động và nhận nhánh thường; khi có vé có thể nhận nhánh VIP dựa trên hoạt động đã tích lũy.
- VND cộng vào số dư `account.vnd`/`MySession.vnd` đang dùng mở VIP ToriBot; không tăng `tongnap`, không tự nâng cấp VIP.
- Điểm cộng vào `player.event_point`/`PlayerEvent.eventPoint` đang đổi tại Quy Lão Kame; không nhầm với `account.event_point` hay các loại `point_sukien` khác.
- Tích lũy tất cả nhiệm vụ từ lúc cập nhật, không cần đang đứng ở đúng nhiệm vụ; mỗi nhánh nhận theo thứ tự 1–7, mỗi nhiệm vụ một lần.
- Nhiệm vụ 5 là **Capsule hồng ID 722**, không phải Hồng ngọc ID 861. Chỉ bổ sung nhiệm vụ/bộ đếm trước; người dùng sẽ thêm nguồn rơi vào boss sau. Lần triển khai này không thêm drop hay đổi tỷ lệ drop.
- Giữ nguyên toàn bộ mức thưởng, kể cả bước nhảy điểm nhiệm vụ 7.

## Danh sách và phần thưởng

| NV | Điều kiện | VND thường | Điểm thường | VND VIP | Điểm VIP |
|---|---|---:|---:|---:|---:|
| 1 | Hạ 10.000 Mộc nhân khi dùng Tự động luyện tập | 100.000 | 1.000 | 150.000 | 1.500 |
| 2 | Hạ 100.000 Quỷ chim ở Rừng đá Fide hoặc Hang quỷ chim khi dùng Tự động luyện tập | 200.000 | 2.000 | 250.000 | 2.500 |
| 3 | Diệt đủ 10 turn Fide; mỗi turn đủ Fide 1 + Fide 2 + Fide 3, cộng dồn | 300.000 | 3.000 | 350.000 | 3.500 |
| 4 | Hạ Xên Hoàn Thiện tại Võ đài Xên Bọ Hung 3 lần | 400.000 | 4.000 | 450.000 | 4.500 |
| 5 | Nhặt thành công 200 Capsule hồng ID 722 | 500.000 | 5.000 | 550.000 | 5.500 |
| 6 | Hạ Black Goku 10 lần | 600.000 | 6.000 | 650.000 | 6.500 |
| 7 | Hạ Super Black Goku 10 lần | 700.000 | 50.000 | 750.000 | 100.000 |
| Tổng | Hoàn thành một nhánh | **2.800.000** | **71.000** | **3.150.000** | **124.000** |

Nhận hết cả hai nhánh: **5.950.000 VND + 195.000 điểm**. Đây là số dư game, không mô tả như tiền có thể rút ngoài game. Theo source hiện tại, ví VND còn được dùng đổi vàng/ngọc qua Input; vì người dùng chọn ví đang có, không tự giới hạn ví này chỉ dùng ở ToriBot. ToriBot giữ nguyên giá gói 50.000/100.000/150.000/200.000, mùa VIP và giới hạn 4 lượt mua/mùa.

## Quy tắc kỹ thuật đề xuất để thực hiện đúng yêu cầu

- Mộc nhân: `ConstMob.MOC_NHAN = 0`. Đếm tại chuyển trạng thái sống → chết thật, không đếm số đòn đánh hoặc số lần tạo drop. Không thêm hạn chế map ngoài yêu cầu.
- Quỷ chim: `ConstMob.QUY_CHIM = 50`, chỉ map **77 hoặc 81**, không nhầm map 8 Rừng đá trên Trái Đất.
- Tự động luyện tập: kiểm tra server `itemTime.isUseTDLT` và thời gian còn hiệu lực tại thời điểm hạ quái. `autoTrainState` là trạng thái mua đồ, không dùng để xác nhận đang bật TDLT. Không suy ra TDLT từ cờ client/bùa Thu hút.
- Người được ghi nhận: nhân vật chính được server xác nhận là người hạ mục tiêu/nhặt đồ. Không ghi trực tiếp vào đệ tử, bot, boss hoặc newPet, không chia cho người chỉ đứng cùng map. Mặc định không quy đổi cú kết liễu của đệ tử sang chủ; nếu muốn đổi quy tắc này phải ghi rõ trước triển khai.
- Fide dùng `BossID.FIDE = -28`, ba dạng là `currentLevel` 0/1/2. Lưu **ba bộ đếm riêng**, số turn = `min(fide1, fide2, fide3)`. Không dùng `tổng số lần hạ / 3`. Ví dụ 30 lần Fide 1 và 0 lần hai dạng còn lại = 0 turn; 12/10/11 = 10 turn. Không yêu cầu ba dạng trong cùng một lần xuất hiện vì người dùng yêu cầu cộng dồn.
- Xên đúng: `BossID.SIEU_BO_HUNG = -101`, `currentLevel == 0`, map **103**. Không tính dạng Siêu Bọ Hung level 1, Xên con, hoặc `BossID.XEN_BO_HUNG` level 2 tại map 100 dù có tên gần giống.
- Black Goku dùng `BossID.BLACK_GOKU = -203`: `currentLevel == 0` cho NV6, `currentLevel == 1` cho NV7. Không so sánh tên boss vì có hậu tố ngẫu nhiên.
- Capsule: đếm **số lượng** nhặt thành công từ đất, không đếm khi cấp bằng giftcode/shop/giao dịch/chuyển rương. Dùng/mất capsule sau khi nhặt không giảm tiến độ; nhận thưởng không trừ capsule. Một stack 20 viên = 20 tiến độ. Không tự thêm cơ chế giới hạn nguồn rơi khi người dùng chưa yêu cầu.
- Hai nhánh dùng chung bộ đếm hoạt động. Nhận thường không tiêu hao tiến độ VIP và ngược lại. Tiến độ đã đạt yêu cầu được giữ qua relog/restart; không reset theo ngày và không tự mở vòng nhiệm vụ mới.
- Giao diện cho biết nhiệm vụ, số đã đạt/yêu cầu, VND + điểm thưởng; NV3 hiển thị số turn và cả ba số lần hạ. Chỉ hiện nút nhận khi đủ điều kiện; server vẫn xác minh toàn bộ khi bấm nhận.

## Bằng chứng từ source và SQL

- `QuyLaoKame.java:74`: 7 nhiệm vụ VIP cũ được hardcode, thưởng vật phẩm. `:445`: kiểm tra vé 1825 trong bag. `:524`: tăng stage riêng.
- `Mob.java:682`: bộ đếm cũ `monsterKillCountAutoTrain` tăng trong tạo drop, **không kiểm tra TDLT** và không phân biệt loại quái. Không dùng số cũ để cấp nhiệm vụ mới.
- `Mob.java:164`: đường chết thật nằm trong `injured`; phải đặt hook ở đây.
- `Fide.java:21`: ba dạng cùng class/ID. `BlackGoku.java:26`: hai dạng cùng class/ID.
- `BossesData.java:716`: Xên Hoàn Thiện tại map 103; dạng gần tên khác nằm ở map 100.
- `Zone.java:325`: nhặt vật phẩm được khóa theo `itemMap`; `:347` xác nhận `addItemBag` thành công; lưu `pickupQuantity` trước vì addItem có thể sửa item.
- `PlayerDAO.java:354`: autosave đã `synchronized(player)`, có thể dùng chung khóa cho nhận thưởng và đếm tiến độ.
- `MrBlue.java:1299`, `PlayerDAO.java:903`: nạp/lưu `nhiem_vu_kol`; dữ liệu legacy không tương đương bộ đếm mới.
- `ToriBot.java:129`: đang trừ VND trong session; source không có cập nhật `account.vnd` tại nhánh này. Khi bổ sung cộng VND lưu DB, cần sửa debit ToriBot để relog không làm hồi tiền đã tiêu.
- `PlayerEvent.java`: điểm sự kiện nằm trên nhân vật. `ShopService.java:735`: giá shop điểm hiện thấp hơn nhiều so với thưởng mới; giữ số thưởng người dùng yêu cầu, không tự sửa giá.
- Hai dump đều có 722/1825; account/player dùng InnoDB. Snapshot kiểm tra dump: `.analysis/kol-plan/sql-inspection.json`. Lần này không kiểm tra hoặc thay database live.

## Dữ liệu và tính an toàn nhận thưởng

Thêm trạng thái KOL v2 độc lập với JSON legacy: `version=2`, hai stage bắt đầu 1, 9 bộ đếm (Mộc nhân, Quỷ chim, Fide 1/2/3, Xên, Capsule, Black, Super Black). Bắt đầu đếm từ lúc bản cập nhật có hiệu lực; không chuyển các số legacy thành tiến độ mới. Lưu dưới khóa `v2` trong `nhiem_vu_kol`, giữ nguyên các trường cũ để có thể rollback.

Thêm bảng ledger `kol_reward_claim` khóa duy nhất `(player_id, quest_version, branch, quest_id)` cho việc nhận một lần. Một transaction InnoDB khóa account rồi player; kiểm tra chưa nhận, số dư không tràn INT; ghi ledger + cộng VND + cộng điểm + lưu JSON stage mới; COMMIT rồi mới cập nhật session/PlayerEvent và thông báo. Lỗi DB phải ROLLBACK toàn bộ, không mất quyền nhận hoặc phát một nửa thưởng. Không chỉ cộng vào RAM rồi chờ autosave.

Các thao tác tiến độ/điểm/nhận thưởng dùng chung khóa player với autosave, không giữ khóa boss/mob khi gọi JDBC. Ví VND dùng phép cộng/trừ SQL có điều kiện và đọc lại số dư, không ghi đè bằng giá trị session cũ. Những giao dịch khác sử dụng cùng ví không được bị ghi đè bởi thưởng KOL.

Rollback giữ ledger đã nhận để không phát lại tiền khi triển khai lại cùng phiên bản. Phần thưởng đã commit không tự thu hồi khi rollback JAR; cần báo cáo giao dịch trước khi có quyết định xử lý số dư. Không tự đổi `tongnap` hay lịch sử nạp tiền.

## Trạng thái

Đã chốt nội dung nhiệm vụ, hai nhánh, ví/điểm, cách tích lũy và Capsule ID 722/chưa có drop. Các quy tắc kỹ thuật phía trên là thiết kế đề xuất trong kế hoạch, chưa có source ứng dụng, migration live hoặc JAR mới. Cần người dùng duyệt kế hoạch để chuyển sang triển khai.
