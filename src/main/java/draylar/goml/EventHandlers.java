package draylar.goml;

import com.jamieswhiteshirt.rtree3i.Entry;
import com.jamieswhiteshirt.rtree3i.Selection;
import draylar.goml.api.Claim;
import draylar.goml.api.ClaimBox;
import draylar.goml.api.ClaimUtils;
import draylar.goml.api.PermissionReason;
import draylar.goml.api.event.ClaimEvents;
import draylar.goml.block.entity.ClaimAnchorBlockEntity;
import draylar.goml.registry.GOMLBlocks;
import draylar.goml.registry.GOMLTags;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.ApiStatus;

import static draylar.goml.GetOffMyLawn.id;

@ApiStatus.Internal
public class EventHandlers {
    public static final Identifier GOML_PHASE = id("protection");

    private EventHandlers() {
        // NO-OP
    }

    public static void init() {
        for (var x : new Event<?>[] {
                UseEntityCallback.EVENT,
                AttackEntityCallback.EVENT,
                UseBlockCallback.EVENT,
                PlayerBlockBreakEvents.BEFORE,
                AttackBlockCallback.EVENT
        }) {
            x.addPhaseOrdering(GOML_PHASE, Event.DEFAULT_PHASE);
        }

        registerBreakBlockCallback();
        registerInteractBlockCallback();
        registerAttackEntityCallback();
        registerInteractEntityCallback();
        registerAnchorAttackCallback();
    }

    private static void registerInteractEntityCallback() {
        UseEntityCallback.EVENT.register(GOML_PHASE, (playerEntity, world, hand, entity, entityHitResult) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }

            if (ClaimUtils.isInAdminMode(playerEntity)) {
                return InteractionResult.PASS;
            }

            if (GetOffMyLawn.CONFIG.canInteract(entity)
                    || entity.is(GOMLTags.ALLOWED_INTERACTIONS_ENTITY)) {
                return InteractionResult.PASS;
            }

            if (entity instanceof OwnableEntity tameable && tameable.getOwner() == playerEntity) {
                return InteractionResult.PASS;
            }

            if (entity instanceof Player attackedPlayer) {
                var claims = ClaimUtils.getClaimsAt(world, entity.blockPosition());

                if (claims.isEmpty()) {
                    return InteractionResult.PASS;
                } else {
                    claims = claims.filter((e) -> e.getValue().hasAugment(GOMLBlocks.PVP_ARENA.getFirst()));

                    if (claims.isEmpty()) {
                        return GetOffMyLawn.CONFIG.enablePvPinClaims ? InteractionResult.PASS : InteractionResult.FAIL;
                    } else {
                        var obj = new MutableObject<InteractionResult>(InteractionResult.PASS);
                        claims.forEach((e) -> {
                            if (obj.getValue() instanceof InteractionResult.Fail) {
                                return;
                            }
                            var claim = e.getValue();

                            obj.setValue(switch (claim.getData(GOMLBlocks.PVP_ARENA.getFirst().key)) {
                                case EVERYONE -> InteractionResult.PASS;
                                case DISABLED -> InteractionResult.FAIL;
                                case TRUSTED -> claim.hasPermission(playerEntity) && claim.hasPermission(attackedPlayer)
                                        ? InteractionResult.PASS : InteractionResult.FAIL;
                                case UNTRUSTED -> !claim.hasPermission(playerEntity) && !claim.hasPermission(attackedPlayer)
                                        ? InteractionResult.PASS : InteractionResult.FAIL;
                            });
                        });

                        return obj.getValue();
                    }
                }
            }

            Selection<Entry<ClaimBox, Claim>> claimsFound = ClaimUtils.getClaimsAt(world, entity.blockPosition());
            return testPermission(claimsFound, playerEntity, hand, entity.blockPosition(), PermissionReason.ENTITY_PROTECTED);
        });
    }

    private static void registerAttackEntityCallback() {
        AttackEntityCallback.EVENT.register(GOML_PHASE, (playerEntity, world, hand, entity, entityHitResult) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }
            return ClaimUtils.canDamageEntity(world, entity, world.damageSources().playerAttack(playerEntity)) ? InteractionResult.PASS : InteractionResult.FAIL;
        });
    }

    private static void registerInteractBlockCallback() {
        UseBlockCallback.EVENT.register(GOML_PHASE, (playerEntity, world, hand, blockHitResult) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }
            if (!(playerEntity.getItemInHand(hand).getItem() instanceof BlockItem)) {
                var blockState = world.getBlockState(blockHitResult.getBlockPos());

                if (GetOffMyLawn.CONFIG.canInteract(blockState.getBlock()) || blockState.is(GOMLTags.ALLOWED_INTERACTIONS_BLOCKS)) {
                    return InteractionResult.PASS;
                }
            }

            var claimsFound = ClaimUtils.getClaimsAt(world, blockHitResult.getBlockPos());

            var ac = testPermission(claimsFound, playerEntity, hand, blockHitResult.getBlockPos(), PermissionReason.AREA_PROTECTED);

            if (ac == InteractionResult.PASS) {
                var claimsFound2 = ClaimUtils.getClaimsAt(world, blockHitResult.getBlockPos().relative(blockHitResult.getDirection()));
                return testPermission(claimsFound2, playerEntity, hand, blockHitResult.getBlockPos().relative(blockHitResult.getDirection()), PermissionReason.AREA_PROTECTED);
            }

            return ac;
        });
    }

    private static void registerBreakBlockCallback() {
        AttackBlockCallback.EVENT.register(GOML_PHASE, (playerEntity, world, hand, blockPos, direction) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }
            Selection<Entry<ClaimBox, Claim>> claimsFound = ClaimUtils.getClaimsAt(world, blockPos);
            return testPermission(claimsFound, playerEntity, hand, blockPos, PermissionReason.BLOCK_PROTECTED);
        });

        PlayerBlockBreakEvents.BEFORE.register(GOML_PHASE, (world, player, pos, state, blockEntity) -> {
            if (world.isClientSide()) {
                return true;
            }
            Selection<Entry<ClaimBox, Claim>> claimsFound = ClaimUtils.getClaimsAt(world, pos);
            InteractionResult result = testPermission(claimsFound, player, InteractionHand.MAIN_HAND, pos, PermissionReason.BLOCK_PROTECTED);
            return !result.equals(InteractionResult.FAIL);
        });
    }

    private static void registerAnchorAttackCallback() {
        AttackBlockCallback.EVENT.register(GOML_PHASE, (playerEntity, world, hand, blockPos, direction) -> {
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }
            var be = world.getBlockEntity(blockPos);

            if (be instanceof ClaimAnchorBlockEntity) {
                if (!(((ClaimAnchorBlockEntity) be).getClaim().isOwner(playerEntity) || ClaimUtils.isInAdminMode(playerEntity))) {
                    return InteractionResult.FAIL;
                }
            }

            return InteractionResult.PASS;
        });
    }

    @ApiStatus.Internal
    public static InteractionResult testPermission(Selection<Entry<ClaimBox, Claim>> claims, Player player, InteractionHand hand, BlockPos pos, PermissionReason reason) {
        if (player.level().isClientSide()) {
            return InteractionResult.PASS;
        }

        if (!claims.isEmpty()) {
            boolean noPermission = claims.anyMatch((Entry<ClaimBox, Claim> boxInfo) -> !boxInfo.getValue().hasPermission(player));

            if (noPermission && !ClaimUtils.isInAdminMode(player)) {
                InteractionResult check = ClaimEvents.PERMISSION_DENIED.invoker().check(player, player.level(), hand, pos, reason);

                if (check.consumesAction() || check.equals(InteractionResult.PASS)) {
                    player.sendOverlayMessage(reason.getReason());
                    return InteractionResult.FAIL;
                }
            }
        }

        return InteractionResult.PASS;
    }
}
