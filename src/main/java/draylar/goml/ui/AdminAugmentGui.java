package draylar.goml.ui;

import draylar.goml.GetOffMyLawn;
import draylar.goml.api.Claim;
import draylar.goml.api.ClaimBox;
import draylar.goml.api.event.ClaimEvents;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;

public class AdminAugmentGui extends SimpleGui {
    private final Claim claim;
    private final Runnable onClose;
    private int claimHeight;
    private int claimRadius;
    private ClaimBox claimBox;


    public AdminAugmentGui(Claim claim, ServerPlayerEntity player, @Nullable Runnable onClose) {
        super(ScreenHandlerType.HOPPER, player, false);
        this.setTitle(Text.translatable("text.goml.gui.admin_settings.title"));
        this.claim = claim;
        this.onClose = onClose;
        this.claimBox = claim.getClaimBox();
        this.claimHeight = this.claimBox.radiusY();
        this.claimRadius = this.claimBox.radius();


        this.addSlot(new GuiElementBuilder(Items.STONE_SLAB)
                .setName(Text.translatable("text.goml.radius", this.claimRadius))
                .setCallback((i, a, c, g) -> {
                    PagedGui.playClickSound(this.player);
                    if (a.isLeft) {
                        this.claimRadius = Math.max(this.claimRadius - (a.shift ? 10 : 1), 1);
                    } else if (a.isRight) {
                        this.claimRadius += a.shift ? 10 : 1;
                    }
                    g.getSlot(i).getItemStack().set(DataComponentTypes.CUSTOM_NAME, Text.translatable("text.goml.radius", this.claimRadius).setStyle(Style.EMPTY.withItalic(false)));
                })
        );
        this.addSlot(new GuiElementBuilder(Items.ANDESITE_WALL)
                .setName(Text.translatable("text.goml.height", this.claimHeight))
                .setCallback((i, a, c, g) -> {
                    PagedGui.playClickSound(this.player);
                    if (a.isLeft) {
                        this.claimHeight = Math.max(this.claimHeight - (a.shift ? 10 : 1), 1);
                    } else if (a.isRight) {
                        this.claimHeight += a.shift ? 10 : 1;
                    }
                    g.getSlot(i).getItemStack().set(DataComponentTypes.CUSTOM_NAME, Text.translatable("text.goml.height", this.claimHeight).setStyle(Style.EMPTY.withItalic(false)));

                })
        );

        this.addSlot(new GuiElementBuilder(Items.SLIME_BALL)
                .setName(Text.translatable("text.goml.apply"))
                .setCallback((i, a, c, g) -> {
                    PagedGui.playClickSound(this.player);
                    GetOffMyLawn.CLAIM.get(claim.getWorldInstance(player.server)).remove(this.claim);
                    var oldSize = claim.getClaimBox();
                    this.claimBox = new ClaimBox(this.claimBox.getOrigin(), this.claimRadius, this.claimHeight, this.claimBox.noShift());
                    claim.internal_setClaimBox(this.claimBox);
                    GetOffMyLawn.CLAIM.get(claim.getWorldInstance(player.server)).add(this.claim);
                    claim.internal_updateChunkCount(player.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, this.claim.getWorld())));
                    ClaimEvents.CLAIM_RESIZED.invoker().onResizeEvent(claim, oldSize, this.claimBox);
                })
        );

        this.open();
    }

    @Override
    public void onClose() {
        this.onClose.run();
    }
}
