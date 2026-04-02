package draylar.goml.block;

import draylar.goml.api.Augment;
import draylar.goml.api.Claim;
import draylar.goml.api.ClaimUtils;
import draylar.goml.block.entity.ClaimAnchorBlockEntity;
import draylar.goml.block.entity.ClaimAugmentBlockEntity;
import draylar.goml.registry.GOMLEntities;
import draylar.goml.registry.GOMLTextures;
import eu.pb4.polymer.core.api.block.PolymerHeadBlock;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;

import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ClaimAugmentBlock extends Block implements Augment, EntityBlock, PolymerHeadBlock {

    private final String texture;
    private BooleanSupplier isEnabled = () -> true;

    @Deprecated
    public ClaimAugmentBlock(Properties settings) {
        this(settings.forceSolidOn(), GOMLTextures.MISSING_TEXTURE);
    }

    public ClaimAugmentBlock(Properties settings, String texture) {
        super(settings.forceSolidOn());
        this.texture = texture;
    }

    @Override
    public MutableComponent getName() {
        return Component.translatable("block.goml.anchor_augment", Component.translatable(this.getDescriptionId()));
    }

    public MutableComponent getGuiName() {
        return Component.translatable("text.goml.augment", Component.translatable(this.getDescriptionId()));
    }

    @Override
    public Component getAugmentName() {
        return Component.translatable(this.getDescriptionId());
    }

    @ApiStatus.Internal
    public void setEnabledCheck(BooleanSupplier check) {
        this.isEnabled = check;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player playerEntity, BlockHitResult hit) {
        if (playerEntity instanceof ServerPlayer player && this.hasSettings()) {
            var blockEntity = world.getBlockEntity(pos, GOMLEntities.CLAIM_AUGMENT);

            if (blockEntity.isPresent() && blockEntity.get().getClaim() != null) {
                var claim = blockEntity.get().getClaim();

                if (claim != null && claim.isOwner(player)) {
                    this.openSettings(claim, player, null);
                }

                return InteractionResult.SUCCESS;
            }
        }

        return super.useWithoutItem(state, world, pos, playerEntity, hit);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        if (world instanceof Level) {
            for (Direction direction : Direction.values()) {
                var blockEntity = world.getBlockEntity(pos.relative(direction));

                if (blockEntity instanceof ClaimAnchorBlockEntity claimAnchorBlockEntity && claimAnchorBlockEntity.getClaim() != null) {
                    return this.canPlace(claimAnchorBlockEntity.getClaim(), (Level) world, pos);
                } else if (blockEntity instanceof ClaimAugmentBlockEntity claimAugmentBlockEntity && claimAugmentBlockEntity.getClaim() != null && claimAugmentBlockEntity.getClaim() != null) {
                    return this.canPlace(claimAugmentBlockEntity.getClaim(), (Level) world, pos);
                }
            }
        }

        return false;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (world == null || world.isClientSide()) {
            return;
        }

        ClaimAugmentBlockEntity thisBE = (ClaimAugmentBlockEntity) world.getBlockEntity(pos);

        if (thisBE == null) {
            return;
        }

        thisBE.initialize(this);

        for (Direction direction : Direction.values()) {
            BlockPos offsetPos = pos.relative(direction);
            BlockState offsetState = world.getBlockState(offsetPos);
            Block offsetBlock = offsetState.getBlock();

            // Neighbor is a core element, set parent directly
            if (offsetBlock instanceof ClaimAnchorBlock) {
                if (world.getBlockEntity(offsetPos) instanceof ClaimAnchorBlockEntity be) {
                    thisBE.setParent(be.getBlockPos(), be.getClaim());
                }
                return;
            }

            // Neighbor is another augment, grab parent from it
            if (offsetBlock instanceof ClaimAugmentBlock) {

                if ( world.getBlockEntity(offsetPos) instanceof ClaimAugmentBlockEntity be) {
                    thisBE.setParent(be.getBlockPos(), be.getClaim());
                }

                return;
            }
        }
        super.setPlacedBy(world, pos, state, placer, itemStack);
    }

    @Override
    public void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        if (world == null || world.isClientSide()) {
            return;
        }

        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide() && world.getBlockEntity(pos) instanceof ClaimAugmentBlockEntity be) {
            be.remove();
        }

        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public boolean canPlace(Claim claim, Level world, BlockPos pos) {
        return !claim.hasAugment(this);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter world, BlockPos pos) {
        if (ClaimUtils.isInAdminMode(player) || (world instanceof ServerLevel serverWorld && ClaimUtils.getClaimsAt(serverWorld, pos).anyMatch(x -> x.getValue().isOwner(player)))) {
            return super.getDestroyProgress(state, player, world, pos);
        } else {
            return 0;
        }
    }

    @Override
    public boolean isEnabled(Claim claim, Level world) {
        return this.isEnabled.getAsBoolean();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ClaimAugmentBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return ClaimAugmentBlockEntity::tick;
    }

    @Override
    public String getPolymerSkinValue(BlockState state, BlockPos pos, PacketContext player) {
        return this.texture;
    }
}
