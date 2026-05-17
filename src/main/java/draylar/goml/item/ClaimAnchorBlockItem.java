package draylar.goml.item;

import draylar.goml.GetOffMyLawn;
import draylar.goml.api.ClaimUtils;
import draylar.goml.block.ClaimAnchorBlock;
import draylar.goml.other.FabricPermissionBridge;
import draylar.goml.registry.GOMLBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Objects;
import java.util.function.Consumer;

import static draylar.goml.GetOffMyLawn.id;

public class ClaimAnchorBlockItem extends TooltippedBlockItem {

    private final ClaimAnchorBlock claimBlock;

    public ClaimAnchorBlockItem(ClaimAnchorBlock block, Properties settings, int lines) {
        super(block, settings, lines);
        this.claimBlock = block;
    }

    @Override
    public void addLines(Consumer<Component> textConsumer) {
        super.addLines(textConsumer);
        textConsumer.accept(Component.translatable("text.goml.radius",
                Component.literal("" + this.claimBlock.getRadius()).withStyle(ChatFormatting.WHITE)
        ).withStyle(ChatFormatting.YELLOW));
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        if (context.getLevel().isClientSide()) {
            return true;
        }

        var pos = context.getClickedPos();
        var radius = this.claimBlock.getRadius();

        if (radius <= 0 && !ClaimUtils.isInAdminMode(context.getPlayer())) {
            context.getPlayer().sendSystemMessage(GetOffMyLawn.CONFIG.prefix(Component.translatable("text.goml.cant_place_claim.admin_only").withStyle(ChatFormatting.RED)));
            return false;
        }

        radius = Math.max(radius, 1);
        var checkBox = ClaimUtils.createClaimBox(pos, radius);

        if (!ClaimUtils.isInAdminMode(context.getPlayer())) {
            var count = ClaimUtils.getClaimsOwnedBy(context.getLevel(), Objects.requireNonNull(context.getPlayer()).getUUID()).filter(x -> x.getValue().getType() != GOMLBlocks.ADMIN_CLAIM_ANCHOR.getFirst()).count();

            var allowedCount = FabricPermissionBridge.checkPermissionInteger(context.getPlayer(), id("claim_limit"));
            var allowedCount2 = FabricPermissionBridge.checkPermissionInteger(context.getPlayer(), id("claim_limit/" +
                    context.getLevel().dimension().identifier().getNamespace() + "/" + context.getLevel().dimension().identifier().getPath()));

            var maxCount = allowedCount2.orElse(allowedCount.orElse(GetOffMyLawn.CONFIG.maxClaimsPerPlayer));

            if (maxCount != -1
                    && count >= maxCount
            ) {
                context.getPlayer().sendSystemMessage(GetOffMyLawn.CONFIG.prefix(Component.translatable("text.goml.cant_place_claim.max_count_reached", count, GetOffMyLawn.CONFIG.maxClaimsPerPlayer).withStyle(ChatFormatting.RED)));
                return false;
            }

            if (GetOffMyLawn.CONFIG.isBlacklisted(context.getLevel(), checkBox.toBox())) {
                context.getPlayer().sendSystemMessage(GetOffMyLawn.CONFIG.prefix(Component.translatable("text.goml.cant_place_claim.blacklisted_area", context.getLevel().dimension().identifier().toString(), context.getClickedPos().toShortString()).withStyle(ChatFormatting.RED)));
                return false;
            }
        }


        var claims = ClaimUtils.getClaimsInBox(context.getLevel(), checkBox.rtree3iBox());
        if (GetOffMyLawn.CONFIG.allowClaimOverlappingIfSameOwner) {
            claims = claims.filter(x -> !x.getValue().isOwner(context.getPlayer()) || x.getKey().toBox().equals(checkBox.toBox()));
        }

        if (claims.isNotEmpty()) {
            var list = Component.literal("");

            claims.forEach((c) -> {
                var box = c.getKey().toBox();

                list.append(Component.literal("[").withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(box.x1() + ", " + box.y1() + ", " + box.z1()).withStyle(ChatFormatting.WHITE))
                        .append(" | ")
                        .append(Component.literal(box.x2() + ", " + box.y2() + ", " + box.z2()).withStyle(ChatFormatting.WHITE))
                        .append("] ")
                );
            });

            context.getPlayer().sendSystemMessage(GetOffMyLawn.CONFIG.prefix(Component.translatable("text.goml.cant_place_claim.collides_with", list).withStyle(ChatFormatting.RED)));
            return false;
        }

        return super.canPlace(context, state);
    }
}
