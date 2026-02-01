package com.parkinsons_disease_identifier;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Инициализация Python
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        // Настройка кнопок
        setupButtons();
    }

    private void setupButtons() {
        Button btnSpeechAnalysis = findViewById(R.id.btnSpeechAnalysis);
        Button btnVowelPhonation = findViewById(R.id.btnVowelPhonation);
        Button btnSettings = findViewById(R.id.btnSettings);
        Button btnAbout = findViewById(R.id.btnAbout);

        btnSpeechAnalysis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SpeechAnalysisActivity.class);
                startActivity(intent);
            }
        });

        // Текст кнопки с мелким шрифтом для предупреждения
        String fullText = getString(R.string.btn_vowel_phonation_with_warning);
        int newlinePos = fullText.indexOf('\n');
        SpannableString spannable = new SpannableString(fullText);
        if (newlinePos > 0) {
            spannable.setSpan(new RelativeSizeSpan(0.65f), newlinePos, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        btnVowelPhonation.setText(spannable);

        btnVowelPhonation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, VowelPhonationActivity.class);
                startActivity(intent);
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intent);
            }
        });
    }
}