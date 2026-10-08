# Satellite fixes implementation plan

Goal: sửa lỗi đã được người dùng yêu cầu sau báo cáo vệ tinh, triển khai JAR có backup; giữ bốn tác dụng, phạm vi và điều kiện bang.

Architecture: `SatelliteService` quản lý đặt, đếm, hết hạn và cấp hiệu ứng. Lock theo thứ tự player → zone lúc đặt; vòng cập nhật lấy snapshot rồi nhả lock zone trước khi khóa từng player. Không giữ item/zone monitor khi cập nhật player. Dùng packet -30/subcommand9 hiện có, đủ HP/HP tối đa, không có byte hiệu ứng và không gây đọc thiếu. Không đổi client.

Tech stack: Java17 bytecode, JDK21 compiler/runtime, Unity C# client hiện tại.

- [x] Regression test trên JAR cũ: race cap, spam một item, expiry nhà và tick hết hạn không hồi, stale membership, zero heal, packet parser, player snapshot/concurrency.
- [x] Implement `SatelliteService`; routing `UseItem`, `ItemMap`; snapshot item/player `Zone`; đồng bộ cờ `Satellite`.
- [x] Compile tất cả source, chạy satellite tests và regression black balls/Hirudegarn/Mabư; benchmark chỉ là fixture, không cam kết năng lực client.
- [x] So sánh nội dung hướng dẫn Omega giữ mới; build candidate; source hash guard.
- [x] Bảo trì lưu dữ liệu, xác nhận JVM dừng, snapshot SQL, backup JAR/SHA/metadata, cài và restart.
- [x] Probe JVM chỉ đọc xác nhận vệ tinh mới và event/map cũ; ghi tài liệu, báo giới hạn kiểm thử thực tế.

Acceptance: tối đa3/khu, dùng một item một lần, thất bại không mất item,30phút ở mọi map, không cấp hiệu ứng sau hết hạn/đổi khu, không packet khi HP/KI không đổi, không concurrent iteration khi đặt/di chuyển người, packetHP vẫn tương thích client hiện có. Không sửa cơ chế phòng thủ quái/PvP/boss hoặc buff20%tiềm năng.
