from pathlib import Path
import subprocess,datetime,json
root=Path(__file__).resolve().parents[2]
mysql=r'C:/xampp/mysql/bin/mysql.exe'
name='codex_gift_test_'+datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
assert name.startswith('codex_gift_test_')
def sql(body, database=None):
 args=[mysql,'-u','root','--batch','--skip-column-names']
 if database:args+=['--database',database]
 return subprocess.run(args,input=body,text=True,capture_output=True,encoding='utf-8')
def require(result,ok=True):
 if (result.returncode==0)!=ok:raise AssertionError(result.stdout+result.stderr)
 return result
require(sql('CREATE DATABASE '+name+' CHARACTER SET utf8mb4'))
for filename in ('20261007_live_settings.sql','20261007_boss_live_override.sql'):
 body=(root/'SRC/sql/migrations'/filename).read_text(encoding='utf-8')
 require(sql(body,name))
 require(sql(body,name))
seed="""CREATE TABLE player(id BIGINT PRIMARY KEY,account_id INT NOT NULL UNIQUE,data_inventory TEXT NOT NULL,items_bag TEXT NOT NULL,giftcode TEXT NOT NULL) ENGINE=InnoDB;
CREATE TABLE giftcode(id INT AUTO_INCREMENT PRIMARY KEY,code TEXT NOT NULL,count_left INT NOT NULL,detail TEXT NOT NULL,datecreate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,expired TIMESTAMP NOT NULL DEFAULT '2037-12-31 17:00:00') ENGINE=InnoDB;
INSERT INTO player VALUES(1,10,'[0,0,0,0,0,777]','[]','[]'),(2,20,'[0,0,0,0,0]','[]','[" old "]'),(3,30,'[0,0,0,0,0]','[]','["deleted_old"]');
INSERT INTO giftcode(code,count_left,detail,datecreate,expired) VALUES('ABC',2,'[{"id":-1,"quantity":2}]','2000-01-01','2037-12-31'),(' abc ',2,'[]','2000-01-01','2037-12-31');
"""
require(sql(seed,name))
migration=(root/'SRC/sql/migrations/20261007_giftcode_claims.sql').read_text(encoding='utf-8')
require(sql(migration,name),False)
require(sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='giftcode_claim'",name))
assert sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='giftcode_claim'",name).stdout.strip()=='0'
require(sql("DELETE FROM giftcode WHERE code=' abc '; UPDATE player SET giftcode='broken' WHERE id=1",name))
require(sql(migration,name),False)
assert sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='giftcode_claim'",name).stdout.strip()=='0'
require(sql("UPDATE player SET giftcode='' WHERE id=1",name))
require(sql(migration,name))
# Rerunning migration must preserve claims and keep canonical unique index.
require(sql(migration,name))
require(sql("""INSERT INTO giftcode(code,count_left,detail,datecreate,expired) VALUES
('EXPIRED',5,'[]','2000-01-01','2001-01-01'),('FAIL',5,'[]','2000-01-01','2037-12-31'),('UNLIMITED',-1,'[]','2000-01-01','2037-12-31'),('OLD',5,'[]','2000-01-01','2037-12-31'),('LAST',1,'[]','2000-01-01','2037-12-31'),('SQLFAIL',5,'[]','2000-01-01','2037-12-31'),('LOST',5,'[]','2000-01-01','2037-12-31'),('UNCERTAIN',5,'[]','2000-01-01','2037-12-31');""",name))
work=root/'.analysis/gift-live-build'
(work/'isolated-db.json').write_text(json.dumps({'database':name,'production_touched':False},indent=2))
build=json.loads((root/'.analysis/web-manager-build-latest.json').read_text(encoding='utf-8'))
result=subprocess.run(['java','-cp',build['tests']+';'+build['candidate']+';'+str(root/'SRC/lib/*'),'nro.models.services.LiveSyncGiftClaimTest',name],cwd=root,capture_output=True,text=True,encoding='utf-8')
print(result.stdout+result.stderr)
if result.returncode:raise SystemExit(result.returncode)
print('Migration preflight/duplicate/malformed/backfill/rerun PASS; isolated database: '+name)
