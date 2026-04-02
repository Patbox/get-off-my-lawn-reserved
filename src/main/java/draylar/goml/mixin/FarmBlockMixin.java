package draylar.goml.mixin;

import draylar.goml.api.ClaimUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmlandBlock.class)
public abstract class FarmBlockMixin extends Block {
    public FarmBlockMixin(Properties settings) {
        super(settings);
    }

    @Inject(method = "turnToDirt", at = @At("HEAD"), cancellable = true)
    private static void goml$protectFarmland(Entity entity, BlockState state, Level world, BlockPos pos, CallbackInfo ci) {
        if (world.isClientSide()) {
            return;
        }
        if (!ClaimUtils.canModify(world, pos, entity instanceof Player player ? player : null)) {
            ci.cancel();
        }
    }
}
