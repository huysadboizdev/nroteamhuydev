# Kiểm tra Hổ mập vàng 942 – 07/10/2026

Kết quả: chưa tái hiện lỗi phía server ở các đường dùng, trang bị, chỉ số, triệu hồi, khôi phục pet đang mặc và tháo vật phẩm. Chưa có căn cứ kỹ thuật từ các kiểm tra này để tạm loại điều ước/vật phẩm Hổ mập vàng.

Kiểm tra trên JAR đang cài `SRC/20.jar`, SHA-256 `df410a864f13cbdb586f2a34685b57fe43813f7015f3c9a4519754e6e445028b`. Không thay source sản phẩm, không thay JAR, không ghi SQL/live player và không restart server. Áp dụng `systematic-debugging` và `verification-before-completion`.

## Kiểm thử thực thi

`SRC/test/nro/models/services/TigerPetAuditTest.java` dùng `UseItem.useItem`, `InventoryService.itemBagToBody/itemBodyToBag`, `NPoint.calPoint`, `PetService.Pet2`, `NewPet.joinMapMaster/dispose` và `Player.sendNewPet` thật từ JAR. Fixture chỉ thay đầu ra packet và catalog tối thiểu, không thay thuật toán trang bị/chỉ số/pet.

Ba hồ sơ option hợp lệ được cấp bởi điều ước Namếc: `77/10` (%HP), `103/10` (%KI), `50/10` (%sức đánh), mỗi hồ sơ kèm option `30/0`. Với mỗi hồ sơ:

- Dùng vật phẩm chuyển đúng Item từ túi sang body slot 7, không nhân bản vật phẩm.
- Model NewPet là head/body/leg `966/967/968`, HP/KI dương và thuộc đúng chủ.
- Tính lại chỉ số 10 lần vẫn chỉ cộng đúng 10% của option đang trang bị.
- Pet vào khu của chủ và xuất hiện trong membership khu thật.
- Gọi `sendNewPet` khi pet đã có giữ đúng pet hiện tại.
- Triệu hồi lại bằng `Pet2` xóa pet cũ khỏi membership, bỏ master/zone cũ.
- Mô phỏng đăng nhập bằng pet đang mặc nhưng chưa có actor: `sendNewPet` tạo lại đúng model, đúng chủ.
- Tháo trả đúng Item về túi, xóa NewPet khỏi chủ/khu và khôi phục chỉ số ban đầu; slot 7 rỗng không triệu hồi lại.

Lệnh biên dịch Java 17 và chạy fixture đều exit 0. Log: `.analysis/tiger-pet-audit-20261007/result.log`; kết quả `TIGER_PET_AUDIT PASS 3 legitimate stat profiles`.

## Catalog và resource

SQL chỉ đọc xác nhận template `942`, type `27`, gender `3`, icon `8768`; part `966/967/968` lần lượt type `0/1/2`. Dữ liệu các part tham chiếu icon `2955`, `8761`–`8767`. Icon vật phẩm `8768` và toàn bộ icon part đều tồn tại ở `SRC/data/icon/x1` tới `x4`: 36/36 file có mặt.

## Giới hạn

Đây là kiểm thử trong JVM độc lập. Chưa quan sát ảnh/animation trực tiếp bằng client Unity, chưa đăng nhập qua mạng thật, chưa thực hiện điều ước Namếc thật hoặc kiểm tra lưu/load SQL toàn tuyến. Ba option HP/KI/sức đánh được kiểm tra; các option ngẫu nhiên khác của điều ước không được kiểm thử riêng trong tác vụ này. Việc đủ file ảnh không chứng minh client thực tế hiển thị mọi frame đúng.
