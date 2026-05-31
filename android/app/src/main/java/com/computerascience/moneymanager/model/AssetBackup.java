package com.computerascience.moneymanager.model;

import java.util.List;

public final class AssetBackup {
    public final List<AssetRecord> assets;
    public final List<AssetSnapshot> snapshots;
    public final List<AssetUpdateEvent> updateEvents;
    public final PortfolioSettings settings;

    public AssetBackup(
            List<AssetRecord> assets,
            List<AssetSnapshot> snapshots,
            List<AssetUpdateEvent> updateEvents,
            PortfolioSettings settings
    ) {
        this.assets = assets;
        this.snapshots = snapshots;
        this.updateEvents = updateEvents;
        this.settings = settings;
    }
}
