package draylar.goml.ui;

import draylar.goml.GetOffMyLawn;
import draylar.goml.api.Claim;
import draylar.goml.api.ClaimBox;
import draylar.goml.api.event.ClaimEvents;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Items;

public class AdminAugmentGui extends SimpleGui {
    private final Claim claim;
    private final Runnable onClose;
    private int claimHeight;
    private int claimRadius;
    private ClaimBox claimBox;


    public AdminAugmentGui(Claim claim, ServerPlayer player, @Nullable Runnable onClose) {
        super(MenuType.HOPPER, player, false);
        this.setTitle(Component.translatable("text.goml.gui.admin_settings.title"));
        this.claim = claim;
        this.onClose = onClose;
        this.claimBox = claim.getClaimBox();
        this.claimHeight = this.claimBox.radiusY();
        this.claimRadius = this.claimBox.radius();


        this.addSlot(new GuiElementBuilder(Items.STONE_SLAB)
                .setName(Component.translatable("text.goml.radius", this.claimRadius))
                .setCallback((i, a, c, g) -> {
                    PagedGui.playClickSound(this.player);
                    if (a.isLeft) {
                        this.claimRadius = Math.max(this.claimRadius - (a.shift ? 10 : 1), 1);
                    } else if (a.isRight) {
                        this.claimRadius += a.shift ? 10 : 1;
                    }
                    g.getGuiElement(i).getItemStack().set(DataComponents.CUSTOM_NAME, Component.translatable("text.goml.radius", this.claimRadius).setStyle(Style.EMPTY.withItalic(false)));
                })
        );
        this.addSlot(new GuiElementBuilder(Items.ANDESITE_WALL)
                .setName(Component.translatable("text.goml.height", this.claimHeight))
                .setCallback((i, a, c, g) -> {
                    PagedGui.playClickSound(this.player);
                    if (a.isLeft) {
                        this.claimHeight = Math.max(this.claimHeight - (a.shift ? 10 : 1), 1);
                    } else if (a.isRight) {
                        this.claimHeight += a.shift ? 10 : 1;
                    }
                    g.getGuiElement(i).getItemStack().set(DataComponents.CUSTOM_NAME, Component.translatable("text.goml.height", this.claimHeight).setStyle(Style.EMPTY.withItalic(false)));

                })
        );

        this.addSlot(new GuiElementBuilder(Items.SLIME_BALL)
                .setName(Component.translatable("text.goml.apply"))
                .setCallback((i, a, c, g) -> {
                    PagedGui.playClickSound(this.player);
                    GetOffMyLawn.CLAIM.get(claim.getWorldInstance(player.level().getServer())).remove(this.claim);
                    var oldSize = claim.getClaimBox();
                    this.claimBox = new ClaimBox(this.claimBox.getOrigin(), this.claimRadius, this.claimHeight, this.claimBox.noShift());
                    claim.internal_setClaimBox(this.claimBox);
                    GetOffMyLawn.CLAIM.get(claim.getWorldInstance(player.level().getServer())).add(this.claim);
                    claim.internal_updateChunkCount(player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, this.claim.getWorld())));
                    ClaimEvents.CLAIM_RESIZED.invoker().onResizeEvent(claim, oldSize, this.claimBox);
                })
        );

        this.open();
    }

    @Override
    public void onManualClose() {
        this.onClose.run();
    }
}
