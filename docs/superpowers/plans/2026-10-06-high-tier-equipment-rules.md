# Đồ Hủy Diệt/Thiên Sứ: khóa giao dịch và yêu cầu sức mạnh

**Trạng thái:** đã triển khai. 61 lượt kiểm thử PASS trên classes, candidate và JAR đã cài; runtime xác nhận đủ 28 món, cache shop và hash class. Server PID13408, cổng14445. Client Game1/Game2 đã sửa và full Unity Roslyn compile PASS; cần chạy lại Play để xem giao diện, chưa kiểm tra trực tiếp hình ảnh trong Unity. Báo cáo: `docs/source-analysis/high-tier-equipment-rules-2026-10-06.md`.

Người dùng yêu cầu khóa giao dịch toàn bộ 13 món Hủy Diệt650–662 và15món Thiên Sứ1048–1062, bao gồm đồ đã có. Hiển thị yêu cầu sức mạnh như tooltip shop cho áo/quần/găng/giày/rada (nhẫn), cả Game1/Game2. Giá và bonus Beerus giữ nguyên.

Mức đã chốt: Hủy Diệt áo40, quần44, găng50, giày43, nhẫn47 tỷ; Thiên Sứ tất cả80tỷ. Dùng option21 vì power_require SQL/template hiện là int, không chứa được các mức này. Chuẩn hóa option21 và30 tại lớp hoàn thiện vật phẩm đã dùng ở factory/grant/login; kiểm tra mặc đồ dùng cùng quy tắc, không dựa vào option thiếu/cũ. Chặn trade/ký gửi trực tiếp bằngID để đồ legacy không có30 cũng không lọt; không có ngoại lệ cho optionấn/87. Không sửa chỉ số nền, bonus hoặc thời hạn.

- [x] TDD kiểm tra28ID, legacy trade/ký gửi, dedup30, untouchedThần.
- [x] TDD powerpolicy, factory/load/grant và mặc đồ đúngngưỡng/belowcho5slot; áp dụng policyvàoInventoryService.
- [x] Đồng bộ option21/30 trong tất cả shop chứa28ID, migration idempotent và4dump/seed; không đổi power_requireint sangsố vượt phạmvi.
- [x] Tooltip cả2client lấy option21tỷ nếu có, fallbacktemplate; hiển thị một dòng sức mạnh yêu cầu và không lặp lại option21 trên trangbị.
- [x] Compilefullsource, hồi quy59Beerus/ThiênSứ/Santa/ruby/kýgửi/points, thêmtests mới; reviewdiff chỉ scopefeature.
- [x] Maintenance lưu dữ liệu, backup JAR timestamp/hash, snapshotSQL và rollbackoptions phạmvi28ID, install/start/verify cache/classhash/singleJAR. Client sửa sourceUnity; nêu rõ chưabuildbinaryUnitynếukhôngbuildđược.
