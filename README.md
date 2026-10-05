# nroteamhuydev

Ma nguon server Teamobi2026 (Java 17, Ant/NetBeans).

- `SRC/src`: ma nguon Java.
- `SRC/test`: cac bai kiem tra.
- `SRC/data`: tai nguyen game.
- `SRC/lib`: thu vien dung de build va chay.
- `SRC/sql/migrations`: cac ban cap nhat database.
- `SRC/sql/team2026.seed.sql`, `SRC/sql/nro1.seed.sql`: schema va du lieu mau game; da loai du lieu tai khoan, nguoi choi, thanh toan va cau hinh rieng.
- `docs`: tai lieu phan tich va thay doi.

## Cai dat

1. Cai JDK 17, Apache Ant va MySQL.
2. Tao database `team2026`, import `SRC/sql/team2026.seed.sql`, sau do ap dung cac migration phu hop trong `SRC/sql/migrations`.
3. Sao chep `SRC/Config.example.properties` thanh `SRC/Config.properties` va dien cau hinh may.
4. Build trong `SRC` bang `ant jar` hoac mo project bang NetBeans.
5. Xem `SRC/run.bat` va `AGENTS.md` de trien khai JAR. Script chay su dung `SRC/20.jar`; build Ant tao JAR trong `SRC/dist`.

Log, ban build, JAR backup, cau hinh may va dump database goc duoc giu o may, khong dua len Git.
