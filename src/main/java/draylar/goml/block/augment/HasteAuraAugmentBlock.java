package draylar.goml.block.augment;

import draylar.goml.api.Claim;
import draylar.goml.block.SelectiveClaimAugmentBlock;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

public class HasteAuraAugmentBlock extends SelectiveClaimAugmentBlock {

    public HasteAuraAugmentBlock(Settings settings, String texture) {
        super("haste_aura", settings, texture);
    }

    @Override
    public void playerTick(Claim claim, PlayerEntity player) {
        if (this.canApply(claim, player)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 80, 1, true, false));
        }
    }

    @Override
    public boolean ticks() { return true; }
}