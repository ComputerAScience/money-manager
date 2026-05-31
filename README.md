# Money Manager

一个原生 Android 资产更新助手。它不登录银行或券商账户，只记录每项资产、更新时间、更新周期和绑定 App；需要核对时，一键打开对应银行、券商或钱包 App，再手动保存最新余额。

仓库根目录的 `index.html` 是 GitHub Pages 分享承接页；Android App 可以生成只读分享链接，把当前资产看板带到 Pages 页面展示。

## 当前重点

- 资产记录：按机构管理资产；一条资产属于一个机构，也可以绑定一个 App 作为打开入口
- App 绑定：从已安装 App 列表选择，只显示 App 名称，不需要输入包名
- 总览：净资产、资产总额、负债、更新状态、资产比例、机构分布和年度目标
- 资产类型：支持自定义资产类型，并可选择哪些类型出现在投资 Tab
- 投资：按资产类型开关汇总投资资产，并按机构分组展示
- 兼容：旧版银行 / 券商账户明细会在升级或导入备份时自动转换为按机构管理的扁平资产，并记录本地 schema 版本
- 趋势：总金额趋势、资产类型分布变化、单项资产趋势和历史快照
- 行动中心：合并待更新、数据质量、目标偏离和优先核对资产，并按机构组织
- 汇率：启动时自动获取常用币种实时汇率，也可在设置里刷新或手动维护
- 分享：设置里可生成 GitHub Pages 只读看板链接；数据放在链接 fragment 中，页面在浏览器本地解析
- 设置：隐私模式、基准币种、目标比例、备份恢复、版本检查和 APK 下载；右侧低存在感索引条可边滑边选择目录，也可点击展开本页目录
- 更新：GitHub Actions 构建 master / dev APK，并发布到固定 Release 地址

详见 [android/README.md](android/README.md)。

## 下载 APK

GitHub Actions 会发布两个 APK 渠道：

- [下载 master 版 APK](https://github.com/ComputerAScience/money-manager/releases/download/android-master-latest/money-manager-master.apk)
- [下载开发版 APK](https://github.com/ComputerAScience/money-manager/releases/download/android-dev-latest/money-manager-dev.apk)

master 版和开发版使用同一个 Android 包名和同一套稳定签名，因此不能同时安装，但可以互相覆盖安装，正常情况下本机数据会保留。开发版的 App 名称会显示为 `Money Manager Dev`，用于区分当前装的是哪个渠道。

手机打开这个仓库首页，或用相机扫描下面二维码下载 master 版：

<a href="https://github.com/ComputerAScience/money-manager/releases/download/android-master-latest/money-manager-master.apk">
  <img src="https://api.qrserver.com/v1/create-qr-code/?size=220x220&amp;margin=12&amp;data=https%3A%2F%2Fgithub.com%2FComputerAScience%2Fmoney-manager%2Freleases%2Fdownload%2Fandroid-master-latest%2Fmoney-manager-master.apk" alt="扫码下载 Money Manager master APK" width="220" height="220">
</a>

这个 APK 适合个人自用和测试。手机安装时可能需要允许“安装未知来源应用”。

从 `0.6.7` 开始，master / dev 渠道会使用同一套稳定签名。若手机里已经安装过更早的分支构建并提示“签名不一致”，Android 不允许直接覆盖；请先在 App 设置里导出备份，再卸载旧签名版本，安装 `0.6.7` 之后导入备份。完成这一次切换后，后续 master 和 dev 之间覆盖安装会保留数据。

## 本地开发

1. 用 Android Studio 打开 `android/` 目录。
2. 等待 Gradle 同步。
3. 连接 Android 手机或启动模拟器。
4. 运行 `app` 模块。

命令行构建：

```bash
cd android
gradle :app:assembleMasterDebug :app:assembleDevDebug
```
