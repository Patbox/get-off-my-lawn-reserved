package draylar.goml.ui;

import com.jamieswhiteshirt.rtree3i.Entry;
import com.mojang.authlib.GameProfile;
import draylar.goml.api.Claim;
import draylar.goml.api.ClaimBox;
import draylar.goml.api.ClaimUtils;
import draylar.goml.other.FabricPermissionBridge;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static draylar.goml.GetOffMyLawn.id;

@ApiStatus.Internal
public class ClaimListGui extends PagedGui {

    private final List<Entry<ClaimBox, Claim>> claimList = new ArrayList<>();

    protected ClaimListGui(ServerPlayer player, GameProfile target) {
        super(player, null);

        ClaimUtils.getClaimsWithAccess(player.level(), target.id()).forEach(this.claimList::add);
        this.setTitle(Component.translatable(
                player.getGameProfile().id().equals(target.id()) ? "text.goml.your_claims" : "text.goml.someones_claims",
                target.name()
        ));

        this.updateDisplay();
    }

    public static void open(ServerPlayer player, GameProfile target) {
        new ClaimListGui(player, target).open();
    }

    @Override
    protected int getPageAmount() {
        return this.claimList.size() / PAGE_SIZE + 1;
    }

    @Override
    protected DisplayElement getElement(int id) {
        if (this.claimList.size() > id) {
            var server = this.player.level().getServer();
            var entry = this.claimList.get(id);
            var claim = entry.getValue();

            var icon = GuiElementBuilder.from(claim.getIcon());
            icon.setName(Component.literal(claim.getOrigin().toShortString()).append(Component.literal(" (" + claim.getWorld().toString() + ")").withStyle(ChatFormatting.GRAY)));
            var lore = ClaimUtils.getClaimText(server, entry.getValue());
            lore.removeFirst();
            icon.setLore(lore);

            icon.setCallback(() -> {
                if (FabricPermissionBridge.checkPermission(this.player, id("teleport"), PermissionLevel.ADMINS)) {
                    var world = server.getLevel(ResourceKey.create(Registries.DIMENSION, claim.getWorld()));
                    if (world != null) {
                        this.player.teleportTo(world, claim.getOrigin().getX(), claim.getOrigin().getY() + 1, claim.getOrigin().getZ(), Set.of(), this.player.getYRot(), this.player.getXRot(), false);
                    }
                }
            });

            return DisplayElement.of(icon);
        }

        return DisplayElement.empty();
    }


}
