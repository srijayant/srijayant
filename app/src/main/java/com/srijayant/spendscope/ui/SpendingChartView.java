package com.srijayant.spendscope.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.srijayant.spendscope.model.ExpenseCategory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SpendingChartView extends View {
    private static final int[] BAR_COLORS = {
            0xFF5B4CF0,
            0xFF13B88A,
            0xFFF1A33C,
            0xFFE25C79,
            0xFF3C8DD9
    };

    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint amountPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private List<Map.Entry<ExpenseCategory, BigDecimal>> categories = Collections.emptyList();

    public SpendingChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        labelPaint.setColor(0xFF344054);
        labelPaint.setTextSize(sp(12));
        labelPaint.setFakeBoldText(true);
        amountPaint.setColor(0xFF667085);
        amountPaint.setTextSize(sp(11));
        amountPaint.setTextAlign(Paint.Align.RIGHT);
        trackPaint.setColor(0xFFEFF2F7);
    }

    public void setData(List<Map.Entry<ExpenseCategory, BigDecimal>> values) {
        categories = new ArrayList<>(values.subList(0, Math.min(values.size(), 5)));
        StringBuilder description = new StringBuilder("Spending by category.");
        for (Map.Entry<ExpenseCategory, BigDecimal> entry : categories) {
            description.append(' ')
                    .append(entry.getKey().getDisplayName())
                    .append(": ")
                    .append(currency.format(entry.getValue()))
                    .append('.');
        }
        setContentDescription(description.toString());
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (categories.isEmpty()) {
            return;
        }

        float left = getPaddingLeft();
        float right = getWidth() - getPaddingRight();
        float top = getPaddingTop();
        float rowHeight = (getHeight() - getPaddingTop() - getPaddingBottom())
                / (float) categories.size();
        float barLeft = left + dp(104);
        float barRight = right - dp(68);
        float barHeight = dp(9);
        float radius = barHeight / 2f;
        BigDecimal maximum = categories.get(0).getValue();

        for (int i = 0; i < categories.size(); i++) {
            Map.Entry<ExpenseCategory, BigDecimal> entry = categories.get(i);
            float centerY = top + (rowHeight * i) + rowHeight / 2f;
            float textBaseline = centerY - (labelPaint.ascent() + labelPaint.descent()) / 2f;

            canvas.drawText(entry.getKey().getDisplayName(), left, textBaseline, labelPaint);
            canvas.drawText(currency.format(entry.getValue()), right, textBaseline, amountPaint);

            RectF track = new RectF(
                    barLeft,
                    centerY - barHeight / 2f,
                    barRight,
                    centerY + barHeight / 2f
            );
            canvas.drawRoundRect(track, radius, radius, trackPaint);

            float ratio = entry.getValue().divide(maximum, 4, java.math.RoundingMode.HALF_UP)
                    .floatValue();
            RectF bar = new RectF(
                    barLeft,
                    centerY - barHeight / 2f,
                    barLeft + (barRight - barLeft) * ratio,
                    centerY + barHeight / 2f
            );
            barPaint.setColor(BAR_COLORS[i % BAR_COLORS.length]);
            canvas.drawRoundRect(bar, radius, radius, barPaint);
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
