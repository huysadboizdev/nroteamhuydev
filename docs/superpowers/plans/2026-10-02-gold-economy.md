# Gold economy implementation plan

Goal: Support 1,000,000,000,000 bag gold; dragon wishes award 200/150/100 item 457; every bar sells for 500,000,000 gold.

Architecture: Keep the existing long balance and database JSON format. Put the gold cap and bar price in Inventory. Keep existing shop/input flows, validate quantities before mutation, multiply prices as long, and reject sales that exceed the cap without consuming items. Dragon rewards use the existing inventory insertion result, allowing retry on a full bag.

Execution: Inline in the current workspace as authorized by the user; no Git repository is present.

- [x] Add standalone behavioral regression tests and a compiler/test runner. Confirm failures for the old cap, retail/bulk price, negative quantities, giftcode balance loss, dragon rewards, and doubled gold pickups near the cap.
- [x] Update Inventory.LIMIT_GOLD to 1_000_000_000_000L and add GOLD_BAR_SELL_PRICE = 500_000_000L. Use overflow-safe long additions for rewards.
- [x] Update GiftCodeService to use Inventory.addGold and refresh money. Fix InventoryService gold pickup to validate the complete doubled amount before adding.
- [x] Update ShopService sell confirmation/payment to use long prices and the shared bar price. Validate quantity and indexes before mutations.
- [x] Update Input.BANSLL to reject zero, negative and malformed quantities, use the shared price and preserve resources when insufficient stock or balance room. Display the price in the sale form.
- [x] Update SummonDragon labels, tutorial and award branches to item 457 quantities 200/150/100. Return to wishes on insertion failure and protect repeated confirmation after the dragon leaves.
- [x] Compile all source with javac --release 17; execute new tests and the existing crystalization tests through python .analysis/verify_gold_economy.py. Review changed source and preserve the original jar until deployment is requested.
