package draylar.goml.ui;

import com.mojang.authlib.GameProfile;
import draylar.goml.registry.GOMLTextures;
import eu.pb4.sgui.api.SguiUtils;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.AnvilInputGui;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.item.Items;

import static draylar.goml.ui.PagedGui.playClickSound;

public class NamePlayerSelectorGui extends AnvilInputGui {
    private final Runnable regularClose;
    private final Consumer<NameAndId> playerConsumer;
    private final Predicate<NameAndId> shouldDisplay;
    private boolean ignore = false;
    private NameAndId selectedPlayer;
    private int tick;
    private String currentName;
    private int timer;

    public NamePlayerSelectorGui(ServerPlayer player, Predicate<NameAndId> shouldDisplay, Runnable regularClose, Consumer<NameAndId> playerConsumer) {
        super(player, false);
        this.regularClose = regularClose;
        this.playerConsumer = playerConsumer;
        this.shouldDisplay = shouldDisplay;
        this.updateIconInvalid();
        this.setTitle(Component.translatable("text.goml.gui.player_selector.input.title"));
        this.setSlot(2, new GuiElementBuilder(Items.STRUCTURE_VOID)
                .setName(Component.translatable("text.goml.gui.back").withStyle(ChatFormatting.RED))
                .hideDefaultTooltip()
                .setCallback(() -> {
                    playClickSound(this.player);
                    this.close(true);
                }));
        this.open();
    }

    @Override
    public void onManualClose() {
        if (!ignore) {
            this.regularClose.run();
        }
    }

    @Override
    public void onTick() {
        this.tick++;

        if (this.timer-- == 0 && this.currentName != null) {
            this.updateIcon();
            CompletableFuture.supplyAsync(() -> this.player.level().getServer().services().nameToIdCache().get(this.currentName)).thenAccept((profile) -> {
                this.player.level().getServer().execute(() -> {
                    if (profile.isPresent()) {
                        this.selectedPlayer = profile.get();
                        if (this.shouldDisplay.test(profile.get())) {
                            this.updateIcon();
                        } else {
                            this.updateIconInvalid();
                        }
                    } else {
                        this.updateIconInvalid();
                    }
                    this.updateBack();
                });
            });
        }
        this.updateBack();
    }

    @Override
    public void onInput(String input) {
        super.onInput(input);
        if (input.equals(this.currentName)) {
            return;
        }
        this.selectedPlayer = null;
        if (input.length() < 3 || input.length() > 16) {
            this.updateIconInvalid();
        } else {
            this.timer = 20;
            this.currentName = input;
            this.updateIcon();
        }
        this.updateBack();
    }

    private void updateIconInvalid() {
        this.setSlot(1, new GuiElementBuilder(Items.PLAYER_HEAD)
                .setName(Component.translatable("text.goml.gui.player_selector.input.invalid").withStyle(ChatFormatting.RED))
                        .hideDefaultTooltip()
                .setProfileSkinTexture(GOMLTextures.GUI_QUESTION_MARK)
                .setCallback(() -> this.updateBack())
                .hideDefaultTooltip());
    }

    private void updateIcon() {
        var b = new GuiElementBuilder(Items.PLAYER_HEAD)
                .setName(Component.translatable("text.goml.gui.player_selector.input.select_player", this.selectedPlayer != null ? this.selectedPlayer.name() : this.currentName).withStyle(ChatFormatting.WHITE));

        if (this.selectedPlayer != null) {
            b.setProfile(this.selectedPlayer.id());
        } else {
            b.setProfileSkinTexture(GOMLTextures.GUI_QUESTION_MARK);
        }

        this.setSlot(1, b
                .hideDefaultTooltip()
                .setCallback(() -> {
                    if (this.selectedPlayer != null) {
                        playClickSound(this.player);
                        this.ignore = true;
                        this.close(true);
                        this.playerConsumer.accept(this.selectedPlayer);
                    } else {
                        this.updateBack();
                    }
                }));
    }

    private void updateBack() {
        if (this.wrappedMenu != null) {
            SguiUtils.sendSlotUpdate(this.player, this.wrappedMenu.containerId, 2, this.getGuiElement(2).getItemStackForDisplay(this));
        }
    }
}
