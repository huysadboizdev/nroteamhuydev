from pathlib import Path
import subprocess,datetime
ROOT=Path(__file__).resolve().parents[2]
DB='codex_boss_admin_schema_'+datetime.datetime.now().strftime('%Y%m%d%H%M%S%f')
def sql(text,db=None,check=True):
    return subprocess.run(['C:/xampp/mysql/bin/mysql.exe','-u','root','-N','-B',*([db] if db else [])],input=text if isinstance(text,bytes) else text.encode(),capture_output=True,check=check)
try:
    sql('CREATE DATABASE '+DB+';')
    migration=(ROOT/'SRC/sql/migrations/20261008_boss_admin_live.sql').read_bytes()
    sql(migration,DB)
    sql((ROOT/'SRC/sql/migrations/20261008_boss_admin_defcrit.sql').read_bytes(),DB)
    sql("INSERT INTO boss_admin_config(boss_key,phase,hp,revision) VALUES('id:-4',0,2147483647,1); UPDATE boss_admin_state SET revision=1 WHERE id=1;",DB)
    sql(migration,DB)
    assert sql('SELECT revision FROM boss_admin_state WHERE id=1;',DB).stdout.strip()==b'1'
    assert sql('SELECT hp FROM boss_admin_config;',DB).stdout.strip()==b'2147483647'
    sql("UPDATE boss_admin_config SET defense=1000,critical=15;",DB)
    assert sql('SELECT defense,critical FROM boss_admin_config;',DB).stdout.strip()==b'1000\t15'
    for change in ("hp=0","damage=-1","cap_percent=100.001","apply_mode='bad'","drops='{}'","drops='invalid'","phase=32","defense=-1","critical=101","critical=-1"):
        assert sql('UPDATE boss_admin_config SET '+change+';',DB,False).returncode!=0,change
    sql("INSERT INTO boss_admin_audit(boss_key,phase,revision,actor_id,after_json) VALUES('id:-4',0,1,1,'{}');",DB)
    assert sql("INSERT INTO boss_admin_audit(boss_key,phase,revision,actor_id,after_json) VALUES('id:-4',0,1,1,'{}');",DB,False).returncode!=0
    print('BOSS_ADMIN_SCHEMA PASS replay,preserve-revisions,INTmax,invalid-settings,JSON,duplicate-audit')
finally:sql('DROP DATABASE IF EXISTS '+DB+';')
