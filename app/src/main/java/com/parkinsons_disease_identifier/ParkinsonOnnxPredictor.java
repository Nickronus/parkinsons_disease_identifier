package com.parkinsons_disease_identifier;

import android.content.Context;
import android.util.Log;

import ai.onnxruntime.OnnxMap;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Загрузка ONNX-моделей (речь / голос) и предсказание вероятности болезни Паркинсона
 * по признакам, полученным от парсера (get_data).
 */
public class ParkinsonOnnxPredictor {

    private static final String TAG = "ParkinsonOnnxPredictor";

    /** Целевая метка IS SICK: Yes/No → LabelEncoder → 1=Yes=болен, 0=No=здоров. Вероятность класса 1 = вероятность болезни. */
    private static final boolean CLASS_1_IS_DISEASE = true;

    public enum ModelType {
        SPEECH("speech_model.onnx", SPEECH_FEATURE_ORDER, null),
        VOICE("voice_model.onnx", VOICE_FEATURE_ORDER, null);

        final String assetName;
        final String[] featureOrder;
        final Map<String, String> parserKeyByModelKey;

        ModelType(String assetName, String[] featureOrder, Map<String, String> parserKeyByModelKey) {
            this.assetName = assetName;
            this.featureOrder = featureOrder;
            this.parserKeyByModelKey = parserKeyByModelKey;
        }
    }

    private static final String[] SPEECH_FEATURE_ORDER = {
            "JITTER_LOCAL", "PPE", "SHIMMER_APQ11", "SHIMMER_APQ3", "HNR",
            "JITTER_ABS", "JITTER_PPQ5", "SHIMMER_DB"
    };

    private static final String[] VOICE_FEATURE_ORDER = {
            "F2", "F1", "SHIMMER_LOCAL", "JITTER_PPQ5", "F0_RANGE", "INTENSITY_RANGE", "HNR"
    };

    private final OrtEnvironment env;
    private final OrtSession session;
    private final ModelType modelType;
    private final String inputName;
    private final String outputName;

    public ParkinsonOnnxPredictor(Context context, ModelType type) throws Exception {
        this.modelType = type;
        env = OrtEnvironment.getEnvironment();
        File modelFile = copyAssetToFile(context, type.assetName);
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        session = env.createSession(modelFile.getAbsolutePath(), options);
        String[] inputNames = session.getInputNames().toArray(new String[0]);
        String[] outputNames = session.getOutputNames().toArray(new String[0]);
        inputName = inputNames.length > 0 ? inputNames[0] : null;
        outputName = outputNames.length > 0 ? outputNames[0] : null;
        Log.d(TAG, "Model loaded: " + type.assetName + ", input=" + inputName + ", output=" + outputName);
    }

    private static File copyAssetToFile(Context context, String assetName) throws Exception {
        File out = new File(context.getFilesDir(), assetName);
        try (InputStream in = context.getAssets().open(assetName);
             FileOutputStream outStream = new FileOutputStream(out)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                outStream.write(buf, 0, n);
            }
        }
        return out;
    }

    /**
     * Предсказание вероятности класса 1 (болезнь) по признакам из парсера.
     *
     * @param parserData словарь от get_data() (ключи — имена признаков парсера)
     * @return вероятность 0.0 .. 1.0 или -1 при ошибке
     */
    public double predict(Map<String, ?> parserData) {
        if (inputName == null || outputName == null) {
            Log.e(TAG, "Model has no input/output names");
            return -1.0;
        }
        float[] inputFloats = buildInputVector(parserData);
        if (inputFloats == null) {
            return -1.0;
        }
        try {
            long[] shape = new long[]{1, inputFloats.length};
            OnnxTensor inputTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputFloats), shape);
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put(inputName, inputTensor);
            try (OrtSession.Result result = session.run(inputs)) {
                inputTensor.close();
                OnnxValue probOut = result.get("probabilities").orElse(null);
                if (probOut != null) {
                    Object probVal = probOut.getValue();
                    if (probVal instanceof List && !((List<?>) probVal).isEmpty()) {
                        Object first = ((List<?>) probVal).get(0);
                        Map<?, ?> probMap = null;
                        if (first instanceof Map) {
                            probMap = (Map<?, ?>) first;
                        } else if (first instanceof OnnxMap) {
                            Object mapVal = ((OnnxMap) first).getValue();
                            if (mapVal instanceof Map) probMap = (Map<?, ?>) mapVal;
                        }
                        if (probMap != null) {
                            Double p1 = extractProbClass1(probMap);
                            if (p1 != null) {
                                return CLASS_1_IS_DISEASE ? p1 : (1.0 - p1);
                            }
                        }
                    }
                }
                OnnxValue firstOut = result.get(0);
                if (firstOut != null) {
                    Object raw = firstOut.getValue();
                    if (raw instanceof float[]) {
                        float[] arr = (float[]) raw;
                        if (arr.length >= 2) return arr[1];
                        if (arr.length == 1) return arr[0];
                    }
                    if (raw instanceof float[][]) {
                        float[][] arr = (float[][]) raw;
                        if (arr.length > 0 && arr[0].length >= 2) return arr[0][1];
                        if (arr.length > 0 && arr[0].length == 1) return arr[0][0];
                    }
                }
                return -1.0;
            }
        } catch (Exception e) {
            Log.e(TAG, "Predict failed", e);
            return -1.0;
        }
    }

    private static Double extractProbClass1(Map<?, ?> probMap) {
        Double p0 = null;
        Double p1 = null;
        for (Map.Entry<?, ?> e : probMap.entrySet()) {
            Object k = e.getKey();
            Object v = e.getValue();
            if (!(k instanceof Number) || !(v instanceof Number)) continue;
            long key = ((Number) k).longValue();
            double val = ((Number) v).doubleValue();
            if (key == 0) p0 = val;
            else if (key == 1) p1 = val;
        }
        if (p0 != null && p1 != null) {
            Log.d(TAG, "ОННХ вероятности: класс 0 = " + String.format("%.4f", p0) + ", класс 1 = " + String.format("%.4f", p1));
        }
        if (p1 != null) return p1;
        if (p0 != null) return p0;
        return null;
    }

    private float[] buildInputVector(Map<String, ?> parserData) {
        String[] order = modelType.featureOrder;
        Map<String, String> keyMap = modelType.parserKeyByModelKey;
        float[] vec = new float[order.length];
        for (int i = 0; i < order.length; i++) {
            String modelKey = order[i];
            String parserKey = (keyMap != null && keyMap.containsKey(modelKey)) ? keyMap.get(modelKey) : modelKey;
            Object val = parserData.get(parserKey);
            if (val == null) {
                Log.w(TAG, "Missing feature: " + parserKey + " (model: " + modelKey + ")");
                return null;
            }
            float f;
            if (val instanceof Number) {
                f = ((Number) val).floatValue();
            } else {
                try {
                    f = Float.parseFloat(String.valueOf(val));
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Cannot parse feature " + parserKey + " = " + val);
                    return null;
                }
            }
            if (Float.isNaN(f) || Float.isInfinite(f)) {
                Log.w(TAG, "Invalid value for " + parserKey + ": " + f);
                return null;
            }
            vec[i] = f;
        }
        return vec;
    }

    public void close() {
        try {
            if (session != null) {
                session.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing session", e);
        }
    }
}
