from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[2]/'src/nro/models'
class EventRuntimeSafety(unittest.TestCase):
 def test_disabled_summer_clears_only_temporary_buffs_on_map_entry(self):
  s=(ROOT/'event/EventRuntimeService.java').read_text(encoding='utf8')
  self.assertTrue('clearTemporarySummerBuffs' in s,'disabled bonus clearing missing from map entry')
 def test_disabled_shop_rows_hidden(self):
  s=(ROOT/'shop/ShopService.java').read_text(encoding='utf8')
  self.assertTrue('EventItemPolicy.isBlocked(row.temp.id)' in s,'disabled event shop rows remain visible')
 def test_noel_constructs_only_selected_and_tagged_drops(self):
  s=(ROOT/'boss/event_noel/OngGiaNoel.java').read_text(encoding='utf8')
  start=s.index('private void giftBox()')
  end=s.index('@Override',start)
  body=s[start:end]
  self.assertTrue('dropGift(' in body and body.count('new ItemMap') == 1,'Noel chance creates untagged ghost gifts')
 def test_admission_drains_before_policy_publish(self):
  s=(ROOT/'event/EventItemPolicy.java').read_text(encoding='utf8')
  self.assertTrue('ReentrantReadWriteLock' in s,'mutation admission missing')
 def test_defaults_do_not_enable_event(self):
  s=(ROOT/'event/EventManager.java').read_text(encoding='utf8')
  self.assertNotIn('HUNG_VUONG = true',s)
  self.assertNotIn('new HungVuong()',s)
  self.assertNotIn('new TopUp()',s)
 def test_seasonal_constructor_does_not_publish_to_running_manager(self):
  s=(ROOT/'boss/Boss.java').read_text(encoding='utf8')
  self.assertNotIn('HungVuongEventManager.gI().addBoss(this)',s)
 def test_runtime_exists_and_tracks_retirement(self):
  p=ROOT/'event/EventRuntimeService.java'
  self.assertTrue(p.exists(),'live runtime missing')
 def test_recipe_checks_quantities(self):
  s=(ROOT/'event/NoiBanh.java').read_text(encoding='utf8')
  self.assertIn('comnep.quantity >= 99',s)
 def test_disabled_use_revalidated(self):
  s=(ROOT/'services_func/UseItem.java').read_text(encoding='utf8')
  self.assertIn('EventItemPolicy.isBlocked',s)
if __name__=='__main__':unittest.main()
