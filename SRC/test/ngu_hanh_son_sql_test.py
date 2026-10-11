from pathlib import Path
import subprocess,json,sys,struct
root=Path(__file__).resolve().parents[2]
database='codex_ngu_hanh_test'
def sql(s,db=None):
 p=subprocess.run(['C:/xampp/mysql/bin/mysql.exe','-u','root','--default-character-set=utf8mb4','--raw','-N','-B']+([db] if db else []),input=s.encode(),capture_output=True)
 if p.returncode:raise AssertionError(p.stderr.decode(errors='replace'))
 return p.stdout.decode()
def check(condition,why):
 if not condition:raise AssertionError(why)
b=(root/'SRC/data/map/tile_set_info').read_bytes();pos=1;tops={}
for tile in range(b[0]):
 count=b[pos];pos+=1
 for _ in range(count):
  typ=struct.unpack_from('>i',b,pos)[0];pos+=4;n=b[pos];pos+=1
  indices=list(b[pos:pos+n]);pos+=n
  if typ==2:tops[tile]=indices
def ground(mid,x):
 b=(root/f'SRC/data/map/tile_map_data/{mid}').read_bytes();w,h=b[:2]
 if x<0 or x>=w*24:return None
 return next((y*24 for y in range(h) if b[2+y*w+x//24] in tops[0]),None)
sql('CREATE DATABASE IF NOT EXISTS '+database+' CHARACTER SET utf8mb4;')
try:
 sql('DROP TABLE IF EXISTS map_template,npc_template,item_template,part; CREATE TABLE map_template LIKE team2026.map_template; INSERT INTO map_template SELECT * FROM team2026.map_template; CREATE TABLE npc_template LIKE team2026.npc_template; INSERT INTO npc_template SELECT * FROM team2026.npc_template; CREATE TABLE item_template LIKE team2026.item_template; INSERT INTO item_template SELECT * FROM team2026.item_template; CREATE TABLE part LIKE team2026.part; INSERT INTO part SELECT * FROM team2026.part;',database)
 before={str(r[0]):r[1:] for r in [json.loads(line) for line in sql('SELECT JSON_ARRAY(id,npcs,mobs) FROM map_template ORDER BY id;',database).splitlines()]}
 migration=root/'SRC/sql/migrations/20261010_ngu_hanh_son.sql'
 check(migration.exists(),'Missing Ngũ Hành Sơn setup migration')
 sql(migration.read_text(encoding='utf8'),database)
 first=sql('SELECT id,name,npcs,mobs,waypoints FROM map_template ORDER BY id;',database)
 sql(migration.read_text(encoding='utf8'),database)
 check(sql('SELECT id,name,npcs,mobs,waypoints FROM map_template ORDER BY id;',database)==first,'Idempotent migration')
 rows=[json.loads(line) for line in sql('SELECT JSON_ARRAY(id,npcs,mobs,waypoints) FROM map_template ORDER BY id;',database).splitlines()]
 for sid,npcs,mobs,waypoints in rows:
  mid=int(sid);sid=str(sid);entries=[json.loads(n) if isinstance(n,str) else n for n in json.loads(npcs)]
  check(mobs==before[sid][1],'Preserve monsters '+sid)
  old=[json.loads(n) if isinstance(n,str) else n for n in json.loads(before[sid][0])]
  check([n for n in entries if n[0] not in(48,49)]==[n for n in old if n[0] not in(48,49)],'Preserve unrelated NPC '+sid)
  if mid in(0,7,14):check(len([n for n in entries if n[0]==49])==1,'One guide per village')
  if mid==124:check({n[0] for n in entries}>={48,49},'Both guides at final map')
  if mid in(122,123):check(not any(n[0] in(48,49) for n in entries),'Guides confined to final map')
  if mid in(122,123,124):
   targets={w[7] for w in json.loads(waypoints)}
   check(targets=={122:{123},123:{122,124},124:{123}}[mid],'Linear reversible map route')
   check(bool(json.loads(mobs)),'Populated region map')
   for wp in json.loads(waypoints):
    destination=wp[7]
    if destination>mid:
     check(wp[1]>500,'Forward portal must be on right, map '+sid)
    else:check(wp[3]<=72,'Return portal must be on left, map '+sid)
    walkable=[(x,ground(mid,x)) for x in range(max(0,wp[1]),wp[3]+1,12)]
    check(any(y is not None and wp[2]<=y<=wp[4] for x,y in walkable),'Portal intersects walkable terrain, map '+sid)
    check(ground(destination,wp[8])==wp[9],'Arrival stands on terrain, map '+sid)
 fixture=[dict(id=int(r[0]),waypoints=json.loads(r[3])) for r in rows if int(r[0]) in(122,123,124)]
 (root/'.analysis/ngu-hanh-son/route-fixture.json').write_text(json.dumps(fixture),encoding='utf8')
 print('NGU_HANH_SQL PASS replay, guides, rightward progression, walkable portals, grounded arrivals, preserved mobs/NPCs')
finally:sql('DROP DATABASE IF EXISTS '+database+';')
