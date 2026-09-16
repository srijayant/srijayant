package com.srijayant.spendscope;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
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
import android.widget.Toast;

import com.srijayant.spendscope.data.CategoryRuleStore;
import com.srijayant.spendscope.data.SmsExpenseReader;
import com.srijayant.spendscope.data.TransactionJsonExporter;
import com.srijayant.spendscope.domain.ExpenseParser;
import com.srijayant.spendscope.domain.MerchantRuleKey;
import com.srijayant.spendscope.domain.TransactionDeduplicator;
import com.srijayant.spendscope.domain.TransactionMessageParser;
import com.srijayant.spendscope.model.ClassificationConfidence;
import com.srijayant.spendscope.model.ClassificationSource;
import com.srijayant.spendscope.model.Expense;
import com.srijayant.spendscope.model.ExpenseCategory;
import com.srijayant.spendscope.model.ExportMode;
import com.srijayant.spendscope.model.MonthlyReport;
import com.srijayant.spendscope.ui.SpendingChartView;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class MainActivity extends Activity {
    private static final int SMS_PERMISSION_REQUEST = 100;
    private static final int EXPORT_FULL_DOCUMENT_REQUEST = 200;
    private static final int EXPORT_MERCHANT_DOCUMENT_REQUEST = 201;
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
    private CategoryRuleStore categoryRules;
    private TransactionJsonExporter transactionExporter;

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
    private View reviewCard;
    private TextView suggestionSummary;
    private TextView exportButton;
    private List<SuggestionGroup> pendingSuggestions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();

        categoryRules = new CategoryRuleStore(this);
        expenseReader = new SmsExpenseReader(
                getContentResolver(),
                new ExpenseParser(),
                categoryRules
        );
        transactionExporter = new TransactionJsonExporter(
                getContentResolver(),
                new TransactionMessageParser(
                        new ExpenseParser(),
                        categoryRules::getCategory
                ),
                new TransactionDeduplicator()
        );
        monthLabel = findViewById(R.id.monthLabel);
        nextMonthButton = findViewById(R.id.nextMonthButton);

        findViewById(R.id.previousMonthButton).setOnClickListener(view -> changeMonth(-1));
        nextMonthButton.setOnClickListener(view -> changeMonth(1));
        findViewById(R.id.refreshButton).setOnClickListener(view -> refresh());
        exportButton.setOnClickListener(view -> startExport());
        findViewById(R.id.reviewSuggestionsButton)
                .setOnClickListener(view -> startSuggestionReview());
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
        if (hasSmsPermission()) {
            if (lastLoadedMonth == null && !loadInProgress) {
                loadReport();
            }
        } else {
            showPermissionPrompt(false);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == EXPORT_FULL_DOCUMENT_REQUEST
                || requestCode == EXPORT_MERCHANT_DOCUMENT_REQUEST)
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            ExportMode mode = requestCode == EXPORT_MERCHANT_DOCUMENT_REQUEST
                    ? ExportMode.MERCHANT_NAMES_ONLY
                    : ExportMode.FULL_TRANSACTIONS;
            exportTransactions(data.getData(), mode);
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
        reviewCard = findViewById(R.id.reviewCard);
        suggestionSummary = findViewById(R.id.suggestionSummary);
        exportButton = findViewById(R.id.exportButton);
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

    private void startExport() {
        if (!hasSmsPermission()) {
            showPermissionPrompt(false);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_export_title)
                .setItems(
                        new String[]{
                                getString(R.string.export_full_option),
                                getString(R.string.export_merchants_option)
                        },
                        (dialog, which) -> openExportDocument(
                                which == 1
                                        ? ExportMode.MERCHANT_NAMES_ONLY
                                        : ExportMode.FULL_TRANSACTIONS
                        )
                )
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void openExportDocument(ExportMode mode) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(
                Intent.EXTRA_TITLE,
                getString(
                        mode == ExportMode.MERCHANT_NAMES_ONLY
                                ? R.string.export_merchants_filename
                                : R.string.export_filename,
                        LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                )
        );
        startActivityForResult(
                intent,
                mode == ExportMode.MERCHANT_NAMES_ONLY
                        ? EXPORT_MERCHANT_DOCUMENT_REQUEST
                        : EXPORT_FULL_DOCUMENT_REQUEST
        );
    }

    private void exportTransactions(Uri destination, ExportMode mode) {
        setExporting(true);
        Toast.makeText(this, R.string.export_started, Toast.LENGTH_LONG).show();
        executor.execute(() -> {
            try {
                TransactionJsonExporter.ExportSummary summary =
                        transactionExporter.export(destination, mode);
                runOnUiThread(() -> {
                    setExporting(false);
                    int message = summary.getMode() == ExportMode.MERCHANT_NAMES_ONLY
                            ? R.string.export_merchants_complete
                            : R.string.export_complete;
                    Toast.makeText(
                            this,
                            summary.getMode() == ExportMode.MERCHANT_NAMES_ONLY
                                    ? getString(message, summary.getExportedItems())
                                    : getString(
                                            message,
                                            summary.getUniqueTransactions(),
                                            summary.getDuplicatesRemoved()
                                    ),
                            Toast.LENGTH_LONG
                    ).show();
                });
            } catch (IOException | RuntimeException error) {
                runOnUiThread(() -> {
                    setExporting(false);
                    Toast.makeText(this, R.string.export_failed, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setExporting(boolean exporting) {
        exportButton.setEnabled(!exporting);
        exportButton.setAlpha(exporting ? 0.5f : 1f);
        exportButton.setText(exporting ? R.string.exporting : R.string.export_json);
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
        transactionCount.setText(getString(
                R.string.transactions_count,
                report.getTransactionCount()
        ));
        averageAmount.setText(currency.format(report.getAverage()));
        topCategory.setText(report.getTopCategory().getDisplayName());

        List<Map.Entry<ExpenseCategory, BigDecimal>> categories = report.getCategoriesBySpend();
        spendingChart.setData(categories);
        renderSuggestionReview(report.getExpenses());
        renderCategories(categories, report.getTotal());
        renderTransactions(report.getExpenses());
    }

    private void renderSuggestionReview(List<Expense> expenses) {
        Map<String, SuggestionGroup> grouped = new LinkedHashMap<>();
        for (Expense expense : expenses) {
            if (expense.isUserCategorized()
                    || !MerchantRuleKey.isEligibleMerchant(expense.getMerchant())) {
                continue;
            }
            String key = MerchantRuleKey.fromMerchant(expense.getMerchant());
            SuggestionGroup group = grouped.get(key);
            if (group == null) {
                grouped.put(key, new SuggestionGroup(expense));
            } else {
                group.add(expense);
            }
        }

        pendingSuggestions = new ArrayList<>(grouped.values());
        pendingSuggestions.sort(Comparator.comparingInt(group ->
                group.suggestion.getAutomaticClassification().getConfidence().getScore()
        ));
        if (pendingSuggestions.isEmpty()) {
            reviewCard.setVisibility(View.GONE);
            return;
        }
        reviewCard.setVisibility(View.VISIBLE);
        suggestionSummary.setText(getString(
                R.string.suggestion_summary,
                pendingSuggestions.size()
        ));
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
        int visibleCount = expenses.size();
        for (int i = 0; i < visibleCount; i++) {
            Expense expense = expenses.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(15), 0, dp(15));
            row.setBackgroundResource(android.R.drawable.list_selector_background);
            row.setClickable(true);
            row.setFocusable(true);
            row.setOnClickListener(view -> showCategoryDialog(expense));
            row.setContentDescription(getString(
                    R.string.categorize_expense_description,
                    expense.getMerchant(),
                    currency.format(expense.getAmount()),
                    expense.getCategory().getDisplayName()
            ));

            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            TextView merchant = textView(expense.getMerchant(), 15, 0xFF172033, true);
            merchant.setMaxLines(1);
            merchant.setEllipsize(android.text.TextUtils.TruncateAt.END);
            details.addView(merchant);
            String ruleLabel = expense.isUserCategorized()
                    ? getString(R.string.personal_rule)
                    : getString(
                            R.string.suggested_rule,
                            confidenceLabel(
                                    expense.getAutomaticClassification().getConfidence()
                            )
                    );
            String metadata = expense.getCategory().getDisplayName() + " · " + ruleLabel + " · "
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

    private void startSuggestionReview() {
        if (!pendingSuggestions.isEmpty()) {
            showSuggestion(0, false);
        }
    }

    private void showSuggestion(int index, boolean changed) {
        if (index >= pendingSuggestions.size()) {
            if (changed) {
                lastLoadedMonth = null;
                loadReport();
            }
            return;
        }

        SuggestionGroup group = pendingSuggestions.get(index);
        Expense suggestion = group.suggestion;
        ExpenseCategory suggestedCategory =
                suggestion.getAutomaticClassification().getCategory();
        ExpenseCategory[] categories = ExpenseCategory.values();
        String[] options = new String[categories.length];
        for (int i = 0; i < categories.length; i++) {
            options[i] = categories[i].getDisplayName();
        }
        int[] selected = {suggestedCategory.ordinal()};

        new AlertDialog.Builder(this)
                .setTitle(getString(
                        R.string.suggestion_title,
                        suggestion.getMerchant(),
                        suggestedCategory.getDisplayName()
                ))
                .setMessage(getString(
                        R.string.suggestion_message,
                        group.count,
                        confidenceLabel(
                                suggestion.getAutomaticClassification().getConfidence()
                        ),
                        sourceLabel(suggestion.getAutomaticClassification().getSource())
                ))
                .setSingleChoiceItems(options, selected[0], (dialog, which) -> selected[0] = which)
                .setNegativeButton(R.string.done, (dialog, which) -> {
                    if (changed) {
                        lastLoadedMonth = null;
                        loadReport();
                    }
                })
                .setNeutralButton(R.string.skip, (dialog, which) ->
                        showSuggestion(index + 1, changed)
                )
                .setPositiveButton(R.string.confirm_next, (dialog, which) -> {
                    categoryRules.setCategory(
                            suggestion.getMerchant(),
                            categories[selected[0]]
                    );
                    showSuggestion(index + 1, true);
                })
                .setOnCancelListener(dialog -> {
                    if (changed) {
                        lastLoadedMonth = null;
                        loadReport();
                    }
                })
                .show();
    }

    private String confidenceLabel(ClassificationConfidence confidence) {
        switch (confidence) {
            case HIGH:
                return getString(R.string.confidence_high);
            case MEDIUM:
                return getString(R.string.confidence_medium);
            case LOW:
            default:
                return getString(R.string.confidence_low);
        }
    }

    private String sourceLabel(ClassificationSource source) {
        switch (source) {
            case KNOWN_MERCHANT:
                return getString(R.string.source_known_merchant);
            case MESSAGE_KEYWORD:
                return getString(R.string.source_message_keyword);
            case TRANSACTION_TYPE:
                return getString(R.string.source_transaction_type);
            case UNKNOWN:
            default:
                return getString(R.string.source_unknown);
        }
    }

    private void showCategoryDialog(Expense expense) {
        if (!MerchantRuleKey.isEligibleMerchant(expense.getMerchant())) {
            Toast.makeText(
                    this,
                    R.string.merchant_not_detected,
                    Toast.LENGTH_LONG
            ).show();
            return;
        }
        ExpenseCategory[] categories = ExpenseCategory.values();
        String[] options = new String[categories.length + 1];
        options[0] = getString(R.string.automatic_category);
        for (int i = 0; i < categories.length; i++) {
            options[i + 1] = categories[i].getDisplayName();
        }

        int[] selected = {0};
        categoryRules.getCategory(expense.getMerchant()).ifPresent(category -> {
            selected[0] = category.ordinal() + 1;
        });

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.categorize_title, expense.getMerchant()))
                .setMessage(getString(R.string.categorize_message, expense.getMerchant()))
                .setSingleChoiceItems(options, selected[0], (dialog, which) -> selected[0] = which)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.apply, (dialog, which) -> {
                    if (selected[0] == 0) {
                        categoryRules.removeCategory(expense.getMerchant());
                    } else {
                        categoryRules.setCategory(
                                expense.getMerchant(),
                                categories[selected[0] - 1]
                        );
                    }
                    lastLoadedMonth = null;
                    loadReport();
                })
                .show();
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
        permissionButton.setOnClickListener(view -> handlePermissionAction());
        boolean requested = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PERMISSION_REQUESTED, false);
        boolean canRequest = !requested
                || shouldShowRequestPermissionRationale(Manifest.permission.READ_SMS);
        if (denied) {
            permissionTitle.setText(R.string.permission_denied);
            permissionDescription.setText(R.string.permission_description);
            permissionButton.setText(canRequest ? R.string.retry : R.string.open_settings);
        } else {
            permissionTitle.setText(R.string.permission_title);
            permissionDescription.setText(R.string.permission_description);
            permissionButton.setText(canRequest ? R.string.grant_access : R.string.open_settings);
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

    private static final class SuggestionGroup {
        private Expense suggestion;
        private int count = 1;

        private SuggestionGroup(Expense firstExpense) {
            suggestion = firstExpense;
        }

        private void add(Expense expense) {
            count++;
            if (expense.getAutomaticClassification().getConfidence().getScore()
                    > suggestion.getAutomaticClassification().getConfidence().getScore()) {
                suggestion = expense;
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
