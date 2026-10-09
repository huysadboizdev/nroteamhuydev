from pathlib import Path
import importlib.util,unittest

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('catalog',ROOT/'.analysis/generate_boss_admin_catalog.py')
catalog=importlib.util.module_from_spec(spec);spec.loader.exec_module(catalog)

class CatalogTest(unittest.TestCase):
    def setUp(self):self.rows=catalog.generate()
    def test_repeated_mini_is_one_type(self):
        for cls in ('AnTrom','MatTroi'):
            matches=[r for r in self.rows if r['class_name'].endswith('.'+cls)]
            self.assertEqual(len(matches),1)
            self.assertTrue(matches[0]['key'].startswith('class:'))
    def test_fide_forms_are_preserved(self):
        fide=next(r for r in self.rows if r['class_name']=='nro.models.boss.Frieza.Fide')
        self.assertEqual(len(fide['phases']),3)
        self.assertTrue(fide['quest'])
        self.assertTrue(all(p['hp']>0 for p in fide['phases']))
    def test_every_concrete_boss_is_represented(self):
        covered={r['class_name'] for r in self.rows}
        self.assertTrue(catalog.concrete_classes().issubset(covered),catalog.concrete_classes()-covered)
        self.assertEqual(len({r['key'] for r in self.rows}),len(self.rows))
        for row in self.rows:
            self.assertTrue(row['phases'],row['key'])
            self.assertEqual([p['phase'] for p in row['phases']],list(range(len(row['phases']))))
    def test_real_outgoing_percentage_group(self):
        hunt=[r for r in self.rows if '.HuntingTrio.' in r['class_name']]
        self.assertTrue(hunt)
        self.assertTrue(all(r['limited'] for r in hunt))
        black=next(r for r in self.rows if r['class_name'].endswith('.BlackGoku'))
        self.assertFalse(black['limited'])
    def test_hunting_members_keep_distinct_policies(self):
        members=[r for r in self.rows if '.HuntingTrio.' in r['class_name']]
        self.assertEqual({r['name'] for r in members},{'Basil','Lavender','Bergamo','Pi Láp','Mai','Su'})
        self.assertEqual(len({r['key'] for r in members}),6)

if __name__=='__main__':unittest.main()
