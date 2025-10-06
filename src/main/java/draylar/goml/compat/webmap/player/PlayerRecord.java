package draylar.goml.compat.webmap.player;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import draylar.goml.GetOffMyLawn;
import eu.pb4.polymer.core.api.block.PolymerHeadBlock;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a player record with their name and head icon for web map display.
 * 
 * <p>If the player or block cannot be resolved, placeholders are used.
 * 
 * @see PlayerHeadIcon
 */
public class PlayerRecord {
    private static final String PROFILE_LOOKUP_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final Integer REQUEST_TIMEOUT = 3000;

    private UUID uuid = null;
    private PlayerHeadIcon playerIcon = null;
    private String name = null;
    private MinecraftServer server = null;

    /**
     * Explicitly defines name and icon, cannot be updated after construction.
     * 
     * @param playerIcon the player head icon
     * @param playerName the player name, or null for placeholder
     */
    public PlayerRecord(PlayerHeadIcon playerIcon, @Nullable String playerName){
        this.name = (playerName != null) ? playerName : this.name;
        this.playerIcon = playerIcon;
    }
    /**
     * Gets player name and icon from server player registry.
     * 
     * @param uuid the player's UUID
     * @param server the Minecraft server instance
     * @param name the fallback name if lookup fails, or null
     */
    public PlayerRecord(UUID uuid, MinecraftServer server, @Nullable String name) {
        this.server = server;
        this.uuid = uuid;
        this.resolvePlayer();

        // If name is still null after player resolution, assume it failed and use defaults
        if (this.name == null) {
            this.name = name;
            this.playerIcon = new PlayerHeadIcon(null);
        }

        if (this.name == null) {
            this.name = Text.translatable("text.goml.webmap.label.unknown", "Unknown?").getString();
        }
    }
    /**
     * Gets icon for {@link PolymerHeadBlock} and sets display name.
     * 
     * @param headBlock the polymer head block
     * @param displayName the display name, or null for placeholder
     */
    public PlayerRecord(PolymerHeadBlock headBlock, @Nullable String displayName) {
        final String DEFAULT_BLOCK_ICON = "iVBORw0KGgoAAAANSUhEUgAAAAgAAAAICAIAAABLbSncAAAACXBIWXMAAAsTAAALEwEAmpwYAAAAX0lEQVQImWWMyRGAIBAEG4qvq8EQhEEYhpmYAkHwMARykSMBHyBUYb9mu2ZWXSfHzoTzqOdmWxdsHjpITEUDzQYhSD9NUz/MiP1bEEDPzW9t/qqinSemElPBZmyu2XlexGAiqcN7MbsAAAAASUVORK5CYII=";
        this.name = (displayName != null) ? displayName : this.name;
        this.playerIcon = new PlayerHeadIcon(getHeadImage(headBlock.getPolymerSkinValue(null, null, null)).orElse(DEFAULT_BLOCK_ICON));
    }

    /**
     * @return the name of the player. Only updated by calling {@link #refreshPlayer(MinecraftServer)}.
     */
    public String getName() {
        return this.name;
    }
    /**
     * @return {@link PlayerHeadIcon} containing the player's head. Only updated when calling {@link #resolvePlayer()}.
     */
    public PlayerHeadIcon getHeadIcon() {
        return this.playerIcon;
    }
    
    /**
     * Resolves the player's name and icon using Minecraft API. Has no effect if not constructed with player UUID (i.e with PolymerHeadBlock).
     */
    public void resolvePlayer() {
        if(this.uuid != null) {
            try {
                // Check cache first
                String json = PlayerRecordCache.getProfile(this.uuid);

                // On cache miss
                if (json == null) {
                    json = openConnection(URI.create(PROFILE_LOOKUP_URL + this.uuid));
                    PlayerRecordCache.putProfile(this.uuid, json);
                }
                
                Gson gson = new Gson();
                Map<String, Object> profile = gson.fromJson(json, new TypeToken<Map<String, Object>>(){}.getType());

                // Extract player data and create icon
                this.name = (String) profile.get("name");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> properties = (List<Map<String, Object>>) profile.get("properties");
                if (properties != null) {
                    for (Map<String, Object> property : properties) {
                        if ("textures".equals(property.get("name"))) {
                            this.playerIcon = new PlayerHeadIcon(getHeadImage((String) property.get("value")).orElse(null));
                            break;
                        }
                    }
                }
            } catch (Exception exception) {
                GetOffMyLawn.LOGGER.warn("Unable to get data for player with UUID {}: ", this.uuid, exception);
            }

            // If name is still null, attempt to retrieve it from UserCache
            server.getApiServices().nameToIdCache().getByUuid(uuid).ifPresent(
                profile -> this.name = profile.name()
            );
        }
    }

	private static Optional<String> getHeadImage(@Nullable String skinData) {
		if (skinData == null || skinData.isBlank()) {
			return Optional.empty();
		}

		String textureUrl = PlayerHeadRenderer.getUrlFromSkinValue(skinData);
		if (textureUrl == null || textureUrl.isBlank()) {
			return Optional.empty();
		}

		return PlayerHeadRenderer.headImageFromSkinUrl(textureUrl);
	}

    private static String openConnection(URI resourceUri) throws IOException {
        URLConnection connection = resourceUri.toURL().openConnection();
        connection.setReadTimeout(REQUEST_TIMEOUT);
        connection.setConnectTimeout(REQUEST_TIMEOUT);

        try (InputStream stream = connection.getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            if (connection instanceof java.net.HttpURLConnection httpConn) {
                httpConn.disconnect();
            }
        }
    }
}