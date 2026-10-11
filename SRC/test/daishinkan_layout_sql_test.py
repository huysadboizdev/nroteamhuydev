"""Exercise the actual layout migration on disposable MariaDB tables."""
import datetime
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[2]
MYSQL = 'C:/xampp/mysql/bin/mysql.exe'
MIGRATION = (ROOT / 'SRC/sql/migrations/20261010_center_daishinkan_villages.sql').read_bytes()
DB = 'codex_daishinkan_layout_' + datetime.datetime.now().strftime('%Y%m%d%H%M%S%f')
ORIGINAL = {0: [[7,228,432],[67,590,432],[74,426,432],[111,1100,432]],
            7: [[6,564,432],[8,300,432],[74,607,432],[111,1100,432]],
            14: [[9,396,408],[6,252,408],[74,286,409],[111,1100,408]],
            21: [[0,228,336]]}
EXPECTED = {0: [111,508,432], 7: [111,454,432], 14: [111,341,408]}

def query(sql, db=DB, check=True):
    result = subprocess.run([MYSQL, '-u', 'root', '--default-character-set=utf8mb4', '-N', '-B',
                             *([db] if db else [])], input=sql if isinstance(sql,bytes) else sql.encode(),
                            capture_output=True, check=check)
    return result

def rows():
    return {int(mid): json.loads(npcs) for mid,npcs in
            (line.split('\t',1) for line in query('SELECT id,npcs FROM map_template ORDER BY id;').stdout.decode().splitlines())}

try:
    query('CREATE DATABASE ' + DB + ';', None)
    query('CREATE TABLE map_template(id INT PRIMARY KEY,npcs LONGTEXT) ENGINE=InnoDB;')
    for mid,npcs in ORIGINAL.items():
        query("INSERT INTO map_template VALUES(" + str(mid) + ",'" + json.dumps(npcs) + "');")
    assert sum(rows()[mid][-1] != expected for mid,expected in EXPECTED.items()) == 3
    query(MIGRATION)
    first = rows()
    for mid,expected in EXPECTED.items():
        assert first[mid][-1] == expected, (mid,first[mid])
        assert first[mid][:-1] == ORIGINAL[mid][:-1]
    assert first[21] == ORIGINAL[21]
    query(MIGRATION)
    assert rows() == first, 'Replay altered NPC tuples'
    for bad in ('[[9,396,408],[111,1100,408]]',
                '[[9,396,408],[74,286,409],[111,1100,408],[111,1,2]]', 'invalid'):
        query("UPDATE map_template SET npcs='" + bad + "' WHERE id=14;")
        before = query('SELECT id,npcs FROM map_template ORDER BY id;').stdout
        rejected = query(MIGRATION, check=False)
        assert rejected.returncode != 0, 'Invalid/ambiguous layout accepted'
        assert query('SELECT id,npcs FROM map_template ORDER BY id;').stdout == before, 'Partial write on rejection'
    print('DAISHINKAN_LAYOUT_SQL PASS 3 centered villages, other NPC preservation, replay, missing anchor, duplicate, invalid JSON')
finally:
    query('DROP DATABASE IF EXISTS ' + DB + ';', None)
