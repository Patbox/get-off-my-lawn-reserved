package draylar.goml.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import draylar.goml.api.ClaimUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level world) {
        super(entityType, world);
    }

    @ModifyExpressionValue(method = "doSweepAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<Entity> filterEntities(List<Entity> original) {
        original.removeIf(x -> !ClaimUtils.canDamageEntity(this.level(), x, this, null));
        return original;
    }


    @Inject(method = "cannotAttack", at = @At("HEAD"), cancellable = true)
    private void preventAttack(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!ClaimUtils.canDamageEntity(this.level(), entity, this, null)) {
            cir.setReturnValue(true);
        }
    }
}
