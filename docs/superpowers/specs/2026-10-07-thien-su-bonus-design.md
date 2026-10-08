# Thiết kế bonus chế tạo Thiên Sứ — 2026-10-07

**Trạng thái:** Đã được người dùng duyệt và triển khai; xem `docs/thien-su-crafting-deployment-20261007.md`. Giữ tài liệu này làm mô tả thiết kế đã chốt.

## Yêu cầu người dùng

1. Bonus phụ có tối đa 7% trên mỗi dòng.
2. Bonus chỉ số chính của đồ từ 1–30%, là phần tăng so với chỉ số gốc.
3. Công thức: 999 mảnh + 1 công thức + 1 đá nâng cấp + 1 đá may mắn + 50 hồng ngọc; yêu cầu mặc đủ bộ Hủy Diệt.
4. Mỗi lượt chế tạo hợp lệ tiêu món Hủy Diệt đang mặc tương ứng với loại đồ tạo ra, cả khi thành công và khi thất bại theo tỷ lệ.

## Thiết kế đề xuất

- Giữ ngẫu nhiên 1–3 dòng khác nhau từ sức đánh %, HP %, KI %, giảm sát thương % và sức đánh chí mạng %. Mỗi dòng lấy số nguyên 1–7 đều nhau, bao gồm cả hai đầu. Một món không có hai dòng cùng loại. Mức 7% là giới hạn trên mỗi dòng của món được tạo, không giới hạn tổng hiệu ứng từ các món khác trên nhân vật.
- Giữ xác suất thành công, xác suất có dòng phụ và cách chọn số dòng theo đá hiện tại; không làm mọi món chắc chắn có ba dòng/7%.
- Mỗi món thành công lấy một lần bonus chính nguyên 1–30 đều nhau. Chỉ áp dụng cho chỉ số chính: áo giáp option47, quần HP option22, găng sức đánh option0, giầy KI option23, nhẫn chí mạng option14. Áp dụng trước khi thêm dòng phụ; không nhân dòng phụ thêm 30%.
- “Gốc” là giá trị vừa được `ItemService.DoThienSu` tạo ra, đã gồm ưu thế hành tinh 10%, trước đoạn nhân đôi bị lỗi hiện tại. Không lấy giá trị đã bị nhân đôi làm gốc. Áp dụng đúng một lần.
- Làm tròn tăng lên theo đơn vị option hiện có, đồng thời kẹp mức tăng không quá phần nguyên của 30% gốc. Công thức số nguyên dùng long: `increase = min((base * roll + 99) / 100, base * 30 / 100)`; `result = base + increase`. Chỉ số nền của các đồ này đều đủ lớn để khoảng tăng hợp lệ khác 0. Cách này bảo đảm đồ mới tăng ít nhất 1% và không vượt 30% kể cả nhẫn, nhưng tỷ lệ thực có thể khác số roll một chút do độ phân giải số nguyên.
- Ví dụ: HP nền 120.000 → 122.000–156.000 vì option22 lưu theo đơn vị nghìn; sức đánh nền 10.000 → 10.100–13.000; nhẫn nền 20% → 21–26% chí mạng. Nhẫn không thành 50% khi roll30 và không giữ nguyên20% ở roll thấp vì làm tròn xuống. HP/KI cũng chịu độ phân giải đơn vị nghìn, nên mức tăng thực được làm tròn và không vượt30%.
- Không tăng option21 yêu cầu sức mạnh, option30 khóa giao dịch, slot sao, timer hoặc các marker khác. Giữ yêu cầu 80 tỷ sức mạnh và chính sách không giao dịch/ký gửi.
- Đồ đã sở hữu giữ nguyên options, không reroll, không tự tăng và không tự sửa các món cũ bị nhân đôi. Chỉ áp dụng cho chế tạo mới qua luồng được sửa.

## Hai cách thực hiện được cân nhắc

1. **Khuyến nghị: thống nhất chế tạo rồi sửa bonus.** Có một hàm xem yêu cầu không trừ đồ và một hàm xác nhận, cùng validator/công thức/roller. Giải quyết lỗi tiêu nguyên liệu khi chọn, lệch hai công thức và nhân đôi chỉ số.
2. Chỉ sửa hai khoảng random ở code hiện tại: ít thay đổi hơn nhưng vẫn có luồng trừ đồ ngay và nhánh chế tạo không dùng bonus mới. Không đáp ứng một hành vi chế tạo thống nhất nên không khuyến nghị.

## Công thức đã chốt

Chính xác 4 nhóm vật phẩm trong túi: 999 mảnh cùng loại + 1 công thức thường/VIP + 1 đá nâng cấp + 1 đá may mắn; phí 50 hồng ngọc, không thu vàng. Hai loại công thức hiện cùng quy tắc bonus; không tự thêm lợi ích riêng cho VIP.

Phải mặc đủ 5 món Hủy Diệt tại thời điểm xác nhận. Tiêu đúng món đang mặc theo loại mảnh: 1066 → áo/ô0, 1067 → quần/ô1, 1070 → găng/ô2, 1068 → giày/ô3, 1069 → nhẫn/ô4. Không lấy món Hủy Diệt trong túi thay thế. Cả thành công và thất bại theo tỷ lệ đều mất món tương ứng, đủ nguyên liệu và 50 hồng ngọc theo lựa chọn người dùng. Sau lượt đó cần mặc bù bộ Hủy Diệt để chế tạo tiếp.

Thiếu nguyên liệu, thiếu hồng ngọc, túi đầy, trang bị đã đổi hoặc lỗi kỹ thuật nhận đồ không phải thất bại theo tỷ lệ: không tiêu tài sản. Bỏ cả hai mức phí vàng cũ khỏi luồng này.

## Luồng chế tạo đề xuất cho phương án khuyến nghị

1. Whis kiểm tra đủ 5 món Hủy Diệt để mở bảng như hiện tại; kiểm tra lại lúc xác nhận.
2. Chọn item chỉ hiện tên đồ kết quả, nguyên liệu, phí 50 hồng ngọc, món Hủy Diệt đang mặc sẽ mất kể cả thất bại, tỷ lệ thành công và hai khoảng bonus; không trừ tài sản, không roll hoặc tạo đồ.
3. Bấm xác nhận: khóa theo người chơi, chặn giao dịch/phần thưởng đang chờ theo các guard hiện có; kiểm tra item vẫn thuộc túi, số lượng đủ, không chọn trùng cùng đối tượng hoặc slot, đủ 50 hồng ngọc, đủ bộ Hủy Diệt và còn ô trống. Kiểm tra lại đúng món ở ô trang bị tương ứng, không dùng tham chiếu cũ sau khi người chơi đổi đồ.
4. Roll thành công một lần. Nếu thành công, tạo một đồ mới; tăng chỉ số chính một lần 1–30%; chọn dòng phụ như cũ, mỗi dòng 1–7%; loại bỏ option15 đánh dấu số dòng sai nghĩa. Hiển thị các dòng thật, không dùng option15 phản đòn để đánh dấu.
5. Chuẩn bị túi, trang bị và số dư hồng ngọc mới trong bản sao, kiểm tra khả năng nhận đồ trước khi thay trạng thái thật. Nếu nhận đồ lỗi hoặc dữ liệu thay đổi, không trừ tài sản. Nếu roll chế tạo thất bại hợp lệ, vẫn tiêu nguyên liệu, 50 hồng ngọc và món Hủy Diệt ở ô tương ứng; không phát đồ Thiên Sứ.
6. Công bố kết quả một lần, đồng bộ túi/trang bị/hồng ngọc và lưu qua cơ chế player hiện có. Tính lại bộ trang bị, HP/KI/sức đánh và gửi chỉ số sau khi tháo món Hủy Diệt. Đồ Thiên Sứ thành công vào túi, không tự mặc. Chặn bấm lại từ cùng lượt xác nhận để không trả thưởng/trừ đồ hai lần. Tạo xác nhận mới nếu muốn chế tạo lượt tiếp theo.

## Nghiệm thu

Kiểm thử tất cả 15 template/ba hành tinh, min/max và toàn bộ các roll; kiểm tra số nguyên trên nhẫn không đứng nguyên hoặc vượt +30%. Mỗi dòng phụ không vượt 7%, không lặp loại; hai loại bonus không nhân chéo. Mở bảng/chọn/hủy không mất tài sản. Thành công và thất bại theo tỷ lệ đều tiêu đúng món đang mặc, 999 mảnh, một công thức, hai đá và 50 hồng ngọc; không tiêu vàng hoặc bốn món Hủy Diệt còn lại. Xác nhận trùng/túi đầy/hồng ngọc thiếu/item đã chuyển/giao dịch chờ/lỗi nhận đồ không làm mất tài sản. Kiểm tra chỉ số sau mất đồ và yêu cầu mặc bù bộ cho lượt sau. Đồ cũ và các đường chế tạo khác không bị thay đổi. Build staging, rà soát, thử UI rồi cập nhật JAR theo quy tắc backup/bảo trì nếu người dùng duyệt triển khai.
