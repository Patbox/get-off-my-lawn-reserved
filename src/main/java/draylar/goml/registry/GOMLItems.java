package draylar.goml.registry;

import draylar.goml.GetOffMyLawn;
import draylar.goml.block.ClaimAnchorBlock;
import draylar.goml.item.GogglesItem;
import draylar.goml.item.UpgradeKitItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GOMLItems {
    public static List<Item> BASE_ITEMS = new ArrayList<>();

    public static final Item primitive_UPGRADE_KIT = registerUpgradeKit("primitive_upgrade_kit", GOMLBlocks.MAKESHIFT_CLAIM_ANCHOR.getFirst(), GOMLBlocks.PRIMITIVE_CLAIM_ANCHOR.getFirst(), Items.COPPER_INGOT);
    public static final Item REINFORCED_UPGRADE_KIT = registerUpgradeKit("reinforced_upgrade_kit", GOMLBlocks.PRIMITIVE_CLAIM_ANCHOR.getFirst(), GOMLBlocks.REINFORCED_CLAIM_ANCHOR.getFirst(), Items.IRON_INGOT);
    public static final Item GLISTENING_UPGRADE_KIT = registerUpgradeKit("glistening_upgrade_kit", GOMLBlocks.REINFORCED_CLAIM_ANCHOR.getFirst(), GOMLBlocks.GLISTENING_CLAIM_ANCHOR.getFirst(), Items.GOLD_INGOT);
    public static final Item EMERADIC_UPGRADE_KIT = registerUpgradeKit("emeradic_upgrade_kit", GOMLBlocks.GLISTENING_CLAIM_ANCHOR.getFirst(), GOMLBlocks.EMERADIC_CLAIM_ANCHOR.getFirst(), Items.EMERALD);
    public static final Item CRYSTAL_UPGRADE_KIT = registerUpgradeKit("crystal_upgrade_kit", GOMLBlocks.EMERADIC_CLAIM_ANCHOR.getFirst(), GOMLBlocks.CRYSTAL_CLAIM_ANCHOR.getFirst(), Items.DIAMOND);
    public static final Item NETHERED_UPGRADE_KIT = registerUpgradeKit("nethered_upgrade_kit", GOMLBlocks.CRYSTAL_CLAIM_ANCHOR.getFirst(), GOMLBlocks.NETHERED_CLAIM_ANCHOR.getFirst(), Items.NETHERITE_INGOT);

    public static final Item GOGGLES = register("goggles", GogglesItem::new);

    private static UpgradeKitItem registerUpgradeKit(String name, ClaimAnchorBlock from, ClaimAnchorBlock to, Item item) {
        return register(name, (s) -> new UpgradeKitItem(s, from, to, item));
    }

    private static <T extends Item> T register(String name, Function<Item.Settings, T> item) {
        var id = GetOffMyLawn.id(name);
        var value = item.apply(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id)));
        BASE_ITEMS.add(value);
        return Registry.register(Registries.ITEM, id, value);
    }

    public static void init() {
        // NO-OP
    }

    private GOMLItems() {
        // NO-OP
    }
}
