package draylar.goml.block.augment;

import draylar.goml.block.SelectiveClaimAugmentBlock;
import io.github.ladysnake.pal.AbilitySource;
import io.github.ladysnake.pal.Pal;
import io.github.ladysnake.pal.VanillaAbilities;
import net.minecraft.entity.player.PlayerEntity;

public class HeavenWingsAugmentBlock extends SelectiveClaimAugmentBlock {

    public static final AbilitySource HEAVEN_WINGS = Pal.getAbilitySource("goml", "heaven_wings");

    public HeavenWingsAugmentBlock(Settings settings, String texture) {
        super("heaven_wings", settings, texture);
    }

    @Override
    public void applyEffect(PlayerEntity player) {
        HEAVEN_WINGS.grantTo(player, VanillaAbilities.ALLOW_FLYING);
    }

    @Override
    public void removeEffect(PlayerEntity player) {
        HEAVEN_WINGS.revokeFrom(player, VanillaAbilities.ALLOW_FLYING);
    }
}
