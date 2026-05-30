const STORAGE_KEY = "money-manager-state-v1";

const CURRENCIES = ["CNY", "USD", "HKD", "EUR", "JPY"];
const FALLBACK_RATES_TO_CNY = {
  CNY: 1,
  USD: 7.2,
  HKD: 0.92,
  EUR: 7.85,
  JPY: 0.05,
};

const TYPE_META = {
  cash: { label: "现金", color: "#126b5f" },
  deposit: { label: "存款", color: "#4f8a38" },
  stock: { label: "股票", color: "#315f9f" },
  fund: { label: "基金", color: "#3b7f91" },
  crypto: { label: "加密资产", color: "#b85c2f" },
  property: { label: "房产", color: "#72518a" },
  debt: { label: "负债", color: "#b74955" },
  other: { label: "其他", color: "#9a7720" },
};

const LIQUIDITY_META = {
  high: "高",
  medium: "中",
  low: "低",
};

const ICONS = {
  refresh: '<svg viewBox="0 0 24 24" fill="none"><path d="M21 12a9 9 0 0 1-15.6 6.1L3 16"/><path d="M3 21v-5h5"/><path d="M3 12a9 9 0 0 1 15.6-6.1L21 8"/><path d="M21 3v5h-5"/></svg>',
  download: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 3v12"/><path d="m7 10 5 5 5-5"/><path d="M5 21h14"/></svg>',
  upload: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 21V9"/><path d="m7 14 5-5 5 5"/><path d="M5 3h14"/></svg>',
  save: '<svg viewBox="0 0 24 24" fill="none"><path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2Z"/><path d="M17 21v-8H7v8"/><path d="M7 3v5h8"/></svg>',
  x: '<svg viewBox="0 0 24 24" fill="none"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>',
  edit: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg>',
  trash: '<svg viewBox="0 0 24 24" fill="none"><path d="M3 6h18"/><path d="M8 6V4h8v2"/><path d="m19 6-1 14H6L5 6"/><path d="M10 11v5"/><path d="M14 11v5"/></svg>',
};

const SAMPLE_ASSETS = [
  {
    id: "sample-cash",
    name: "现金备用金",
    type: "cash",
    institution: "钱包和活期",
    currency: "CNY",
    quantity: 1,
    price: 28000,
    costBasis: 28000,
    dayChangePct: 0,
    liquidity: "high",
    quoteProvider: "manual",
    symbol: "",
    note: "日常周转",
    lastUpdated: null,
  },
  {
    id: "sample-deposit",
    name: "定期存款",
    type: "deposit",
    institution: "银行",
    currency: "CNY",
    quantity: 1,
    price: 120000,
    costBasis: 120000,
    dayChangePct: 0,
    liquidity: "medium",
    quoteProvider: "manual",
    symbol: "",
    note: "稳健资金",
    lastUpdated: null,
  },
  {
    id: "sample-fund",
    name: "沪深300 ETF",
    type: "fund",
    institution: "证券账户",
    currency: "CNY",
    quantity: 6000,
    price: 4.12,
    costBasis: 3.92,
    dayChangePct: 0.56,
    liquidity: "high",
    quoteProvider: "manual",
    symbol: "",
    note: "权益配置",
    lastUpdated: null,
  },
  {
    id: "sample-stock",
    name: "纳指 ETF",
    type: "stock",
    institution: "海外券商",
    currency: "USD",
    quantity: 80,
    price: 180.5,
    costBasis: 150,
    dayChangePct: 1.2,
    liquidity: "high",
    quoteProvider: "manual",
    symbol: "",
    note: "手动更新价格",
    lastUpdated: null,
  },
  {
    id: "sample-btc",
    name: "Bitcoin",
    type: "crypto",
    institution: "冷钱包",
    currency: "USD",
    quantity: 0.12,
    price: 68000,
    costBasis: 42000,
    dayChangePct: 2.1,
    liquidity: "high",
    quoteProvider: "coingecko",
    symbol: "bitcoin",
    note: "CoinGecko 自动刷新",
    lastUpdated: null,
  },
  {
    id: "sample-property",
    name: "房产估值",
    type: "property",
    institution: "家庭资产",
    currency: "CNY",
    quantity: 1,
    price: 1500000,
    costBasis: 1380000,
    dayChangePct: 0,
    liquidity: "low",
    quoteProvider: "manual",
    symbol: "",
    note: "按季度评估",
    lastUpdated: null,
  },
  {
    id: "sample-mortgage",
    name: "房贷余额",
    type: "debt",
    institution: "银行",
    currency: "CNY",
    quantity: 1,
    price: 620000,
    costBasis: 620000,
    dayChangePct: 0,
    liquidity: "low",
    quoteProvider: "manual",
    symbol: "",
    note: "剩余本金",
    lastUpdated: null,
  },
];

let state = loadState();

const els = {
  netWorth: document.querySelector("#net-worth"),
  netWorthSub: document.querySelector("#net-worth-sub"),
  grossAssets: document.querySelector("#gross-assets"),
  grossAssetsSub: document.querySelector("#gross-assets-sub"),
  liabilities: document.querySelector("#liabilities"),
  liabilitiesSub: document.querySelector("#liabilities-sub"),
  dayChange: document.querySelector("#day-change"),
  dayChangeSub: document.querySelector("#day-change-sub"),
  baseCurrency: document.querySelector("#base-currency"),
  search: document.querySelector("#asset-search"),
  typeFilter: document.querySelector("#type-filter"),
  liquidityFilter: document.querySelector("#liquidity-filter"),
  form: document.querySelector("#asset-form"),
  formTitle: document.querySelector("#form-title"),
  clearForm: document.querySelector("#clear-form"),
  table: document.querySelector("#asset-table"),
  empty: document.querySelector("#empty-state"),
  allocationDonut: document.querySelector("#allocation-donut"),
  allocationList: document.querySelector("#allocation-list"),
  insights: document.querySelector("#insights"),
  marketStatus: document.querySelector("#market-status"),
  refreshMarket: document.querySelector("#refresh-market"),
  exportData: document.querySelector("#export-data"),
  importData: document.querySelector("#import-data"),
  fields: {
    name: document.querySelector("#asset-name"),
    type: document.querySelector("#asset-type"),
    institution: document.querySelector("#asset-institution"),
    currency: document.querySelector("#asset-currency"),
    quantity: document.querySelector("#asset-quantity"),
    price: document.querySelector("#asset-price"),
    costBasis: document.querySelector("#asset-cost"),
    dayChangePct: document.querySelector("#asset-day-change"),
    liquidity: document.querySelector("#asset-liquidity"),
    quoteProvider: document.querySelector("#asset-quote-provider"),
    symbol: document.querySelector("#asset-symbol"),
    note: document.querySelector("#asset-note"),
  },
};

init();

function init() {
  hydrateIcons();
  populateControls();
  bindEvents();
  resetForm();
  render();
  window.setInterval(() => refreshMarketData({ silent: true }), 120000);
}

function loadState() {
  const baseState = {
    baseCurrency: "CNY",
    assets: cloneAssets(SAMPLE_ASSETS),
    ratesToCny: { ...FALLBACK_RATES_TO_CNY },
    lastMarketRefresh: null,
    filters: {
      query: "",
      type: "all",
      liquidity: "all",
    },
    editingId: null,
  };

  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return baseState;
    const saved = JSON.parse(raw);
    if (!saved || !Array.isArray(saved.assets)) return baseState;
    return {
      ...baseState,
      ...saved,
      assets: saved.assets.map(normalizeAsset),
      ratesToCny: {
        ...FALLBACK_RATES_TO_CNY,
        ...(saved.ratesToCny || {}),
      },
      filters: {
        ...baseState.filters,
        ...(saved.filters || {}),
      },
      editingId: null,
    };
  } catch (error) {
    console.warn("Failed to load saved portfolio", error);
    return baseState;
  }
}

function cloneAssets(assets) {
  return assets.map((asset) => ({ ...asset }));
}

function persist() {
  const payload = {
    baseCurrency: state.baseCurrency,
    assets: state.assets,
    ratesToCny: state.ratesToCny,
    lastMarketRefresh: state.lastMarketRefresh,
    filters: state.filters,
  };
  localStorage.setItem(STORAGE_KEY, JSON.stringify(payload));
}

function populateControls() {
  els.baseCurrency.innerHTML = CURRENCIES.map(
    (currency) => `<button type="button" data-currency="${currency}" aria-pressed="false">${currency}</button>`,
  ).join("");

  fillSelect(
    els.typeFilter,
    [{ value: "all", label: "全部类型" }, ...Object.entries(TYPE_META).map(([value, meta]) => ({ value, label: meta.label }))],
  );
  fillSelect(
    els.liquidityFilter,
    [
      { value: "all", label: "全部流动性" },
      ...Object.entries(LIQUIDITY_META).map(([value, label]) => ({ value, label })),
    ],
  );
  fillSelect(
    els.fields.type,
    Object.entries(TYPE_META).map(([value, meta]) => ({ value, label: meta.label })),
  );
  fillSelect(
    els.fields.currency,
    CURRENCIES.map((currency) => ({ value: currency, label: currency })),
  );
  fillSelect(
    els.fields.liquidity,
    Object.entries(LIQUIDITY_META).map(([value, label]) => ({ value, label })),
  );
}

function fillSelect(select, options) {
  select.innerHTML = options.map((option) => `<option value="${option.value}">${option.label}</option>`).join("");
}

function bindEvents() {
  els.baseCurrency.addEventListener("click", (event) => {
    const button = event.target.closest("[data-currency]");
    if (!button) return;
    state.baseCurrency = button.dataset.currency;
    persist();
    render();
  });

  els.search.addEventListener("input", () => {
    state.filters.query = els.search.value.trim();
    persist();
    render();
  });

  els.typeFilter.addEventListener("change", () => {
    state.filters.type = els.typeFilter.value;
    persist();
    render();
  });

  els.liquidityFilter.addEventListener("change", () => {
    state.filters.liquidity = els.liquidityFilter.value;
    persist();
    render();
  });

  els.form.addEventListener("submit", (event) => {
    event.preventDefault();
    saveAssetFromForm();
  });

  els.clearForm.addEventListener("click", resetForm);
  els.refreshMarket.addEventListener("click", () => refreshMarketData({ silent: false }));
  els.exportData.addEventListener("click", exportPortfolio);
  els.importData.addEventListener("change", importPortfolio);

  els.table.addEventListener("click", (event) => {
    const button = event.target.closest("[data-action]");
    if (!button) return;
    const id = button.dataset.id;
    if (button.dataset.action === "edit") editAsset(id);
    if (button.dataset.action === "delete") deleteAsset(id);
  });
}

function saveAssetFromForm() {
  const asset = normalizeAsset({
    id: state.editingId || createId(),
    name: els.fields.name.value.trim(),
    type: els.fields.type.value,
    institution: els.fields.institution.value.trim(),
    currency: els.fields.currency.value,
    quantity: toNumber(els.fields.quantity.value),
    price: toNumber(els.fields.price.value),
    costBasis: toNumber(els.fields.costBasis.value),
    dayChangePct: toNumber(els.fields.dayChangePct.value),
    liquidity: els.fields.liquidity.value,
    quoteProvider: els.fields.quoteProvider.value,
    symbol: els.fields.symbol.value.trim(),
    note: els.fields.note.value.trim(),
    lastUpdated: state.editingId ? findAsset(state.editingId)?.lastUpdated || null : null,
  });

  if (!asset.name) return;

  if (state.editingId) {
    state.assets = state.assets.map((item) => (item.id === state.editingId ? asset : item));
  } else {
    state.assets = [asset, ...state.assets];
  }

  persist();
  resetForm();
  render();
}

function normalizeAsset(asset) {
  const normalizedId = String(asset.id || createId()).replace(/[^\w-]/g, "").slice(0, 80) || createId();
  return {
    id: normalizedId,
    name: String(asset.name || "未命名资产").slice(0, 40),
    type: TYPE_META[asset.type] ? asset.type : "other",
    institution: String(asset.institution || "").slice(0, 40),
    currency: CURRENCIES.includes(asset.currency) ? asset.currency : "CNY",
    quantity: toNumber(asset.quantity),
    price: toNumber(asset.price),
    costBasis: toNumber(asset.costBasis),
    dayChangePct: toNumber(asset.dayChangePct),
    liquidity: LIQUIDITY_META[asset.liquidity] ? asset.liquidity : "medium",
    quoteProvider: asset.quoteProvider === "coingecko" ? "coingecko" : "manual",
    symbol: String(asset.symbol || "").slice(0, 60),
    note: String(asset.note || "").slice(0, 140),
    lastUpdated: asset.lastUpdated || null,
  };
}

function resetForm() {
  state.editingId = null;
  els.form.reset();
  els.formTitle.textContent = "新增资产";
  els.fields.type.value = "cash";
  els.fields.currency.value = state.baseCurrency;
  els.fields.quantity.value = 1;
  els.fields.price.value = "";
  els.fields.costBasis.value = "";
  els.fields.dayChangePct.value = 0;
  els.fields.liquidity.value = "high";
  els.fields.quoteProvider.value = "manual";
}

function editAsset(id) {
  const asset = findAsset(id);
  if (!asset) return;
  state.editingId = id;
  els.formTitle.textContent = "编辑资产";
  Object.entries(els.fields).forEach(([key, field]) => {
    field.value = asset[key] ?? "";
  });
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function deleteAsset(id) {
  const asset = findAsset(id);
  if (!asset) return;
  const ok = window.confirm(`删除「${asset.name}」？`);
  if (!ok) return;
  state.assets = state.assets.filter((item) => item.id !== id);
  if (state.editingId === id) resetForm();
  persist();
  render();
}

function findAsset(id) {
  return state.assets.find((asset) => asset.id === id);
}

function render() {
  els.search.value = state.filters.query;
  els.typeFilter.value = state.filters.type;
  els.liquidityFilter.value = state.filters.liquidity;

  renderBaseCurrency();
  renderSummary();
  renderAllocation();
  renderInsights();
  renderTable();
  renderMarketStatus();
}

function renderBaseCurrency() {
  els.baseCurrency.querySelectorAll("[data-currency]").forEach((button) => {
    button.setAttribute("aria-pressed", String(button.dataset.currency === state.baseCurrency));
  });
}

function getEnrichedAssets() {
  return state.assets.map((asset) => enrichAsset(asset));
}

function getVisibleAssets() {
  const query = state.filters.query.toLowerCase();
  return getEnrichedAssets()
    .filter(({ asset }) => {
      const matchesQuery =
        !query ||
        [asset.name, asset.institution, asset.note, asset.symbol]
          .filter(Boolean)
          .some((text) => text.toLowerCase().includes(query));
      const matchesType = state.filters.type === "all" || asset.type === state.filters.type;
      const matchesLiquidity = state.filters.liquidity === "all" || asset.liquidity === state.filters.liquidity;
      return matchesQuery && matchesType && matchesLiquidity;
    })
    .sort((a, b) => Math.abs(b.valueBase) - Math.abs(a.valueBase));
}

function enrichAsset(asset) {
  const valueOriginal = getSignedValue(asset);
  const valueBase = convertToBase(valueOriginal, asset.currency);
  const costOriginal = getCostValue(asset);
  const pnlOriginal = asset.type === "debt" || costOriginal === 0 ? 0 : valueOriginal - costOriginal;
  const pnlBase = convertToBase(pnlOriginal, asset.currency);
  const todayOriginal = asset.type === "debt" ? 0 : valueOriginal * (toNumber(asset.dayChangePct) / 100);
  const todayBase = convertToBase(todayOriginal, asset.currency);
  const pnlPct = costOriginal > 0 ? (pnlOriginal / costOriginal) * 100 : 0;

  return {
    asset,
    valueOriginal,
    valueBase,
    costOriginal,
    pnlBase,
    pnlPct,
    todayBase,
  };
}

function getSignedValue(asset) {
  const raw = toNumber(asset.quantity) * toNumber(asset.price);
  return asset.type === "debt" ? -Math.abs(raw) : raw;
}

function getCostValue(asset) {
  if (asset.type === "debt") return 0;
  return toNumber(asset.quantity) * toNumber(asset.costBasis);
}

function convertToBase(amount, currency) {
  const fromRate = state.ratesToCny[currency] || FALLBACK_RATES_TO_CNY[currency] || 1;
  const baseRate = state.ratesToCny[state.baseCurrency] || FALLBACK_RATES_TO_CNY[state.baseCurrency] || 1;
  return (amount * fromRate) / baseRate;
}

function getTotals() {
  return getEnrichedAssets().reduce(
    (totals, item) => {
      if (item.valueBase >= 0) {
        totals.gross += item.valueBase;
      } else {
        totals.liabilities += Math.abs(item.valueBase);
      }
      totals.net += item.valueBase;
      totals.today += item.todayBase;
      totals.pnl += item.pnlBase;
      return totals;
    },
    { gross: 0, liabilities: 0, net: 0, today: 0, pnl: 0 },
  );
}

function renderSummary() {
  const totals = getTotals();
  els.netWorth.textContent = formatCurrency(totals.net, state.baseCurrency);
  els.netWorthSub.textContent = `${state.assets.length} 笔资产，累计收益 ${formatSignedCurrency(totals.pnl, state.baseCurrency)}`;
  els.grossAssets.textContent = formatCurrency(totals.gross, state.baseCurrency);
  els.grossAssetsSub.textContent = `负债率 ${formatPercent(totals.gross ? totals.liabilities / totals.gross * 100 : 0)}`;
  els.liabilities.textContent = formatCurrency(totals.liabilities, state.baseCurrency);
  els.liabilitiesSub.textContent = totals.liabilities ? "已计入净资产" : "无负债记录";
  els.dayChange.textContent = formatSignedCurrency(totals.today, state.baseCurrency);
  els.dayChange.className = totals.today >= 0 ? "gain" : "loss";
  els.dayChangeSub.textContent = `约 ${formatPercent(totals.gross ? totals.today / totals.gross * 100 : 0)}`;
}

function renderAllocation() {
  const entriesByType = new Map();
  getEnrichedAssets().forEach((item) => {
    const type = item.valueBase < 0 ? "debt" : item.asset.type;
    const value = Math.abs(item.valueBase);
    if (value === 0) return;
    entriesByType.set(type, (entriesByType.get(type) || 0) + value);
  });

  const entries = [...entriesByType.entries()]
    .map(([type, value]) => ({
      type,
      value,
      label: TYPE_META[type]?.label || TYPE_META.other.label,
      color: TYPE_META[type]?.color || TYPE_META.other.color,
    }))
    .sort((a, b) => b.value - a.value);

  const total = entries.reduce((sum, entry) => sum + entry.value, 0);
  if (!total) {
    els.allocationDonut.style.background = "conic-gradient(#d9ddd5 0 100%)";
    els.allocationList.innerHTML = '<p class="muted">暂无配置数据</p>';
    return;
  }

  let cursor = 0;
  const slices = entries.map((entry) => {
    const start = cursor;
    cursor += (entry.value / total) * 100;
    return `${entry.color} ${start}% ${cursor}%`;
  });
  els.allocationDonut.style.background = `conic-gradient(${slices.join(", ")})`;
  els.allocationList.innerHTML = entries
    .map(
      (entry) => `
        <div class="allocation-item">
          <span class="allocation-name">
            <span class="swatch" style="background:${entry.color}"></span>
            <span>${escapeHtml(entry.label)}</span>
          </span>
          <span class="allocation-value">${formatPercent((entry.value / total) * 100)}</span>
        </div>
      `,
    )
    .join("");
}

function renderInsights() {
  const items = getEnrichedAssets();
  const positive = items.filter((item) => item.valueBase > 0).sort((a, b) => b.valueBase - a.valueBase);
  const totals = getTotals();
  const cash = items
    .filter((item) => item.asset.type === "cash" || item.asset.type === "deposit")
    .reduce((sum, item) => sum + Math.max(0, item.valueBase), 0);
  const lowLiquidity = items
    .filter((item) => item.asset.liquidity === "low")
    .reduce((sum, item) => sum + Math.max(0, item.valueBase), 0);
  const liveCount = state.assets.filter((asset) => asset.quoteProvider === "coingecko" && asset.symbol).length;

  const rows = [
    ["最大持仓", positive[0] ? `${positive[0].asset.name} · ${formatCurrency(positive[0].valueBase, state.baseCurrency)}` : "暂无"],
    ["现金/存款", formatPercent(totals.gross ? (cash / totals.gross) * 100 : 0)],
    ["低流动性", formatPercent(totals.gross ? (lowLiquidity / totals.gross) * 100 : 0)],
    ["自动行情", `${liveCount} 笔`],
  ];

  els.insights.innerHTML = rows.map(([label, value]) => `<li><span>${label}</span><strong>${escapeHtml(value)}</strong></li>`).join("");
}

function renderTable() {
  const visible = getVisibleAssets();
  els.empty.hidden = visible.length > 0;
  els.table.innerHTML = visible
    .map(({ asset, valueOriginal, valueBase, pnlBase, pnlPct }) => {
      const typeMeta = TYPE_META[asset.type] || TYPE_META.other;
      const pnlClass = pnlBase >= 0 ? "gain" : "loss";
      const provider = asset.quoteProvider === "coingecko" ? "CoinGecko" : "手动";
      return `
        <tr>
          <td>
            <div class="asset-name">
              <strong>${escapeHtml(asset.name)}</strong>
              <small>${escapeHtml([asset.institution, asset.note].filter(Boolean).join(" · ") || "未填写机构")}</small>
            </div>
          </td>
          <td><span class="tag ${asset.type}">${escapeHtml(typeMeta.label)}</span></td>
          <td>
            <div class="amount">
              <strong>${formatNumber(asset.quantity, 6)}</strong>
              <span class="amount-sub">${escapeHtml(asset.currency)}</span>
            </div>
          </td>
          <td>
            <div class="amount">
              <strong>${formatCurrency(asset.price, asset.currency)}</strong>
              <span class="amount-sub">${provider}${asset.symbol ? ` · ${escapeHtml(asset.symbol)}` : ""}</span>
            </div>
          </td>
          <td>
            <div class="amount">
              <strong>${formatCurrency(valueBase, state.baseCurrency)}</strong>
              <span class="amount-sub">${formatCurrency(valueOriginal, asset.currency)}</span>
            </div>
          </td>
          <td>
            <div class="amount">
              <strong class="${pnlClass}">${formatSignedCurrency(pnlBase, state.baseCurrency)}</strong>
              <span class="amount-sub">${formatPercent(pnlPct)}</span>
            </div>
          </td>
          <td><span class="tag">${escapeHtml(LIQUIDITY_META[asset.liquidity])}</span></td>
          <td>
            <div class="row-actions">
              <button class="table-action" type="button" data-action="edit" data-id="${asset.id}" title="编辑" aria-label="编辑 ${escapeHtml(asset.name)}">
                <span class="icon" data-icon="edit" aria-hidden="true"></span>
              </button>
              <button class="table-action" type="button" data-action="delete" data-id="${asset.id}" title="删除" aria-label="删除 ${escapeHtml(asset.name)}">
                <span class="icon" data-icon="trash" aria-hidden="true"></span>
              </button>
            </div>
          </td>
        </tr>
      `;
    })
    .join("");
  hydrateIcons(els.table);
}

function renderMarketStatus() {
  els.marketStatus.classList.remove("is-live", "is-error");
  if (!state.lastMarketRefresh) {
    els.marketStatus.textContent = "等待同步";
    return;
  }
  els.marketStatus.classList.add("is-live");
  els.marketStatus.textContent = `已同步 ${formatTime(state.lastMarketRefresh)}`;
}

async function refreshMarketData({ silent }) {
  if (!silent) {
    setMarketStatus("同步中", "live");
  }

  const tasks = [refreshFxRates(), refreshCryptoQuotes()];
  const results = await Promise.allSettled(tasks);
  const failures = results.filter((result) => result.status === "rejected");

  if (failures.length === tasks.length) {
    setMarketStatus("同步失败", "error");
    console.warn("Market refresh failed", failures);
    return;
  }

  state.lastMarketRefresh = new Date().toISOString();
  persist();
  render();

  if (failures.length) {
    setMarketStatus("部分同步", "error");
  }
}

async function refreshFxRates() {
  const targets = CURRENCIES.filter((currency) => currency !== "USD").join(",");
  const response = await fetch(`https://api.frankfurter.app/latest?from=USD&to=${targets}`, { cache: "no-store" });
  if (!response.ok) throw new Error("FX request failed");
  const data = await response.json();
  const usdTo = { USD: 1, ...(data.rates || {}) };
  if (!usdTo.CNY) throw new Error("Missing CNY rate");

  CURRENCIES.forEach((currency) => {
    if (!usdTo[currency]) return;
    state.ratesToCny[currency] = currency === "CNY" ? 1 : usdTo.CNY / usdTo[currency];
  });
}

async function refreshCryptoQuotes() {
  const assets = state.assets.filter((asset) => asset.quoteProvider === "coingecko" && asset.symbol.trim());
  if (!assets.length) return;

  const ids = [...new Set(assets.map((asset) => asset.symbol.trim().toLowerCase()))];
  const vsCurrencies = [...new Set(assets.map((asset) => asset.currency.toLowerCase()))];
  const response = await fetch(
    `https://api.coingecko.com/api/v3/simple/price?ids=${ids.map(encodeURIComponent).join(",")}&vs_currencies=${vsCurrencies.join(",")}&include_24hr_change=true`,
    { cache: "no-store" },
  );
  if (!response.ok) throw new Error("Crypto request failed");
  const data = await response.json();
  const timestamp = new Date().toISOString();

  state.assets = state.assets.map((asset) => {
    if (asset.quoteProvider !== "coingecko" || !asset.symbol.trim()) return asset;
    const id = asset.symbol.trim().toLowerCase();
    const vs = asset.currency.toLowerCase();
    const quote = data[id];
    if (!quote || typeof quote[vs] !== "number") return asset;
    return {
      ...asset,
      price: quote[vs],
      dayChangePct: typeof quote[`${vs}_24h_change`] === "number" ? quote[`${vs}_24h_change`] : asset.dayChangePct,
      lastUpdated: timestamp,
    };
  });
}

function setMarketStatus(text, mode) {
  els.marketStatus.classList.remove("is-live", "is-error");
  if (mode === "live") els.marketStatus.classList.add("is-live");
  if (mode === "error") els.marketStatus.classList.add("is-error");
  els.marketStatus.textContent = text;
}

function exportPortfolio() {
  const payload = {
    app: "money-manager",
    version: 1,
    exportedAt: new Date().toISOString(),
    baseCurrency: state.baseCurrency,
    assets: state.assets,
    ratesToCny: state.ratesToCny,
    lastMarketRefresh: state.lastMarketRefresh,
  };
  const blob = new Blob([JSON.stringify(payload, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `money-manager-${new Date().toISOString().slice(0, 10)}.json`;
  document.body.append(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

function importPortfolio(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  const reader = new FileReader();
  reader.addEventListener("load", () => {
    try {
      const data = JSON.parse(String(reader.result || "{}"));
      const assets = Array.isArray(data) ? data : data.assets;
      if (!Array.isArray(assets)) throw new Error("Invalid import file");

      state.assets = assets.map(normalizeAsset);
      state.baseCurrency = CURRENCIES.includes(data.baseCurrency) ? data.baseCurrency : state.baseCurrency;
      state.ratesToCny = { ...FALLBACK_RATES_TO_CNY, ...(data.ratesToCny || state.ratesToCny) };
      state.lastMarketRefresh = data.lastMarketRefresh || null;
      state.editingId = null;
      persist();
      resetForm();
      render();
    } catch (error) {
      window.alert("导入失败，请确认文件是 Money Manager 导出的 JSON。");
      console.warn(error);
    } finally {
      event.target.value = "";
    }
  });
  reader.readAsText(file);
}

function hydrateIcons(root = document) {
  root.querySelectorAll(".icon[data-icon]").forEach((element) => {
    element.innerHTML = ICONS[element.dataset.icon] || "";
  });
}

function createId() {
  if (window.crypto && typeof window.crypto.randomUUID === "function") {
    return window.crypto.randomUUID();
  }
  return `asset-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function toNumber(value) {
  const number = Number(value);
  return Number.isFinite(number) ? number : 0;
}

function formatNumber(value, maximumFractionDigits = 2) {
  return new Intl.NumberFormat("zh-CN", {
    maximumFractionDigits,
  }).format(toNumber(value));
}

function formatCurrency(value, currency) {
  try {
    return new Intl.NumberFormat("zh-CN", {
      style: "currency",
      currency,
      maximumFractionDigits: currency === "JPY" ? 0 : 2,
    }).format(toNumber(value));
  } catch {
    return `${currency} ${formatNumber(value)}`;
  }
}

function formatSignedCurrency(value, currency) {
  const number = toNumber(value);
  const sign = number > 0 ? "+" : "";
  return `${sign}${formatCurrency(number, currency)}`;
}

function formatPercent(value) {
  const number = toNumber(value);
  const sign = number > 0 ? "+" : "";
  return `${sign}${number.toFixed(2)}%`;
}

function formatTime(iso) {
  if (!iso) return "--";
  return new Intl.DateTimeFormat("zh-CN", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(iso));
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => {
    const entities = {
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#39;",
    };
    return entities[char];
  });
}
