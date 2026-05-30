package com.computerascience.moneymanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final String[] CATEGORIES = {"银行", "券商", "基金", "加密资产", "房产", "负债", "其他"};
    private static final int BG = Color.rgb(245, 246, 241);
    private static final int PANEL = Color.WHITE;
    private static final int INK = Color.rgb(30, 35, 32);
    private static final int MUTED = Color.rgb(102, 112, 104);
    private static final int LINE = Color.rgb(217, 221, 213);
    private static final int ACCENT = Color.rgb(18, 107, 95);
    private static final int DANGER = Color.rgb(183, 73, 85);
    private static final int AMBER = Color.rgb(154, 119, 32);

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    private AssetStore store;
    private List<AssetRecord> assets = new ArrayList<>();
    private LinearLayout list;
    private TextView summary;
    private String pendingLaunchAssetId;
    private boolean waitingForExternalReturn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new AssetStore(this);
        assets = store.load();
        buildUi();
        render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pendingLaunchAssetId != null) {
            waitingForExternalReturn = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (waitingForExternalReturn && pendingLaunchAssetId != null) {
            String assetId = pendingLaunchAssetId;
            pendingLaunchAssetId = null;
            waitingForExternalReturn = false;
            AssetRecord asset = findAsset(assetId);
            if (asset != null) {
                showMarkUpdatedDialog(asset);
            }
        }
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(28));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        TextView eyebrow = label("Money Manager");
        root.addView(eyebrow);

        TextView title = text("资产更新助手", 28, INK, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = text("记录资产更新时间，一键打开对应 App 核对余额。", 15, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams subtitleParams = lp(-1, -2);
        subtitleParams.topMargin = dp(6);
        subtitleParams.bottomMargin = dp(18);
        root.addView(subtitle, subtitleParams);

        summary = text("", 15, MUTED, Typeface.NORMAL);
        summary.setPadding(dp(14), dp(12), dp(14), dp(12));
        summary.setBackground(cardBackground(Color.TRANSPARENT, LINE));
        LinearLayout.LayoutParams summaryParams = lp(-1, -2);
        summaryParams.bottomMargin = dp(12);
        root.addView(summary, summaryParams);

        Button addButton = primaryButton("新增资产");
        addButton.setOnClickListener(view -> showEditDialog(null));
        LinearLayout.LayoutParams addParams = lp(-1, dp(48));
        addParams.bottomMargin = dp(14);
        root.addView(addButton, addParams);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list, lp(-1, -2));

        setContentView(scrollView);
    }

    private void render() {
        list.removeAllViews();
        int staleCount = 0;
        for (AssetRecord asset : assets) {
            if (isStale(asset)) {
                staleCount += 1;
            }
            list.addView(assetCard(asset));
        }

        summary.setText("共 " + assets.size() + " 项资产，" + staleCount + " 项需要更新。"
                + "打开外部 App 后，回到这里可标记为已更新。");
        if (assets.isEmpty()) {
            TextView empty = text("还没有资产。先新增一项，再绑定对应 App。", 16, MUTED, Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(18), dp(28), dp(18), dp(28));
            list.addView(empty, lp(-1, -2));
        }
    }

    private View assetCard(AssetRecord asset) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(14));
        card.setBackground(cardBackground(PANEL, statusColor(asset)));

        LinearLayout.LayoutParams cardParams = lp(-1, -2);
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        header.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1));

        TextView name = text(asset.name, 19, INK, Typeface.BOLD);
        titleGroup.addView(name);

        String institution = asset.institution.isEmpty() ? "未填写机构" : asset.institution;
        TextView meta = text(asset.category + " · " + institution, 13, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams metaParams = lp(-1, -2);
        metaParams.topMargin = dp(3);
        titleGroup.addView(meta, metaParams);

        TextView status = statusChip(asset);
        header.addView(status);
        card.addView(header);

        LinearLayout valueRow = new LinearLayout(this);
        valueRow.setOrientation(LinearLayout.HORIZONTAL);
        valueRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams valueParams = lp(-1, -2);
        valueParams.topMargin = dp(16);
        card.addView(valueRow, valueParams);

        TextView amount = text(formatAmount(asset), 25, INK, Typeface.BOLD);
        valueRow.addView(amount, new LinearLayout.LayoutParams(0, -2, 1));

        TextView cadence = text("每 " + asset.updateEveryDays + " 天更新", 13, MUTED, Typeface.BOLD);
        valueRow.addView(cadence);

        TextView updated = text(lastUpdatedText(asset), 14, MUTED, Typeface.NORMAL);
        LinearLayout.LayoutParams updatedParams = lp(-1, -2);
        updatedParams.topMargin = dp(10);
        card.addView(updated, updatedParams);

        if (!asset.note.isEmpty()) {
            TextView note = text(asset.note, 14, MUTED, Typeface.NORMAL);
            LinearLayout.LayoutParams noteParams = lp(-1, -2);
            noteParams.topMargin = dp(8);
            card.addView(note, noteParams);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = lp(-1, -2);
        actionsParams.topMargin = dp(14);
        card.addView(actions, actionsParams);

        Button launch = secondaryButton("打开 App");
        launch.setOnClickListener(view -> openLinkedApp(asset));
        actions.addView(launch, new LinearLayout.LayoutParams(0, dp(44), 1));

        SpaceView gap1 = new SpaceView(this, dp(8), 1);
        actions.addView(gap1);

        Button mark = secondaryButton("已更新");
        mark.setOnClickListener(view -> markUpdated(asset));
        actions.addView(mark, new LinearLayout.LayoutParams(0, dp(44), 1));

        SpaceView gap2 = new SpaceView(this, dp(8), 1);
        actions.addView(gap2);

        Button edit = secondaryButton("编辑");
        edit.setOnClickListener(view -> showEditDialog(asset));
        actions.addView(edit, new LinearLayout.LayoutParams(0, dp(44), 1));

        return card;
    }

    private TextView statusChip(AssetRecord asset) {
        TextView chip = text(statusText(asset), 12, Color.WHITE, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(10), dp(6), dp(10), dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(statusColor(asset));
        bg.setCornerRadius(dp(999));
        chip.setBackground(bg);
        return chip;
    }

    private void showEditDialog(AssetRecord original) {
        boolean creating = original == null;
        AssetRecord draft = creating ? new AssetRecord() : copyOf(original);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        form.setPadding(pad, dp(6), pad, 0);

        EditText name = input("资产名称", draft.name, InputType.TYPE_CLASS_TEXT);
        form.addView(name);

        Spinner category = new Spinner(this);
        category.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CATEGORIES));
        category.setSelection(indexOf(CATEGORIES, draft.category));
        form.addView(fieldBox("类型", category));

        EditText institution = input("机构", draft.institution, InputType.TYPE_CLASS_TEXT);
        form.addView(institution);

        LinearLayout amountRow = row();
        EditText amount = input("金额", draft.amount, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText currency = input("币种", draft.currency, InputType.TYPE_CLASS_TEXT);
        amountRow.addView(amount, new LinearLayout.LayoutParams(0, -2, 1));
        amountRow.addView(new SpaceView(this, dp(8), 1));
        amountRow.addView(currency, new LinearLayout.LayoutParams(0, -2, 0.55f));
        form.addView(amountRow);

        EditText cadence = input("更新周期（天）", String.valueOf(draft.updateEveryDays), InputType.TYPE_CLASS_NUMBER);
        form.addView(cadence);

        EditText packageName = input("App 包名", draft.packageName, InputType.TYPE_CLASS_TEXT);
        packageName.setHint("例如 com.example.bank");
        form.addView(packageName);

        EditText launchUri = input("启动链接（可选）", draft.launchUri, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        launchUri.setHint("例如 bankapp://home");
        form.addView(launchUri);

        EditText note = input("备注", draft.note, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        note.setMinLines(2);
        form.addView(note);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(creating ? "新增资产" : "编辑资产")
                .setView(scroll)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", null);
        if (!creating) {
            builder.setNeutralButton("删除", null);
        }
        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(view -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            save.setTextColor(ACCENT);
            save.setOnClickListener(button -> {
                String assetName = clean(name.getText().toString());
                if (assetName.isEmpty()) {
                    toast("资产名称不能为空");
                    return;
                }

                int everyDays = parsePositiveInt(cadence.getText().toString(), 7);
                draft.name = assetName;
                draft.category = String.valueOf(category.getSelectedItem());
                draft.institution = clean(institution.getText().toString());
                draft.amount = clean(amount.getText().toString());
                draft.currency = clean(currency.getText().toString()).isEmpty()
                        ? "CNY"
                        : clean(currency.getText().toString()).toUpperCase(Locale.ROOT);
                draft.updateEveryDays = everyDays;
                draft.packageName = clean(packageName.getText().toString());
                draft.launchUri = clean(launchUri.getText().toString());
                draft.note = clean(note.getText().toString());

                if (creating) {
                    assets.add(draft);
                } else {
                    replaceAsset(draft);
                }
                store.save(assets);
                render();
                dialog.dismiss();
            });

            Button delete = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            if (delete != null) {
                delete.setTextColor(DANGER);
                delete.setOnClickListener(button -> new AlertDialog.Builder(this)
                        .setTitle("删除资产")
                        .setMessage("确定删除「" + original.name + "」吗？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("删除", (confirm, which) -> {
                            assets.removeIf(asset -> asset.id.equals(original.id));
                            store.save(assets);
                            render();
                            dialog.dismiss();
                        })
                        .show());
            }
        });

        dialog.show();
    }

    private void openLinkedApp(AssetRecord asset) {
        if (asset.launchUri.isEmpty() && asset.packageName.isEmpty()) {
            toast("先编辑资产，填写对应 App 包名或启动链接。");
            return;
        }

        if (!asset.launchUri.isEmpty()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(asset.launchUri));
                if (!asset.packageName.isEmpty()) {
                    intent.setPackage(asset.packageName);
                }
                pendingLaunchAssetId = asset.id;
                startActivity(intent);
                return;
            } catch (ActivityNotFoundException error) {
                pendingLaunchAssetId = null;
            }
        }

        if (!asset.packageName.isEmpty()) {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(asset.packageName);
            if (launchIntent != null) {
                pendingLaunchAssetId = asset.id;
                startActivity(launchIntent);
                return;
            }
            openMarket(asset.packageName);
            return;
        }

        toast("没有找到可打开的 App。");
    }

    private void openMarket(String packageName) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
        } catch (ActivityNotFoundException error) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
            } catch (ActivityNotFoundException ignored) {
                toast("没有找到这个 App，请检查包名。");
            }
        }
    }

    private void showMarkUpdatedDialog(AssetRecord asset) {
        new AlertDialog.Builder(this)
                .setTitle("标记已更新？")
                .setMessage("刚才打开了「" + asset.name + "」。如果你已经核对余额，可以把更新时间记为现在。")
                .setNegativeButton("暂不", null)
                .setPositiveButton("标记已更新", (dialog, which) -> markUpdated(asset))
                .show();
    }

    private void markUpdated(AssetRecord asset) {
        asset.lastUpdatedAt = System.currentTimeMillis();
        store.save(assets);
        render();
        toast("已更新「" + asset.name + "」。");
    }

    private AssetRecord findAsset(String id) {
        for (AssetRecord asset : assets) {
            if (asset.id.equals(id)) {
                return asset;
            }
        }
        return null;
    }

    private void replaceAsset(AssetRecord updated) {
        for (int index = 0; index < assets.size(); index += 1) {
            if (assets.get(index).id.equals(updated.id)) {
                assets.set(index, updated);
                return;
            }
        }
    }

    private AssetRecord copyOf(AssetRecord asset) {
        return AssetRecord.copyOf(asset);
    }

    private boolean isStale(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return true;
        }
        long days = (System.currentTimeMillis() - asset.lastUpdatedAt) / DAY_MS;
        return days >= asset.updateEveryDays;
    }

    private int statusColor(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return AMBER;
        }
        return isStale(asset) ? DANGER : ACCENT;
    }

    private String statusText(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return "待更新";
        }
        return isStale(asset) ? "已过期" : "新鲜";
    }

    private String lastUpdatedText(AssetRecord asset) {
        if (asset.lastUpdatedAt <= 0) {
            return "最后更新：从未更新";
        }
        long days = Math.max(0, (System.currentTimeMillis() - asset.lastUpdatedAt) / DAY_MS);
        return "最后更新：" + dateFormat.format(new Date(asset.lastUpdatedAt)) + " · " + days + " 天前";
    }

    private String formatAmount(AssetRecord asset) {
        if (asset.amount.isEmpty()) {
            return "-- " + asset.currency;
        }
        try {
            double value = Double.parseDouble(asset.amount);
            DecimalFormat format = new DecimalFormat("#,##0.##");
            return format.format(value) + " " + asset.currency;
        } catch (NumberFormatException error) {
            return asset.amount + " " + asset.currency;
        }
    }

    private EditText input(String label, String value, int inputType) {
        EditText input = new EditText(this);
        input.setHint(label);
        input.setText(value);
        input.setSingleLine((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0);
        input.setInputType(inputType);
        input.setTextColor(INK);
        input.setHintTextColor(MUTED);
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        input.setBackground(cardBackground(Color.WHITE, LINE));

        LinearLayout.LayoutParams params = lp(-1, dp(52));
        params.bottomMargin = dp(10);
        input.setLayoutParams(params);
        return input;
    }

    private View fieldBox(String label, View field) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView text = label(label);
        box.addView(text);
        box.addView(field, lp(-1, dp(48)));
        LinearLayout.LayoutParams params = lp(-1, -2);
        params.bottomMargin = dp(10);
        box.setLayoutParams(params);
        return box;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutParams(lp(-1, -2));
        return row;
    }

    private TextView label(String value) {
        TextView text = text(value.toUpperCase(Locale.ROOT), 12, MUTED, Typeface.BOLD);
        text.setIncludeFontPadding(false);
        return text;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, style);
        text.setLineSpacing(0, 1.08f);
        return text;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ACCENT);
        bg.setCornerRadius(dp(8));
        button.setBackground(bg);
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(INK);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setBackground(cardBackground(Color.WHITE, LINE));
        return button;
    }

    private GradientDrawable cardBackground(int fill, int border) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(8));
        bg.setStroke(dp(1), border);
        return bg;
    }

    private LinearLayout.LayoutParams lp(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private int parsePositiveInt(String value, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException error) {
            return fallback;
        }
    }

    private int indexOf(String[] values, String value) {
        for (int index = 0; index < values.length; index += 1) {
            if (values[index].equals(value)) {
                return index;
            }
        }
        return 0;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static final class SpaceView extends FrameLayout {
        SpaceView(Activity activity, int width, int height) {
            super(activity);
            setLayoutParams(new LinearLayout.LayoutParams(width, height));
        }
    }
}
