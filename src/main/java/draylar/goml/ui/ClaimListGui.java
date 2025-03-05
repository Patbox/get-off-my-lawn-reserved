package draylar.goml.ui;

import com.jamieswhiteshirt.rtree3i.Entry;
import com.mojang.authlib.GameProfile;
import draylar.goml.api.Claim;
import draylar.goml.api.ClaimBox;
import draylar.goml.api.ClaimUtils;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ApiStatus.Internal
public class ClaimListGui extends PagedGui {

    private final List<Entry<ClaimBox, Claim>> claimList = new ArrayList<>();

    protected ClaimListGui(ServerPlayerEntity player, GameProfile target) {
        super(player, null);

        ClaimUtils.getClaimsWithAccess(player.getWorld(), target.getId()).forEach(this.claimList::add);
        this.setTitle(Text.translatable(
                player.getGameProfile().getId().equals(target.getId()) ? "text.goml.your_claims" : "text.goml.someones_claims",
                target.getName()
        ));

        this.updateDisplay();
    }

    public static void open(ServerPlayerEntity player, GameProfile target) {
        new ClaimListGui(player, target).open();
    }

    @Override
    protected int getPageAmount() {
        return this.claimList.size() / PAGE_SIZE + 1;
    }

    @Override
    protected DisplayElement getElement(int id) {
        if (this.claimList.size() > id) {
            var server = this.player.getServer();
            var entry = this.claimList.get(id);
            var claim = entry.getValue();

            var icon = GuiElementBuilder.from(claim.getIcon());
            icon.setName(Text.literal(claim.getOrigin().toShortString()).append(Text.literal(" (" + claim.getWorld().toString() + ")").formatted(Formatting.GRAY)));
            var lore = ClaimUtils.getClaimText(server, entry.getValue());
            lore.removeFirst();
            icon.setLore(lore);

            icon.setCallback((x, y, z) -> {
                if (Permissions.check(this.player, "goml.teleport", 3)) {
                    var world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, claim.getWorld()));
                    if (world != null) {
                        this.player.teleport(world, claim.getOrigin().getX(), claim.getOrigin().getY() + 1, claim.getOrigin().getZ(), Set.of(), this.player.getYaw(), this.player.getPitch(), false);
                    }
                }
            });

            return DisplayElement.of(icon);
        }

        return DisplayElement.empty();
    }


}
