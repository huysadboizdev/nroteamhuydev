# Jiren portrait correction — 06/10/2026

Reported screenshot: oversized cropped portrait in the pet panel. Pet type4 returns head876; head_avatar maps876 to8094. Part876/877/878 exactly matches the original res moi part.sql. Client Panel calls SmallImage.drawSmallImage without resizing, and DataGame.sendIcon reads SRC/data/icon/x{session.zoomLevel}/8094.png directly on each request.

Root cause: portrait PNG dimensions had been enlarged. Old x1=51×44, x2=306×264, x3=459×396, x4=612×528. Original resource sizes are x1=38×33, x2=76×66, x3=114×99, x4=153×132. At x2 the displayed portrait was roughly four times its intended dimensions.

Restored only8094.png at all four zooms by copying the original files from Downloads/filefolder/res moi/res moi. Each old file backed up under .analysis/jiren-portrait/backup-TIMESTAMP; old/new SHA256 and original dimensions recorded in [repair.json](../../.analysis/jiren-portrait/repair.json). All four installed files match original hashes and verify as PNGs. No JAR or SQL changes, no server restart.

Audited132 images for the Jiren parts/avatar. Remaining source dimension differences involve transparent placeholder2955 and one-pixel padding on in-map head8063/8064; these were not altered. No evidence these cause the oversized portrait.

Client caches icons in SmallImage.imgNew/imageRaw in memory. Restart the client, or stop and restart Unity Play, to request the corrected files. Re-login alone may preserve these static caches. Actual UI rendering after refresh has not yet been observed by the agent.
