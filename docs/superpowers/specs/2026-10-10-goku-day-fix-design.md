# Goku Day gift repair

User authorized repair of the audit findings for boxes1591/1594. Keep the shared six-costume reward pool and current stats. Replace item1593 body/leg1471/1472 (reindeer assets) with the existing authentic Goku uniform1450/1451, retaining red Goku head1467. Reuse complete existing animations; preserve reindeer images and unrelated templates.

For the shared opening handler, use a uniform roll0..999: values0..4 award permanent items with no option93, other values add option93=3. Keep the two-free-slot condition and change its error message to two slots. Other gift handlers are out of scope.

Verify the real handler on permanent/timed roll boundaries, correct box consumption, reward stats, expiration behavior, and failed inventory insertion. Verify the SQL migration replay and preservation on isolated tables. Compile the complete source and install only the changed UseItem classes into a candidate based on the active JAR, preserving unrelated release contents. Append the SQL repair after existing migrations in all four seed/dump files.

Use application data-saving maintenance, verify server stopped, take timestamped SQL/JAR backups and SHA256 metadata, install the tested candidate and start the server hidden. Verify live template1593, database, images, backup hashes, startup listener and existing village NPC layout. Existing items resolve appearance from template; player inventory records need no rewrite. Unity visual inspection remains manual.
