package com.computerascience.moneymanager;

import java.util.List;

final class AssetBackup {
    final List<AssetRecord> assets;
    final List<AssetSnapshot> snapshots;
    final List<AssetUpdateEvent> updateEvents;
    final PortfolioSettings settings;

    AssetBackup(
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
