package draylar.goml.block;

import draylar.goml.GetOffMyLawn;
import draylar.goml.api.Claim;
import draylar.goml.api.DataKey;
import draylar.goml.other.StatusEnum;
import draylar.goml.ui.PagedGui;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.block.AbstractBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class SelectiveClaimAugmentBlock extends ClaimAugmentBlock {

    public final DataKey<StatusEnum.TargetPlayer> key;
    private static final Map<UUID, HashSet<BlockPos>> playerAugmentSources = new HashMap<>();

    public SelectiveClaimAugmentBlock(String key, AbstractBlock.Settings settings, String texture) {
        super(settings, texture);
        this.key = DataKey.ofEnum(GetOffMyLawn.id(key + "/mode"), StatusEnum.TargetPlayer.class, StatusEnum.TargetPlayer.EVERYONE);
    }

    @Override
    public void onPlayerEnter(Claim claim, PlayerEntity player) {
        UUID playerId = player.getUuid();
        HashSet<BlockPos> currentRefs = playerAugmentSources.getOrDefault(playerId, new HashSet<BlockPos>());
        if (this.canApply(claim, player) && currentRefs.isEmpty()) {
            this.applyEffect(player);
        }
        currentRefs.add(claim.getOrigin());
        playerAugmentSources.put(playerId, currentRefs);
    }

    protected boolean canApply(Claim claim, PlayerEntity player) {
        var mode = claim.getData(key);
        return mode == StatusEnum.TargetPlayer.EVERYONE
                || (mode == StatusEnum.TargetPlayer.TRUSTED && claim.hasPermission(player))
                || (mode == StatusEnum.TargetPlayer.UNTRUSTED && !claim.hasPermission(player));
    }

    @Override
    public void onPlayerExit(Claim claim, PlayerEntity player) {
        UUID playerId = player.getUuid();
        HashSet<BlockPos> currentRefs = playerAugmentSources.getOrDefault(playerId, new HashSet<BlockPos>());

        currentRefs.remove(claim.getOrigin());
        if (currentRefs.isEmpty()) {
            playerAugmentSources.remove(playerId);
            this.removeEffect(player);
        }
    }

    public void applyEffect(PlayerEntity player) {};

    public void removeEffect(PlayerEntity player) {};

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public void openSettings(Claim claim, ServerPlayerEntity player, @Nullable Runnable closeCallback) {

        var gui = new SimpleGui(ScreenHandlerType.HOPPER, player, false) {
            @Override
            public void onClose() {
                if (closeCallback != null) {
                    closeCallback.run();
                }
            }
        };

        gui.setTitle(this.getGuiName());

        var change = new MutableObject<Runnable>();
        change.setValue(() -> {
            var currentMode = claim.getData(key);
            gui.setSlot(0, new GuiElementBuilder(currentMode.getIcon())
                    .setName(Text.translatable("text.goml.mode_toggle", currentMode.getName()))
                    .addLoreLine(Text.translatable("text.goml.mode_toggle.help").formatted(Formatting.GRAY))
                    .setCallback((x, y, z) -> {
                        PagedGui.playClickSound(player);
                        var mode = currentMode.getNext();
                        claim.setData(key, mode);
                        for (var p : claim.getPlayersIn(player.server)) {
                            this.removeEffect(player);

                            if (this.canApply(claim, p)) {
                                this.applyEffect(player);
                            }
                        }
                        change.getValue().run();
                    })
            );
        });

        change.getValue().run();

        gui.setSlot(4, new GuiElementBuilder(Items.STRUCTURE_VOID)
                .setName(Text.translatable(closeCallback != null ? "text.goml.gui.back" : "text.goml.gui.close").formatted(Formatting.RED))
                .setCallback((x, y, z) -> {
                    PagedGui.playClickSound(player);
                    gui.close();
                })
        );

        while (gui.getFirstEmptySlot() != -1) {
            gui.addSlot(PagedGui.DisplayElement.filler().element());
        }

        gui.open();
    }
}
