package com.computerascience.moneymanager;

import java.util.List;

final class AssetBackup {
    final List<AssetRecord> assets;
    final List<AssetSnapshot> snapshots;
    final PortfolioSettings settings;

    AssetBackup(List<AssetRecord> assets, List<AssetSnapshot> snapshots, PortfolioSettings settings) {
        this.assets = assets;
        this.snapshots = snapshots;
        this.settings = settings;
    }
}
