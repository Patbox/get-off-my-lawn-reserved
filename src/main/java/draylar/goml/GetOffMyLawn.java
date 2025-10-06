package draylar.goml;

import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;
import draylar.goml.api.Claim;
import draylar.goml.api.GomlProtectionProvider;
import draylar.goml.cca.ClaimComponent;
import draylar.goml.cca.WorldClaimComponent;
import draylar.goml.compat.ArgonautsCompat;
import draylar.goml.compat.webmap.WebmapCompat;
import draylar.goml.other.CardboardWarning;
import draylar.goml.other.ClaimCommand;
import draylar.goml.config.GOMLConfig;
import draylar.goml.other.PlaceholdersReg;
import draylar.goml.other.VanillaTeamGroups;
import draylar.goml.registry.GOMLBlocks;
import draylar.goml.registry.GOMLEntities;
import draylar.goml.registry.GOMLItems;
import eu.pb4.common.protection.api.CommonProtection;
import eu.pb4.polymer.core.api.item.PolymerItemGroupUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.WorldChunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

public class GetOffMyLawn implements ModInitializer, WorldComponentInitializer {
    public static final String MOD_ID = "goml";
    public static final ComponentKey<ClaimComponent> CLAIM = ComponentRegistryV3.INSTANCE.getOrCreate(id("claims"), ClaimComponent.class);
    public static final ItemGroup GROUP = ItemGroup.create(null, -1)
            .displayName(Text.translatable("itemGroup.goml.group"))
            .icon(() -> new ItemStack(GOMLBlocks.WITHERED_CLAIM_ANCHOR.getSecond()))
            .entries((ctx, c) -> {
                GOMLBlocks.ANCHORS.forEach(c::add);
                GOMLBlocks.AUGMENTS.forEach(c::add);
                GOMLItems.BASE_ITEMS.forEach(c::add);
            })
            .build();
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static GOMLConfig CONFIG = new GOMLConfig();

    public static Identifier id(String name) {
        return Identifier.of(MOD_ID, name);
    }

    @Override
    public void onInitialize() {
        CardboardWarning.checkAndAnnounce();
        GOMLBlocks.init();
        GOMLItems.init();
        GOMLEntities.init();
        EventHandlers.init();
        ClaimCommand.init();
        PlaceholdersReg.init();

        PolymerItemGroupUtils.registerPolymerItemGroup(id("group"), GROUP);

        CommonProtection.register(Identifier.of(MOD_ID, "claim_protection"), GomlProtectionProvider.INSTANCE);

        ServerLifecycleEvents.SERVER_STARTING.register((s) -> {
            CardboardWarning.checkAndAnnounce();
            GetOffMyLawn.CONFIG = GOMLConfig.loadOrCreateConfig();
        });

        ServerTickEvents.END_WORLD_TICK.register((world) -> CLAIM.get(world).getClaims().values().forEach(x -> x.tick(world)));

        VanillaTeamGroups.init();
        if (FabricLoader.getInstance().isModLoaded("argonauts")) {
            ArgonautsCompat.init();
        }

        ServerLifecycleEvents.SERVER_STARTED.register(WebmapCompat::init);

        ServerChunkEvents.CHUNK_LOAD.register((world, server) -> GetOffMyLawn.onChunkEvent(world, server, Claim::internal_incrementChunks));
        ServerChunkEvents.CHUNK_UNLOAD.register((world, server) -> GetOffMyLawn.onChunkEvent(world, server, Claim::internal_decrementChunks));
    }

    @Override
    public void registerWorldComponentFactories(WorldComponentFactoryRegistry registry) {
        registry.register(CLAIM, WorldClaimComponent::new);
    }

    private static void onChunkEvent(ServerWorld world, WorldChunk chunk, Consumer<Claim> chunkHandler) {
        CLAIM.get(world).getClaims().entries().filter(x -> {
            var minX = ChunkSectionPos.getSectionCoord(x.getKey().toBox().x1());
            var minZ = ChunkSectionPos.getSectionCoord(x.getKey().toBox().z1());

            var maxX = ChunkSectionPos.getSectionCoord(x.getKey().toBox().x2());
            var maxZ = ChunkSectionPos.getSectionCoord(x.getKey().toBox().z2());

            return (minX <= chunk.getPos().x && maxX >= chunk.getPos().x && minZ <= chunk.getPos().z && maxZ >= chunk.getPos().z);
        }).forEach(x -> chunkHandler.accept(x.getValue()));
    }
}
