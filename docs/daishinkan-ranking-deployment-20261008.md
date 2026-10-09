# Daishinkan và bảng top — triển khai 08/10/2026

Đã tắt Bò Mộng ở nhà Gôhan/Moori/Broly (21/22/23), giữ các nơi khác. Daishinkan NPC111 ở làng Aru/Mori/Kakarot (0/7/14) tại x1100, y432/432/408; tránh portal phải x1224. Hình thật parts703/704/705, avatar6578; DB/binary part và icon x1–x4 đã kiểm tra. Client dùng giao thức hiện có; catalog vsMap6 buộc tải lại NPC khi đăng nhập.

Menu Top sức mạnh, Top nhiệm vụ, Đóng. Top100 từ dữ liệu đã lưu; cache mỗi bảng tối đa60giây. Sức mạnh viết tắt Việt Nam:59,4tỷ/850tr/120nghìn (có khoảng trắng trong game). Nhiệm vụ chỉ số, xếp theo nhiệm vụ/bước/tiến độ giảm dần và ID ổn định. Dùng projection SQL, không tải Player đầy đủ; loại JSON lỗi/âm/overflow; packet-96 đúng số hàng và trường theo phiên bản.

Kiểm chứng: build658sources;15nhóm tests trên classes và cùng15 trên candidate, gồm bảng top, SQL numeric/top100/JSON, NPC/menu, catalog refresh, thông báo dame/giftcode hồi quy. Migration isolated chạy lặp và conflict checks; live queries POWER/TASK đều trả2người chơi. Rà soát spec và quality không blocker.

Triển khai bằng bảo trì lưu dữ liệu, backup DB/JAR timestamp và đối chiếu SHA256. Server chạy PID 23484, port14445. Chỉ SRC/20.jar ở thư mục chạy.

- JAR mới SHA256: `ebf8e33593b2e6099a9ddff1b7bf3d32ac7c6313d6a04b1017a14dfad321190e`.
- JAR cũ: `SRC\jar-backup\20-before-daishinkan-20261008-145126.jar`, SHA256 `12218be2a27ebb7e2533589b95bbc05aeaeb5e12bf8adbc91d7fb348cb787ad1`.
- SQL backup: `.analysis\daishinkan-deploy-20261008-145126\team2026-before-daishinkan.sql`.
- Metadata: `.analysis/daishinkan-deployment-latest.json` gồm dữ liệu6map trước/sau và tài nguyên kiểm chứng.

Chưa quan sát NPC/bảng top trong Unity chạy thật. Đăng xuất và đăng nhập lại để client nhận catalog mới. Bảng top phản ánh dữ liệu server đã lưu, có thể trễ tới60giây sau lần lưu.

Rollback: bảo trì lưu dữ liệu, backup JAR hiện tại, khôi phục JAR cũ và chỉ các hàng map/NPC111 từ prior_map_rows/prior_npc_rows trong metadata. Không restore toàn bộ SQL snapshot đè dữ liệu người chơi mới.
