# Nhiệm vụ KOL nhận VND và điểm — 2026-10-04

Đã triển khai KOL v2 vào `SRC/20.jar`, cập nhật hai dump SQL và database đang dùng. Server PID 15512 đã khởi động lại, nhận kết nối TCP cổng 14445; probe xác nhận class KOL v2 được nạp. Catalog vật phẩm giữ phiên bản 13.

## Quy tắc đã triển khai

- Mọi nhiệm vụ cộng dồn từ bản cập nhật; thường và VIP nhận theo thứ tự 1–7, độc lập, mỗi lượt chỉ một lần. Tiến độ không bị trừ khi nhận.
- Vé VIP ID1825 phải ở hành trang, không bị tiêu hao. Có vé được nhận cả hai nhánh.
- VND cộng `account.vnd`, dùng mua VIP ToriBot; điểm cộng điểm sự kiện của nhân vật, dùng tại Quy Lão Kame. Không tăng `tongnap`.
- Mộc nhân 10.000 và Quỷ chim 100.000 cần TDLT đang còn hiệu lực; Quỷ chim chỉ map77/81. Chỉ tính nhân vật trực tiếp kết liễu.
- Fide: 10 turn bằng số nhỏ nhất của ba bộ đếm Fide1/2/3. Xên Hoàn Thiện: 3 lần đúng dạng/map103. Black và Super Black: mỗi dạng 10 lần riêng.
- Capsule hồng ID722: nhặt thành công 200 đơn vị từ mặt đất. Không tính nhận qua shop/giftcode/chuyển rương. Chưa thêm nguồn rơi mới theo yêu cầu người dùng.

| NV | Thường: VND / điểm | VIP: VND / điểm |
|---|---:|---:|
| 1 | 100.000 / 1.000 | 150.000 / 1.500 |
| 2 | 200.000 / 2.000 | 250.000 / 2.500 |
| 3 | 300.000 / 3.000 | 350.000 / 3.500 |
| 4 | 400.000 / 4.000 | 450.000 / 4.500 |
| 5 | 500.000 / 5.000 | 550.000 / 5.500 |
| 6 | 600.000 / 6.000 | 650.000 / 6.500 |
| 7 | 700.000 / 50.000 | 750.000 / 100.000 |

Tổng hai nhánh: **5.950.000 VND và 195.000 điểm**.

## Lưu dữ liệu và giao dịch

State mới nằm trong JSON `player.nhiem_vu_kol`; giữ dữ liệu legacy, không suy ra tiến độ mới từ bộ đếm cũ. JSON số sai/âm/tràn/null bị từ chối cấp thưởng. Các nhân vật cũ khởi tạo v2 với counters0 và stage1/1 khi nạp; migration không sửa JSON hoặc số dư của người chơi.

`kol_reward_claim` dùng khóa duy nhất player/version/branch/quest. Tiền, điểm, stage và ledger commit cùng transaction; RAM chỉ cập nhật sau xác nhận. COMMIT chưa rõ kết quả sẽ giữ guard, đối chiếu dưới khóa account và chặn autosave ghi state cũ. Khi phục hồi vẫn giữ hoạt động/điểm kiếm thêm trong thời gian chờ.

`wallet_debit_receipt` lưu biên nhận UUID trong transaction trừ tiền. Các đường mua đã chuyển sang helper giữ yêu cầu gốc khi mất kết nối, đối chiếu trước khi mua tiếp và cấp gói một lần khi tiến trình vẫn chạy. Không suy đoán bằng chênh lệch số dư. **Giới hạn còn có của hệ thống mua:** crash tiến trình giữa trừ tiền và lưu vật phẩm/VIP chưa có cơ chế phục hồi đơn hàng đầy đủ; cần lưu đơn hàng và phần cấp thưởng bền vững trong một thay đổi riêng.

## Kiểm chứng

- 44/44 nhóm PASS trên JAR candidate và JAR cài đặt; classpath không dùng class production rời. Bao gồm state/catalog/tracking/hook chết/pickup/menu/context, suite giao dịch MySQL riêng và 34 nhóm hồi quy.
- Schema test riêng `kol_verify_20261004_111410`: rollback ở các bước ghi, đồng thời, overflow, ownership, đủ14 thưởng, restart đọc lại state, debit và mất xác nhận COMMIT. Không cấp thưởng hoặc sửa counters nhân vật thật để kiểm thử.
- Rà soát spec và quality độc lập; phát hiện và sửa JSON coercion, COMMIT chưa rõ kết quả, biên nhận thanh toán.
- Migration kiểm tra InnoDB/unique key và đối chiếu số dư/điểm/JSON trước-sau không thay đổi. Hai dump giữ nguyên toàn bộ byte gốc, chỉ nối DDL.
- Đã kiểm tra runtime/TCP; **chưa nghiệm thu bằng gameplay client thực tế**. Các bài mô phỏng thiếu service phụ có log lỗi fixture sau hook; assertion hành vi và exit code được kiểm tra, không dùng log fixture để khẳng định đường chơi thực tế hoàn tất.

## Backup và khôi phục

Server đã bảo trì có lưu dữ liệu, dừng đúng PID và đóng cổng trước thay JAR. Bản cũ đã chuyển vào `20-before-kol-money-point-20261004-114707.jar` trong `SRC/jar-backup`.

- SHA-256 cũ: `a88ca08e3f934ce931b9c57baf225f00d85835df4ba5a7e46bc4cb3b9a8e9029`.
- SHA-256 mới: `3ae6d7833bc89e12be24ac78179ea0039dd63f6c15b8cee9c118c1a073ca187d`.
- Metadata: `.analysis/kol-plan/deployment.json`, `.analysis/latest-deployment.json` và JSON cạnh backup.
- Source/SQL backup: `.analysis/kol-plan/source-sql-backup.json`; SQL hash: `sql-file-changes.json`; database trước/sau: `database-maintenance-before.json`/`database-after.json`.
- Bằng chứng test: `red-behavior.log`, `green-behavior.log`, `jar-tests.log`, `runtime-verification.txt` trong `.analysis/kol-plan`.

Rollback phải bảo trì/lưu dữ liệu rồi backup JAR đang dùng trước khi phục hồi bản cũ. Giữ cả ledger và biên nhận, giữ tiền/điểm đã commit; không DROP lịch sử hoặc tự thu hồi thưởng đã nhận/tiền đã tiêu. Bản JAR cũ không cung cấp menu v2; khi triển khai lại v2, stage/ledger bảo đảm không cấp lại thưởng.
