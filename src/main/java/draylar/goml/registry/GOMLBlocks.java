package draylar.goml.registry;

import com.mojang.datafixers.util.Pair;
import draylar.goml.GetOffMyLawn;
import draylar.goml.block.ClaimAnchorBlock;
import draylar.goml.block.ClaimAugmentBlock;
import draylar.goml.block.SelectiveClaimAugmentBlock;
import draylar.goml.block.augment.*;
import draylar.goml.item.ClaimAnchorBlockItem;
import draylar.goml.item.ToggleableBlockItem;
import eu.pb4.polymer.core.api.block.PolymerHeadBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;

public class GOMLBlocks {
    public static final List<ClaimAnchorBlock> ANCHORS = new ArrayList<>();
    public static final List<ClaimAugmentBlock> AUGMENTS = new ArrayList<>();

    public static final Pair<ClaimAnchorBlock, Item> MAKESHIFT_CLAIM_ANCHOR = register("makeshift_claim_anchor", () -> GetOffMyLawn.CONFIG.makeshiftRadius, 10, GOMLTextures.MAKESHIFT_CLAIM_ANCHOR);
    public static final Pair<ClaimAnchorBlock, Item> PRIMITIVE_CLAIM_ANCHOR = register("primitive_claim_anchor", () -> GetOffMyLawn.CONFIG.primitiveRadius, 10, GOMLTextures.PRIMITIVE_CLAIM_ANCHOR);
    public static final Pair<ClaimAnchorBlock, Item> REINFORCED_CLAIM_ANCHOR = register("reinforced_claim_anchor", () -> GetOffMyLawn.CONFIG.reinforcedRadius, 10, GOMLTextures.REINFORCED_CLAIM_ANCHOR);
    public static final Pair<ClaimAnchorBlock, Item> GLISTENING_CLAIM_ANCHOR = register("glistening_claim_anchor", () -> GetOffMyLawn.CONFIG.glisteningRadius, 15, GOMLTextures.GLISTENING_CLAIM_ANCHOR);
    public static final Pair<ClaimAnchorBlock, Item> EMERADIC_CLAIM_ANCHOR = register("emeradic_claim_anchor", () -> GetOffMyLawn.CONFIG.emeradicRadius, 20, GOMLTextures.EMERADIC_CLAIM_ANCHOR);
    public static final Pair<ClaimAnchorBlock, Item> CRYSTAL_CLAIM_ANCHOR = register("crystal_claim_anchor", () -> GetOffMyLawn.CONFIG.crystalRadius, 20, GOMLTextures.CRYSTAL_CLAIM_ANCHOR);
    public static final Pair<ClaimAnchorBlock, Item> NETHERED_CLAIM_ANCHOR = register("nethered_claim_anchor", () -> GetOffMyLawn.CONFIG.netheredRadius, 25, GOMLTextures.NETHERED_CLAIM_ANCHOR);
    public static final Pair<ClaimAnchorBlock, Item> ADMIN_CLAIM_ANCHOR = register("admin_claim_anchor", () -> -1, -1, GOMLTextures.ADMIN_CLAIM_ANCHOR);

    public static final Pair<ClaimAugmentBlock, Item> ENDER_BINDING = register("ender_binding", (s) -> new EnderBindingAugmentBlock(s.hardness(10).resistance(3600000.0F), GOMLTextures.ENDER_BINDING), 2);
    public static final Pair<ClaimAugmentBlock, Item> LAKE_SPIRIT_GRACE = register("lake_spirit_grace",(s) ->  new LakeSpiritGraceAugmentBlock(s.hardness(10).resistance(3600000.0F), GOMLTextures.LAKE_SPIRIT_GRACE), 2);
    public static final Pair<ClaimAugmentBlock, Item> ANGELIC_AURA = register("angelic_aura", (s) -> new AngelicAuraAugmentBlock(s.hardness(10).resistance(3600000.0F), GOMLTextures.ANGELIC_AURA), 2);
    public static final Pair<ClaimAugmentBlock, Item> HEAVEN_WINGS = register("heaven_wings", (s) -> new HeavenWingsAugmentBlock(s.hardness(10).resistance(3600000.0F), GOMLTextures.HEAVEN_WINGS), 2);
    public static final Pair<ClaimAugmentBlock, Item> VILLAGE_CORE = register("village_core",(s) ->  new ClaimAugmentBlock(s.hardness(10).resistance(3600000.0F), GOMLTextures.VILLAGE_CORE), 2);
    public static final Pair<ClaimAugmentBlock, Item> WITHERING_SEAL = register("withering_seal", (s) -> new WitheringSealAugmentBlock(s.hardness(10).resistance(3600000.0F), GOMLTextures.WITHERING_SEAL), 2);
    public static final Pair<ClaimAugmentBlock, Item> CHAOS_ZONE = register("chaos_zone", (s) -> new ChaosZoneAugmentBlock(s.hardness(10).resistance(3600000.0F), GOMLTextures.CHAOS_ZONE), 2);
    public static final Pair<ClaimAugmentBlock, Item> GREETER = register("greeter", (s) -> new GreeterAugmentBlock(s.hardness(10).resistance(3600000.0F), GOMLTextures.GREETER), 2);
    public static final Pair<SelectiveClaimAugmentBlock, Item> PVP_ARENA = register("pvp_arena", (s) -> new SelectiveClaimAugmentBlock("pvp_arena", s.hardness(10).resistance(3600000.0F), GOMLTextures.PVP_ARENA), 2);
    public static final Pair<ClaimAugmentBlock, Item> EXPLOSION_CONTROLLER = register("explosion_controller", (s) -> new ExplosionControllerAugmentBlock(s.hardness(10).resistance(3600000.0F), GOMLTextures.EXPLOSION_CONTROLLER), 2);

    public static final Pair<ClaimAugmentBlock, Item> FORCE_FIELD = register("force_field", (s) -> new ForceFieldAugmentBlock(s.hardness(10).resistance(3600000.0F), GOMLTextures.FORCE_FIELD), 2);

    private static Pair<ClaimAnchorBlock, Item> register(String name, IntSupplier radius, float hardness, String texture) {
        var id = GetOffMyLawn.id(name);
        var claimAnchorBlock = Registry.register(
                    Registries.BLOCK,
                    id,
                    new ClaimAnchorBlock(AbstractBlock.Settings.create().registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)).strength(hardness, 3600000.0F), radius, texture)
            );


        var registeredItem = Registry.register(Registries.ITEM, id, new ClaimAnchorBlockItem(claimAnchorBlock,
                new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, GetOffMyLawn.id(name))).useBlockPrefixedTranslationKey(), 0));
        ANCHORS.add(claimAnchorBlock);
        return Pair.of(claimAnchorBlock, registeredItem);
    }

    private static <T extends ClaimAugmentBlock> Pair<T, Item> register(String name, Function<Block.Settings, T> augment) {
        return register(name, augment, 0);
    }


    private static <T extends ClaimAugmentBlock> Pair<T, Item> register(String name, Function<Block.Settings, T> augment, int tooltipLines) {
        var id = GetOffMyLawn.id(name);
        T registered = Registry.register(
                Registries.BLOCK,
                id,
                augment.apply(AbstractBlock.Settings.create().registryKey(RegistryKey.of(RegistryKeys.BLOCK, id)))
        );
        BooleanSupplier check = () -> GetOffMyLawn.CONFIG.enabledAugments.getOrDefault(registered, true);

        registered.setEnabledCheck(check);

        Item registeredItem = Registry.register(Registries.ITEM, id, new ToggleableBlockItem((Block & PolymerHeadBlock) registered,
                new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, id)).useBlockPrefixedTranslationKey(), tooltipLines, check));
        AUGMENTS.add(registered);

        GOMLAugments.register(id, registered);

        return Pair.of(registered, registeredItem);
    }

    public static void init() {
        // NO-OP
    }

    private GOMLBlocks() {
        // NO-OP
    }
}
