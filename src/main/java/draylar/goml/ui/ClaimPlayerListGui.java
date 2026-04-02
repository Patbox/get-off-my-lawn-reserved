package draylar.goml.ui;

import com.mojang.authlib.GameProfile;
import draylar.goml.api.Claim;
import draylar.goml.api.group.PlayerGroup;
import draylar.goml.registry.GOMLTextures;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.item.Items;

@ApiStatus.Internal
public class ClaimPlayerListGui extends GenericPlayerListGui {
    private final Claim claim;
    private final boolean isAdmin;

    private final boolean canModifyOwners;
    private final boolean canModifyTrusted;
    private final List<PlayerGroup> groups = new ArrayList<>();

    public ClaimPlayerListGui(ServerPlayer player, Claim claim, boolean canModifyTrusted, boolean canModifyOwners, boolean isAdmin, @Nullable Runnable onClose) {
        super(player, onClose);
        this.claim = claim;
        this.setTitle(Component.translatable("text.goml.gui.player_list.title"));
        this.canModifyOwners = canModifyOwners;
        this.canModifyTrusted = canModifyTrusted;
        this.isAdmin = isAdmin;
        this.updateDisplay();
        this.open();
    }

    public static void open(ServerPlayer player, Claim claim, boolean admin, @Nullable Runnable onClose) {
        new ClaimPlayerListGui(player, claim, claim.isOwner(player) || admin, admin, admin, onClose);
    }

    @Override
    protected DisplayElement getElement(int id) {
        return id < this.groups.size() ? this.getGroupElement(id) : super.getElement(id - this.groups.size());
    }

    private DisplayElement getGroupElement(int id) {
        var group = this.groups.get(id);

        var builder = GuiElementBuilder.from(group.icon())

                .setName(Component.empty().append(group.selfDisplayName()).append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.empty().append(group.provider().getName()).setStyle(Style.EMPTY.withColor(0x45abff))
                        .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY)))
                        )
                ).hideDefaultTooltip();


        if (this.canModifyTrusted) {
            builder.addLoreLine(Component.translatable("text.goml.gui.click_to_remove"));
            builder.setCallback(() -> {
                playClickSound(player);
                this.claim.untrust(group);
                this.updateDisplay();
            });
        }

        return DisplayElement.of(builder);
    }

    @Override
    protected void updateDisplay() {
        this.groups.clear();
        this.groups.addAll(this.claim.getGroups());
        this.groups.sort(Comparator.comparing(x -> x.getKey().toString()));
        this.uuids.clear();
        this.uuids.addAll(this.claim.getOwners());
        this.uuids.addAll(this.claim.getTrusted());
        super.updateDisplay();
    }

    @Override
    protected DisplayElement getNavElement(int id) {
        return switch (id) {
            case 5 -> this.canModifyTrusted
                    ? DisplayElement.of(new GuiElementBuilder(Items.PLAYER_HEAD)
                    .setName(Component.translatable("text.goml.gui.player_list.add_player").withStyle(ChatFormatting.GREEN))
                    .setProfileSkinTexture(GOMLTextures.GUI_ADD)
                    .setCallback(() -> {
                        playClickSound(this.player);
                        this.ignoreCloseCallback = true;
                        this.close(true);
                        this.ignoreCloseCallback = false;
                        new GenericPlayerAndGroupSelectionGui(
                                this.player,
                                Component.translatable("text.goml.gui.player_add_gui.title"),
                                (p) -> !this.claim.hasDirectPermission(p.id()),
                                (p) -> !this.claim.getGroups().contains(p),
                                (p) -> this.claim.trust(p.id()),
                                this.claim::trust,
                                this::refreshOpen).updateAndOpen();
                    }))
                    : DisplayElement.filler();
            default -> super.getNavElement(id);
        };
    }

    @Override
    protected void modifyBuilder(GuiElementBuilder builder, Optional<NameAndId> optional, UUID uuid) {
        var exist = optional.isPresent();
        var gameProfile = exist ? optional.get() : null;
        var isOwner = this.claim.isOwner(uuid);
        var canRemove = isOwner ? this.canModifyOwners : this.canModifyTrusted;

        builder.setName(Component.literal(exist ? gameProfile.name() : uuid.toString())
                .withStyle(isOwner ? ChatFormatting.GOLD : ChatFormatting.WHITE).append(isOwner
                        ? Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.translatable("text.goml.owner").withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY))
                        : Component.empty()
                ));

        if (canRemove) {
            builder.addLoreLine(Component.translatable("text.goml.gui.click_to_remove"));
            builder.setCallback(() -> {
                playClickSound(player);
                (isOwner ? this.claim.getOwners() : this.claim.getTrusted()).remove(uuid);
                this.updateDisplay();
            });
        }
    }
}
