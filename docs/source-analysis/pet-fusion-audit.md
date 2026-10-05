# Kiểm tra hợp thể Uub, Kid Beer, Kid Jiren

Báo cáo lịch sử trước sửa trên JAR cũ. Công thức hiện tại đã được sửa và triển khai; xem [pet-fusion-20-percent.md](pet-fusion-20-percent.md).

Ngày 02/10/2026. Chỉ kiểm tra source và chạy probe trên JAR hiện tại, không sửa production/JAR/SQL.

## Giới hạn sức mạnh

NPoint.MAX_LIMIT=9; POWER_LIMITS[9]=90_010_000_000, tức 90 tỷ 10 triệu. getPowerNextLimit ở bậc 9 trả 0 và OpenPowerService chặn mở thêm khi limitPower>=9. powerUp tăng dương cho người chơi/đệ dùng min với phần còn lại dưới trần. Probe đã thử sát trần, tăng Long.MAX_VALUE và tiếp tục tăng 1: cả sư phụ lẫn đệ dừng đúng 90.010.000.000, không vượt hoặc tràn long trong trường hợp đó. Không kiểm chứng mọi nguồn ghi trực tiếp power hoặc dữ liệu vốn đã vượt trần.

Kế hoạch khởi tạo đệ 40 tỷ ở bậc 4 phải giữ đầy đủ bậc 5–9 và trần trên; không sửa bảng POWER_LIMITS.

## Dòng tính chỉ số

NPoint.calPoint tính đệ trước, rồi sư phụ. Khi sư phụ hợp thể khác NON_FUSION, setHpMax/setMpMax/setDame cộng hpMax/mpMax/dame của đệ vào phần sư phụ. setDef/setCrit không cộng giáp/chí mạng của đệ.

Với Porata 1/2/3 (typeFusion 6/8/9), ba đệ có các nhánh riêng:

| Đệ | HP phần đệ | KI phần đệ | Sức đánh phần đệ | Giáp/chí mạng chuyển sang sư phụ |
| --- | --- | --- | --- | --- |
| Uub, typePet 2 | Tăng 20% một lần | Cộng 20% từ mpMax cũ, phụ thuộc lần tính trước | Tăng 20% một lần | Không |
| Kid Beer, typePet 3 | Tăng 20% một lần | Như Uub | Hai nhánh cùng tăng 20%: x1,2 x1,2, khoảng +44% | Không |
| Kid Jiren, typePet 4 | Tăng 20% một lần | Như Uub | Tăng 20% một lần | Không |

Phần tăng riêng này áp dụng lên chỉ số của đệ ở vị trí tương ứng trong chuỗi tính, không phải cộng thẳng 20% tổng chỉ số sư phụ. Trang bị/buff/set và thứ tự áp dụng có thể thay kết quả cuối. Porata 2 đọc toàn bộ option item 921 trong túi sư phụ; Porata 3 đọc item 1819. Đây là tác động bông tai của sư phụ, không phải đặc tính riêng một loại đệ.

Hợp thể thường LUONG_LONG_NHAT_THE (4) và HOP_THE_GOGETA (10) vẫn cộng HP/KI/sức đánh đệ nhưng không chạy các bonus riêng nêu trên. Cải trang/cặp đồ Gogeta có nhánh khác, không kết luận từ tên mode rằng mọi hợp thể Gogeta đều có bonus cố định.

## Hai phát hiện

1. NPoint.setDame có hai điều kiện typePet==3 với cùng ba loại Porata, mỗi điều kiện cộng 20%. Với đệ không đồ/buff, dame 20.000 thành 28.800; Uub/Jiren thành 24.000. Đây là cộng trùng trong source hiện tại, không có chênh lệch mới do kế hoạch bậc 4.
2. NPoint.setMpMax khai báo biến local mpMax, nhưng cả ba bonus đệ dùng this.mpMax (kết quả lần trước). Nếu đệ có KI 400.000 trước hợp thể, ba lần tính lại liên tiếp cho 480.000, 496.000, 499.200 mặc dù dữ liệu đầu vào không đổi. Công thức không bất biến khi tính lại. Khác với HP dùng đúng local hpMax.

Thông tin đã giải thích trước đó rằng cả ba luôn cộng 20% HP/KI/sức đánh được đính chính bởi kết quả này. Chưa sửa hai đoạn code này vì người dùng đang yêu cầu kiểm tra.

## Kiểm chứng thực tế bằng JAR

Probe .analysis/PetFusionAudit.java chạy với SRC/20.jar đứng đầu classpath, tạo fixture sư phụ HP/KI 100.000, dame 1.000, giáp gốc 100 (giáp thực 400), chí mạng 5; đệ HP/KI 400.000, dame 20.000, giáp gốc 9, chí mạng 2; không đồ/buff/set. Chạy 3 loại x 6 mode x 3 lần tính lại = 54 trường hợp. Ví dụ Porata 1:

| Loại | HP sư phụ | KI sư phụ, lần 1 → 2 → 3 | Dame sư phụ | Giáp | Chí mạng |
| --- | ---: | --- | ---: | ---: | ---: |
| Uub | 580.000 | 580.000 → 596.000 → 599.200 | 25.000 | 400 | 5 |
| Kid Beer | 580.000 | 580.000 → 596.000 → 599.200 | 29.800 | 400 | 5 |
| Kid Jiren | 580.000 | 580.000 → 596.000 → 599.200 | 25.000 | 400 | 5 |

54 phép đối chiếu xác nhận công thức hiện tại và hai bất thường, không phải 54 chứng nhận công thức không có lỗi. Log .analysis/pet-fusion-audit/runtime.log. Đã đối chiếu SHA-256 installed JAR với metadata và class NPoint trong JAR với build cuối. Chưa thử client thật, không thay JAR hay restart server.
