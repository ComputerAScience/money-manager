#!/usr/bin/env node

const assert = require("assert");
const { migrateBackup } = require("./migrate-backup-v8");

const backup = {
  version: 7,
  schemaVersion: 7,
  assets: [
    {
      id: "bank-1",
      name: "招商银行",
      category: "银行账户",
      institution: "待绑定银行 App",
      amount: "280",
      currency: "CNY",
      updateEveryDays: 7,
      lastUpdatedAt: 1710000000000,
      appName: "招商银行",
      packageName: "cmb.pb",
      launchUri: "",
      note: "主账户",
      bankDepositAmount: "100",
      bankWealthAmount: "200",
      bankDebtAmount: "50",
      investmentHoldingAmount: "",
      investmentCashAmount: "",
      investmentPositions: "",
    },
    {
      id: "broker-1",
      name: "券商账户",
      category: "投资账户",
      institution: "某券商",
      amount: "0",
      currency: "CNY",
      updateEveryDays: 1,
      lastUpdatedAt: 1710000000000,
      appName: "某券商",
      packageName: "broker.app",
      launchUri: "",
      note: "",
      bankDepositAmount: "",
      bankWealthAmount: "",
      bankDebtAmount: "",
      investmentHoldingAmount: "500",
      investmentCashAmount: "30",
      investmentPositions: "AAPL 2",
    },
    {
      id: "custom-1",
      name: "美股账户",
      category: "美股",
      institution: "某券商",
      amount: "1000",
      currency: "USD",
      updateEveryDays: 1,
      lastUpdatedAt: 1710000000000,
      appName: "某券商",
      packageName: "broker.app",
      launchUri: "",
      note: "",
    },
  ],
  settings: {
    baseCurrency: "CNY",
    ratesToBase: { CNY: 1 },
    allocationTargets: {},
  },
};

const firstReport = migrateBackup(backup);
assert.strictEqual(backup.schemaVersion, 8);
assert.strictEqual(backup.version, 8);
assert.strictEqual(firstReport.changedAssets, 2);
assert.strictEqual(firstReport.generatedDebts, 1);
assert.strictEqual(firstReport.customCategories, 1);

const bank = backup.assets.find((asset) => asset.id === "bank-1");
assert.strictEqual(bank.institution, "招商银行");
assert.strictEqual(bank.amount, "300");
assert.strictEqual(bank.bankDepositAmount, "");
assert.strictEqual(bank.bankWealthAmount, "");
assert.strictEqual(bank.bankDebtAmount, "");

const debt = backup.assets.find((asset) => asset.category === "负债" && asset.name === "招商银行负债");
assert.ok(debt);
assert.strictEqual(debt.amount, "50");
assert.strictEqual(debt.institution, "招商银行");
assert.strictEqual(debt.packageName, "cmb.pb");

const broker = backup.assets.find((asset) => asset.id === "broker-1");
assert.strictEqual(broker.amount, "530");
assert.strictEqual(broker.investmentHoldingAmount, "");
assert.strictEqual(broker.investmentCashAmount, "");
assert.strictEqual(broker.investmentPositions, "");
assert.ok(broker.note.includes("旧版持股备注：AAPL 2"));

assert.ok(backup.settings.customCategories.includes("美股"));
assert.ok(backup.settings.investmentCategories.includes("投资账户"));

const assetCount = backup.assets.length;
const secondReport = migrateBackup(backup);
assert.strictEqual(backup.assets.length, assetCount);
assert.strictEqual(secondReport.generatedDebts, 0);
assert.strictEqual(secondReport.customCategories, 0);

console.log("migrate-backup-v8 tests passed");
