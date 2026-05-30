package com.computerascience.moneymanager;

import java.util.List;

final class AssetBackup {
    final List<AssetRecord> assets;
    final List<AssetSnapshot> snapshots;

    AssetBackup(List<AssetRecord> assets, List<AssetSnapshot> snapshots) {
        this.assets = assets;
        this.snapshots = snapshots;
    }
}
