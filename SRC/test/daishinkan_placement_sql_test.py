"""Execute the actual migration twice on isolated tables and preserve unrelated NPCs."""
from pathlib import Path
import subprocess,json,datetime

ROOT=Path(__file__).resolve().parents[2]
MYSQL='C:/xampp/mysql/bin/mysql.exe'
SQL=(ROOT/'SRC/sql/migrations/20261008_daishinkan_village_rankings.sql').read_bytes()
DB='codex_daishinkan_test_'+datetime.datetime.now().strftime('%Y%m%d%H%M%S%f')

def query(sql,db=None,check=True):
    return subprocess.run([MYSQL,'-u','root','--default-character-set=utf8mb4','-N','-B',*([db] if db else [])],input=sql if isinstance(sql,bytes) else sql.encode(),capture_output=True,check=check)

original={0:[[7,228,432],[84,950,432],[111,111,432],[111,112,432]],7:[[6,564,432],[84,853,432]],14:[[9,396,408],[84,953,408]],21:[[0,228,336],[17,573,336],[3,84,336]],22:[[17,105,336],[2,516,336]],23:[[17,570,336],[1,228,336],[17,300,336]],47:[[17,100,200]]}
try:
    query('CREATE DATABASE '+DB+' CHARACTER SET utf8mb4;')
    query('CREATE TABLE npc_template(id INT PRIMARY KEY,NAME VARCHAR(50),head INT,body INT,leg INT,avatar INT); CREATE TABLE part(id INT PRIMARY KEY); INSERT INTO part VALUES(703),(704),(705); CREATE TABLE map_template(id INT PRIMARY KEY,npcs LONGTEXT);',DB)
    for mid,rows in original.items():
        query("INSERT INTO map_template VALUES("+str(mid)+",'"+json.dumps(rows)+"');",DB)
    query(SQL,DB)
    first=query('SELECT id,npcs FROM map_template ORDER BY id;',DB).stdout
    query(SQL,DB)
    assert query('SELECT id,npcs FROM map_template ORDER BY id;',DB).stdout==first,'Migration duplicates or alters NPCs on replay'
    rows={int(line.split('\t')[0]):json.loads(line.split('\t')[1]) for line in first.decode().splitlines()}
    for mid in (21,22,23): assert rows[mid]==[r for r in original[mid] if r[0]!=17],(mid,rows[mid])
    for mid in (0,7,14):
        assert rows[mid][:-1]==[r for r in original[mid] if r[0]!=111]
        assert rows[mid][-1]==[111,1100,408 if mid==14 else 432],(mid,rows[mid][-1])
    assert rows[47]==original[47],'Bo Mong outside houses removed'
    assert query('SELECT head,body,leg,avatar FROM npc_template WHERE id=111;',DB).stdout.strip()==b'703\t704\t705\t6578'
    query("UPDATE npc_template SET NAME='Other NPC' WHERE id=111;",DB)
    failed=query(SQL,DB,False)
    assert failed.returncode!=0 and b'already belongs' in failed.stderr,'Conflicting template overwritten'
    assert query('SELECT id,npcs FROM map_template ORDER BY id;',DB).stdout==first,'Conflict partially mutated placement'
    query("UPDATE npc_template SET NAME='Daishinkan' WHERE id=111; UPDATE map_template SET npcs='invalid' WHERE id=0;",DB)
    failed=query(SQL,DB,False)
    assert failed.returncode!=0 and b'invalid starter' in failed.stderr,'Invalid placement not rejected'
    print('DAISHINKAN_SQL PASS replay,duplicates,home-only-removal,preserve-other-NPCs,parts,conflict,invalid-json')
finally:
    query('DROP DATABASE IF EXISTS '+DB+';')
