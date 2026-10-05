# Mở giới hạn sức mạnh tại Quốc Vương

## Phí hiện tại: 500 triệu mọi bậc

Theo yêu cầu tiếp theo ngày 2026-10-02, mọi lần mở đều trừ **500.000.000 vàng**, bao gồm bậc 0–3 và cả sư phụ/đệ tử. Đã bỏ menu mở chờ miễn phí; API mở thường và timer cũ không còn tăng giới hạn. Bậc thấp trừ phí ở dịch vụ, NPC không trừ lần hai. Điều kiện sư phụ 17.999.999.999 và lựa chọn đồ đang mặc của sư phụ/đệ tử ở bậc cao giữ nguyên.

Chín lần mở từ bậc 0 đến 9 tốn **4,5 tỷ vàng**, cùng 2 thần linh + 2 hủy diệt + 1 thiên sứ cho năm bậc cao. Kiểm thử đủ/thiếu 1 vàng, số dư long, cả hai chủ thể, menu hủy/xác nhận, timer và chuỗi mở đã đạt **49/49 lượt source và 49/49 trên JAR**. Review độc lập không còn lỗi xác nhận được.

Đã chuyển nguyên vẹn JAR cũ vào `SRC/jar-backup/20-before-uniform-power-fees-20261002-160122.jar` trước khi cài mới; kiểm tra checksum backup và JAR mới. SHA-256 mới: `f9782f004d1d42aeb13569e0458753d7b113a7b3c70d26f51a032de0de7d701e`. Chỉ còn `SRC/20.jar` trong thư mục chạy, không còn JAR dư trong các folder release. Server mới PID 1672 đang lắng nghe cổng 14445. Chưa kiểm tra thao tác tiêu hao trên tài khoản thật/client hoặc lưu-đăng nhập lại.

Các đoạn bên dưới có phí 50 triệu/mở thường là lịch sử bản trước; đã được thay bằng phí đồng nhất ở mục này.

## Quy tắc hiện tại sau điều chỉnh

Sư phụ đạt **17.999.999.999 sức mạnh** là có thể mở lần lượt mọi bậc đến tối đa cho bản thân hoặc đệ tử, không yêu cầu người được mở đạt trần sức mạnh từng bậc. Người dùng đã chọn mở từng bậc, không thêm nút mở toàn bộ. Trần và phí giữ nguyên: bậc 0–3 mở ngay 50 triệu/bậc; bậc 4/5 mất một thần linh, 6/7 mất một hủy diệt, 8 mất một thiên sứ, mỗi lần 500 triệu vàng.

Mở bản thân dùng đồ bản thân đang mặc. Mở đệ tử có thể chọn đồ đang mặc của **sư phụ hoặc đệ tử**; yêu cầu cấp đồ theo bậc của đệ tử, vàng luôn lấy từ sư phụ. Menu ghi nguồn đồ và xác nhận toàn bộ món sẽ mất. Preview giữ thêm wearer; xác nhận kiểm tra lại gate sư phụ và nguồn món, tiêu hao đúng body của wearer. Tính lại set/chỉ số và ngoại hình cho cả người mở và người mất đồ.

Đã đạt **48/48 kiểm thử source và 48/48 trên JAR**, gồm hai mode mới `master-gate`/`master-gear` bắt lỗi trước sửa trên JAR cũ và kiểm tra chuỗi mở 0–9 khi sức mạnh sư phụ giữ nguyên 17.999.999.999, đệ tử sức mạnh 0. Phí cả chuỗi mở ngay là 2,7 tỷ vàng (4×50 triệu + 5×500 triệu); vật phẩm năm bậc cao là 2 thần linh, 2 hủy diệt, 1 thiên sứ, mỗi món phải đang mặc lúc xác nhận. Review độc lập không còn vấn đề xác nhận được.

Đã bảo trì bằng `bt`, xác nhận Java cũ dừng, sao lưu nguyên vẹn `SRC/jar-backup/20-before-master-power-gate-20261002-154836.jar`, cài và kiểm tra SHA-256 JAR mới `2e1c624b0991af385c4c80401d014d9e23e7e907781f279dd1994a78707656c2`. Server mới PID 17580 đã lắng nghe cổng 14445. Chưa tiêu hao trên tài khoản thật hoặc kiểm tra client/lưu-đăng nhập lại.

Các mục phía dưới ghi lịch sử bản triển khai trước điều chỉnh; yêu cầu đạt trần từng bậc và chỉ lấy đồ của người được mở đã được thay thế bằng quy tắc ở trên.

## Lịch sử bản trước

Đã triển khai ngày 2026-10-02 theo kế hoạch được người dùng duyệt. Quốc Vương là NPC duy nhất mở giới hạn cho bản thân và đệ tử; Tổ sư Kaio giữ luyện tập, không còn menu hoặc handler mở giới hạn.

| Sức mạnh tối thiểu | Trần sau mở | Trang bị đang mặc bị tiêu hao | Vàng |
| ---: | ---: | --- | ---: |
| 39.999.999.999 | 49.999.999.999 | 1 thần linh | 500.000.000 |
| 49.999.999.999 | 59.999.999.999 | 1 thần linh | 500.000.000 |
| 59.999.999.999 | 69.999.999.999 | 1 hủy diệt | 500.000.000 |
| 69.999.999.999 | 79.999.999.999 | 1 hủy diệt | 500.000.000 |
| 79.999.999.999 | 90.010.000.000 | 1 thiên sứ | 500.000.000 |

Mở cho ai lấy đồ đang mặc của người đó ở một trong năm ô áo/quần/găng/giày/rada, vàng lấy từ chủ/nhân vật người chơi. Thần linh ID 555–567, hủy diệt 650–662, thiên sứ 1048–1062. Menu cho chọn món rồi xác nhận, hiện tên/ô/sao/cấp nâng và các tùy chọn; món bị mất nguyên vẹn cùng mọi option. Chỉ trừ đúng một món và phí khi đủ điều kiện, mở ngay thành công 100%.

Dưới 40 tỷ giữ mở thường cho bản thân (2 giờ 24 phút) hoặc mở ngay 50 triệu vàng cho bản thân/đệ tử. Cả hai cách phải đạt trần hiện tại. Từ bậc 4, chặn cả API mở thường/mở nhanh cũ; timer cũ bị hủy để không bỏ qua đồ/500 triệu. Trần chỉ số gốc giữ nguyên.

Preview giữ chủ thể, bậc, slot, instance và trạng thái template/options/quantity; xác nhận kiểm tra lại sức mạnh, phí, sở hữu đệ tử và giao dịch. Xóa preview trước commit để chặn dùng lại. Tiêu hao và phí dùng cùng thứ tự khóa chủ → inventory với nâng SKH/bán đồ. Các thao tác chuyển đồ đang mặc và snapshot lưu PlayerDAO cùng khóa chủ để tránh lấy lại món hoặc lưu lệch vàng/đồ/bậc.

Đã sửa powerUp cho nhân vật/đệ tử: tăng dương dừng tại trần và tránh overflow; dữ liệu đã vượt trần không bị hạ xuống nhưng không tiếp tục tăng cho tới khi mở bậc phù hợp. Không áp dụng giới hạn mới này lên boss/bot. Các giá trị đã lưu và bậc 9 không bị thu phí hồi tố.

## Kiểm chứng

- Compile 552 file production exit 0; JAR không trùng entry, không chứa test/cache, mọi class production khớp compile.
- **46/46 lượt source và 46/46 lượt trên chính JAR đạt**, gồm 10 mode mới: thresholds/equipment/fees/confirmation/replay/legacy/pet/menu/timer/growth.
- Test trước sửa xác nhận trần cũ 50.010.000.000 sai với yêu cầu 49.999.999.999; các API/menu/timer cũ không đúng quy tắc mới. Growth test trước clamp phát hiện vượt trần với cap-1 + 2.
- Test bao phủ template/ô đúng SQL, cả bản thân và đệ tử, món được chọn, phí long/đủ/thiếu, sức mạnh thiếu/đúng/vượt, đổi/tháo/options/quantity/bậc/vàng/giao dịch sau preview, hủy và xác nhận lặp/đồng thời, menu Quốc Vương/Kaio và timer.
- Hai review độc lập về yêu cầu/chất lượng đã kiểm tra lại sau sửa, không còn blocker xác nhận được.
- javac trong sandbox có chẩn đoán AccessDeniedException khi đóng dependency ZIP; compile exit 0 và toàn bộ class/JAR cùng kiểm thử runtime đã đối chiếu. Không coi là đã kiểm chứng client thật hoặc round-trip database.

## Triển khai

Đã thay `SRC/20.jar` bằng JAR kiểm thử, SHA-256 `dbcda5dc488cbb9bd322fb4930af6c7487486377a51b9628948b7483d7c8f943`. Server đã dừng trước lúc thay, không gửi bt hoặc kill tiến trình. Sao lưu bản cũ nguyên vẹn tại `SRC/jar-backup/20-before-quoc-vuong-power-20261002-153902.jar`; checksum backup khớp JAR cũ.

Khởi động bằng `run.bat` trong cửa sổ ẩn. Xác nhận Java PID 17360 chạy `20.jar` và lắng nghe cổng 14445. Metadata ở `.analysis/latest-deployment.json`; JAR dự phòng mới ở `SRC/20-quoc-vuong-power.jar`.

Chưa thao tác trên tài khoản thật để tiêu hao đồ/vàng, chưa kiểm tra trực tiếp client hoặc lưu/đăng nhập lại. Cần kiểm tra trong game bằng tài khoản kiểm thử; không coi kiểm thử offline là xác nhận những bước này.
