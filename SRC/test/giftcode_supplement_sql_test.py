"""Disposable-schema migration and real DAO integration tests. Never targets production."""
from pathlib import Path
import argparse
import datetime
import json
import os
import re
import subprocess
import uuid

ROOT = Path(__file__).resolve().parents[2]
MYSQL = r'C:/xampp/mysql/bin/mysql.exe'
WORK = ROOT / '.analysis/giftcode-supplements-build'


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--classpath', default=str(ROOT / 'SRC/20.jar'))
    parser.add_argument('--red', action='store_true', help='Show old DAO refuses a real supplement')
    args = parser.parse_args()
    name = 'codex_gift_test_supp_' + datetime.datetime.now().strftime('%Y%m%d_%H%M%S_') + uuid.uuid4().hex[:8]
    assert re.fullmatch(r'codex_gift_test_supp_[0-9_]+[a-f0-9]{8}', name)
    WORK.mkdir(parents=True, exist_ok=True)

    def sql(body, database=name, ok=True):
        command = [MYSQL, '-u', 'root', '--batch', '--skip-column-names', '--default-character-set=utf8mb4']
        if database:
            assert database == name
            command += ['--database', database]
        result = subprocess.run(command, input=body, text=True, capture_output=True, encoding='utf-8')
        if (result.returncode == 0) != ok:
            raise AssertionError(result.stdout + result.stderr)
        return result.stdout.strip()

    sql('CREATE DATABASE ' + name + ' CHARACTER SET utf8mb4', None)
    try:
        sql('''CREATE TABLE player(id BIGINT PRIMARY KEY,account_id INT NOT NULL UNIQUE,data_inventory TEXT NOT NULL,items_bag TEXT NOT NULL,giftcode TEXT NOT NULL) ENGINE=InnoDB;
CREATE TABLE giftcode(id INT AUTO_INCREMENT PRIMARY KEY,code TEXT NOT NULL,count_left INT NOT NULL,detail TEXT NOT NULL,datecreate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,expired TIMESTAMP NOT NULL DEFAULT '2037-12-31 17:00:00') ENGINE=InnoDB;
INSERT INTO player VALUES(1,10,'[0,0,0,0,0,777]','[]','[]'),(2,20,'[0,0,0,0,0]','[]','["legacy","recreated_unknown"]');
INSERT INTO giftcode(code,count_left,detail,datecreate,expired) VALUES('LEGACY',5,'[{"id":457,"quantity":10,"options":[{"id":30,"param":0},{"id":73,"param":5}]}]','2000-01-01','2037-12-31');''')
        sql((ROOT / 'SRC/sql/migrations/20261007_giftcode_claims.sql').read_text(encoding='utf-8'))
        if not args.red:
            migration = (ROOT / 'SRC/sql/migrations/20261008_giftcode_supplements.sql').read_text(encoding='utf-8')
            original_claims = sql('SELECT CONCAT_WS("|",request_id,UNIX_TIMESTAMP(claimed_at)) FROM giftcode_claim ORDER BY canonical_code')
            # Preflight must reject malformed baseline before schema changes.
            for invalid in ['broken', '{}', '[1]', '[{"id":457,"quantity":0}]', '[{"id":457,"quantity":1,"options":{}}]', '[{"id":457,"quantity":1},{"id":457,"quantity":2}]']:
                sql("UPDATE giftcode SET detail='" + invalid + "' WHERE code='LEGACY'")
                sql(migration, ok=False)
                assert sql("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='giftcode_claim' AND column_name='delivered_detail'") == '0'
                assert sql("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('giftcode_claim_request','giftcode_schema_migration')") == '0'
            sql('''UPDATE giftcode SET detail='[{"id":457,"quantity":10,"options":[{"id":30,"param":0},{"id":73,"param":5}]}]' WHERE code='LEGACY';''')
            sql("CREATE TRIGGER giftcode_baseline_failure BEFORE UPDATE ON giftcode_claim FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Injected migration failure'")
            sql(migration, ok=False)
            assert sql('SELECT COUNT(*) FROM giftcode_schema_migration') == '0'
            assert sql('SELECT COUNT(*) FROM giftcode_claim_request') == '0'
            assert sql('SELECT COUNT(*) FROM giftcode_claim WHERE delivered_detail IS NOT NULL') == '0'
            sql('DROP TRIGGER giftcode_baseline_failure')
            # Fail the final DML after baseline and receipt writes: all three roll back.
            sql("CREATE TRIGGER giftcode_marker_failure BEFORE INSERT ON giftcode_schema_migration FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Injected final migration failure'")
            sql(migration, ok=False)
            assert sql('SELECT COUNT(*) FROM giftcode_schema_migration') == '0'
            assert sql('SELECT COUNT(*) FROM giftcode_claim_request') == '0'
            assert sql('SELECT COUNT(*) FROM giftcode_claim WHERE delivered_detail IS NOT NULL') == '0'
            sql('DROP TRIGGER giftcode_marker_failure')
            sql(migration)
            baseline = sql("SELECT delivered_detail FROM giftcode_claim WHERE canonical_code='LEGACY'")
            assert json.loads(baseline)[0]['quantity'] == 10, baseline
            assert sql("SELECT delivered_detail IS NULL FROM giftcode_claim WHERE canonical_code='RECREATED_UNKNOWN'") == '1'
            assert sql('SELECT COUNT(*) FROM giftcode_claim_request') == '2'
            assert sql("SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='giftcode_claim' AND index_name='giftcode_claim_code'") == 'canonical_code,account_id'
            assert sql('SELECT CONCAT_WS("|",request_id,UNIX_TIMESTAMP(claimed_at)) FROM giftcode_claim ORDER BY canonical_code') == original_claims
            sql('''UPDATE giftcode SET detail='[{"id":457,"quantity":15,"options":[{"id":30,"param":0},{"id":73,"param":5}]}]' WHERE code='LEGACY';
INSERT INTO giftcode(code,count_left,detail,datecreate,expired) VALUES('RECREATED_UNKNOWN',5,'[{"id":457,"quantity":50,"options":[]}]','2000-01-01','2037-12-31');''')
            # A recreated catalog never converts an unknowable legacy watermark into a known one.
            sql(migration)
            assert json.loads(sql("SELECT delivered_detail FROM giftcode_claim WHERE canonical_code='LEGACY'"))[0]['quantity'] == 10
            assert sql("SELECT delivered_detail IS NULL FROM giftcode_claim WHERE canonical_code='RECREATED_UNKNOWN'") == '1'
            assert sql('SELECT COUNT(*) FROM giftcode_claim_request') == '2'
            assert sql("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='giftcode_claim' AND index_name='giftcode_claim_code'") == '2'
            print('Migration preflight/baseline/marker/rerun/unknown/timestamps PASS')
        tests = WORK / ('red-tests' if args.red else 'tests')
        tests.mkdir(exist_ok=True)
        classpath = args.classpath + os.pathsep + str(ROOT / 'SRC/lib/*')
        compile_result = subprocess.run(['javac', '-encoding', 'UTF-8', '-cp', classpath, '-d', str(tests), str(ROOT / 'SRC/test/nro/models/services/GiftCodeSupplementDatabaseTest.java')], capture_output=True, text=True, encoding='utf-8')
        if compile_result.returncode:
            raise AssertionError(compile_result.stdout + compile_result.stderr)
        command = ['java', '-cp', str(tests) + os.pathsep + classpath, 'nro.models.services.GiftCodeSupplementDatabaseTest', name]
        if args.red:
            command.append('--red')
        result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, encoding='utf-8')
        output = result.stdout + result.stderr
        print(output)
        (WORK / ('red-result.json' if args.red else 'integration-result.json')).write_text(json.dumps({'database': name, 'production_touched': False, 'returncode': result.returncode, 'output': output}, indent=2), encoding='utf-8')
        if args.red:
            assert result.returncode != 0 and 'Account already claimed this giftcode' in output, 'RED must fail on old duplicate-only DAO'
            print('RED confirmed: installed JAR rejects supplementary claim')
        elif result.returncode:
            raise SystemExit(result.returncode)
    finally:
        assert name.startswith('codex_gift_test_supp_') and re.fullmatch(r'[A-Za-z0-9_]+', name)
        sql('DROP DATABASE ' + name, None)
        print('Disposed isolated schema: ' + name)


if __name__ == '__main__':
    main()
