# Anime appearance resource import ? 2026-10-09

Installed after explicit approval on2026-10-09. Saved maintenance and SQL/JAR backups completed. ServerPID12360 listens14445; startup log confirms successful load. Active/backup hashes match.

Source: Downloads/filefolder/res moi/part.sql,item_template.sql,res moi/x1?x4.

| Asset | Item ID | Destination parts | Source parts | Runtime |
|---|---:|---|---|---|
| Goku Ultra Instinct (source name Goku can vo cuc) |2020|2120/2121/2122|1992/1995/1996|costume type5|
| Naruto |2026|2123/2124/2125|2000/2001/2002|costume type5|
| Sasuke |2027|2126/2127/2128|2003/2004/2005|costume type5|
| Kurama / Cuu Vi |2019|2129/2130/2131|1989/1990/1991|companion type27, existing slot7/NewPet flow|
| Mangekyo Sharingan flag |2030|flag_bag157|icons17082/17083|accessory type11|

Alternate Goku head parts1993/1994 retained at2132/2133. Three head_avatar rows added. All5item IDs are beyond live max2000 and unoccupied. All14part IDs append beyond live max2119 with no gaps. New flag157 follows live max156; icon_data uses comma-separated values, matching Manager parser. Sharingan variant icons17054?17088 preserved as resources; active flag uses matching two-frame floral variant17082/17083. 540PNGfiles staged across all4zooms. Conflicting existing icon bytes remapped to private24100+ IDs; existing images never overwritten.

Source costume body slots3?6 are transparent placeholders. Reused each outfit's visible first body pose for these slots to prevent invisible torso rendering. Non-sentinel costume head/body/leg frames checked for visible alpha, offsets byte range, PNG decoding. Kurama's transparent head/body frames are intentional because it is drawn by the fox body/legs sprites. Source pet type18 is unsupported by current equip code; mapped to existing type27 CompanionCatalog instead. No invented combat powers or copied claims of automatic source set bonuses (Naruto7%HP/Sasuke1%final damage/Sharingan illusion50%3s) are included. This import covers rendering/equipment resources; those mechanics require a separate behavior request.

SQL: SRC/sql/migrations/20261009_anime_appearances.sql; appended exactly once to both team2026.seed.sql and nro1.seed.sql. Transaction inserts14parts,3avatars,5item templates,1flag. Apply and rollback passed on isolated copies of all4production table schemas/data; live DB unmodified. .analysis/anime-assets/assets.json tracks resources/remaps/hash metadata.

Source changes: CompanionCatalog maps2019 to2129/2130/2131; DataGame vsData14?15 and vsItem19?20 force client reload of part+item lists. Javac release17 succeeded. AnimeAppearanceTest failed on old JAR missing Kurama mapping and passed on candidate with immutable returned parts/version fields. No shop/drop grants added.

Candidate: C:\Users\Admin\Downloads\nroteam\Teamobi2026\SRC\jar-backup\20-anime-assets-candidate-20261009-113644.jar
SHA256: 3a6fd3fb93c15d8134a6fef47fc1586db86eed673b55dcca041b29a6687b41d4

Deployment prepared: .analysis/anime-assets/install.py holds automatic restart around saved-maintenance installer, verifies all hashes/free SQL IDs, backs up JAR and four SQL tables, installs only absent images/transaction rows/JAR, restores restart script and launches hidden with detached standard handles. Rollback removes only newly created resources/rows and restores prior binary/catalog/JAR if the install phase fails. Runtime rendering/equip verification remains pending actual installation.


Deployment verification: all540PNG hashes match;5item templates,14newbinary parts identical to liveSQL,3avatars and flag157 confirmed. Previous2120binaryparts unchanged. Companion/version test passes against installedJAR. Original restart script restored. OldJAR: C:\Users\Admin\Downloads\nroteam\Teamobi2026\SRC\jar-backup\20-before-anime-assets-20261009-113920.jar. SQL backup: C:\Users\Admin\Downloads\nroteam\Teamobi2026\.analysis\anime-assets\deployment-20261009-113920/part-head-avatar-before.sql (all4tables). User requested to wait for their option changes; no new stats/bonuses/default option mappings were added. Client rendering/equip should be checked after next login.
