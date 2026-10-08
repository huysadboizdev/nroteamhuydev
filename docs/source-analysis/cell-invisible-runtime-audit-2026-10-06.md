# Xên tàng hình — kiểm tra trực tiếp 06/10/2026

Người chơi báo Xên hoàn thiện ở Võ đài Xên bọ hung tàng hình nhưng vẫn đánh. Kiểm tra chỉ đọc trên serverPID9156 lúc07:14:56 UTC+7; không dừng server, không thay boss, không sửa sourceứngdụng/JAR.

Runtime `SieuBoHung` id-101: `ACTIVE`,level0,HP150000000/150000000,typePk5,prepareBomfalse,callCellConfalse,map103/khu0, vị trí331/288; `inBossList=false`, `inHumanoids=false`. Lúc snapshot khu0 không có người chơi. `XenBoHung` id-100/map100/khu0 cũng không có trong hai danh sách. Không phát hiện deadlock.

Nguyên nhân đối chiếu source: `Boss.joinMap` gán `this.zone` trước khi gọi `ChangeMapService.changeMap`. `commitToMap` có dòng `if(player.zone==zoneJoin)return true;`, vì vậy bỏ qua `zone.addPlayer` dù boss chưa được đăng ký vào danh sách. BossManager vẫn cập nhật boss từ danh sách riêng; người chơi vào khu nhận danh sách nhân vật qua `Zone.load_Another_To_Me` nên không được gửi boss bị thiếu. Đây là lỗi đăng ký nhân vật khi vào khu, phù hợp tình trạng vẫn đánh nhưng không hiện.

Hướng sửa cần kiểm thử: trường hợp cùngzone phải đảm bảo membership đầy đủ/idempotent, không gọi exitMap hoặc đăng ký trùng. Kiểm tra boss đã gánzone, spawn đầu, Xêncon, chuyển giai đoạn, người vào sau spawn và đổi khu; giữ admission/capacity/deadline của blackballs/Hirudegarn. Server đang online có các boss bị thiếu membership từ trước, cần xử lý trạng thái cũ khi triển khai hoặc khởi động lại qua bảo trì lưu dữ liệu. Chưa áp dụng sửa/hotfix trong lần kiểm tra này.

Bằng chứng: `.analysis/cell-visibility/runtime1.txt`; công cụ `CellVisibilityProbe.java` chỉ đọc trạng thái boss/list và deadlock.
