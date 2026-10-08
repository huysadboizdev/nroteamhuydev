# Uub portrait and advanced pet exchange

User approved: item “Đổi đệ tử 2”, green-gem shop, 100 gems, visible only with Uub/Kid Beer/Jiren. Import original template/icon 1890/15767 from res moi. Uub portrait uses 8617 rather than in-map sprite 8586; retain body mapping 946–948.

- [ ] Test eligibility, exchange to a different advanced pet, no consumption on invalid/stale item or equipped pet, and shop visibility/purchase checks.
- [ ] Implement locked exchange using existing PetService creation; require unequipped pet, recreate at 40 billion power with fresh skills, consume one item after success.
- [ ] Filter personal shop copy and revalidate each purchase; original shared shop remains intact.
- [ ] Restore blank item template 1890, import all four icon zooms, add one Santa row at 100 green gems; sync SQL dumps and avatar mapping.
- [ ] Compile full server, run regression tests, maintenance-save online players, back up application JAR before replacement, verify hashes and restart.
- [ ] Verify live templates, resource images, installed class hashes and listener; document gameplay verification limits.
