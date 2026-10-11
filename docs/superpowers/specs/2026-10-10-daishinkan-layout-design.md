# Daishinkan village layout

User confirmed the anchor pair: Tori-Bot and Mr Popo in Aru, with the equivalent pair in the other villages. Move only Daishinkan; preserve other NPC positions, menus and assets.

Place Daishinkan halfway between existing anchors, rounding the midpoint to the nearest integer: Aru (map0) Tori-Bot426 / Mr Popo590 -> x508,y432; Mori (map7) Dende300 / Tori-Bot607 -> x454,y432; Kakarot (map14) Tori-Bot286 / Appule396 -> x341,y408. Each village is symmetric to within one map unit. Different existing anchor distances remain intact.

Persist through a new SQL migration appended after older migrations in all four seed/dump files. Validate all three maps and unique anchors/Daishinkan before any write. Back up original map rows and seed files with timestamps and hashes. Test migration replay and preservation on disposable tables.

Update the three existing NPC objects in the active port14445 JVM through a temporary attach tool, with all expected old positions checked before changing any. Verify database and runtime coordinates and unchanged application JAR SHA256. Players receive new positions on entering the village again. No application JAR replacement is needed.
