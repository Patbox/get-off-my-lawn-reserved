package draylar.goml.mixin.compat;

import draylar.goml.api.ClaimUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//import vazkii.botania.common.entity.EntityManaBurst;

/*@Pseudo
@Mixin(EntityManaBurst.class)
public abstract class BotaniaEntityManaBurstMixin extends ThrowableProjectile {
    protected BotaniaEntityManaBurstMixin(EntityType<? extends ThrowableProjectile> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true)
    private void goml_protectClaims(BlockHitResult receiver, CallbackInfo ci) {
        if (this.getOwner() instanceof Player player && !ClaimUtils.canModify(this.level(), receiver.getBlockPos(), player)) {
            this.discard();
            ci.cancel();
        }
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"), cancellable = true)
    private void goml_protectEntities(EntityHitResult hit, CallbackInfo ci) {
        if (this.getOwner() instanceof Player player && !ClaimUtils.canModify(this.level(), BlockPos.containing(hit.getLocation()), player)) {
            this.discard();
            ci.cancel();
        }
    }
}
*/