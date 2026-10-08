-- BOSS_NOTICE_CATEGORIES_20261008: additive; backup SQL before production deployment.
SET NAMES utf8mb4;
ALTER TABLE boss_notice_history
 ADD COLUMN IF NOT EXISTS category TINYINT NOT NULL DEFAULT 0,
 ADD COLUMN IF NOT EXISTS semantic_boss_id INT NOT NULL DEFAULT 0,
 ADD COLUMN IF NOT EXISTS quest_support BOOLEAN NOT NULL DEFAULT FALSE,
 ADD COLUMN IF NOT EXISTS zone_id INT NOT NULL DEFAULT -1,
 ADD COLUMN IF NOT EXISTS schema_version INT NOT NULL DEFAULT 1;
-- Historical support/dynamic identities cannot be inferred safely. Do not classify by name.
UPDATE boss_notice_history SET semantic_boss_id=boss_id,
 category=CASE WHEN boss_id IN (-20,-21,-22,-23,-24,-25,-26,-27,-30,-31,-32,-33,-34,-36,-35,-37,-102,-103,-104,-105,-106,-107,-108,-233,-237,-343,-234,-238,-235,-236) THEN 1
 WHEN boss_id IN (-28,-100) AND form_id BETWEEN 0 AND 2 THEN 1
 WHEN boss_id=-101 AND form_id=1 THEN 1 ELSE 2 END
 WHERE category=0 AND schema_version=1 AND boss_id IN (-386998,-203999,-82282,-1822,-926,-925,-924,-799,-613,-612,-611,-610,-609,-502,-385,-371,-365,-364,-363,-362,-361,-360,-359,-358,-357,-355,-354,-353,-351,-350,-349,-348,-346,-345,-344,-343,-342,-341,-340,-339,-338,-337,-336,-335,-334,-333,-332,-331,-330,-329,-328,-327,-326,-325,-324,-323,-322,-321,-320,-319,-318,-317,-316,-315,-314,-313,-312,-311,-309,-308,-238,-237,-236,-235,-234,-233,-223,-214,-208,-207,-206,-205,-204,-203,-108,-107,-106,-105,-104,-103,-102,-101,-100,-97,-96,-95,-94,-93,-92,-91,-90,-89,-88,-87,-86,-85,-84,-83,-82,-81,-80,-79,-78,-77,-37,-36,-35,-34,-33,-32,-31,-30,-29,-28,-27,-26,-25,-24,-23,-22,-21,-20,-17,-16,-15,-14,-13,-12,-11,-10,-9,-8,-7,-6,-5,-4);
