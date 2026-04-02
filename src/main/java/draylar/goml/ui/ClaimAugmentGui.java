package draylar.goml.ui;

import draylar.goml.api.Augment;
import draylar.goml.api.Claim;
import draylar.goml.api.ClaimUtils;
import draylar.goml.block.entity.ClaimAnchorBlockEntity;
import draylar.goml.item.TooltippedBlockItem;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;

@ApiStatus.Internal
public class ClaimAugmentGui extends PagedGui {
    private final Claim claim;
    private final boolean canModify;
    private final ClaimAnchorBlockEntity blockEntity;
    private final List<Map.Entry<BlockPos, Augment>> cachedEntries = new ArrayList<>();

    public ClaimAugmentGui(ServerPlayer player, Claim claim, boolean canModify, @Nullable Runnable onClose) {
        super(player, onClose);
        this.claim = claim;
        this.blockEntity = ClaimUtils.getAnchor(player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, claim.getWorld())), claim);
        this.canModify = canModify;
        this.setTitle(Component.translatable("text.goml.gui.augment_list.title"));
        this.updateDisplay();
        this.open();
    }

    @Override
    protected void updateDisplay() {
        this.cachedEntries.clear();
        var rest = new ArrayList<Map.Entry<BlockPos, Augment>>();

        for (var entry : this.blockEntity.getAugments().entrySet()) {
            (entry.getValue().hasSettings() ? this.cachedEntries : rest).add(entry);
        }
        
        this.cachedEntries.addAll(rest);
        super.updateDisplay();
    }

    @Override
    protected int getPageAmount() {
        return this.blockEntity.getAugments().size() / PAGE_SIZE;
    }

    @Override
    protected DisplayElement getElement(int id) {
        if (this.cachedEntries.size() > id) {
            var entry = this.cachedEntries.get(id);
            var builder = new GuiElementBuilder();
            var item = entry.getValue() instanceof Block block ? block.asItem() : null;
            builder.hideDefaultTooltip();
            builder.addLoreLine(Component.translatable("text.goml.position",
                    Component.literal(entry.getKey().toShortString()).withStyle(ChatFormatting.WHITE)
            ).withStyle(ChatFormatting.BLUE));

            builder.setName(entry.getValue().getAugmentName());

            if (item != null) {
                builder.setItem(item);

                if (item instanceof TooltippedBlockItem tooltipped) {
                    builder.addLoreLine(Component.empty());

                    tooltipped.addLines(builder::addLoreLine);
                }
            }

            if (this.canModify && entry.getValue().hasSettings()) {
                builder.addLoreLine(Component.empty());
                builder.addLoreLine(Component.translatable("text.goml.gui.click_to_modify").withStyle(ChatFormatting.RED));
                builder.setCallback(() -> {
                    playClickSound(this.player);
                    entry.getValue().openSettings(this.claim, this.player, () -> {
                        new ClaimAugmentGui(this.player, this.claim, this.canModify, this.closeCallback);
                    });
                });
            }

            return DisplayElement.of(builder);
        }
        return DisplayElement.empty();
    }
}
