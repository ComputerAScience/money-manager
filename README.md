# Money Manager

一个可以直接部署到 GitHub Pages 的个人资产管理 Web 应用。它是纯静态页面，默认把资产数据保存在当前浏览器的 `localStorage` 中，不需要数据库、服务器或 API 密钥。

## 功能

- 记录现金、存款、基金、股票、加密资产、房产、负债和其他资产
- 按基准币种实时汇总净资产、资产总额、负债和今日浮动
- 按类型查看资产配置，并观察现金比例、低流动性比例和最大持仓
- 支持本地 JSON 导入、导出备份
- 支持外汇汇率和 CoinGecko 加密资产价格的浏览器端刷新
- 手动资产价格修改后，所有统计会即时重算

## 本地使用

直接用浏览器打开 `index.html` 即可。也可以在仓库目录启动一个静态服务器：

```bash
python3 -m http.server 4173
```

然后访问 `http://localhost:4173`。

## 部署到 GitHub Pages

1. 将代码推送到仓库的 `master` 分支。
2. 打开仓库的 `Settings -> Pages`。
3. 在 `Build and deployment` 中选择 `GitHub Actions`。
4. 等待 `Deploy to GitHub Pages` 工作流完成。

部署完成后，GitHub 会在 workflow 页面或 Pages 设置中显示站点地址。

## 数据和隐私

资产明细只保存在浏览器本地。换浏览器、清理站点数据或更换设备前，先用页面右上角的“导出”保存 JSON 备份。

外部行情刷新只会请求公开行情接口：

- `https://api.frankfurter.app` 用于外汇汇率
- `https://api.coingecko.com` 用于加密资产价格

股票、基金、房产和负债等资产默认采用手动价格。后续如果需要接入券商、银行或付费行情 API，可以在静态前端之外加一个带鉴权的后端服务。

## Android App

仓库里也包含一个原生 Android 版本，位于 `android/`。它的目标更低也更稳：记录每项资产的更新时间，并在需要更新时一键打开对应银行、券商或钱包 App。

Android 版本已经包含一体式底部导航、总览里的资产比例图、行动中心、总资产趋势、分布变化、单项资产趋势、本地快照、常用币种实时汇率、顶部设置入口、应用内下载更新、隐私模式、本地备份恢复，以及可折叠、可搜索和可筛选的资产管理区域。

详见 [android/README.md](android/README.md)。

### 扫码下载 APK

GitHub Actions 每次构建后会更新固定的 debug APK 下载地址：

[下载最新版 Android APK](https://github.com/ComputerAScience/money-manager/releases/download/android-debug-latest/money-manager-debug.apk)

手机打开 GitHub 这个 README，或用相机扫描下面的二维码即可下载：

<a href="https://github.com/ComputerAScience/money-manager/releases/download/android-debug-latest/money-manager-debug.apk">
  <img src="https://api.qrserver.com/v1/create-qr-code/?size=220x220&amp;margin=12&amp;data=https%3A%2F%2Fgithub.com%2FComputerAScience%2Fmoney-manager%2Freleases%2Fdownload%2Fandroid-debug-latest%2Fmoney-manager-debug.apk" alt="扫码下载 Money Manager Android APK" width="220" height="220">
</a>

这个 APK 是 debug 版，适合个人自用和测试。手机安装时可能需要允许“安装未知来源应用”。

`0.3.2` 之后 GitHub Actions 会缓存 debug 签名，后续下载通常可以直接覆盖安装。若从旧签名版本更新时仍提示“签名不一致”，需要先卸载旧版再安装一次新版。
