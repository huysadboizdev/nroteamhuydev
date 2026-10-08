# Uub portrait and Đổi đệ tử 2 — 06/10/2026

Uub panel used in-map head image 8586 (19×17 at x1), instead of portrait 8617 (52×61). Corrected head_avatar946 to8617; body/head/leg mapping946–948 unchanged. Existing portrait verified at all four zooms.

Original res moi/item_template.sql contains item1890 “Đổi đệ tử 2”, icon15767. Restored reserved blank template1890, copied original icon x1–x4, and added one Santa green-gem shop row(tab17), price100 gems. Item version18 forces client template refresh after login.

Personal shop catalog hides this item unless pet.typePet is2/3/4(Uub/Kid Beer/Jiren). Purchase revalidates eligibility, shopSANTA, currency1 and price100 under owner lock, so an outdated open shop cannot bypass eligibility. Shared shop remains unchanged.

Use requires a positive-quantity item1890 present in the owner's bag and an eligible pet with no equipped items. It randomly selects one of the other two forms, creates a fresh pet using existing PetService, then consumes one token. New pet starts at40 billion power with freshly initialized stats/skills; the old pet is disposed via existing replacement lifecycle. Pending protected transactions block use. This item does not affect NewPet companion.

Deployment uses data-saving maintenance and pre-migration database snapshot. Previous application JAR archived as SRC/jar-backup/20-before-advanced-pet-20261006-143527.jar. Application restarted as PID6396, listener14445. Installed SHA256:1553f1f9519baafdcafd0aeacf34304bd601dc44e80fb50d253c903fce29512c.

Runtime read-only probe confirms avatar8617, item1890 name/icon15767, Santa price100/currency1 and loaded class hashes. 104 test groups passed on compiled classes and candidate JAR. Final installed-JAR checks are recorded in [.analysis/advanced-pet-verification.json](../../.analysis/advanced-pet-verification.json).

Actual Unity UI/gameplay after login has not been observed by the agent. User should log in again to refresh item templates/avatar and open Santa's green-gem shop.
