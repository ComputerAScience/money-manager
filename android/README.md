# Money Manager Android

这是一个原生 Android 资产更新助手。它不登录银行或券商账户，只记录每项资产的更新时间，并在你点击时打开对应的银行、券商或钱包 App。

## 当前功能

- 新增、编辑、删除资产
- 记录资产名称、类型、机构、金额、币种、更新周期、最后更新时间
- 总资产页展示净资产、资产总额、负债、更新状态
- 按资产类型展示比例图和比例明细
- 记录本地每日总资产快照，展示近一年变化趋势
- 资产管理区域可折叠和展开
- 待办提醒会提示过期资产、未绑定 App、多币种和负债率风险
- 根据更新周期标记“新鲜 / 待更新 / 已过期”
- 首次绑定时可以从已安装 App 列表中选择，不需要手动查包名
- 点击“打开 App”拉起对应应用
- 从外部 App 返回后，询问是否把该资产标记为已更新
- 数据保存在本机 `SharedPreferences`

## 运行方式

1. 用 Android Studio 打开 `android/` 目录。
2. 等待 Gradle 同步。
3. 连接 Android 手机或启动模拟器。
4. 运行 `app` 模块。

命令行构建需要本机安装 Java、Android SDK 和 Gradle：

```bash
cd android
gradle :app:assembleDebug
```

当前 Codex 环境没有 Java / Gradle / Android SDK，所以这里没有直接编译 APK。

## 不写代码，直接下载 APK

这个仓库已经配置了 GitHub Actions 自动构建：

1. 打开 GitHub 仓库页面。
2. 进入 `Actions`。
3. 点击 `Build Android APK`。
4. 选择最新一次成功运行。
5. 在页面底部 `Artifacts` 下载 `money-manager-debug-apk`。
6. 解压后得到 `money-manager-debug.apk`，发送到 Android 手机安装。

如果想手动重新构建：

1. 进入 `Actions -> Build Android APK`。
2. 点击 `Run workflow`。
3. 等待运行成功后下载 artifact。

这个 APK 是 debug 版，适合个人自用和测试。手机安装时可能需要允许“安装未知来源应用”。

## 如何绑定 App

编辑资产时填写：

- `App 包名`：可以点击“选择已安装 App”自动填入，也可以手动输入
- `启动链接`：可选，如果某个 App 提供 deep link，可以填入类似 `xxx://home`

如果只填包名，应用会尝试打开该 App 的主入口。如果没有安装，会尝试跳到应用商店页面。

## 趋势如何产生

App 会在这些时刻记录当天总资产快照：

- 新增、编辑或删除资产
- 点击“已更新”
- 点击“记录今日快照”

同一天多次记录会覆盖当天快照，只保留最新值。趋势图保留最近约一年数据。

当前总资产计算是轻量版本：负债类型会从净资产中扣除；多币种资产会提示风险，但还没有自动汇率换算。

## 权限说明

`AndroidManifest.xml` 里使用了：

```xml
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

这是为了个人自用时能根据你手动输入的任意包名拉起外部 App。这个权限对 Play Store 上架比较敏感。如果未来要公开发布，建议改成固定 allowlist 的 `<queries>` 配置，或只支持用户填写 deep link。
