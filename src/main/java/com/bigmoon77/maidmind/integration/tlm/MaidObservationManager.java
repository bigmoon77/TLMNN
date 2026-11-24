package com.bigmoon77.maidmind.integration.tlm;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
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
            com.bigmoon77.maidmind.MaidMind.LOGGER.info(
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

            snapshots.put(entry.getKey(), MaidSnapshot.capture(maid));
        }
    }

    /**
     * Returns a stable, read-only copy of the latest observations.
     */
    public Map<UUID, MaidSnapshot> getSnapshots() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(snapshots));
    }

    public int getTrackedMaidCount() {
        return trackedMaids.size();
    }
}
