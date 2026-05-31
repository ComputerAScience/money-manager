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
- 设置：隐私模式、基准币种、目标比例、备份恢复、版本检查和 APK 下载；右侧进度条可拖动选择目录，也可点击展开本页目录
- 更新：GitHub Actions 构建 debug APK，并发布到固定 Release 地址

详见 [android/README.md](android/README.md)。

## 下载 APK

GitHub Actions 每次构建后会更新固定 debug APK：

[下载最新版 Android APK](https://github.com/ComputerAScience/money-manager/releases/download/android-debug-latest/money-manager-debug.apk)

手机打开这个仓库首页，或用相机扫描下面二维码即可下载：

<a href="https://github.com/ComputerAScience/money-manager/releases/download/android-debug-latest/money-manager-debug.apk">
  <img src="https://api.qrserver.com/v1/create-qr-code/?size=220x220&amp;margin=12&amp;data=https%3A%2F%2Fgithub.com%2FComputerAScience%2Fmoney-manager%2Freleases%2Fdownload%2Fandroid-debug-latest%2Fmoney-manager-debug.apk" alt="扫码下载 Money Manager Android APK" width="220" height="220">
</a>

这个 APK 是 debug 版，适合个人自用和测试。手机安装时可能需要允许“安装未知来源应用”。

`0.4.1` 之后 GitHub Actions 会复用稳定 debug 签名，后续下载通常可以直接覆盖安装。若从旧签名版本更新时仍提示“签名不一致”，需要先卸载旧版再安装一次新版。

## 本地开发

1. 用 Android Studio 打开 `android/` 目录。
2. 等待 Gradle 同步。
3. 连接 Android 手机或启动模拟器。
4. 运行 `app` 模块。

命令行构建：

```bash
cd android
gradle :app:assembleDebug
```
