# Daishinkan layout — 10/10/2026

Centered NPC111 between Tori-Bot and Mr Popo in Aru, Dende and Tori-Bot in Mori, and Tori-Bot and Appule in Kakarot. All other NPC positions and templates remain unchanged.

| Village | Map | Daishinkan x/y | Left/right spacing |
|---|---:|---|---|
| Aru | 0 | 508 / 432 | 82 / 82 |
| Mori | 7 | 454 / 432 | 154 / 153 |
| Kakarot | 14 | 341 / 408 | 55 / 55 |

Verified the SQL migration twice on disposable tables, preservation of other NPC tuples, and rejection of missing anchors, duplicate Daishinkan and invalid JSON without partial updates. Migration appended after earlier migrations in all four seed/dump files.

The previous server stopped during deployment before live position application. Started the unchanged `SRC/20.jar` and read back the JVM NPC list. PID19124 listens on port14445; all three runtime map NPC lists exactly match persisted database data. Application JAR SHA256 is unchanged. No application JAR replacement occurred.

Backup directory and hashes, original/expected map rows, seed backups, full SQL snapshot and verification metadata: `.analysis/daishinkan-layout/latest.json`. Timestamped metadata is also saved in the backup directory referenced there.

Players must reenter their village (or log in again) to receive the new coordinates. Visual inspection in Unity remains for the user; positions were verified in the database and running server.

Rollback: restore only NPC111 coordinates from `prior_map_rows` in metadata, preserving all other current tuples and player data; restore the four seed files from their recorded backups after checking hashes. Reload positions through the prepared attach plan, or use data-saving maintenance before restarting. Do not restore the complete SQL snapshot over newer player data.
