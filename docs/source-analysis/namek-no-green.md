# Đã bỏ ngọc xanh rơi từ boss Tiểu đội sát thủ Namek

Đã xóa khối rơi ngọc ID77/type10 trong SO1_NM, SO2_NM, SO3_NM, SO4_NM và TDT_NM. Capsule722, cải trang, ngọc rồng, điểm sự kiện và cơ chế thưởng mỗi đời vẫn được kiểm thử. Không thay số dư hay DB.

Kiểm thử trước sửa thất bại đúng vì vẫn rơi77. Sau sửa, candidate67/67 nhóm PASS; kiểm thử reward và respawn trên SRC/20.jar PASS. Chỉ5 class boss thay đổi. Server dừng bằng bảo trì có lưu, xác minh cổng đóng trước MOVE/cài; cơ chế tự khởi động chạy PID17472 lúc sau thời điểm cài. Runtime hồng ngọc/vé/phí PASS, cổng14445 hoạt động. Chưa thao tác bằng client.

JAR cũ: `SRC/jar-backup/20-before-namek-no-green-20261004-135938.jar`.

SHA cũ: `3115c50ac2d663dbb66e0b76f8b6db8481557a4b76133c6d3dbdd48d77508b94`.

SHA mới: `016d54f85ade9e51e9c32dc94f227d96208f3829f487e1da6bc4e1e06e5069b7`.

Source backup và logs: `.analysis/namek-no-green/`. Metadata cạnh JAR backup và `.analysis/namek-no-green/deployment.json`. Khôi phục cần bảo trì lưu dữ liệu, backup JAR hiện tại trước thay và đối chiếu SHA; không cần rollback DB cho thay đổi này.
