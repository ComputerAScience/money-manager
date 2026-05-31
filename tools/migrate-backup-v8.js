#!/usr/bin/env node

const fs = require("fs");

const SCHEMA_VERSION = 8;
const BANK_ACCOUNT = "银行账户";
const INVESTMENT_ACCOUNT = "投资账户";
const BANK_DEPOSIT = "银行存款";
const BANK_WEALTH = "银行理财";
const BROKER_HOLDING = "券商持仓";
const STOCK_HOLDING = "股票持仓";
const BROKER_CASH = "券商现金";
const DEBT = "负债";

const DEFAULT_CATEGORIES = new Set([
  BANK_ACCOUNT,
  INVESTMENT_ACCOUNT,
  "基金",
  "加密资产",
  "房产",
  DEBT,
  "其他",
]);

const DEFAULT_INVESTMENT_CATEGORIES = [
  INVESTMENT_ACCOUNT,
  "基金",
  "加密资产",
  BROKER_HOLDING,
  STOCK_HOLDING,
  BROKER_CASH,
];

function migrateBackup(backup) {
  if (!backup || typeof backup !== "object") {
    throw new Error("Backup root must be a JSON object.");
  }
  if (!Array.isArray(backup.assets)) {
    throw new Error("Backup JSON must contain an assets array.");
  }

  const report = {
    changedAssets: 0,
    generatedDebts: 0,
    customCategories: 0,
  };
  const generatedAssets = [];

  for (const asset of backup.assets) {
    let changed = false;
    changed = migrateLegacyAccountAsset(asset) || changed;
    changed = normalizeInstitution(asset) || changed;
    if (asset.category === BANK_ACCOUNT) {
      const result = flattenBankAccount(asset);
      changed = result.changed || changed;
      if (result.debt) {
        generatedAssets.push(result.debt);
        report.generatedDebts += 1;
      }
    } else if (asset.category === INVESTMENT_ACCOUNT) {
      changed = flattenInvestmentAccount(asset) || changed;
    }
    if (changed) {
      report.changedAssets += 1;
    }
  }

  backup.assets.push(...generatedAssets);
  ensureSettings(backup, report);
  backup.version = SCHEMA_VERSION;
  backup.schemaVersion = SCHEMA_VERSION;
  return report;
}

function migrateLegacyAccountAsset(asset) {
  let changed = false;
  const category = clean(asset.category);
  if (category === "银行" || category === BANK_DEPOSIT) {
    if (!hasText(asset.bankDepositAmount)) {
      asset.bankDepositAmount = clean(asset.amount);
    }
    asset.category = BANK_ACCOUNT;
    changed = true;
  } else if (category === BANK_WEALTH) {
    if (!hasText(asset.bankWealthAmount)) {
      asset.bankWealthAmount = clean(asset.amount);
    }
    asset.category = BANK_ACCOUNT;
    changed = true;
  } else if (category === "券商" || category === BROKER_HOLDING || category === STOCK_HOLDING) {
    if (!hasText(asset.investmentHoldingAmount)) {
      asset.investmentHoldingAmount = clean(asset.amount);
    }
    asset.category = INVESTMENT_ACCOUNT;
    changed = true;
  } else if (category === BROKER_CASH) {
    if (!hasText(asset.investmentCashAmount)) {
      asset.investmentCashAmount = clean(asset.amount);
    }
    asset.category = INVESTMENT_ACCOUNT;
    changed = true;
  }

  if (asset.category === BANK_ACCOUNT && !hasBankBreakdown(asset) && hasText(asset.amount)) {
    asset.bankDepositAmount = clean(asset.amount);
    changed = true;
  }
  if (asset.category === INVESTMENT_ACCOUNT && !hasInvestmentBreakdown(asset) && hasText(asset.amount)) {
    asset.investmentHoldingAmount = clean(asset.amount);
    changed = true;
  }
  return changed;
}

function normalizeInstitution(asset) {
  const institution = clean(asset.institution);
  if (!institution.includes("待绑定")) {
    return false;
  }
  asset.institution = hasText(asset.appName) ? clean(asset.appName) : "";
  return true;
}

function flattenBankAccount(asset) {
  if (!hasBankBreakdown(asset)) {
    return { changed: false, debt: null };
  }

  const deposit = positiveAmount(asset.bankDepositAmount);
  const wealth = positiveAmount(asset.bankWealthAmount);
  const debtAmount = positiveAmount(asset.bankDebtAmount);
  const gross = deposit + wealth;
  if (gross > 0 || !hasText(asset.amount)) {
    asset.amount = formatStoredAmount(gross);
  }
  const debt = debtAmount > 0 ? derivedDebtAsset(asset, debtAmount) : null;
  asset.bankDepositAmount = "";
  asset.bankWealthAmount = "";
  asset.bankDebtAmount = "";
  return { changed: true, debt };
}

function flattenInvestmentAccount(asset) {
  const hasBreakdown = hasInvestmentBreakdown(asset) || hasText(asset.investmentPositions);
  if (!hasBreakdown) {
    return false;
  }

  const holding = positiveAmount(asset.investmentHoldingAmount);
  const cash = positiveAmount(asset.investmentCashAmount);
  const total = holding + cash;
  if (total > 0 || !hasText(asset.amount)) {
    asset.amount = formatStoredAmount(total);
  }
  if (hasText(asset.investmentPositions)) {
    asset.note = appendNote(asset.note, `旧版持股备注：${clean(asset.investmentPositions)}`);
  }
  asset.investmentHoldingAmount = "";
  asset.investmentCashAmount = "";
  asset.investmentPositions = "";
  return true;
}

function derivedDebtAsset(source, debtAmount) {
  return {
    id: `asset-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`,
    name: `${hasText(source.name) ? clean(source.name) : "银行账户"}负债`,
    category: DEBT,
    institution: clean(source.institution),
    amount: formatStoredAmount(debtAmount),
    currency: clean(source.currency) || "CNY",
    updateEveryDays: positiveInteger(source.updateEveryDays, 7),
    lastUpdatedAt: positiveInteger(source.lastUpdatedAt, 0),
    appName: clean(source.appName),
    packageName: clean(source.packageName),
    launchUri: clean(source.launchUri),
    note: appendNote(source.note, "由旧版银行账户负债明细迁移生成。"),
    bankDepositAmount: "",
    bankWealthAmount: "",
    bankDebtAmount: "",
    investmentHoldingAmount: "",
    investmentCashAmount: "",
    investmentPositions: "",
  };
}

function ensureSettings(backup, report) {
  if (!backup.settings || typeof backup.settings !== "object") {
    backup.settings = {};
  }
  const settings = backup.settings;
  if (!Array.isArray(settings.customCategories)) {
    settings.customCategories = [];
  }
  if (!Array.isArray(settings.investmentCategories)) {
    settings.investmentCategories = DEFAULT_INVESTMENT_CATEGORIES.slice();
  }

  const custom = new Set(settings.customCategories.map(clean).filter(Boolean));
  for (const asset of backup.assets) {
    const category = clean(asset.category);
    if (category && !DEFAULT_CATEGORIES.has(category) && !custom.has(category)) {
      settings.customCategories.push(category);
      custom.add(category);
      report.customCategories += 1;
    }
  }
}

function hasBankBreakdown(asset) {
  return hasText(asset.bankDepositAmount) || hasText(asset.bankWealthAmount) || hasText(asset.bankDebtAmount);
}

function hasInvestmentBreakdown(asset) {
  return hasText(asset.investmentHoldingAmount) || hasText(asset.investmentCashAmount);
}

function parseAmount(raw) {
  const value = clean(raw).replace(/[,¥$€￥]/g, "");
  if (!value) {
    return 0;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function positiveAmount(raw) {
  return Math.abs(parseAmount(raw));
}

function positiveInteger(raw, fallback) {
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed) : fallback;
}

function formatStoredAmount(value) {
  return String(Math.max(0, Number(value) || 0)).replace(/(\.\d*?)0+$/, "$1").replace(/\.$/, "");
}

function appendNote(note, addition) {
  const base = clean(note);
  const extra = clean(addition);
  if (!extra) {
    return base;
  }
  if (!base) {
    return extra;
  }
  return base.includes(extra) ? base : `${base}\n${extra}`;
}

function hasText(value) {
  return clean(value).length > 0;
}

function clean(value) {
  return value == null ? "" : String(value).trim();
}

function usage() {
  return [
    "Usage:",
    "  node tools/migrate-backup-v8.js <input.json> [output.json]",
    "",
    "When output.json is omitted, migrated JSON is printed to stdout.",
  ].join("\n");
}

function main(argv) {
  const [, , inputPath, outputPath] = argv;
  if (!inputPath || inputPath === "-h" || inputPath === "--help") {
    console.error(usage());
    process.exit(inputPath ? 0 : 1);
  }

  const backup = JSON.parse(fs.readFileSync(inputPath, "utf8"));
  const report = migrateBackup(backup);
  const output = `${JSON.stringify(backup, null, 2)}\n`;
  if (outputPath) {
    fs.writeFileSync(outputPath, output, "utf8");
  } else {
    process.stdout.write(output);
  }
  console.error(
    `Migrated to schema ${SCHEMA_VERSION}: changed=${report.changedAssets}, generatedDebts=${report.generatedDebts}, customCategories=${report.customCategories}`,
  );
}

if (require.main === module) {
  main(process.argv);
}

module.exports = { migrateBackup };
