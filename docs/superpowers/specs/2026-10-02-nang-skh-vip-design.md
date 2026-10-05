# Thiết kế Nâng SKH VIP

Trạng thái 2026-10-02: đã triển khai source, thay SRC/20.jar và lưu JAR cũ trong SRC/jar-backup. Xem [kết quả triển khai](../../source-analysis/skh-vip.md); chưa chạy server hoặc kiểm thử client thật.

Ngày 02/10/2026. Trạng thái: đã kiểm tra source/SQL và chốt các quy tắc chính với người dùng; tài liệu để xem xét trước khi sửa gameplay.

## Quy tắc đã chốt

1. Chọn N món hủy diệt làm đồ chính và N món thần linh làm phôi; 1 ≤ N ≤ 5. Tổng nguyên liệu là 2–10 món.
2. Đặt nhóm hủy diệt trước, nhóm thần phía sau. Phôi không bắt buộc cùng hành tinh hoặc cùng loại với đồ chính.
3. Phôi thần không trùng loại: tối đa một áo, một quần, một găng, một giày và một nhẫn. Hai áo khác hành tinh vẫn bị coi là trùng loại.
4. Phí 1.000.000.000 vàng/cặp, thu theo số cặp thử: N=5 tốn 5.000.000.000 vàng/lượt. Không hoàn phí của cặp xịt.
5. Mỗi cặp có 50% thành công, 50% xịt, độc lập với các cặp khác.
6. Thành công: tiêu hao cả đồ hủy diệt và phôi, nhận một trang bị SKH có loại và hành tinh theo đồ hủy diệt.
7. Xịt: chỉ mất phôi thần; đồ hủy diệt giữ nguyên instance, chỉ số, sao và cấp nâng.
8. Random toàn bộ cấp trang bị như capsule 1655, không ép mọi kết quả về cấp 12. Chỉ số nền lấy mức tối đa có trong shop làng cho đúng template đã random.
9. Trong các kết quả thành công: cấp 1–8 chiếm tổng 95%; cấp 9–12 chiếm tổng 5%. Loại set lấy một trong năm bộ của hành tinh như capsule.

## Các cách xử lý đã chọn

- **Giao diện:** ưu tiên một tab combine, đặt hủy diệt trước và thần sau. Phương án hai bước chọn đồ chính/phôi chỉ là phương án thay thế nếu client giới hạn số ô dưới 10; không tự cắt lượt 5 cặp thành các lượt nhỏ.
- **Bảng đồ:** dùng catalog capsule có hai danh sách tách rõ. Capsule cũ giữ đúng danh sách và phân bố hiện tại; VIP dùng danh sách đủ 12 cấp được đối chiếu shop/SQL. Tránh copy bảng sang nhiều nơi hoặc sửa ngầm tỷ lệ capsule cũ.
- **Đổi đồ:** đặt kết quả thành công vào chính slot hủy diệt vừa tiêu hao, dọn các slot phôi. Không bắt người chơi chừa thêm N ô trống và không thêm đồ sau khi đã xóa nguyên liệu bằng một API có thể trả false.

## Cách hiểu cần thể hiện rõ trong giao diện

- Thành công tạo đồ mới; không chuyển sao pha lê, cấp nâng hay chỉ số của đồ chính/phôi sang kết quả.
- Quy tắc không trùng loại hiện chỉ áp dụng cho phôi thần. Không thêm điều kiện cấm trùng loại đồ hủy diệt khi người dùng chưa yêu cầu.
- Đồ chính có thể thuộc hành tinh khác nhân vật; mỗi món sinh kết quả theo hành tinh riêng của nó. Phôi không ảnh hưởng hành tinh đầu ra.
- Nhẫn hủy diệt 656 dùng chung, gender=3: rada đầu ra lấy set theo hành tinh người chơi (0/1/2). Đây là quy tắc đã chốt; ghi hành tinh cụ thể trong trang xác nhận. Không lấy hành tinh phôi và không chuyển gender=3 của template rada thành Xayda.
- Chốt hành tinh kết quả một lần cho từng cặp và dùng cùng giá trị để chọn catalog lẫn set. Với món chính khác nhẫn 656, luôn dùng hành tinh của món hủy diệt dù khác hành tinh người chơi. Nếu hành tinh không hợp lệ hoặc thay đổi so với preview, từ chối lượt trước khi thu phí/tiêu hao.
- Đề xuất từ chối nguyên liệu có hạn sử dụng, quantity khác 1 hoặc đã mang option SKH; đây là quy tắc kiểm tra bổ sung của thiết kế, không phải yêu cầu người dùng đã nêu riêng.
- Lượt 5 cặp có thể thành công 0–5 món. Không áp dụng quy tắc “xịt vẫn được 3 món” trong dự thảo SKH Thường vào VIP.

## Đối chiếu capsule và shop

- SQL: item 1655 là `Cápsule Kích hoạt 1 món tự chọn`. Item 1787 là vé riêng tư, không phải capsule SKH dù comment trong UseItem gọi nhầm.
- `UseItem.CapsuleKichHoat` mở menu áo/quần/găng/giày/rada. `NpcFactory` gọi `ItemService.OpenSKH(player, 1655, select)`.
- `OpenSKH` lấy hành tinh nhân vật, random template trong nhóm được chọn, random đều một trong năm option tên set, rồi gọi `createItemSKH`.
- `createItemSKH` lấy chỉ số từ `getListOptionItemShop`, thêm option tên, hiệu ứng qua mapping chung và option 30. Không có roll min–max cho chỉ số cơ bản.
- Shop làng BUNMA/DENDE/APPULE là shop 1/2/3, tab 1–9. Không dùng shop 31 `SHOP_VIP` của Santa để xác định trang bị VIP trong yêu cầu này.
- Shop không gắn cờ VIP cho trang bị; nhóm VIP cấp 9–12 là phân loại đã được người dùng chốt.
- Rada có ba định nghĩa shop làng, cùng chỉ số trong dump hiện tại. Helper lấy định nghĩa đầu tiên và trả các tham chiếu ItemOption dùng chung; builder VIP phải tạo ItemOption mới.

Trong dữ liệu hiện tại, mỗi template chỉ có một mức chỉ số nền tương ứng; vì vậy “max” là mức shop của đúng template, không phải chỉ số của cấp 12 áp vào mọi cấp thấp. Nếu dữ liệu shop có nhiều định nghĩa cho cùng template, lấy param lớn nhất của từng option nền từ các shop làng.

### Chỗ cần chuẩn hóa riêng cho danh sách VIP

| Nhóm | Bảng capsule hiện tại | Danh sách VIP đề xuất |
| --- | --- | --- |
| Găng Trái Đất | Thiếu 255 — Găng vàng Goku, cấp 10 | Bổ sung 255 |
| Áo Namek | Thiếu 234 — Áo sắt Tron, cấp 9 | Bổ sung 234 |
| Găng Namek | Thiếu 258 — Găng sắt Tron, cấp 9 | Bổ sung 258 |
| Quần Xayda | Lặp 174, thiếu 175 — Quần da Xayda, cấp 8 | Một 174 và một 175 |

Đã kiểm tra 15 nhóm hành tinh × loại đồ: sau chuẩn hóa mỗi nhóm có 12 template khác nhau, đủ level 1–12, đúng type/gender và có option shop làng. Danh sách và chỉ số SQL chi tiết: [skh-vip-capsule-audit.json](../../source-analysis/skh-vip-capsule-audit.json).

## Random

Mỗi cặp lần lượt xét: thành công → nhóm cấp → template trong nhóm → loại set. Chỉ cặp thành công mới cần roll đầu ra. Các lần roll độc lập, không bảo đảm các kết quả cùng một set.

| Bước | Tỷ lệ |
| --- | --- |
| Thành công / xịt | 50% / 50% |
| Cấp 1–8, trong món thành công | Tổng 95%, chia đều 8 cấp, mỗi cấp 11,875% |
| Cấp 9–12, trong món thành công | Tổng 5%, chia đều 4 cấp, mỗi cấp 1,25% |
| Mỗi bộ SKH của hành tinh | 20% |

Một cặp có 2,5% khả năng vừa thành công vừa ra nhóm cấp 9–12. Cấp 12 cụ thể với một bộ set cụ thể có xác suất 0,125% trên một cặp. Năm cặp trung bình thành công 2,5 món; xác suất xịt cả năm là 3,125%. Đây là hệ quả của các roll độc lập, không phải bảo đảm kết quả cho từng lượt.

## Bảo toàn dữ liệu và kiểm chứng

Kiểm tra danh sách/slot/identity/số lượng trước preview và kiểm tra lại lúc xác nhận. Preview không roll hoặc trừ đồ/vàng. Dùng khóa cùng chuẩn combine hiện tại và kiểm tra toàn bộ chi phí trước mutation. Tạo đủ kết quả trước khi ghi thay đổi; đóng lựa chọn sau một lần xử lý để chống bấm lặp. Không gọi removeItem có dispose lên instance cần giữ để rollback hoặc lên đồ hủy diệt bị xịt.

Test tối thiểu: N=1–5; phôi khác loại/hành tinh; phôi trùng loại; thứ tự sai; số cặp lệch; đầu ra đúng loại/hành tinh; rada cho cả ba hành tinh người chơi với đủ năm lựa chọn set mỗi hành tinh; hành tinh không hợp lệ; lượt trộn rada và đồ hủy diệt khác hành tinh người chơi; phí 5 tỉ không tràn int; thành công/xịt hỗn hợp; xịt giữ toàn bộ đồ chính; đủ/thiếu vàng; hành trang đầy; nguyên liệu đã di chuyển; bấm xác nhận hai lần; biên random và bảng đủ 12 cấp; deep copy option. Sau đó chạy lại toàn bộ test mapping, hiệu ứng set, kinh tế vàng và pha lê.

Server nhận số item qua packet -81 và giữ thứ tự, không thấy giới hạn 5 trong handler. Workspace chỉ có source server; chưa xác minh giới hạn giao diện client. Cần kiểm tra client chọn đủ 10 món trước khi kết luận giao diện một bước đáp ứng lượt 5 cặp.
