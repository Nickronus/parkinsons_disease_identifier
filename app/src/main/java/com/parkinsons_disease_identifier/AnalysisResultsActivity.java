package com.parkinsons_disease_identifier;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AnalysisResultsActivity extends AppCompatActivity {

    private TextView tvProbability;
    private Button btnRepeat;
    private Button btnFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_analysis_results);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupButtons();
        
        // Получаем результат анализа из Intent
        double probability = getIntent().getDoubleExtra("probability", 0.0);
        displayResults(probability);
    }

    private void initViews() {
        tvProbability = findViewById(R.id.tvProbability);
        btnRepeat = findViewById(R.id.btnRepeat);
        btnFinish = findViewById(R.id.btnFinish);
    }

    private void setupButtons() {
        btnRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Возврат к форме анализа речи
                finish();
            }
        });

        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Возврат на главную форму
                Intent intent = new Intent(AnalysisResultsActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void displayResults(double probability) {
        // Отображаем результат в процентах
        String probabilityText = String.format("%.1f%%", probability);
        tvProbability.setText(probabilityText);
    }
}
