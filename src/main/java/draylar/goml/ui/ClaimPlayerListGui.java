package draylar.goml.ui;

import draylar.goml.api.Claim;
import draylar.goml.registry.GOMLTextures;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import fr.catcore.server.translations.api.LocalizationTarget;
import fr.catcore.server.translations.api.ServerTranslations;
import fr.catcore.server.translations.api.text.LocalizableText;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@ApiStatus.Internal
public class ClaimPlayerListGui extends PagedGui {
    private final Claim claim;
    private final boolean canModifyTrusted;
    private final boolean canModifyOwners;
    private final boolean isAdmin;

    private final List<UUID> cachedOwners = new ArrayList<>();
    private final List<UUID> cachedTrusted = new ArrayList<>();
    @Nullable

    public ClaimPlayerListGui(ServerPlayerEntity player, Claim claim, boolean canModifyTrusted, boolean canModifyOwners, boolean isAdmin, @Nullable Runnable onClose) {
        super(player, onClose);
        this.claim = claim;
        this.canModifyOwners = canModifyOwners;
        this.canModifyTrusted = canModifyTrusted;
        this.isAdmin = isAdmin;
        this.setTitle(Text.translatable("text.goml.gui.player_list.title"));
        this.updateDisplay();
        this.open();
    }

    public static void open(ServerPlayerEntity player, Claim claim, boolean admin, @Nullable Runnable onClose) {
        new ClaimPlayerListGui(player, claim, claim.isOwner(player) || admin, admin, admin, onClose);
    }

    @Override
    protected int getPageAmount() {
        return (this.cachedOwners.size() + this.cachedTrusted.size()) / PAGE_SIZE;
    }

    @Override
    protected void updateDisplay() {
        this.cachedOwners.clear();
        this.cachedTrusted.clear();
        this.cachedOwners.addAll(this.claim.getOwners());
        this.cachedTrusted.addAll(this.claim.getTrusted());
        super.updateDisplay();
    }

    @Override
    protected DisplayElement getElement(int id) {
        var ownerSize = this.cachedOwners.size();
        var trustSize = this.cachedTrusted.size();
        if (ownerSize > id) {
            return getPlayerElement(this.cachedOwners.get(id), true);
        } else if (trustSize > id - ownerSize) {
            return getPlayerElement(this.cachedTrusted.get(id - ownerSize), false);

        }

        return DisplayElement.empty();
    }

    @Override
    protected DisplayElement getNavElement(int id) {
        return switch (id) {
            case 5 -> this.canModifyTrusted
                    ? DisplayElement.of(new GuiElementBuilder(Items.PLAYER_HEAD)
                    .setName(Text.translatable("text.goml.gui.player_list.add_player").formatted(Formatting.GREEN))
                    .setSkullOwner(GOMLTextures.GUI_ADD)
                    .setCallback((x, y, z) -> {
                        playClickSound(this.player);

                        new GenericPlayerSelectionGui(
                                this.player,
                                Text.translatable("text.goml.gui.player_add_gui.title"),
                                (p) -> !this.claim.hasPermission(p),
                                this.claim::trust,
                                () -> open(player, this.claim, this.isAdmin, this.closeCallback));
                    }))
                    : DisplayElement.filler();
            default -> super.getNavElement(id);
        };
    }

    private DisplayElement getPlayerElement(UUID uuid, boolean owner) {
        var optional = this.player.server.getUserCache().getByUuid(uuid);
        var exist = optional.isPresent();
        var gameProfile = exist ? optional.get() : null;

        var canRemove = owner ? this.canModifyOwners : this.canModifyTrusted;

        var builder = new GuiElementBuilder(exist ? Items.PLAYER_HEAD : Items.SKELETON_SKULL)
                .setName(Text.literal(exist ? gameProfile.getName() : uuid.toString())
                        .formatted(owner ? Formatting.GOLD : Formatting.WHITE)
                        .append(owner
                                        ? Text.literal(" (").formatted(Formatting.DARK_GRAY)
                                                .append(((MutableText) LocalizableText.asLocalizedFor(Text.translatable("text.goml.owner"), (LocalizationTarget) this.player)).formatted(Formatting.WHITE))
                                                .append(Text.literal(")").formatted(Formatting.DARK_GRAY))

                                        : Text.empty()
                                )
                );

        if (canRemove) {
            builder.addLoreLine(Text.translatable("text.goml.gui.click_to_remove"));
            builder.setCallback((x, y, z) -> {
                playClickSound(player);
                (owner ? this.claim.getOwners() : this.claim.getTrusted()).remove(uuid);
                this.updateDisplay();
            });
        }

        if (exist) {
            builder.setSkullOwner(gameProfile, null);
        }


        return DisplayElement.of(
                builder
        );
    }
}
