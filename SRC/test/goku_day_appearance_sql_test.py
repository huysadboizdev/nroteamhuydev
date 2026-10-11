from pathlib import Path
from datetime import datetime
import subprocess,json

root=Path(__file__).resolve().parents[2]
migration=(root/'SRC/sql/migrations/20261010_goku_day_god_appearance.sql').read_bytes()
db='codex_goku_day_'+datetime.now().strftime('%Y%m%d%H%M%S%f')
def sql(query,database=db,check=True):
    return subprocess.run(['C:/xampp/mysql/bin/mysql.exe','-u','root','--default-character-set=utf8mb4','-N','-B',
                           *([database] if database else [])],input=query if isinstance(query,bytes) else query.encode(),capture_output=True,check=check)
try:
    sql('CREATE DATABASE '+db+';',None)
    sql('CREATE TABLE item_template LIKE team2026.item_template; INSERT INTO item_template SELECT * FROM team2026.item_template WHERE id IN(1587,1588,1589,1590,1591,1593,1594,1595); CREATE TABLE part LIKE team2026.part; INSERT INTO part SELECT * FROM team2026.part WHERE id IN(1467,1450,1451);')
    sql('UPDATE item_template SET body=1471,leg=1472 WHERE id=1593;')
    before=sql('SELECT * FROM item_template WHERE id<>1593 ORDER BY id;').stdout
    sql(migration)
    assert sql('SELECT head,body,leg FROM item_template WHERE id=1593;').stdout.strip()==b'1467\t1450\t1451'
    assert sql('SELECT * FROM item_template WHERE id<>1593 ORDER BY id;').stdout==before
    first=sql('SELECT * FROM item_template ORDER BY id;').stdout
    sql(migration);assert sql('SELECT * FROM item_template ORDER BY id;').stdout==first
    sql('UPDATE part SET TYPE=0 WHERE id=1450;')
    rejected=sql(migration,check=False)
    assert rejected.returncode!=0 and b'invalid genuine' in rejected.stderr
    assert sql('SELECT * FROM item_template ORDER BY id;').stdout==first
    print('GOKU_DAY_SQL PASS genuine uniform, other templates preserved, replay, invalid part rejection')
finally:sql('DROP DATABASE IF EXISTS '+db+';',None)
