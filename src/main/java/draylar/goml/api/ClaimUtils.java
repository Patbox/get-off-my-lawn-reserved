package draylar.goml.api;

import com.jamieswhiteshirt.rtree3i.Box;
import com.jamieswhiteshirt.rtree3i.Entry;
import com.jamieswhiteshirt.rtree3i.Selection;
import draylar.goml.EventHandlers;
import draylar.goml.GetOffMyLawn;
import draylar.goml.api.event.ClaimEvents;
import draylar.goml.block.augment.ExplosionControllerAugmentBlock;
import draylar.goml.block.entity.ClaimAnchorBlockEntity;
import draylar.goml.other.FabricPermissionBridge;
import draylar.goml.other.GomlPlayer;
import draylar.goml.other.OriginOwner;
import draylar.goml.other.StatusEnum;
import draylar.goml.registry.GOMLBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static draylar.goml.GetOffMyLawn.id;

public class ClaimUtils {

    /**
     * Returns all claims at the given position in the given world.
     *
     * @param world world to check for claim in
     * @param pos   position to check at
     * @return claims at the given position in the given world
     */
    public static Selection<Entry<ClaimBox, Claim>> getClaimsAt(LevelReader world, BlockPos pos) {
        Box checkBox = Box.create(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries(box -> box.contains(checkBox));
    }

    /**
     * Returns all claims with the given origin in the given world.
     *
     * @param world world to check for claim in
     * @param pos   position to check at
     * @return claims at the given position in the given world
     */
    public static Selection<Entry<ClaimBox, Claim>> getClaimsWithOrigin(LevelReader world, BlockPos pos) {
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries().filter(x -> x.getValue().getOrigin().equals(pos));
    }

    /**
     * Returns all claims in the given world where player is owner.
     *
     * <p>Under normal circumstances, only 1 claim will exist at a location, but multiple may still be returned.
     *
     * @param world  world to check for claim in
     * @param player player's uuid to find by
     * @return claims at the given position in the given world
     */
    public static Selection<Entry<ClaimBox, Claim>> getClaimsOwnedBy(LevelReader world, UUID player) {
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries().filter(entry -> entry.getValue().isOwner(player));
    }

    /**
     * Returns all claims in the given world where player is trusted.
     *
     * <p>Under normal circumstances, only 1 claim will exist at a location, but multiple may still be returned.
     *
     * @param world  world to check for claim in
     * @param player player's uuid to find by
     * @return claims at the given position in the given world
     */
    public static Selection<Entry<ClaimBox, Claim>> getClaimsTrusted(LevelReader world, UUID player) {
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries().filter(entry -> entry.getValue().getTrusted().contains(player));
    }

    /**
     * Returns all claims in the given world where player has access.
     *
     * <p>Under normal circumstances, only 1 claim will exist at a location, but multiple may still be returned.
     *
     * @param world  world to check for claim in
     * @param player player's uuid to find by
     * @return claims at the given position in the given world
     */
    public static Selection<Entry<ClaimBox, Claim>> getClaimsWithAccess(LevelReader world, UUID player) {
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries().filter(entry -> entry.getValue().hasPermission(player));
    }

    /**
     * Returns all claims that intersect with a box created by the 2 given positions.
     *
     * @param world world to check for claim in
     * @param lower lower corner of claim
     * @param upper upper corner of claim
     * @return claims that intersect with a box created by the 2 positions in the given world
     */
    public static Selection<Entry<ClaimBox, Claim>> getClaimsInBox(LevelReader world, BlockPos lower, BlockPos upper) {
        Box checkBox = createBox(lower, upper);
        return getClaimsInBox(world, checkBox);
    }

    public static Selection<Entry<ClaimBox, Claim>> getClaimsInBox(LevelReader world, Box checkBox) {
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries(box -> box.intersectsClosed(checkBox));
    }

    public static Selection<Entry<ClaimBox, Claim>> getClaimsInOpenBox(LevelReader world, Box checkBox) {
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries(box -> box.intersectsOpen(checkBox));
    }

    public static Selection<Entry<ClaimBox, Claim>> getClaimsInDimension(LevelReader world) {
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries(a -> true);
    }

    public static Box createBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        return Box.create(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    public static Box createBox(BlockPos pos1, BlockPos pos2) {
        return Box.create(Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()), Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()));
    }

    /**
     * Returns all claims that intersect with a box created by the 2 given positions.
     * If the found box is equal to the ignore box, it is not included.
     *
     * @param world  world to check for claim in
     * @param lower  lower corner of claim
     * @param upper  upper corner of claim
     * @param ignore box to ignore
     * @return claims that intersect with a box created by the 2 positions in the given world
     */
    public static Selection<Entry<ClaimBox, Claim>> getClaimsInBox(LevelReader world, BlockPos lower, BlockPos upper, Box ignore) {
        Box checkBox = Box.create(lower.getX(), lower.getY(), lower.getZ(), upper.getX(), upper.getY(), upper.getZ());
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries(box -> box.intersectsClosed(checkBox) && !box.equals(ignore));
    }

    public static Selection<Entry<ClaimBox, Claim>> getClaimsInBox(LevelReader world, Box checkBox, Box ignore) {
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries(box -> box.intersectsClosed(checkBox) && !box.equals(ignore));
    }

    /**
     * Returns whether or not the information about a claim matches with a {@link Player} and {@link BlockPos}.
     *
     * @param claim       claim to check
     * @param checkPlayer player to check against
     * @param checkPos    position to check against
     * @return whether or not the claim information matches up with the player and position
     */
    public static boolean canDestroyClaimBlock(Entry<ClaimBox, Claim> claim, @Nullable Player checkPlayer, BlockPos checkPos) {
        return (checkPlayer == null || playerHasPermission(claim, checkPlayer)) && claim.getValue().getOrigin().equals(checkPos);
    }

    public static boolean canModifyClaimAt(Level world, BlockPos pos, Entry<ClaimBox, Claim> claim, Player player) {
        return claim.getValue().hasPermission(player)
                || isInAdminMode(player)
                || ClaimEvents.PERMISSION_DENIED.invoker().check(player, world, InteractionHand.MAIN_HAND, pos, PermissionReason.AREA_PROTECTED) == InteractionResult.SUCCESS;
    }

    public static boolean isInAdminMode(Player player) {
        return FabricPermissionBridge.checkPermission(player, id("modify_others"), PermissionLevel.ADMINS) && (player instanceof GomlPlayer adminModePlayer && adminModePlayer.goml_getAdminMode());
    }

    public static boolean canFireDestroy(Level world, BlockPos pos) {
        return ClaimUtils.getClaimsAt(world, pos).isEmpty();
    }

    public static boolean canFluidFlow(Level world, BlockPos cur, BlockPos dest) {
        var claimsDest = ClaimUtils.getClaimsAt(world, dest);
        var claimsCur = ClaimUtils.getClaimsAt(world, cur);
        return claimsDest.isEmpty() || claimsCur.anyMatch(x -> claimsCur.anyMatch(y -> x.equals(y)));
    }

    public static boolean canExplosionDestroy(Level world, BlockPos pos, @Nullable Entity causingEntity) {
        Selection<Entry<ClaimBox, Claim>> claimsFound = ClaimUtils.getClaimsAt(world, pos);

        Player player;

        if (causingEntity instanceof Player playerEntity) {
            player = playerEntity;
        } else if (!GetOffMyLawn.CONFIG.protectAgainstHostileExplosionsActivatedByTrustedPlayers && causingEntity instanceof Mob creeperEntity && creeperEntity.getTarget() instanceof Player playerEntity) {
            player = playerEntity;
        } else {
            player = null;
        }

        if (player != null && claimsFound.isNotEmpty()) {
            return !claimsFound.anyMatch((Entry<ClaimBox, Claim> boxInfo) -> !canModifyClaimAt(world, pos, boxInfo, player));
        }

        return claimsFound.isEmpty() || claimsFound.anyMatch((c) -> {
            if (world.getServer() != null) {
                if (c.getValue().hasAugment(GOMLBlocks.EXPLOSION_CONTROLLER.getFirst())) {
                    return c.getValue().getData(ExplosionControllerAugmentBlock.KEY) == StatusEnum.Toggle.DISABLED;
                }
            }

            return false;
        });
    }

    public static boolean canDamageEntity(Level world, Entity entity, DamageSource source) {
        return canDamageEntity(world, entity, source.getEntity(), source.getDirectEntity());
    }
    public static boolean canDamageEntity(Level world, Entity entity, @Nullable Entity attacker, @Nullable Entity source) {
        if (world.isClientSide()) {
            return true;
        }

        if (entity == attacker) {
            return true;
        }

        Player player;

         if (attacker instanceof Player playerEntity) {
            player = playerEntity;
        } else if (!GetOffMyLawn.CONFIG.protectAgainstHostileExplosionsActivatedByTrustedPlayers && attacker instanceof Mob creeperEntity && creeperEntity.getTarget() instanceof Player playerEntity) {
            player = playerEntity;
        } else if (attacker instanceof Projectile projectileEntity && projectileEntity.getOwner() instanceof Player playerEntity) {
            player = playerEntity;
        } else if (attacker instanceof AreaEffectCloud areaEffectCloudEntity && areaEffectCloudEntity.getOwner() instanceof Player playerEntity) {
            player = playerEntity;
        } else if (attacker instanceof TamableAnimal tameableEntity && tameableEntity.getOwner() instanceof Player playerEntity) {
            player = playerEntity;
        } else if (!(entity instanceof Player) && !(GetOffMyLawn.CONFIG.relaxedEntitySourceProtectionCheck && source instanceof LivingEntity) && source != null && (attacker == null || source == attacker)) {
            return hasMatchingClaims(world, entity.blockPosition(), ((OriginOwner) source).goml$getOriginSafe());
        } else {
            return true;
        }

        if (ClaimUtils.isInAdminMode(player) || entity == player) {
            return true;
        }

        if ((GetOffMyLawn.CONFIG.allowDamagingNamedHostileMobs
                || (GetOffMyLawn.CONFIG.allowDamagingUnnamedHostileMobs && entity.getCustomName() == null))
                && entity instanceof Enemy
        ) {
            return true;
        }
        var claims = ClaimUtils.getClaimsAt(world, entity.blockPosition());

        if (claims.isEmpty()) {
            return true;
        }

        if (entity instanceof Player attackedPlayer) {
            claims = claims.filter((e) -> e.getValue().hasAugment(GOMLBlocks.PVP_ARENA.getFirst()));

            if (claims.isEmpty()) {
                return GetOffMyLawn.CONFIG.enablePvPinClaims;
            } else {
                var obj = new MutableBoolean(true);
                claims.forEach((e) -> {
                    if (!obj.getValue()) {
                        return;
                    }
                    var claim = e.getValue();

                    obj.setValue(switch (claim.getData(GOMLBlocks.PVP_ARENA.getFirst().key)) {
                        case EVERYONE -> true;
                        case DISABLED -> false;
                        case TRUSTED -> claim.hasPermission(player) && claim.hasPermission(attackedPlayer);
                        case UNTRUSTED -> !claim.hasPermission(player) && !claim.hasPermission(attackedPlayer);
                        case null -> false;
                    });
                });

                return obj.getValue();
            }
        }

        return EventHandlers.testPermission(claims, player, InteractionHand.MAIN_HAND, entity.blockPosition(), PermissionReason.ENTITY_PROTECTED) != InteractionResult.FAIL;
    }

    public static boolean canModify(Level world, BlockPos pos, @Nullable Player player) {
        if (GetOffMyLawn.CONFIG.allowFakePlayersToModify && player != null && player.getClass() != ServerPlayer.class && !world.isClientSide()) {
            return true;
        }

        Selection<Entry<ClaimBox, Claim>> claimsFound = ClaimUtils.getClaimsAt(world, pos);
        if (player != null && claimsFound.isNotEmpty()) {
            return !claimsFound.anyMatch((Entry<ClaimBox, Claim> boxInfo) -> !canModifyClaimAt(world, pos, boxInfo, player));
        }

        return claimsFound.isEmpty();
    }

    @Nullable
    public static ClaimAnchorBlockEntity getAnchor(Level world, Claim claim) {
        ClaimAnchorBlockEntity claimAnchor = (ClaimAnchorBlockEntity) world.getBlockEntity(claim.getOrigin());

        if (claimAnchor == null) {
            GetOffMyLawn.LOGGER.warn(String.format("A claim anchor was requested at %s, but no Claim Anchor BE was found! Was the claim not properly removed? Removing the claim now.", claim.getOrigin().toString()));

            // Remove claim
            GetOffMyLawn.CLAIM.get(world).getClaims().entries().forEach(entry -> {
                if (entry.getValue() == claim) {
                    claim.destroy();
                }
            });

            return null;
        }

        return claimAnchor;
    }

    public static List<Component> getClaimText(MinecraftServer server, Claim claim) {
        var owners = getPlayerNames(server, claim.getOwners());
        var trusted = getPlayerNames(server, claim.getTrusted());

        var texts = new ArrayList<Component>();

        texts.add(Component.translatable("text.goml.position",
                Component.literal(claim.getOrigin().toShortString())
                        .append(Component.literal(" (" + claim.getWorld().toString() + ")").withStyle(ChatFormatting.GRAY)).withStyle(ChatFormatting.WHITE)
        ).withStyle(ChatFormatting.BLUE));

        texts.add(Component.translatable("text.goml.radius",
                Component.literal("" + claim.getRadius()).withStyle(ChatFormatting.WHITE)
        ).withStyle(ChatFormatting.YELLOW));

        if (!owners.isEmpty()) {
            texts.add(Component.translatable("text.goml.owners", owners.removeFirst()).withStyle(ChatFormatting.GOLD));

            for (var text : owners) {
                texts.add(Component.literal("   ").append(text));
            }
        }

        if (!trusted.isEmpty()) {
            texts.add(Component.translatable("text.goml.trusted", trusted.removeFirst()).withStyle(ChatFormatting.GREEN));

            for (var text : trusted) {
                texts.add(Component.literal("   ").append(text));
            }
        }

        return texts;
    }

    protected static List<Component> getPlayerNames(MinecraftServer server, Collection<UUID> uuids) {
        var list = new ArrayList<Component>();

        var builder = Component.empty();
        var iterator = uuids.iterator();
        while (iterator.hasNext()) {
            var gameProfile = server.services().nameToIdCache().get(iterator.next());
            if (gameProfile.isPresent()) {
                builder.append(Component.object(new PlayerSprite(ResolvableProfile.createUnresolved(gameProfile.get().id()), true)));
                builder.append(" " + gameProfile.get().name());

                if (iterator.hasNext()) {
                    builder.append(", ");
                }

                if (builder.getString().length() > 32) {
                    list.add(builder.withStyle(ChatFormatting.WHITE));
                    builder = Component.empty();
                }
            }
        }
        if (!builder.getSiblings().isEmpty()) {
            list.add(builder.withStyle(ChatFormatting.WHITE));
        }

        return list;
    }

    @Deprecated
    public static boolean claimMatchesWith(Entry<ClaimBox, Claim> claim, @Nullable Player checkPlayer, BlockPos checkPos) {
        return canDestroyClaimBlock(claim, checkPlayer, checkPos);
    }

    @Deprecated
    public static boolean playerHasPermission(Entry<ClaimBox, Claim> claim, Player checkPlayer) {
        return claim.getValue().getOwners().contains(checkPlayer.getUUID()) || isInAdminMode(checkPlayer);
    }

    public static ClaimBox createClaimBox(BlockPos pos, int radius) {
        if (GetOffMyLawn.CONFIG.makeClaimAreaChunkBound) {
            var chunkPos = SectionPos.of(pos);

            radius = (int) ((Math.ceil(radius / 16d) - 1) * 16) + 8;
            var radiusY = (int) ((Math.ceil((radius * GetOffMyLawn.CONFIG.claimAreaHeightMultiplier) / 16d) - 1) * 16) + 8;

            return new ClaimBox(chunkPos.center(), radius, GetOffMyLawn.CONFIG.claimProtectsFullWorldHeight ? Short.MAX_VALUE : radiusY, true);
        }

        return new ClaimBox(pos, radius, GetOffMyLawn.CONFIG.claimProtectsFullWorldHeight ? Short.MAX_VALUE : (int) (radius * GetOffMyLawn.CONFIG.claimAreaHeightMultiplier));
    }

    public static Tuple<Vec3, Direction> getClosestXZBorder(Claim claim, Vec3 curPos) {
        return getClosestXZBorder(claim, curPos, 0);
    }

    public static Tuple<Vec3, Direction> getClosestXZBorder(Claim claim, Vec3 curPos, double extraDistance) {
        var box = claim.getClaimBox();

        var center = box.noShift() ? Vec3.atLowerCornerOf(box.origin()) : Vec3.atCenterOf(box.getOrigin());
        var vec = curPos.subtract(center);
        var r = (box.noShift() ? box.radius() - 0.5 : box.radius()) + extraDistance;
        var angle = Mth.atan2(vec.z, vec.x);

        var tan = Math.tan(angle);

        if (Double.isNaN(tan)) {
            tan = 1;
        }

        double x, z;

        Direction dir = null;

        if (angle >= -Mth.HALF_PI / 2 && angle <= Mth.HALF_PI / 2) {
            x = r;
            z = tan * r;
            dir = Direction.EAST;
        } else if (angle >= Mth.HALF_PI / 2 && angle <= Mth.HALF_PI * 3 / 2) {
            x = 1 / tan * r;
            z = r;
            dir = Direction.SOUTH;
        } else if (angle <= -Mth.HALF_PI / 2 && angle >= -Mth.HALF_PI * 3 / 2) {
            x = -1 / tan * r;
            z = -r;
            dir = Direction.NORTH;
        } else {
            x = -r;
            z = -Math.tan(angle) * r;
            dir = Direction.WEST;
        }


        return new Tuple<>(center.add(x, 0, z), dir);
    }

    public static boolean hasMatchingClaims(Level world, BlockPos target, BlockPos origin) {
        return hasMatchingClaims(world, target, origin, null);
    }
    public static boolean hasMatchingClaims(Level world, BlockPos target, BlockPos origin, @Nullable UUID uuid) {
        var claims = ClaimUtils.getClaimsAt(world, target);

        if (claims.isEmpty()) {
            return true;
        }
        var originClaims = ClaimUtils.getClaimsAt(world, origin);

        if (originClaims.isEmpty() && uuid == null) {
            return false;
        }

        var trusted = new HashSet<UUID>();
        if (uuid != null) {
            trusted.add(uuid);
        }

        originClaims.forEach(x -> {
            trusted.addAll(x.getValue().getOwners());
            trusted.addAll(x.getValue().getTrusted());
        });

        return claims.anyMatch(x -> x.getValue().hasPermission(trusted));

    }

    private static int claimColorIndex(Claim claim) {
        int hash = 0;

        if (GetOffMyLawn.CONFIG.usePlayerForColor()) {
            // get lexicographically smallest UUID because set order is not stable
            UUID min = null;

            for (UUID id : claim.getOwners()) {
                if (min == null || id.compareTo(min) < 0) {
                    min = id;
                }
            }

            if (min != null) {
                hash = min.hashCode();
            }
        } else {
            hash = claim.getOrigin().hashCode();
        }

        return hash & 0xF;
    }

    // From https://lospec.com/palette-list/minecraft-concrete (matches block order so matches goggles).
    private static final int[] CLAIM_COLORS_RGB = new int[]{0xcfd5d6, 0xe06101, 0xa9309f, 0x2489c7, 0xf1af15, 0x5ea918, 0xd5658f, 0x373a3e, 0x7d7d73, 0x157788, 0x64209c, 0x2d2f8f, 0x603c20, 0x495b24, 0x8e2121, 0x080a0f};

    private static final BlockState[] CLAIM_COLORS_BLOCKS = BuiltInRegistries.BLOCK.stream().filter((b) -> {
        var id = BuiltInRegistries.BLOCK.getKey(b);

        return id.getNamespace().equals("minecraft") && id.getPath().endsWith("_concrete");
    }).map((b) -> b.defaultBlockState()).collect(Collectors.toList()).toArray(new BlockState[0]);

    public static int webMapClaimColor(Claim claim) {
        return CLAIM_COLORS_RGB[claimColorIndex(claim)];
    }

    public static BlockState gogglesClaimColor(Claim claim) {
        return CLAIM_COLORS_BLOCKS[claimColorIndex(claim)];
    }

    public static void drawClaimInWorld(ServerPlayer player, Claim claim) {
        var box = claim.getClaimBox().toBox();
        var minPos = new BlockPos(box.x1(), Math.max(box.y1(), player.level().getMinY()), box.z1());
        var maxPos = new BlockPos(box.x2() - 1, Math.min(box.y2() - 1, player.level().getMaxY()), box.z2() - 1);

        BlockState state = ClaimUtils.gogglesClaimColor(claim);

        WorldParticleUtils.render(player, minPos, maxPos,
                new BlockParticleOption(ParticleTypes.BLOCK_MARKER, state)
        );
    }
}
