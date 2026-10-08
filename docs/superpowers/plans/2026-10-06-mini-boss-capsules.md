# Boss mini: sửa lỗi và capsule hồng ngọc

Cập nhật sau rà soát: cả bốn loại được cấp ID mạng âm riêng từng instance để tránh client nhầm boss đứng chung khu; kiểm tra Sói bằng loại đối tượng. Thưởng Sói, đánh Sói và cho ăn cùng thứ tự khóa người chơi → Sói. Đệ tử thường và đệ tử triệu hồi đều ghi quyền nhặt về chủ.

Người dùng yêu cầu kiểm tra Ăn Trộm, Sói Hẹc Quyn, Mặt Trời, Ở Dơ; thêm1–5capsule722 khi hạ boss, riêng người nhận thưởng nhặt được đến lúc vật phẩm hết hạn. Sói được chốt có cả hai cách: không có xương có thể đánh chết; có xương trong hành trang/rương thì đòn đánh nhắc dùng xương. Xương trong rương phải lấy ra hành trang để dùng. Pet nhận thưởng về chủ.

Thiết kế: helper riêng mỗi instance boss, claim một lần mỗi đời, resetBase mở lại quyền; rơi1–5 vật phẩm riêngquantity1 để quest nhặt capsule tính từng viên. Khóa ItemMap.keepOwnerUntilDespawn, không đổi sang public sau45giây. Vẫn giữ quà/điểm cũ hợp lệ; invalidkiller/map không tiêu thụ claim.

Mặt Trời: sửaIDVirut sangMAT_TROI; bỏng30giây xử lý mỗi active, không gia hạn nếu bị áp dụng trùng, UI30giây, xóa khi rời/xuất hiện lại; giữ xác suất80% theo code và100HP/1damage mỗihit. Ăn Trộm: HP1–100 thay0–99, synchronizedinjured, tránh castskill sau khi chết. Ở Dơ:600giây thay600000giây, reset timer, regen không bị bỏ qua khi thiếu target, cập nhật HP sau sát thương. Sói: sát thương bình thường nếu không cóbone460 hợp lệ; bonefeed atomically cấp quà/claim/consume, không khóa luồng mạng5giây; sửa timerjoin/fed, kiểm tra loại boss/range/map/alive.

Giữ số boss tự sinh:5 Ăn Trộm,2 Sói,5ỞDơ,20MặtTrời; hồi sinh10phút sau rời map, MặtTrời/ĂnTrộm/ỞDơ tự rời sau15phút, Sói sau100–300giây hoặc5giây sau cho ăn. Giữ map spawn như hiện có.

Kiểm tra: TDD helper bounds/ownership/expiry/duplicate/reset/pet/race; tích hợpreward4boss; Sun effects/lifecycle/ID; Sói2cách/box/range/fullbag/cast/race; hồi quy bossvisibility, death, ruby/quests và63 test trước. Runtime đọc bosscount/map/ID/status/rest/registration. Triển khai theoAGENTS: maintenance lưu dữ liệu, snapshotSQL, backupJARtimestamp/hash, chỉ mộtJAR chạy; không thử kill trên tài khoản người chơi thật.
