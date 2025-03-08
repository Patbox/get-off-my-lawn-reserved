package draylar.goml.config;


import com.google.gson.*;
import com.jamieswhiteshirt.rtree3i.Box;
import draylar.goml.GetOffMyLawn;
import draylar.goml.other.WrappedText;
import draylar.goml.registry.GOMLBlocks;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registry;
import net.minecraft.world.World;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GOMLConfig {
    public int makeshiftRadius = 2;
    public int primitiveRadius = 4;
    public int reinforcedRadius = 8;
    public int glisteningRadius = 16;
    public int emeradicRadius = 32;
    public int crystalRadius = 64;
    public int netheredRadius = 128;

    public int maxClaimsPerPlayer = -1;
    public boolean enablePvPinClaims = false;
    public boolean allowDamagingUnnamedHostileMobs = true;
    public boolean allowDamagingNamedHostileMobs = false;

    public boolean claimProtectsFullWorldHeight = false;
    public double claimAreaHeightMultiplier = 1;
    public boolean makeClaimAreaChunkBound = false;
    public boolean allowClaimOverlappingIfSameOwner = false;
    public boolean allowFakePlayersToModify = false;
    public boolean protectAgainstHostileExplosionsActivatedByTrustedPlayers = false;

    public Set<Identifier> dimensionBlacklist = new HashSet<>();
    public Map<Identifier, List<Box>> regionBlacklist = new HashMap<>();

    public Map<Block, Boolean> enabledAugments = new HashMap<>();

    public Set<Block> allowedBlockInteraction = new HashSet<>();

    public Set<EntityType<?>> allowedEntityInteraction = Set.of();

    public WrappedText messagePrefix = WrappedText.of("<dark_gray>[<#a1ff59>GOML</color>]");

    public WrappedText placeholderNoClaimInfo = WrappedText.of("<gray><italic>Wilderness");
    public WrappedText placeholderNoClaimOwners = WrappedText.of("<gray><italic>Nobody");
    public WrappedText placeholderNoClaimTrusted = WrappedText.of("<gray><italic>Nobody");
    public WrappedText placeholderClaimCanBuildInfo = WrappedText.of("${owners} <gray>(<green>${anchor}</green>)");
    public WrappedText placeholderClaimCantBuildInfo = WrappedText.of("${owners} <gray>(<red>${anchor}</red>)");

    public String claimColorSource = "location";

    public boolean canInteract(Block block) {
        return this.allowedBlockInteraction.contains(block);
    }

    public boolean canInteract(Entity entity) {
        return this.allowedEntityInteraction.contains(entity.getType());
    }

    public boolean isBlacklisted(World world, Box claimBox) {
        if (this.dimensionBlacklist.contains(world.getRegistryKey().getValue())) {
            return true;
        }

        var list = this.regionBlacklist.get(world.getRegistryKey().getValue());
        if (list != null) {
            for (var box : list) {
                if (box.intersectsClosed(claimBox)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean useLocationForColor() {
        return !usePlayerForColor();
    }

    public boolean usePlayerForColor() {
        return "player".equalsIgnoreCase(claimColorSource);
    }

    public MutableText prefix(Text text) {
        return Text.empty().append(messagePrefix.text()).append(Text.literal(" ")).append(text);
    }

    public static GOMLConfig loadOrCreateConfig() {
        try {
            GOMLConfig config;
            File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "getoffmylawn.json");

            if (configFile.exists()) {
                String json = IOUtils.toString(new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8));

                config = BaseGson.GSON.fromJson(json, GOMLConfig.class);
            } else {
                config = new GOMLConfig();
            }

            for (var augment : GOMLBlocks.AUGMENTS) {
                if (!config.enabledAugments.containsKey(augment)) {
                    config.enabledAugments.put(augment, true);
                }
            }

            saveConfig(config);
            return config;
        }
        catch(IOException exception) {
            GetOffMyLawn.LOGGER.error("Something went wrong while reading config!");
            exception.printStackTrace();
            return new GOMLConfig();
        }
    }

    public static void saveConfig(GOMLConfig config) {
        File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "getoffmylawn.json");
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8));
            writer.write(BaseGson.GSON.toJson(config));
            writer.close();
        } catch (Exception e) {
            GetOffMyLawn.LOGGER.error("Something went wrong while saving config!");
            e.printStackTrace();
        }
    }
}
