package draylar.goml.block.augment;

import draylar.goml.GetOffMyLawn;
import draylar.goml.api.Claim;
import draylar.goml.api.DataKey;
import draylar.goml.block.ClaimAugmentBlock;
import draylar.goml.other.StatusEnum;
import draylar.goml.ui.GenericPlayerListGui;
import draylar.goml.ui.PagedGui;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

public class ExplosionControllerAugmentBlock extends ClaimAugmentBlock {
    public static final DataKey<StatusEnum.Toggle> KEY = DataKey.ofEnum(GetOffMyLawn.id("explosion_control"), StatusEnum.Toggle.class, StatusEnum.Toggle.ENABLED);


    public ExplosionControllerAugmentBlock(Properties settings, String texture) {
        super(settings, texture);
    }

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
        {
            var change = new MutableObject<Runnable>();
            change.setValue(() -> {
                var currentMode = claim.getData(KEY);
                gui.setSlot(0, new GuiElementBuilder(currentMode.getIcon())
                        .setName(Component.translatable("text.goml.explosion_control_toggle", currentMode.getName()))
                        .addLoreLine(Component.translatable("text.goml.mode_toggle.help").withStyle(ChatFormatting.GRAY))
                        .setCallback(() -> {
                            PagedGui.playClickSound(player);
                            var mode = currentMode.getNext();
                            claim.setData(KEY, mode);
                            change.getValue().run();
                        })
                );
            });

            change.getValue().run();
        }

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
