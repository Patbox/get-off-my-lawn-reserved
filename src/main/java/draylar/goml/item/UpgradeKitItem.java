package draylar.goml.item;

import com.jamieswhiteshirt.rtree3i.Box;
import com.jamieswhiteshirt.rtree3i.Entry;
import com.jamieswhiteshirt.rtree3i.Selection;
import draylar.goml.GetOffMyLawn;
import draylar.goml.api.Claim;
import draylar.goml.api.ClaimBox;
import draylar.goml.api.ClaimUtils;
import draylar.goml.api.event.ClaimEvents;
import draylar.goml.block.ClaimAnchorBlock;
import draylar.goml.block.entity.ClaimAnchorBlockEntity;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.core.HolderLookup;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class UpgradeKitItem extends Item implements PolymerItem {

    private final ClaimAnchorBlock from;
    private final ClaimAnchorBlock to;
    private final Item clientItem;

    public UpgradeKitItem(Properties settings, ClaimAnchorBlock from, ClaimAnchorBlock to, Item display) {
        super(settings);
        this.clientItem = display;

        this.from = from;
        this.to = to;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if(context == null || context.getPlayer() == null || context.getLevel().isClientSide()) {
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        Level world = context.getLevel();
        BlockState block = world.getBlockState(pos);

        if(block.getBlock().equals(from)) {
            // get claims at block position
            Selection<Entry<ClaimBox, Claim>> claimsFound = GetOffMyLawn.CLAIM.get(world).getClaims().entries(box ->
                    box.contains(Box.create(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1))
            );

            if(!claimsFound.isEmpty()) {
                boolean noPermission = claimsFound.anyMatch((Entry<ClaimBox, Claim> boxInfo) -> !boxInfo.getValue().getOwners().contains(context.getPlayer().getUUID()));

                // get claim at location
                AtomicReference<Entry<ClaimBox, Claim>> currentClaim = new AtomicReference<>();
                claimsFound.forEach(claim -> {
                    if (claim.getValue().getOrigin().equals(pos) && claim.getValue().getOwners().contains(context.getPlayer().getUUID())) {
                        currentClaim.set(claim);
                    }
                });


                // if we have permission
                if(!noPermission) {
                    var radius = to.getRadius();
                    var newBox = ClaimUtils.createClaimBox(pos, radius);

                    // if we don't overlap with another claim
                    var claims = ClaimUtils.getClaimsInBox(world, newBox.rtree3iBox(), currentClaim.get().getKey().toBox());
                    if (claims.isEmpty()) {
                        var claimInfo = currentClaim.get().getValue();
                        var oldSize = claimInfo.getClaimBox();

                        // remove claim
                        GetOffMyLawn.CLAIM.get(world).remove(claimInfo);

                        // set block
                        BlockEntity oldBE = world.getBlockEntity(pos);
                        world.setBlockAndUpdate(pos, to.defaultBlockState());

                        if (this.to.asItem() != null) {
                            claimInfo.internal_setIcon(this.to.asItem().getDefaultInstance());
                        }
                        claimInfo.internal_setType(this.to);

                        claimInfo.internal_setClaimBox(newBox);
                        if (world instanceof ServerLevel world1) {
                            claimInfo.internal_updateChunkCount(world1);
                        }
                        claimInfo.internal_setWorld(currentClaim.get().getValue().getWorld());
                        GetOffMyLawn.CLAIM.get(world).add(claimInfo);

                        // decrement stack
                        if(!context.getPlayer().isCreative() && !context.getPlayer().isSpectator()) {
                            context.getItemInHand().shrink(1);
                        }

                        // transfer BE data
                        BlockEntity newBE = world.getBlockEntity(pos);
                        if(oldBE instanceof ClaimAnchorBlockEntity && newBE instanceof ClaimAnchorBlockEntity) {
                            ((ClaimAnchorBlockEntity) newBE).from(((ClaimAnchorBlockEntity) oldBE));
                        }

                        ClaimEvents.CLAIM_RESIZED.invoker().onResizeEvent(claimInfo, oldSize, newBox);
                        return InteractionResult.SUCCESS;
                    } else {
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

                        context.getPlayer().sendSystemMessage(GetOffMyLawn.CONFIG.prefix(Component.translatable("text.goml.cant_upgrade_claim.collides_with", list).withStyle(ChatFormatting.RED)));
                    }
                }
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay displayComponent, Consumer<Component> textConsumer, TooltipFlag type) {
        super.appendHoverText(stack, context, displayComponent, textConsumer, type);
        textConsumer.accept(Component.translatable(from.getDescriptionId()).append(" -> ").append(Component.translatable(to.getDescriptionId())).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return this.clientItem;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context, HolderLookup.Provider l) {
        return null;
    }
}
