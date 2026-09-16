package com.srijayant.spendscope;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.srijayant.spendscope.data.SmsExpenseReader;
import com.srijayant.spendscope.domain.ExpenseParser;
import com.srijayant.spendscope.model.Expense;
import com.srijayant.spendscope.model.ExpenseCategory;
import com.srijayant.spendscope.model.MonthlyReport;
import com.srijayant.spendscope.ui.SpendingChartView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class MainActivity extends Activity {
    private static final int SMS_PERMISSION_REQUEST = 100;
    private static final String PREFS = "spendscope";
    private static final String PERMISSION_REQUESTED = "sms_permission_requested";
    private static final int[] CATEGORY_COLORS = {
            0xFF5B4CF0,
            0xFF13B88A,
            0xFFF1A33C,
            0xFFE25C79,
            0xFF3C8DD9
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicInteger loadGeneration = new AtomicInteger();
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
    private final DateTimeFormatter transactionDateFormatter =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT);

    private YearMonth selectedMonth = YearMonth.now();
    private YearMonth lastLoadedMonth;
    private boolean loadInProgress;
    private SmsExpenseReader expenseReader;

    private TextView monthLabel;
    private TextView nextMonthButton;
    private View permissionCard;
    private TextView permissionTitle;
    private TextView permissionDescription;
    private Button permissionButton;
    private View loadingView;
    private View reportContainer;
    private View emptyView;
    private TextView totalAmount;
    private TextView transactionCount;
    private TextView averageAmount;
    private TextView topCategory;
    private SpendingChartView spendingChart;
    private LinearLayout categoryContainer;
    private LinearLayout transactionContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();

        expenseReader = new SmsExpenseReader(getContentResolver(), new ExpenseParser());
        monthLabel = findViewById(R.id.monthLabel);
        nextMonthButton = findViewById(R.id.nextMonthButton);

        findViewById(R.id.previousMonthButton).setOnClickListener(view -> changeMonth(-1));
        nextMonthButton.setOnClickListener(view -> changeMonth(1));
        findViewById(R.id.refreshButton).setOnClickListener(view -> refresh());
        permissionButton.setOnClickListener(view -> handlePermissionAction());

        updateMonthControls();
        if (hasSmsPermission()) {
            loadReport();
        } else {
            showPermissionPrompt(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasSmsPermission() && lastLoadedMonth == null && !loadInProgress) {
            loadReport();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != SMS_PERMISSION_REQUEST) {
            return;
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadReport();
        } else {
            showPermissionPrompt(true);
        }
    }

    @Override
    protected void onDestroy() {
        loadGeneration.incrementAndGet();
        executor.shutdownNow();
        super.onDestroy();
    }

    private void bindViews() {
        permissionCard = findViewById(R.id.permissionCard);
        permissionTitle = findViewById(R.id.permissionTitle);
        permissionDescription = findViewById(R.id.permissionDescription);
        permissionButton = findViewById(R.id.permissionButton);
        loadingView = findViewById(R.id.loadingView);
        reportContainer = findViewById(R.id.reportContainer);
        emptyView = findViewById(R.id.emptyView);
        totalAmount = findViewById(R.id.totalAmount);
        transactionCount = findViewById(R.id.transactionCount);
        averageAmount = findViewById(R.id.averageAmount);
        topCategory = findViewById(R.id.topCategory);
        spendingChart = findViewById(R.id.spendingChart);
        categoryContainer = findViewById(R.id.categoryContainer);
        transactionContainer = findViewById(R.id.transactionContainer);
    }

    private void changeMonth(int offset) {
        YearMonth candidate = selectedMonth.plusMonths(offset);
        if (candidate.isAfter(YearMonth.now())) {
            return;
        }
        selectedMonth = candidate;
        lastLoadedMonth = null;
        updateMonthControls();
        refresh();
    }

    private void updateMonthControls() {
        monthLabel.setText(selectedMonth.format(monthFormatter));
        boolean canMoveForward = selectedMonth.isBefore(YearMonth.now());
        nextMonthButton.setEnabled(canMoveForward);
        nextMonthButton.setAlpha(canMoveForward ? 1f : 0.3f);
    }

    private void refresh() {
        if (hasSmsPermission()) {
            loadReport();
        } else {
            showPermissionPrompt(false);
        }
    }

    private void loadReport() {
        final int generation = loadGeneration.incrementAndGet();
        final YearMonth month = selectedMonth;
        loadInProgress = true;
        permissionCard.setVisibility(View.GONE);
        reportContainer.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        loadingView.setVisibility(View.VISIBLE);

        executor.execute(() -> {
            try {
                MonthlyReport report = expenseReader.read(month);
                runOnUiThread(() -> {
                    if (generation != loadGeneration.get()) {
                        return;
                    }
                    loadInProgress = false;
                    lastLoadedMonth = month;
                    renderReport(report);
                });
            } catch (RuntimeException error) {
                runOnUiThread(() -> {
                    if (generation == loadGeneration.get()) {
                        loadInProgress = false;
                        showReadError();
                    }
                });
            }
        });
    }

    private void renderReport(MonthlyReport report) {
        loadingView.setVisibility(View.GONE);
        if (report.getExpenses().isEmpty()) {
            reportContainer.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        emptyView.setVisibility(View.GONE);
        reportContainer.setVisibility(View.VISIBLE);
        totalAmount.setText(currency.format(report.getTotal()));
        transactionCount.setText(
                report.getTransactionCount() + " " + getString(R.string.transactions_count)
        );
        averageAmount.setText(currency.format(report.getAverage()));
        topCategory.setText(report.getTopCategory().getDisplayName());

        List<Map.Entry<ExpenseCategory, BigDecimal>> categories = report.getCategoriesBySpend();
        spendingChart.setData(categories);
        renderCategories(categories, report.getTotal());
        renderTransactions(report.getExpenses());
    }

    private void renderCategories(
            List<Map.Entry<ExpenseCategory, BigDecimal>> categories,
            BigDecimal total
    ) {
        categoryContainer.removeAllViews();
        for (int i = 0; i < categories.size(); i++) {
            Map.Entry<ExpenseCategory, BigDecimal> entry = categories.get(i);
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setPadding(0, dp(i == 0 ? 0 : 14), 0, dp(14));

            LinearLayout labels = new LinearLayout(this);
            labels.setOrientation(LinearLayout.HORIZONTAL);
            TextView name = textView(entry.getKey().getDisplayName(), 14, 0xFF344054, true);
            TextView amount = textView(currency.format(entry.getValue()), 14, 0xFF344054, true);
            amount.setGravity(android.view.Gravity.END);
            labels.addView(name, new LinearLayout.LayoutParams(0, dp(24), 1f));
            labels.addView(amount, new LinearLayout.LayoutParams(0, dp(24), 1f));
            item.addView(labels);

            ProgressBar progress = new ProgressBar(
                    this,
                    null,
                    android.R.attr.progressBarStyleHorizontal
            );
            progress.setMax(1000);
            int share = entry.getValue()
                    .multiply(BigDecimal.valueOf(1000))
                    .divide(total, 0, RoundingMode.HALF_UP)
                    .intValue();
            progress.setProgress(share);
            progress.setProgressTintList(
                    ColorStateList.valueOf(CATEGORY_COLORS[i % CATEGORY_COLORS.length])
            );
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xFFEFF2F7));
            item.addView(progress, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(8)
            ));
            categoryContainer.addView(item);
        }
    }

    private void renderTransactions(List<Expense> expenses) {
        transactionContainer.removeAllViews();
        int visibleCount = Math.min(expenses.size(), 25);
        for (int i = 0; i < visibleCount; i++) {
            Expense expense = expenses.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(15), 0, dp(15));

            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            TextView merchant = textView(expense.getMerchant(), 15, 0xFF172033, true);
            merchant.setMaxLines(1);
            merchant.setEllipsize(android.text.TextUtils.TruncateAt.END);
            details.addView(merchant);
            String metadata = expense.getCategory().getDisplayName() + " · "
                    + expense.getTimestamp()
                    .atZone(ZoneId.systemDefault())
                    .format(transactionDateFormatter);
            details.addView(textView(metadata, 12, 0xFF667085, false));
            row.addView(details, new LinearLayout.LayoutParams(0, dp(52), 1f));

            TextView amount = textView("−" + currency.format(expense.getAmount()), 15, 0xFFD94A64, true);
            amount.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
            row.addView(amount, new LinearLayout.LayoutParams(dp(112), dp(52)));
            transactionContainer.addView(row);

            if (i < visibleCount - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(0xFFE4E8F0);
                transactionContainer.addView(divider, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                ));
            }
        }
    }

    private TextView textView(String value, float sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        if (bold) {
            view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        }
        return view;
    }

    private void handlePermissionAction() {
        boolean requested = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PERMISSION_REQUESTED, false);
        if (requested && !shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS)) {
            Intent intent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
            return;
        }

        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(PERMISSION_REQUESTED, true)
                .apply();
        requestPermissions(new String[]{Manifest.permission.READ_SMS}, SMS_PERMISSION_REQUEST);
    }

    private void showPermissionPrompt(boolean denied) {
        loadGeneration.incrementAndGet();
        loadInProgress = false;
        loadingView.setVisibility(View.GONE);
        reportContainer.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        permissionCard.setVisibility(View.VISIBLE);
        if (denied) {
            permissionTitle.setText(R.string.permission_denied);
            permissionDescription.setText(R.string.permission_description);
            permissionButton.setText(R.string.open_settings);
        } else {
            permissionTitle.setText(R.string.permission_title);
            permissionDescription.setText(R.string.permission_description);
            boolean requested = getSharedPreferences(PREFS, MODE_PRIVATE)
                    .getBoolean(PERMISSION_REQUESTED, false);
            permissionButton.setText(requested ? R.string.open_settings : R.string.grant_access);
        }
    }

    private void showReadError() {
        loadingView.setVisibility(View.GONE);
        reportContainer.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        permissionCard.setVisibility(View.VISIBLE);
        permissionTitle.setText(R.string.read_error);
        permissionDescription.setText(R.string.permission_description);
        permissionButton.setText(R.string.retry);
        permissionButton.setOnClickListener(view -> loadReport());
    }

    private boolean hasSmsPermission() {
        return checkSelfPermission(Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
