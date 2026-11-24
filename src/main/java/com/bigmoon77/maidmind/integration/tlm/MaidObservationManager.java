package com.bigmoon77.maidmind.integration.tlm;

import com.bigmoon77.maidmind.MaidMind;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Keeps the latest observation for each currently loaded maid.
 *
 * All methods are intended to be called from the logical server thread.
 */
public final class MaidObservationManager {
    public static final MaidObservationManager INSTANCE = new MaidObservationManager();

    private static final int UPDATE_INTERVAL_TICKS = 20;

    private final Map<UUID, EntityMaid> trackedMaids = new LinkedHashMap<>();
    private final Map<UUID, MaidSnapshot> snapshots = new LinkedHashMap<>();
    private int tickCounter;

    private MaidObservationManager() {
    }

    public void onMaidLoaded(EntityMaid maid) {
        UUID uuid = maid.getUUID();
        boolean newlyTracked = !trackedMaids.containsKey(uuid);

        trackedMaids.put(uuid, maid);
        snapshots.put(uuid, MaidSnapshot.capture(maid));

        if (newlyTracked) {
            MaidSnapshot snapshot = snapshots.get(uuid);
            MaidMind.LOGGER.info(
                    "Detected Touhou Little Maid: uuid={}, task={}, inventorySlots={}, occupiedSlots={}",
                    snapshot.uuid(),
                    snapshot.taskId(),
                    snapshot.inventory().size(),
                    snapshot.inventory().stream().filter(entry -> entry.count() > 0).count()
            );
        }
    }

    public void onMaidUnloaded(EntityMaid maid) {
        UUID uuid = maid.getUUID();
        if (trackedMaids.get(uuid) == maid) {
            trackedMaids.remove(uuid);
            snapshots.remove(uuid);
        }
    }

    public void tick() {
        if (++tickCounter < UPDATE_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        var iterator = trackedMaids.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, EntityMaid> entry = iterator.next();
            EntityMaid maid = entry.getValue();

            if (maid.isRemoved()) {
                iterator.remove();
                snapshots.remove(entry.getKey());
                continue;
            }

            MaidSnapshot previous = snapshots.get(entry.getKey());
            MaidSnapshot current = MaidSnapshot.capture(maid);
            snapshots.put(entry.getKey(), current);

            if (previous != null) {
                logChanges(previous, current);
            }
        }
    }

    private void logChanges(MaidSnapshot previous, MaidSnapshot current) {
        List<String> changes = new ArrayList<>();

        if (blockCoordinateChanged(previous.x(), current.x())
                || blockCoordinateChanged(previous.y(), current.y())
                || blockCoordinateChanged(previous.z(), current.z())) {
            changes.add(String.format(
                    "position=(%.2f, %.2f, %.2f)",
                    current.x(), current.y(), current.z()
            ));
        }

        if (Float.compare(previous.health(), current.health()) != 0) {
            changes.add(String.format(
                    "health=%.1f->%.1f",
                    previous.health(), current.health()
            ));
        }

        if (!Objects.equals(previous.taskId(), current.taskId())) {
            changes.add("task=" + previous.taskId() + "->" + current.taskId());
        }

        List<String> inventoryChanges = getInventoryChanges(previous, current);
        if (!inventoryChanges.isEmpty()) {
            changes.add("inventoryChanges=" + inventoryChanges);
        }

        if (!changes.isEmpty()) {
            MaidMind.LOGGER.info(
                    "Maid observation updated: uuid={}, {}",
                    current.uuid(),
                    String.join(", ", changes)
            );
        }
    }

    private boolean blockCoordinateChanged(double previous, double current) {
        return Math.floor(previous) != Math.floor(current);
    }

    private List<String> getInventoryChanges(MaidSnapshot previous, MaidSnapshot current) {
        List<String> changes = new ArrayList<>();
        int slotCount = Math.max(previous.inventory().size(), current.inventory().size());

        for (int slot = 0; slot < slotCount; slot++) {
            MaidSnapshot.InventoryEntry oldEntry = slot < previous.inventory().size()
                    ? previous.inventory().get(slot)
                    : null;
            MaidSnapshot.InventoryEntry newEntry = slot < current.inventory().size()
                    ? current.inventory().get(slot)
                    : null;

            if (Objects.equals(oldEntry, newEntry)) {
                continue;
            }

            String oldValue = oldEntry == null
                    ? "missing"
                    : oldEntry.itemId() + "x" + oldEntry.count();
            String newValue = newEntry == null
                    ? "missing"
                    : newEntry.itemId() + "x" + newEntry.count();

            changes.add(slot + ":" + oldValue + "->" + newValue);
        }

        return changes;
    }

    /**
     * Returns a stable, read-only copy of the latest observations.
     */
    public Map<UUID, MaidSnapshot> getSnapshots() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(snapshots));
    }

    /**
     * Captures a terrain grid for a currently loaded maid.
     *
     * This method must be called on the logical server thread. Capture is
     * intentionally on-demand so terrain scanning is only performed when
     * combat AI requests it.
     */
    public Optional<TerrainObservation> captureTerrain(UUID uuid) {
        EntityMaid maid = trackedMaids.get(uuid);
        if (maid == null || maid.isRemoved()) {
            return Optional.empty();
        }
        return Optional.of(TerrainObservation.capture(maid));
    }

    public int getTrackedMaidCount() {
        return trackedMaids.size();
    }
}
