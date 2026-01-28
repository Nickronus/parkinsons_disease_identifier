package com.parkinsons_disease_identifier;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
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
            tvCharacteristic.setPadding(16, 12, 16, 12);
            tvCharacteristic.setTextSize(16f);
            tvCharacteristic.setTextColor(0xFF333333);
            tvCharacteristic.setText(String.format("%s: %s", key, valueStr));
            
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
}
