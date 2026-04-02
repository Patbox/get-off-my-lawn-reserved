package draylar.goml.mixin.compat;

import draylar.goml.api.ClaimUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
/*import vazkii.botania.common.item.ItemLaputaShard;

@Unique
@Mixin(ItemLaputaShard.class)
public class BotaniaItemLaputaShardMixin {
    @Redirect(method = {"spawnNextBurst", "updateBurst"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private BlockState goml_canReplace(Level instance, BlockPos pos) {
        if (ClaimUtils.getClaimsAt(instance, pos).isNotEmpty()) {
            return Blocks.BEDROCK.defaultBlockState();
        }

        return instance.getBlockState(pos);
    }

}*/
