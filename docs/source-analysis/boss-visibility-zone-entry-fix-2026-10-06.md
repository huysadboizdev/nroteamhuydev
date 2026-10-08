# Sửa boss tàng hình và ưu tiên khu 0 — 06/10/2026

Đã triển khai trên server lúc 07:29, giờ Việt Nam. Server PID 13156 đang nghe cổng 14445.

## Thay đổi

- `ChangeMapService`: khi boss được gán trước khu spawn, commit cùng khu vẫn bổ sung đăng ký còn thiếu; giữ kiểm tra đóng phó bản, giới hạn sao đen và event gate map 126. Callback notice chỉ bổ sung khi boss chưa được đăng ký.
- `Zone`: đăng ký boss, pet và NPC không thêm trùng; đăng ký cùng đối tượng được bảo vệ bằng khóa player. Các danh sách nhân vật dùng CopyOnWriteArrayList để các thread boss/spawn không ghi đồng thời vào ArrayList thường. Số người chơi và bộ lọc chủ sở hữu boss offline giữ nguyên.
- `Boss.joinMapByZone`: boss đang có khu cũ giữ nguyên tham chiếu nguồn đến khi commit, tránh để boss sót trong khu cũ khi chuyển sang khu mới.
- `MapService.getZone`: chọn khu 0 trước, tiếp theo khu đầu tiên còn chỗ theo thứ tự; dùng `maxPlayer`, không random và không lặp vô hạn khi tất cả khu đầy. Bốn đường về map dự phòng phó bản kiểm tra null trước khi dùng khu đích.
- `ChangeMapService.getMapCanJoin(Player,int)`: dùng chung selector và điều kiện admission, bỏ ngưỡng cố định 7 người. Đổi khu chủ động vẫn giữ khu đã chọn; capsule về chỗ cũ giữ khu cũ theo chức năng hiện có; boss có selector random riêng giữ quy tắc spawn riêng.
- `DungeonExitService`: cưỡng chế về ưu tiên khu 0 nếu còn chỗ, sau đó khu đầu tiên còn chỗ. Nếu mọi khu nhà đều đầy, vẫn dùng khu 0 để hoàn tất cưỡng chế thoát sự kiện, không giữ người ở map đã hết giờ.

Không sửa SQL hoặc source client. Hirudegarn và các boss lớn hệ Mob dùng packet riêng, không bị thêm sai vào danh sách Boss.

## Kiểm thử

Biên dịch toàn bộ **597 file Java**. **62 nhóm kiểm tra PASS**:

- 12 nhóm mới: preassigned, repeat, partial, remove, race, pet, packet Xên/reconnect, offline owner isolation, closing, default capacity, boss move, forced exit zone 0.
- 8 nhóm chọn khu: ordinary, overflow, full, empty, black, hirudegarn, explicit, fallback.
- 3 nhóm boss notice tracker/wire và death lifecycle.
- 37 nhóm hồi quy sao đen, hợp thể pet, thời hạn/admission phó bản, phiên/giai đoạn/thoát/login Hirudegarn, vệ tinh.
- Benchmark Hirudegarn và kiểm tra cưỡng chế Mabu.

Đã thấy test thất bại đúng nguyên nhân trên bản cũ trước khi sửa: thiếu đăng ký boss, thêm trùng, đổi khu khi mới có 7 người, chọn random, tất cả khu đầy, chuyển boss giữ membership khu cũ, cưỡng chế về chọn khu ít người thay vì khu 0. Fixture offline được đăng ký đúng loại map và kiểm tra packet thực tế `-5`, không bỏ qua bộ lọc quyền nhìn thấy.

## Kiểm chứng trên server

| Mẫu | Thời gian Việt Nam | Boss hoạt động/đã spawn | Thiếu hoặc trùng đăng ký |
|---|---|---:|---:|
| Sau cài 1 | 07:29:50 | 362 | 0 |
| Sau cài 2 | 07:30:25 | 363 | 0 |

Cả hai mẫu quét 2.372 khu; mỗi boss ở `ACTIVE/CHAT_S/CHAT_E` đã có map đều xuất hiện đúng một lần trong `zone.bosses` và `zone.humanoids`. Xên hoàn thiện `SieuBoHung` đang ACTIVE ở map 103 khu 0, đăng ký đầy đủ. Không phát hiện deadlock tại các thời điểm lấy mẫu. Hash resource của cả 5 class đang chạy khớp JAR cài.

Các boss REST/AFK/chưa spawn và sự kiện chưa diễn ra không được tính là lỗi hiển thị. Chưa quan sát hình trên Unity của người chơi sau cập nhật; hành vi người vào muộn/reconnect được kiểm tra qua packet fixture. Phiên Tapion được kiểm thử bằng clock fixture, chưa quan sát phiên 15h/22h trên client trong lần triển khai 07h.

## JAR và khôi phục

- JAR đang dùng: `SRC/20.jar`.
- SHA-256 mới: `b3265997819e09ec909d977501b7e39c39c5af464197ec48a09e7490b9bac97b`.
- Backup: `SRC/jar-backup/20-before-boss-visibility-20261006-072856.jar`.
- SHA-256 backup: `51019fb985e9c2db2fd14e65bec35d1ebab66c7a6cb3da88c7effb6ec23f38a6`.
- Metadata cuối cùng: `.analysis/boss-visibility-deployment-latest.json` và file `.deployment.json` cạnh backup.
- SQL snapshot sau khi server cũ lưu dữ liệu: `.analysis/boss-visibility-deploy-20261006-072856/team2026-before-boss-visibility.sql`.

Đã bảo trì lưu dữ liệu và xác nhận JVM cũ dừng trước khi thay JAR. Chỉ 5 entry class thay đổi so với bản trước, các entry khác giữ nguyên byte. Thư mục chạy chỉ còn app JAR `20.jar`; thư viện và JAR công cụ cần dùng giữ nguyên. Rollback phải bảo trì lưu dữ liệu rồi phục hồi backup; không cần đảo migration SQL.

Bằng chứng test: `.analysis/boss-visibility-verify/`; runtime: `.analysis/all-boss-visibility/after1.tsv`, `after2.tsv` cùng tệp managers/classes/mobs. Source đã sửa chưa commit/push GitHub trong lần triển khai này.
