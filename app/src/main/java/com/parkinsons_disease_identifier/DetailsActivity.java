package com.parkinsons_disease_identifier;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.HashMap;
import java.util.Map;

public class DetailsActivity extends AppCompatActivity {

    private LinearLayout llCharacteristics;
    private Button btnBack;


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupButtons();
        displayCharacteristics();
    }

    private void initViews() {
        llCharacteristics = findViewById(R.id.llCharacteristics);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupButtons() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void displayCharacteristics() {
        // Получаем характеристики из Intent
        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            return;
        }

        // Получаем все ключи из Bundle (features передаются как отдельные ключи)
        Map<String, Object> features = new HashMap<>();
        for (String key : extras.keySet()) {
            if (!key.equals("probability")) { // Пропускаем probability
                Object value = extras.get(key);
                if (value != null) {
                    features.put(key, value);
                }
            }
        }

        // Отображаем характеристики
        for (Map.Entry<String, Object> entry : features.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Форматируем значение
            String valueStr;
            if (value instanceof Double || value instanceof Float) {
                double d = ((Number) value).doubleValue();
                if (d == (long) d) {
                    valueStr = String.format("%d", (long) d);
                } else {
                    valueStr = String.format("%.4f", d);
                }
            } else {
                valueStr = value.toString();
            }

            // Создаём TextView для характеристики
            TextView tvCharacteristic = new TextView(this);
            tvCharacteristic.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            int padH = (int) (24 * getResources().getDisplayMetrics().density);
            int padV = (int) (20 * getResources().getDisplayMetrics().density);
            tvCharacteristic.setPadding(padH, padV, padH, padV);
            tvCharacteristic.setTextSize(18f);
            tvCharacteristic.setTextColor(0xFF333333);
            tvCharacteristic.setText(String.format("%s: %s", key, valueStr));
            tvCharacteristic.setClickable(true);
            tvCharacteristic.setFocusable(true);
            final String featureKey = key;
            tvCharacteristic.setOnClickListener(v -> showDescriptionDialog(featureKey));
            
            // Добавляем разделитель
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1));
            divider.setBackgroundColor(0xFFE0E0E0);
            
            llCharacteristics.addView(tvCharacteristic);
            llCharacteristics.addView(divider);
        }
    }

    private void showDescriptionDialog(String featureKey) {
        int resId = getDescriptionResId(featureKey);
        String message = resId != 0
                ? getString(resId)
                : getString(R.string.details_no_description);
        new AlertDialog.Builder(this)
                .setTitle(featureKey)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private int getDescriptionResId(String featureKey) {
        String resName = "desc_" + featureKey.toLowerCase();
        return getResources().getIdentifier(resName, "string", getPackageName());
    }
}
