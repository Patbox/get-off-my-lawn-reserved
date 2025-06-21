package draylar.goml.block.augment;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import draylar.goml.block.SelectiveClaimAugmentBlock;
import io.github.ladysnake.pal.AbilitySource;
import io.github.ladysnake.pal.Pal;
import io.github.ladysnake.pal.VanillaAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class HeavenWingsAugmentBlock extends SelectiveClaimAugmentBlock {

    public static final AbilitySource HEAVEN_WINGS = Pal.getAbilitySource("goml", "heaven_wings");
    private static final Map<UUID, Integer> flightReferences = new HashMap<>();

    public HeavenWingsAugmentBlock(Settings settings, String texture) {
        super("heaven_wings", settings, texture);
    }

    @Override
    public void applyEffect(PlayerEntity player) {
        UUID playerId = player.getUuid();
        int currentRefs = flightReferences.getOrDefault(playerId, 0);
        flightReferences.put(playerId, currentRefs + 1);

        if (currentRefs == 0) {
            HEAVEN_WINGS.grantTo(player, VanillaAbilities.ALLOW_FLYING);
        }
    }

    @Override
    public void removeEffect(PlayerEntity player) {
        UUID playerId = player.getUuid();
        int currentRefs = flightReferences.getOrDefault(playerId, 0);

        if (currentRefs > 1) {
            flightReferences.put(playerId, currentRefs - 1);
        } else {
            flightReferences.remove(playerId);
            HEAVEN_WINGS.revokeFrom(player, VanillaAbilities.ALLOW_FLYING);
        }
    }


    public static void debugFlightReferences(PlayerEntity player) {
        if (flightReferences.isEmpty()) {
            player.sendMessage(Text.literal("§aFlight references map is empty"), false);
        } else {
            player.sendMessage(Text.literal("§eFlight references (" + flightReferences.size() + " entries):"), false);
            for (Map.Entry<UUID, Integer> entry : flightReferences.entrySet()) {
                String playerName = "Unknown";
                // Try to get player name if they're online
                if (player.getWorld().getServer() != null) {
                    var serverPlayer = player.getWorld().getServer().getPlayerManager().getPlayer(entry.getKey());
                    if (serverPlayer != null) {
                        playerName = serverPlayer.getName().getString();
                    }
                }
                player.sendMessage(Text.literal("  §b" + playerName + " §7(" + entry.getKey() + ")§f: §e" + entry.getValue()), false);
            }
        }
    }
}
