package com.parkinsons_disease_identifier;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
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
import java.io.OutputStream;

public class SpeechAnalysisActivity extends AppCompatActivity {

    private static final String TAG = "SpeechAnalysisActivity";
    private static final String AUDIO_FILE_NAME = "speech_recording.wav";
    /** Тестовый WAV из папки audiofiles (Parselmouth не читает 3GP/AMR запись) */
    private static final String ASSET_TEST_WAV = "VU2RLOABREE42M240120171952.wav";

    private Button btnRecord;
    private Button btnCancel;
    private Button btnAnalyze;
    private TextView tvStatus;
    private boolean isRecording = false;
    private boolean hasRecording = false;
    
    private MediaRecorder mediaRecorder;
    private File audioFile;
    
    // Launcher для запроса разрешения на запись аудио
    private ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startRecording();
                } else {
                    Toast.makeText(this, "Разрешение на запись аудио необходимо для работы приложения", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_speech_analysis);
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
        // Сбрасываем состояние при возврате к форме (например, после нажатия "Повторить")
        resetState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Освобождаем ресурсы MediaRecorder
        releaseMediaRecorder();
    }

    private void initViews() {
        btnRecord = findViewById(R.id.btnRecord);
        btnCancel = findViewById(R.id.btnCancel);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        tvStatus = findViewById(R.id.tvStatus);
        
        // Кнопка анализа недоступна по умолчанию
        btnAnalyze.setEnabled(false);
        btnAnalyze.setAlpha(0.5f);
    }

    private void setupButtons() {
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
                // Выполняем анализ (здесь будет ваш код)
                double probability = performAnalysis();
                
                // Переход к форме результатов с передачей процентов
                Intent intent = new Intent(SpeechAnalysisActivity.this, AnalysisResultsActivity.class);
                intent.putExtra("probability", probability);
                startActivity(intent);
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
            // Создаем файл для записи во внутреннем хранилище приложения
            audioFile = new File(getFilesDir(), AUDIO_FILE_NAME);
            
            // Освобождаем предыдущий MediaRecorder, если он существует
            releaseMediaRecorder();
            
            // Создаем новый MediaRecorder
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            
            // Подготавливаем и запускаем запись
            mediaRecorder.prepare();
            mediaRecorder.start();
            
            isRecording = true;
            btnRecord.setText(R.string.btn_stop_recording);
            tvStatus.setText(R.string.status_recording);
            
            Log.d(TAG, "Запись начата: " + audioFile.getAbsolutePath());
            
        } catch (IOException e) {
            Log.e(TAG, "Ошибка при подготовке MediaRecorder", e);
            Toast.makeText(this, "Ошибка при начале записи", Toast.LENGTH_SHORT).show();
            releaseMediaRecorder();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                Log.d(TAG, "Запись остановлена");
            } catch (RuntimeException e) {
                Log.e(TAG, "Ошибка при остановке записи", e);
                // Удаляем невалидный файл
                if (audioFile != null && audioFile.exists()) {
                    audioFile.delete();
                }
            } finally {
                releaseMediaRecorder();
            }
        }
        
        isRecording = false;
        btnRecord.setText(R.string.btn_start_recording);
        
        // Проверяем, что файл был создан и существует
        if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
            tvStatus.setText(R.string.status_recording_completed);
            hasRecording = true;
            
            // Активируем кнопку анализа после завершения записи
            btnAnalyze.setEnabled(true);
            btnAnalyze.setAlpha(1.0f);
            
            Log.d(TAG, "Аудиофайл сохранен: " + audioFile.getAbsolutePath() + " (размер: " + audioFile.length() + " байт)");
        } else {
            tvStatus.setText(R.string.status_ready);
            hasRecording = false;
            Toast.makeText(this, "Запись не удалась", Toast.LENGTH_SHORT).show();
        }
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при освобождении MediaRecorder", e);
            }
            mediaRecorder = null;
        }
    }

    /**
     * Сброс состояния формы к начальному виду
     */
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
        
        // Удаляем предыдущий аудиофайл, если он был сохранен
        deleteAudioFile();
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
     * Копирует тестовый WAV из assets (папка audiofiles) во внутреннее хранилище
     * и возвращает путь к нему. Parselmouth поддерживает только WAV, а запись идёт в 3GP/AMR.
     */
    private String getTestWavPathFromAssets() {
        File outFile = new File(getFilesDir(), "test_speech_from_assets.wav");
        if (outFile.exists()) {
            return outFile.getAbsolutePath();
        }
        try (InputStream in = getAssets().open(ASSET_TEST_WAV);
             OutputStream out = new FileOutputStream(outFile)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            Log.d(TAG, "Скопирован тестовый WAV из assets: " + outFile.getAbsolutePath());
            return outFile.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Ошибка копирования тестового WAV из assets", e);
            return null;
        }
    }

    /**
     * Метод для выполнения анализа речи
     * Используется WAV из папки audiofiles (assets), т.к. запись в 3GP/AMR не поддерживается Parselmouth.
     * @return вероятность наличия болезни Паркинсона в процентах (0.0 - 100.0)
     */
    private double performAnalysis() {
        String audioFilePath = getTestWavPathFromAssets();
        if (audioFilePath == null) {
            Toast.makeText(this, "Тестовый аудиофайл не найден в assets", Toast.LENGTH_SHORT).show();
            return 0.0;
        }
        Log.d(TAG, "Начинаем анализ файла (из audiofiles): " + audioFilePath);

        try {
            Python py = Python.getInstance();
            PyObject module = py.getModule("praat_test");
            PyObject result = module.callAttr("analyze_speech_parselmouth", audioFilePath);
            double value = result.toDouble();
            Log.d(TAG, "Результат praat_test: " + value);
            return value;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка вызова praat_test", e);
            Toast.makeText(this, "Ошибка анализа: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return 0.0;
        }
    }
}
