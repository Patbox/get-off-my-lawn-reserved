package draylar.goml.api;

import com.mojang.authlib.GameProfile;
import draylar.goml.GetOffMyLawn;
import draylar.goml.registry.GOMLBlocks;
import eu.pb4.common.protection.api.ProtectionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

public final class GomlProtectionProvider implements ProtectionProvider {
    public static ProtectionProvider INSTANCE = new GomlProtectionProvider();
    private GomlProtectionProvider() {}

    @Override
    public boolean isProtected(Level world, BlockPos pos) {
        if (world.getServer() == null) {
            return false;
        }
        return ClaimUtils.getClaimsAt(world, pos).isNotEmpty();
    }

    @Override
    public boolean isAreaProtected(Level world, AABB box) {
        if (world.getServer() == null) {
            return false;
        }
        return ClaimUtils.getClaimsInBox(world, BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ)).isNotEmpty();
    }

    @Override
    public boolean canBreakBlock(Level world, BlockPos pos,NameAndId profile, @Nullable Player player) {
        if (world.getServer() == null) {
            return true;
        }

        if (player != null) {
            return ClaimUtils.canModify(world, pos, player);
        } else {
            var claims = ClaimUtils.getClaimsAt(world, pos);
            return claims.isEmpty() || claims.anyMatch((c) -> c.getValue().hasPermission(profile.id()));
        }
    }

    @Override
    public boolean canExplodeBlock(Level world, BlockPos pos, Explosion explosion, NameAndId profile, @Nullable Player player) {
        if (world.getServer() == null) {
            return true;
        }
        return ClaimUtils.canExplosionDestroy(world, pos, player);
    }

    @Override
    public boolean canPlaceBlock(Level world, BlockPos pos, NameAndId profile, @Nullable Player player) {
        return this.canBreakBlock(world, pos, profile, player);
    }

    @Override
    public boolean canInteractBlock(Level world, BlockPos pos, NameAndId profile, @Nullable Player player) {
        if (world.getServer() == null) {
            return true;
        }
        return GetOffMyLawn.CONFIG.canInteract(world.getBlockState(pos).getBlock()) || this.canBreakBlock(world, pos, profile, player);
    }

    @Override
    public boolean canInteractEntity(Level world, Entity entity, NameAndId profile, @Nullable Player player) {
        if (world.getServer() == null) {
            return true;
        }
        return GetOffMyLawn.CONFIG.canInteract(entity) || this.canBreakBlock(world, entity.blockPosition(), profile, player);
    }

    @Override
    public boolean canDamageEntity(Level world, Entity entity, NameAndId profile, @Nullable Player player) {
        if (world.getServer() == null) {
            return true;
        }

        if (entity instanceof Player attackedPlayer) {
            var claims = ClaimUtils.getClaimsAt(world, entity.blockPosition());

            if (claims.isEmpty()) {
                return true;
            } else {
                claims = claims.filter((e) -> e.getValue().hasAugment(GOMLBlocks.PVP_ARENA.getFirst()));

                if (claims.isEmpty()) {
                    return GetOffMyLawn.CONFIG.enablePvPinClaims;
                } else {
                    var obj = new MutableBoolean();

                    claims.forEach((e) -> {
                        var claim = e.getValue();
                        if (!obj.getValue()) {
                            return;
                        }

                        obj.setValue(switch (claim.getData(GOMLBlocks.PVP_ARENA.getFirst().key)) {
                            case EVERYONE -> true;
                            case DISABLED -> player != null && ClaimUtils.isInAdminMode(player);
                            case TRUSTED -> claim.hasPermission(profile.id()) && claim.hasPermission(attackedPlayer);
                            case UNTRUSTED -> !claim.hasPermission(profile.id()) && !claim.hasPermission(attackedPlayer);
                        });
                    });

                    return obj.getValue();
                }
            }
        }

        return this.canBreakBlock(world, entity.blockPosition(), profile, player)
                || (GetOffMyLawn.CONFIG.allowDamagingUnnamedHostileMobs && entity instanceof Monster && entity.getCustomName() == null);
    }
}
