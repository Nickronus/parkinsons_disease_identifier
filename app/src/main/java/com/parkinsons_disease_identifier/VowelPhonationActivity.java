package com.parkinsons_disease_identifier;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.chaquo.python.Python;
import com.chaquo.python.PyObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class VowelPhonationActivity extends AppCompatActivity {

    private static final String TAG = "VowelPhonationActivity";
    private static final String AUDIO_FILE_NAME = "vowel_phonation_recording.wav";

    private Button btnRecord;
    private Button btnLoadFile;
    private Button btnCancel;
    private Button btnAnalyze;
    private TextView tvStatus;
    private boolean isRecording = false;
    private boolean hasRecording = false;
    private boolean isWaitingForFilePicker = false;
    private boolean fileJustLoaded = false;

    private WavRecorder wavRecorder;
    private File audioFile;
    private Map<String, Object> lastFeatures = null; // Сохраняем последние характеристики
    
    // Launcher для запроса разрешения на запись аудио
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startRecording();
                } else {
                    Toast.makeText(this, getString(R.string.toast_permission_required), Toast.LENGTH_LONG).show();
                }
            });

    // Launcher для выбора аудиофайла
    private ActivityResultLauncher<String> pickAudioLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    loadAudioFromUri(uri);
                } else {
                    isWaitingForFilePicker = false;
                }
            });

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vowel_phonation);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupButtons();
        resetState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Не сбрасываем при возврате из выбора файла или сразу после загрузки
        if (fileJustLoaded) {
            fileJustLoaded = false;
            return;
        }
        if (!isWaitingForFilePicker) {
            resetState();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wavRecorder != null) {
            wavRecorder.stop();
        }
    }

    private void initViews() {
        btnRecord = findViewById(R.id.btnRecord);
        btnLoadFile = findViewById(R.id.btnLoadFile);
        btnCancel = findViewById(R.id.btnCancel);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        tvStatus = findViewById(R.id.tvStatus);
        
        // Кнопка анализа недоступна по умолчанию
        btnAnalyze.setEnabled(false);
        btnAnalyze.setAlpha(0.5f);
    }

    private void setupButtons() {
        btnLoadFile.setOnClickListener(v -> {
            isWaitingForFilePicker = true;
            pickAudioLauncher.launch("audio/*");
        });

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRecording();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Возврат на главную форму
            }
        });

        btnAnalyze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAnalyzingState(true);
                // Даём интерфейсу отрисоваться, затем анализ в фоне
                v.post(new Runnable() {
                    @Override
                    public void run() {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final double probability = performAnalysis();
                                final Map<String, Object> features = lastFeatures;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Intent intent = new Intent(VowelPhonationActivity.this, AnalysisResultsActivity.class);
                                        intent.putExtra("probability", probability);
                                        // Передаём характеристики
                                        if (features != null) {
                                            for (Map.Entry<String, Object> entry : features.entrySet()) {
                                                Object value = entry.getValue();
                                                if (value instanceof Double) {
                                                    intent.putExtra(entry.getKey(), (Double) value);
                                                } else if (value instanceof Float) {
                                                    intent.putExtra(entry.getKey(), (Float) value);
                                                } else if (value instanceof Integer) {
                                                    intent.putExtra(entry.getKey(), (Integer) value);
                                                } else if (value instanceof Long) {
                                                    intent.putExtra(entry.getKey(), (Long) value);
                                                } else if (value instanceof String) {
                                                    intent.putExtra(entry.getKey(), (String) value);
                                                }
                                            }
                                        }
                                        startActivity(intent);
                                    }
                                });
                            }
                        }).start();
                    }
                });
            }
        });
    }

    private void toggleRecording() {
        if (isRecording) {
            // Останавливаем запись
            stopRecording();
        } else {
            // Проверяем разрешение перед началом записи
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                    == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                // Запрашиваем разрешение
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        }
    }

    private void startRecording() {
        try {
            audioFile = new File(getFilesDir(), AUDIO_FILE_NAME);
            if (wavRecorder == null) {
                wavRecorder = new WavRecorder();
            }
            wavRecorder.start(audioFile);
            isRecording = true;
            btnRecord.setText(R.string.btn_stop_recording);
            tvStatus.setText(R.string.status_recording);
            Log.d(TAG, "Запись начата: " + audioFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Ошибка при начале записи WAV", e);
            Toast.makeText(this, getString(R.string.toast_record_start_error), Toast.LENGTH_SHORT).show();
            isRecording = false;
            btnRecord.setText(R.string.btn_start_recording);
            tvStatus.setText(R.string.status_ready);
        }
    }

    private void stopRecording() {
        if (wavRecorder != null) {
            wavRecorder.stop();
            Log.d(TAG, "Запись остановлена");
        }
        isRecording = false;
        btnRecord.setText(R.string.btn_start_recording);

        if (audioFile != null && audioFile.exists() && audioFile.length() > WavRecorder.WAV_HEADER_SIZE) {
            tvStatus.setText(R.string.status_recording_completed);
            hasRecording = true;
            btnAnalyze.setEnabled(true);
            btnAnalyze.setAlpha(1.0f);
            Log.d(TAG, "Аудиофайл сохранен: " + audioFile.getAbsolutePath() + " (размер: " + audioFile.length() + " байт)");
        } else {
            tvStatus.setText(R.string.status_ready);
            hasRecording = false;
            if (audioFile != null && audioFile.exists()) {
                audioFile.delete();
            }
            Toast.makeText(this, getString(R.string.toast_record_failed), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Сброс состояния формы к начальному виду
     */
    /**
     * Включить/отключить все кнопки и показать статус «Анализ» во время выполнения анализа.
     */
    private void setAnalyzingState(boolean analyzing) {
        btnRecord.setEnabled(!analyzing);
        btnRecord.setAlpha(analyzing ? 0.5f : 1.0f);
        btnLoadFile.setEnabled(!analyzing);
        btnLoadFile.setAlpha(analyzing ? 0.5f : 1.0f);
        btnAnalyze.setEnabled(!analyzing);
        btnAnalyze.setAlpha(analyzing ? 0.5f : 1.0f);
        btnCancel.setEnabled(!analyzing);
        btnCancel.setAlpha(analyzing ? 0.5f : 1.0f);
        tvStatus.setText(analyzing ? R.string.btn_analyze : R.string.status_ready);
    }

    private void resetState() {
        // Останавливаем запись, если она идет
        if (isRecording) {
            stopRecording();
        }
        
        isRecording = false;
        hasRecording = false;
        
        btnRecord.setText(R.string.btn_start_recording);
        tvStatus.setText(R.string.status_ready);
        
        // Деактивируем кнопку анализа
        btnAnalyze.setEnabled(false);
        btnAnalyze.setAlpha(0.5f);
        btnRecord.setEnabled(true);
        btnRecord.setAlpha(1.0f);
        btnLoadFile.setEnabled(true);
        btnLoadFile.setAlpha(1.0f);
        btnCancel.setEnabled(true);
        btnCancel.setAlpha(1.0f);
        
        // Удаляем предыдущий аудиофайл, если он был сохранен
        deleteAudioFile();
    }

    /**
     * Загрузка аудио из выбранного URI (копирование в файл приложения)
     */
    private void loadAudioFromUri(Uri uri) {
        if (isRecording) {
            stopRecording();
        }
        audioFile = new File(getFilesDir(), AUDIO_FILE_NAME);
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(audioFile)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            hasRecording = true;
            fileJustLoaded = true;
            tvStatus.setText(R.string.status_recording_completed);
            btnAnalyze.setEnabled(true);
            btnAnalyze.setAlpha(1.0f);
            Toast.makeText(this, getString(R.string.toast_file_loaded), Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Аудиофайл загружен: " + audioFile.getAbsolutePath());
            btnAnalyze.post(() -> isWaitingForFilePicker = false);
        } catch (IOException e) {
            Log.e(TAG, "Ошибка загрузки аудиофайла", e);
            Toast.makeText(this, getString(R.string.toast_file_load_error), Toast.LENGTH_SHORT).show();
            if (audioFile != null && audioFile.exists()) {
                audioFile.delete();
            }
            audioFile = null;
            isWaitingForFilePicker = false;
        }
    }

    /**
     * Удаление сохраненного аудиофайла
     */
    private void deleteAudioFile() {
        if (audioFile != null && audioFile.exists()) {
            boolean deleted = audioFile.delete();
            if (deleted) {
                Log.d(TAG, "Аудиофайл удален: " + audioFile.getAbsolutePath());
            } else {
                Log.w(TAG, "Не удалось удалить аудиофайл: " + audioFile.getAbsolutePath());
            }
        }
        audioFile = null;
    }

    /**
     * Получить путь к текущему аудиофайлу
     * @return путь к файлу или null, если файл не существует
     */
    public String getAudioFilePath() {
        if (audioFile != null && audioFile.exists()) {
            return audioFile.getAbsolutePath();
        }
        return null;
    }

    /**
     * Анализ фонации гласных: парсер WAV (запись с микрофона) → признаки → модель голоса ONNX.
     * @return вероятность наличия болезни Паркинсона в процентах (0.0 - 100.0)
     */
    private double performAnalysis() {
        String audioFilePath = getAudioFilePath();
        if (audioFilePath == null) {
            Log.w(TAG, "Нет записанного аудио");
            return 0.0;
        }
        Log.d(TAG, "Анализ файла (голос): " + audioFilePath);

        Map<String, Object> features = getVoiceFeaturesFromPython(audioFilePath);
        if (features == null) {
            lastFeatures = null;
            return 0.0;
        }

        // Сохраняем характеристики для передачи на форму результатов
        lastFeatures = new HashMap<>(features);

        ParkinsonOnnxPredictor predictor = null;
        try {
            predictor = new ParkinsonOnnxPredictor(this, ParkinsonOnnxPredictor.ModelType.VOICE);
            double prob = predictor.predict(features);
            if (prob < 0) {
                Log.e(TAG, "Ошибка предсказания модели");
                return 0.0;
            }
            double percent = prob * 100.0;
            Log.d(TAG, "Вероятность (голос): " + percent + "%");
            return percent;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка загрузки/запуска модели голоса", e);
            return 0.0;
        } finally {
            if (predictor != null) predictor.close();
        }
    }

    private Map<String, Object> getVoiceFeaturesFromPython(String wavPath) {
        try {
            Python py = Python.getInstance();
            PyObject module = py.getModule("audio_analysis");
            PyObject dataPy = module.callAttr("get_voice_features", wavPath);
            Map<PyObject, PyObject> raw = dataPy.asMap();
            Map<String, Object> features = new HashMap<>();
            for (Map.Entry<PyObject, PyObject> entry : raw.entrySet()) {
                String key = entry.getKey().toString();
                if ("FILEPATH".equals(key)) continue;
                PyObject v = entry.getValue();
                if (v != null) {
                    try {
                        features.put(key, v.toDouble());
                    } catch (Exception e) {
                        features.put(key, v.toString());
                    }
                }
            }
            return features;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка вызова парсера (audio_analysis.get_voice_features)", e);
            return null;
        }
    }
}
