package draylar.goml.block.augment;

import draylar.goml.GetOffMyLawn;
import draylar.goml.api.Claim;
import draylar.goml.api.DataKey;
import draylar.goml.block.ClaimAugmentBlock;
import draylar.goml.ui.PagedGui;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public class GreeterAugmentBlock extends ClaimAugmentBlock {

    public static final DataKey<String> MESSAGE_KEY = DataKey.ofString(GetOffMyLawn.id("greeter/message"), "Welcome %player on my claim!");

    public GreeterAugmentBlock(Properties settings, String texture) {
        super(settings, texture);
    }

    @Override
    public void onPlayerEnter(Claim claim, Player player) {
        var text = claim.getData(MESSAGE_KEY);

        if (text != null && !text.isBlank()) {
            player.sendSystemMessage(GetOffMyLawn.CONFIG.messagePrefix.mutableText().append(Component.literal(" " + (text
                    .replace("%player", player.getName().getString())
                    .replace("%p", player.getName().getString()))
            ).withStyle(ChatFormatting.GRAY)));
        }
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    public void openSettings(Claim claim, ServerPlayer player, @Nullable Runnable closeCallback) {
        var currentInput = claim.getData(MESSAGE_KEY);

        var ui = new AnvilInputGui(player, false) {
            @Override
            public void onManualClose() {
                if (closeCallback != null) {
                    closeCallback.run();
                }
            }
        };
        ui.setTitle(Component.translatable("text.goml.gui.input_greeting.title"));
        ui.setDefaultInputValue(currentInput);

        ui.setSlot(1,
                new GuiElementBuilder(Items.SLIME_BALL)
                        .setName(Component.translatable("text.goml.gui.input_greeting.set").withStyle(ChatFormatting.GREEN))
                        .setCallback(() -> {
                            PagedGui.playClickSound(player);
                            claim.setData(MESSAGE_KEY, ui.getInput());
                            player.sendSystemMessage(Component.translatable("text.goml.changed_greeting", Component.literal(ui.getInput()).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GREEN), false);
                        })
        );

        ui.setSlot(2,
                new GuiElementBuilder(Items.STRUCTURE_VOID)
                        .setName(Component.translatable("text.goml.gui.input_greeting.close").withStyle(ChatFormatting.RED))
                        .setCallback(() -> {
                            PagedGui.playClickSound(player);
                            ui.close(closeCallback != null);
                        })
        );

        ui.open();
    }
}
