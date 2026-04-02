package draylar.goml.block;

import draylar.goml.GetOffMyLawn;
import draylar.goml.api.Claim;
import draylar.goml.api.DataKey;
import draylar.goml.other.StatusEnum;
import draylar.goml.ui.PagedGui;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

public class SelectiveClaimAugmentBlock extends ClaimAugmentBlock {

    public final DataKey<StatusEnum.TargetPlayer> key;

    public SelectiveClaimAugmentBlock(String key, BlockBehaviour.Properties settings, String texture) {
        super(settings, texture);
        this.key = DataKey.ofEnum(GetOffMyLawn.id(key + "/mode"), StatusEnum.TargetPlayer.class, StatusEnum.TargetPlayer.EVERYONE);
    }

    @Override
    public void onPlayerEnter(Claim claim, Player player) {
        if (this.canApply(claim, player)) {
            this.applyEffect(player);
        }
    }

    protected boolean canApply(Claim claim, Player player) {
        var mode = claim.getData(key);
        return mode == StatusEnum.TargetPlayer.EVERYONE
                || (mode == StatusEnum.TargetPlayer.TRUSTED && claim.hasPermission(player))
                || (mode == StatusEnum.TargetPlayer.UNTRUSTED && !claim.hasPermission(player));
    }

    @Override
    public void onPlayerExit(Claim claim, Player player) {
        this.removeEffect(player);
    }

    public void applyEffect(Player player) {};

    public void removeEffect(Player player) {};

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public void openSettings(Claim claim, ServerPlayer player, @Nullable Runnable closeCallback) {

        var gui = new SimpleGui(MenuType.HOPPER, player, false) {
            @Override
            public void onManualClose() {
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
                    .setName(Component.translatable("text.goml.mode_toggle", currentMode.getName()))
                    .addLoreLine(Component.translatable("text.goml.mode_toggle.help").withStyle(ChatFormatting.GRAY))
                    .setCallback(() -> {
                        PagedGui.playClickSound(player);
                        var mode = currentMode.getNext();
                        claim.setData(key, mode);
                        for (var p : claim.getPlayersIn(player.level().getServer())) {
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
                .setName(Component.translatable(closeCallback != null ? "text.goml.gui.back" : "text.goml.gui.close").withStyle(ChatFormatting.RED))
                .setCallback(() -> {
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
