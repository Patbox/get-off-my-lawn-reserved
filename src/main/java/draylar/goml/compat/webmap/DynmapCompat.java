package draylar.goml.compat.webmap;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class DynmapCompat extends WebmapCompat {
    private static final DynmapCompat INSTANCE = new DynmapCompat();

    private final AtomicReference<DynmapCommonAPI> apiReference = new AtomicReference<>();
    private final AtomicReference<MarkerSet> markerSetReference = new AtomicReference<>();

    private DynmapCompat() {
        DynmapCommonAPIListener.register(new DynmapCommonAPIListener() {
            @Override
            public void apiEnabled(DynmapCommonAPI api) {
                runOnServer(() -> {
                    apiReference.set(api);
                    initializeMarkers(api);
                });
            }

            @Override
            public void apiDisabled(DynmapCommonAPI api) {
                runOnServer(() -> {
                    clearMarkers(api);
                    markerSetReference.set(null);
                    apiReference.compareAndSet(api, null);
                });
            }
        });
    }

    static DynmapCompat getInstance() {
        return INSTANCE;
    }

    @Override
    protected void onIntegrationRegistered() {
        DynmapCommonAPI api = apiReference.get();
        if (api != null) {
            runOnServer(() -> initializeMarkers(api));
        }
    }

    @Override
    protected void handleMarkerCreated(ClaimMarker marker) {
        withMarkerSet(true, markerSet -> {
            AreaMarker areaMarker = renderAreaMarker(markerSet, marker);
            if (areaMarker != null) {
                applyMarkerStyles(areaMarker, marker);
            }
        });
    }

    @Override
    protected void handleMarkerUpdated(ClaimMarker marker) {
        handleMarkerRemoved(marker);
        handleMarkerCreated(marker);
    }

    @Override
    protected void handleMarkerRemoved(ClaimMarker marker) {
        withMarkerSet(false, markerSet -> {
            AreaMarker existing = markerSet.findAreaMarker(marker.getId());
            if (existing != null) {
                existing.deleteMarker();
            }
        });
    }

    private void initializeMarkers(DynmapCommonAPI api) {
        if (!isServerAvailable() || api == null || !api.markerAPIInitialized()) {
            return;
        }

        MarkerAPI markerApi = api.getMarkerAPI();
        if (markerApi == null) {
            return;
        }

        MarkerSet markerSet = ensureMarkerSet(markerApi);
        if (markerSet == null) {
            return;
        }

        Set<AreaMarker> existingMarkers = Set.copyOf(markerSet.getAreaMarkers());
        existingMarkers.forEach(AreaMarker::deleteMarker);

        for (ClaimMarker marker : snapshotMarkers()) {
            handleMarkerCreated(marker);
        }
    }

    private void clearMarkers(DynmapCommonAPI api) {
        if (api == null) {
            return;
        }

        MarkerAPI markerApi = api.getMarkerAPI();
        if (markerApi == null) {
            return;
        }

        MarkerSet markerSet = markerApi.getMarkerSet(MARKER_SET_ID);
        if (markerSet != null) {
            markerSet.deleteMarkerSet();
        }
    }

    private void withMarkerSet(boolean createIfMissing, Consumer<MarkerSet> consumer) {
        DynmapCommonAPI api = apiReference.get();
        if (api == null || !api.markerAPIInitialized()) {
            return;
        }

        MarkerAPI markerApi = api.getMarkerAPI();
        if (markerApi == null) {
            return;
        }

        MarkerSet markerSet = markerSetReference.get();
        if (markerSet == null) {
            markerSet = markerApi.getMarkerSet(MARKER_SET_ID);
            if (markerSet != null) {
                markerSetReference.compareAndSet(null, markerSet);
            }
        }

        if (markerSet == null && createIfMissing) {
            markerSet = ensureMarkerSet(markerApi);
        }

        if (markerSet != null) {
            consumer.accept(markerSet);
        }
    }

    private MarkerSet ensureMarkerSet(MarkerAPI markerApi) {
        MarkerSet markerSet = markerApi.getMarkerSet(MARKER_SET_ID);
        if (markerSet == null) {
            markerSet = markerApi.createMarkerSet(MARKER_SET_ID, MARKER_SET_LABEL, null, true);
        }

        if (markerSet != null) {
            markerSet.setMarkerSetLabel(MARKER_SET_LABEL);
            markerSet.setHideByDefault(HIDE_BY_DEFAULT);
            markerSetReference.set(markerSet);
        }

        return markerSet;
    }

    private AreaMarker renderAreaMarker(MarkerSet markerSet, ClaimMarker marker) {
        AreaMarker existing = markerSet.findAreaMarker(marker.getId());
        if (existing != null) {
            existing.deleteMarker();
        }

        String worldName = resolveWorldName(marker.getWorld());
        Box box = marker.getClaimBox().minecraftBox();

        double minX = box.minX;
        double maxX = box.maxX;
        double minZ = box.minZ;
        double maxZ = box.maxZ;

        double[] x = new double[] { minX, minX, maxX, maxX };
        double[] z = new double[] { minZ, maxZ, maxZ, minZ };

        return markerSet.createAreaMarker(marker.getId(), marker.getId(), false, worldName, x, z, true);
    }

    private void applyMarkerStyles(AreaMarker areaMarker, ClaimMarker marker) {
        // Box box = marker.getClaimBox().minecraftBox();

        // double minY = box.minY;
        // double maxY = Math.max(box.maxY, minY + 1);

        // areaMarker.setRangeY(minY, maxY);

        areaMarker.setLabel(marker.getId(), false);
        areaMarker.setDescription(marker.renderHtml());
        areaMarker.setFillStyle(0.25, marker.getColor());
        areaMarker.setLineStyle(2, 0.8, marker.getColor());
    }

    // Handle legacy behavior around the nether/end dimension names
    private String resolveWorldName(ServerWorld world) {
        String levelName = world.getServer().getSaveProperties().getLevelName();
        if (world.getRegistryKey().equals(World.OVERWORLD)) {
            return levelName;
        } else if (world.getRegistryKey().equals(World.NETHER)) {
            return "DIM-1";
        } else if (world.getRegistryKey().equals(World.END)) {
            return "DIM1";
        }

        Identifier id = world.getRegistryKey().getValue();
        return (id.getNamespace() + "_" + id.getPath()).replace(':', '_');
    }
}
