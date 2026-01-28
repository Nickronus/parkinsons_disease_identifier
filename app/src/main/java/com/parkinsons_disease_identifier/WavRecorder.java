package com.parkinsons_disease_identifier;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Запись с микрофона в WAV (16-bit PCM, моно).
 * Используется AudioRecord; файл подходит для Parselmouth и анализа.
 */
public class WavRecorder {

    private static final String TAG = "WavRecorder";

    public static final int SAMPLE_RATE_HZ = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static final int WAV_HEADER_SIZE = 44;

    private final int bufferSizeBytes;
    private AudioRecord audioRecord;
    private RandomAccessFile wavFile;
    private File file;
    private Thread recordThread;
    private volatile boolean isRecording;

    public WavRecorder() {
        int minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL_CONFIG, AUDIO_ENCODING);
        this.bufferSizeBytes = Math.max(minBuf, 4096);
    }

    /**
     * Начать запись в указанный файл. Файл будет перезаписан.
     */
    public void start(File outputFile) throws IOException {
        if (audioRecord != null) {
            stop();
        }
        file = outputFile;

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_HZ,
                CHANNEL_CONFIG,
                AUDIO_ENCODING,
                bufferSizeBytes
        );
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release();
            audioRecord = null;
            throw new IOException("AudioRecord init failed");
        }

        wavFile = new RandomAccessFile(file, "rw");
        wavFile.setLength(0);
        wavFile.write(new byte[WAV_HEADER_SIZE]);

        audioRecord.startRecording();
        isRecording = true;
        recordThread = new Thread(this::recordLoop);
        recordThread.start();
        Log.d(TAG, "WAV recording started: " + file.getAbsolutePath());
    }

    private void recordLoop() {
        byte[] buffer = new byte[bufferSizeBytes];
        try {
            while (isRecording && audioRecord != null && audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0 && wavFile != null) {
                    wavFile.write(buffer, 0, read);
                } else if (read == AudioRecord.ERROR_INVALID_OPERATION || read == AudioRecord.ERROR_BAD_VALUE) {
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Write error", e);
        }
    }

    /**
     * Остановить запись и дописать WAV-заголовок.
     */
    public void stop() {
        isRecording = false;
        if (recordThread != null) {
            try {
                recordThread.join(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            recordThread = null;
        }
        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing AudioRecord", e);
            }
            audioRecord = null;
        }
        if (wavFile != null) {
            try {
                long pcmBytes = wavFile.length() - WAV_HEADER_SIZE;
                if (pcmBytes > 0) {
                    writeWavHeader(wavFile, (int) pcmBytes);
                }
                wavFile.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing WAV file", e);
            }
            wavFile = null;
        }
        if (file != null) {
            Log.d(TAG, "WAV recording stopped: " + file.getAbsolutePath() + " length=" + file.length());
        }
    }

    private static void writeWavHeader(RandomAccessFile raf, int pcmDataSize) throws IOException {
        int totalSize = WAV_HEADER_SIZE + pcmDataSize;
        int byteRate = SAMPLE_RATE_HZ * 2; // 16-bit = 2 bytes per sample, mono

        raf.seek(0);
        ByteBuffer header = ByteBuffer.allocate(WAV_HEADER_SIZE);
        header.order(ByteOrder.LITTLE_ENDIAN);

        header.put("RIFF".getBytes());
        header.putInt(totalSize - 8);
        header.put("WAVE".getBytes());
        header.put("fmt ".getBytes());
        header.putInt(16);
        header.putShort((short) 1); // PCM
        header.putShort((short) 1); // mono
        header.putInt(SAMPLE_RATE_HZ);
        header.putInt(byteRate);
        header.putShort((short) 2); // block align
        header.putShort((short) 16); // bits per sample
        header.put("data".getBytes());
        header.putInt(pcmDataSize);

        raf.write(header.array());
    }

    public File getFile() {
        return file;
    }

    public boolean isRecording() {
        return isRecording;
    }
}
