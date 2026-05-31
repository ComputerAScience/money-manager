package com.computerascience.moneymanager.model;

import java.util.List;

public final class AssetBackup {
    public final List<AssetRecord> assets;
    public final List<AssetSnapshot> snapshots;
    public final List<AssetUpdateEvent> updateEvents;
    public final PortfolioSettings settings;
    public final int version;
    public final int schemaVersion;
    public final boolean migratedLegacyAssets;

    public AssetBackup(
            List<AssetRecord> assets,
            List<AssetSnapshot> snapshots,
            List<AssetUpdateEvent> updateEvents,
            PortfolioSettings settings,
            int version,
            int schemaVersion,
            boolean migratedLegacyAssets
    ) {
        this.assets = assets;
        this.snapshots = snapshots;
        this.updateEvents = updateEvents;
        this.settings = settings;
        this.version = version;
        this.schemaVersion = schemaVersion;
        this.migratedLegacyAssets = migratedLegacyAssets;
    }
}
