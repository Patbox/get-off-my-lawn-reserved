package draylar.goml.compat.webmap;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class BluemapCompat extends WebmapCompat {
    private static final BluemapCompat INSTANCE = new BluemapCompat();

    private final AtomicReference<BlueMapAPI> apiReference = new AtomicReference<>();

    private BluemapCompat() {
        BlueMapAPI.onEnable(api -> runOnServer(() -> {
            apiReference.set(api);
            initializeMarkers(api);
        }));

        BlueMapAPI.onDisable(api -> runOnServer(() -> {
            clearMarkerSets(api);
            apiReference.compareAndSet(api, null);
        }));
    }

    static BluemapCompat getInstance() {
        return INSTANCE;
    }

    @Override
    protected void onIntegrationRegistered() {
        BlueMapAPI.getInstance().ifPresent(api -> runOnServer(() -> {
            apiReference.set(api);
            initializeMarkers(api);
        }));
    }

    @Override
    protected void handleMarkerCreated(ClaimMarker marker) {
        withMarkerSets(marker, true, markerSet -> markerSet.put(marker.getId(), renderMapMarker(marker)));
    }

    @Override
    protected void handleMarkerUpdated(ClaimMarker marker) {
        handleMarkerRemoved(marker);
        handleMarkerCreated(marker);
    }

    @Override
    protected void handleMarkerRemoved(ClaimMarker marker) {
        withMarkerSets(marker, false, markerSet -> markerSet.remove(marker.getId()));
    }

    private void initializeMarkers(BlueMapAPI api) {
        if (!isServerAvailable()) {
            return;
        }

        for (ServerWorld world : getServer().getWorlds()) {
            api.getWorld(world).ifPresent(blueWorld -> blueWorld.getMaps().forEach(this::ensureMarkerSet));
        }

        for (ClaimMarker marker : snapshotMarkers()) {
            handleMarkerCreated(marker);
        }
    }

    private void clearMarkerSets(BlueMapAPI api) {
        if (!isServerAvailable()) {
            return;
        }

        for (ServerWorld world : getServer().getWorlds()) {
            api.getWorld(world).ifPresent(blueWorld -> blueWorld.getMaps().forEach(map -> map.getMarkerSets().remove(MARKER_SET_ID)));
        }
    }

    private void withMarkerSets(ClaimMarker marker, boolean createIfMissing, Consumer<MarkerSet> consumer) {
        BlueMapAPI api = apiReference.get();
        if (api == null) {
            return;
        }

        api.getWorld(marker.getWorld()).ifPresent(blueWorld -> blueWorld.getMaps().forEach(map -> {
            MarkerSet markerSet = createIfMissing
                    ? ensureMarkerSet(map)
                    : map.getMarkerSets().get(MARKER_SET_ID);
            if (markerSet != null) {
                consumer.accept(markerSet);
            }
        }));
    }

    private MarkerSet ensureMarkerSet(BlueMapMap map) {
        return map.getMarkerSets().computeIfAbsent(MARKER_SET_ID, key -> MarkerSet.builder().label(MARKER_SET_LABEL).defaultHidden(HIDE_BY_DEFAULT).build());
    }

    private ExtrudeMarker renderMapMarker(ClaimMarker marker) {
        Box box = marker.getClaimBox().minecraftBox();
        Shape shape = Shape.createRect(box.minX, box.minZ, box.maxX, box.maxZ);
        float minY = (float) box.minY;
        float maxY = (float) Math.max(box.maxY, box.minY + 1);

        Color outlineColor = new Color(marker.getColor(), 0.8F);
        Color fillColor = new Color(marker.getColor(), 0.1F);

        return ExtrudeMarker.builder()
                .label(marker.getId())
                .detail(marker.renderHtml())
                .shape(shape, minY, maxY)
                .lineWidth(2)
                .lineColor(outlineColor)
                .fillColor(fillColor)
                .depthTestEnabled(true)
                .build();
    }
}
