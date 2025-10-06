package draylar.goml.compat.webmap;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import draylar.goml.GetOffMyLawn;
import draylar.goml.api.Claim;
import draylar.goml.api.ClaimUtils;
import draylar.goml.api.event.ClaimEvents;
import draylar.goml.api.event.ServerPlayerUpdateEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.loader.api.FabricLoader;

public abstract class WebmapCompat {
    public static final String MARKER_SET_ID = "gomlMarkerSet";
    public static final String MARKER_SET_LABEL = Text.translatable("text.goml.webmap.claims", "Claims").getString();

    public static final Boolean HIDE_BY_DEFAULT = false; // TODO: add config options?

    private static final AtomicBoolean EVENTS_REGISTERED = new AtomicBoolean(false);
    private static final CopyOnWriteArrayList<WebmapCompat> INTEGRATIONS = new CopyOnWriteArrayList<>();
    private static final ConcurrentHashMap<Claim, ClaimMarker> MARKERS = new ConcurrentHashMap<>();

    private static MinecraftServer _server;
    
    protected WebmapCompat() {}

    protected abstract void onIntegrationRegistered();
    protected abstract void handleMarkerCreated(ClaimMarker marker);
    protected abstract void handleMarkerUpdated(ClaimMarker marker);
    protected abstract void handleMarkerRemoved(ClaimMarker marker);

    public static void init(MinecraftServer server) {
        _server = server;
        MARKERS.clear();

        if (!EVENTS_REGISTERED.getAndSet(true)) {
            ClaimEvents.CLAIM_CREATED.register(WebmapCompat::createClaimMarker);
            ClaimEvents.CLAIM_DESTROYED.register(WebmapCompat::deleteClaimMarker);
            ClaimEvents.CLAIM_UPDATED.register(WebmapCompat::updateClaimMarker);
            ClaimEvents.CLAIM_RESIZED.register((claim, oldBox, newBox) -> updateClaimMarker(claim));

            ServerPlayerUpdateEvents.NAME_CHANGED.register(player -> runOnServer(() -> updateClaimMarkersForPlayer(player)));
        }

        INTEGRATIONS.clear();

        if (FabricLoader.getInstance().isModLoaded("bluemap")) {
            registerIntegration(BluemapCompat.getInstance());
        }
        if (FabricLoader.getInstance().isModLoaded("dynmap")) {
            registerIntegration(DynmapCompat.getInstance());
        }

        for (ServerWorld world : _server.getWorlds()) {
            GetOffMyLawn.CLAIM.get(world).getClaims().values().forEach(WebmapCompat::createClaimMarker);
        }
    }

    /**
     * Create claim marker on all maps for the specified claim
     * 
     * @param claim the claim to create a marker for
     */
    public static final void createClaimMarker(Claim claim) {
        runOnServer(() -> {
            ClaimMarker marker = buildMarker(claim);
            if (marker == null) {
                return;
            }

            ClaimMarker previous = MARKERS.put(claim, marker);
            if (previous == null) {
                INTEGRATIONS.forEach(integration -> integration.handleMarkerCreated(marker));
            } else {
                INTEGRATIONS.forEach(integration -> integration.handleMarkerUpdated(marker));
            }
        });
    }

    /**
     * Remove markers from all maps for the specified claim
     * 
     * @param claim the claim to remove markers for
     */
    public static final void deleteClaimMarker(Claim claim) {
        runOnServer(() -> {
            ClaimMarker removed = MARKERS.remove(claim);
            if (removed != null) {
                INTEGRATIONS.forEach(integration -> integration.handleMarkerRemoved(removed));
            }
        });
    }

    /**
     * Refresh and update the specified claim's markers on all maps
     * 
     * @param claim the claim to update markers for
     */
    public static final void updateClaimMarker(Claim claim) {
        runOnServer(() -> {
            ClaimMarker marker = buildMarker(claim);
            if (marker == null) {
                ClaimMarker removed = MARKERS.remove(claim);
                if (removed != null) {
                    INTEGRATIONS.forEach(integration -> integration.handleMarkerRemoved(removed));
                }
                return;
            }

            MARKERS.put(claim, marker);
            INTEGRATIONS.forEach(integration -> integration.handleMarkerUpdated(marker));
        });
    }

    // Update all claim markers where the player is an owner or trusted
    private static final void updateClaimMarkersForPlayer(ServerPlayerEntity player) {
        var uuid = player.getUuid();
        for (ServerWorld world : _server.getWorlds()) {
            ClaimUtils.getClaimsOwnedBy(world, uuid).forEach(claim -> updateClaimMarker(claim.getValue()));
            ClaimUtils.getClaimsTrusted(world, uuid).forEach(claim -> updateClaimMarker(claim.getValue()));
        }
    }

    // Run function thread-safe
    protected static final void runOnServer(Runnable task) {
        if (_server == null) {
            return;
        }

        if (_server.isOnThread()) {
            task.run();
        } else {
            _server.execute(task);
        }
    }

    protected static MinecraftServer getServer() {
        return _server;
    }

    protected static boolean isServerAvailable() {
        return _server != null;
    }

    protected static List<ClaimMarker> snapshotMarkers() {
        return List.copyOf(MARKERS.values());
    }

    private static final void registerIntegration(WebmapCompat integration) {
        if (integration == null) {
            return;
        }

        if (INTEGRATIONS.contains(integration)) {
            return;
        }

        INTEGRATIONS.add(integration);
        integration.onIntegrationRegistered();
    }


    // Construct claim marker using claim
    private static final @Nullable ClaimMarker buildMarker(Claim claim) {
        if (claim == null || _server == null) {
            return null;
        }

        try {
            return new ClaimMarker(claim, MARKER_SET_ID, _server);
        } catch (IllegalArgumentException exception) {
            GetOffMyLawn.LOGGER.debug("Skipping web map marker for claim {} due to: {}", claim.getOrigin(), exception.getMessage());
            return null;
        }
    }
}
