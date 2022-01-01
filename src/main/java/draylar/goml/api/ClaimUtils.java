package draylar.goml.api;

import com.jamieswhiteshirt.rtree3i.Box;
import com.jamieswhiteshirt.rtree3i.Entry;
import com.jamieswhiteshirt.rtree3i.Selection;
import draylar.goml.AdminModePlayer;
import draylar.goml.GetOffMyLawn;
import draylar.goml.api.event.ClaimEvents;
import draylar.goml.entity.ClaimAnchorBlockEntity;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ClaimUtils {

    /**
     * Returns all claims at the given position in the given world.
     *
     * <p>Under normal circumstances, only 1 claim will exist at a location, but multiple may still be returned.
     *
     * @param world world to check for claim in
     * @param pos   position to check at
     * @return claims at the given position in the given world
     */
    public static Selection<Entry<ClaimBox, Claim>> getClaimsAt(WorldView world, BlockPos pos) {
        Box checkBox = Box.create(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries(box -> box.contains(checkBox));
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
    public static Selection<Entry<ClaimBox, Claim>> getClaimsOwnedBy(WorldView world, UUID player) {
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
    public static Selection<Entry<ClaimBox, Claim>> getClaimsTrusted(WorldView world, UUID player) {
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
    public static Selection<Entry<ClaimBox, Claim>> getClaimsWithAccess(WorldView world, UUID player) {
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
    public static Selection<Entry<ClaimBox, Claim>> getClaimsInBox(WorldView world, BlockPos lower, BlockPos upper) {
        Box checkBox = Box.create(lower.getX(), lower.getY(), lower.getZ(), upper.getX(), upper.getY(), upper.getZ());
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries(box -> box.intersectsClosed(checkBox));
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
    public static Selection<Entry<ClaimBox, Claim>> getClaimsInBox(WorldView world, BlockPos lower, BlockPos upper, Box ignore) {
        Box checkBox = Box.create(lower.getX(), lower.getY(), lower.getZ(), upper.getX(), upper.getY(), upper.getZ());
        return GetOffMyLawn.CLAIM.get(world).getClaims().entries(box -> box.intersectsClosed(checkBox) && !box.equals(ignore));
    }

    /**
     * Returns whether or not the information about a claim matches with a {@link PlayerEntity} and {@link BlockPos}.
     *
     * @param claim       claim to check
     * @param checkPlayer player to check against
     * @param checkPos    position to check against
     * @return whether or not the claim information matches up with the player and position
     */
    public static boolean canDestroyClaimBlock(Entry<ClaimBox, Claim> claim, @Nullable PlayerEntity checkPlayer, BlockPos checkPos) {
        return (checkPlayer == null || playerHasPermission(claim, checkPlayer)) && claim.getValue().getOrigin().equals(checkPos);
    }

    public static boolean canModifyClaimAt(World world, BlockPos pos, Entry<ClaimBox, Claim> claim, PlayerEntity player) {
        return claim.getValue().hasPermission(player)
                || isInAdminMode(player)
                || ClaimEvents.PERMISSION_DENIED.invoker().check(player, world, Hand.MAIN_HAND, pos, PermissionReason.AREA_PROTECTED) == ActionResult.SUCCESS;
    }

    public static boolean isInAdminMode(PlayerEntity player) {
        return Permissions.check(player, "goml.modify_others", 3) && (player instanceof AdminModePlayer adminModePlayer && adminModePlayer.goml_getAdminMode());
    }

    public static boolean canExplosionDestroy(World world, BlockPos pos, @Nullable Entity causingEntity) {
        Selection<Entry<ClaimBox, Claim>> claimsFound = ClaimUtils.getClaimsAt(world, pos);

        PlayerEntity player;

        if (causingEntity instanceof PlayerEntity playerEntity){
            player = playerEntity;
        } else if (causingEntity instanceof CreeperEntity creeperEntity && creeperEntity.getTarget() instanceof PlayerEntity playerEntity){
            player = playerEntity;
        } else {
            player = null;
        }

        if (player != null && claimsFound.isNotEmpty()) {
            return !claimsFound.anyMatch((Entry<ClaimBox, Claim> boxInfo) -> !canModifyClaimAt(world, pos, boxInfo, player));
        }

        return claimsFound.isEmpty();
    }

    public static boolean canModify(World world, BlockPos pos, PlayerEntity player) {
        Selection<Entry<ClaimBox, Claim>> claimsFound = ClaimUtils.getClaimsAt(world, pos);
        if (player != null && claimsFound.isNotEmpty()) {
            return !claimsFound.anyMatch((Entry<ClaimBox, Claim> boxInfo) -> !canModifyClaimAt(world, pos, boxInfo, player));
        }

        return claimsFound.isEmpty();
    }

    @Nullable
    public static ClaimAnchorBlockEntity getAnchor(World world, Claim claim) {
        ClaimAnchorBlockEntity claimAnchor = (ClaimAnchorBlockEntity) world.getBlockEntity(claim.getOrigin());

        if (claimAnchor == null) {
            GetOffMyLawn.LOGGER.warn(String.format("A claim anchor was requested at %s, but no Claim Anchor BE was found! Was the claim not properly removed? Removing the claim now.", claim.getOrigin().toString()));

            // Remove claim
            GetOffMyLawn.CLAIM.get(world).getClaims().entries().forEach(entry -> {
                if (entry.getValue() == claim) {
                    GetOffMyLawn.CLAIM.get(world).remove(entry.getKey());
                }
            });

            return null;
        }

        return claimAnchor;
    }

    @Deprecated
    public static boolean claimMatchesWith(Entry<ClaimBox, Claim> claim, @Nullable PlayerEntity checkPlayer, BlockPos checkPos) {
        return canDestroyClaimBlock(claim, checkPlayer, checkPos);
    }

    @Deprecated
    public static boolean playerHasPermission(Entry<ClaimBox, Claim> claim, PlayerEntity checkPlayer) {
        return claim.getValue().getOwners().contains(checkPlayer.getUuid()) || isInAdminMode(checkPlayer);
    }
}
