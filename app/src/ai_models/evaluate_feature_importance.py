"""
Скрипт оценки влияния выбранных признаков на модель.
Использует данные и признаки из script.py, вычисляет важность признаков
(CatBoost, перестановочная важность) и сохраняет отчёт.
"""

import numpy as np
import os
import pandas as pd
from sklearn.metrics import accuracy_score, roc_auc_score, f1_score
from sklearn.inspection import permutation_importance
from sklearn.model_selection import GroupKFold
from sklearn.base import clone
from catboost import CatBoostClassifier
import pickle
import joblib

# Признаки из script.py (для 22000)
SELECTED_FEATURES_22000 = [
    'Jitter(%)', 'PPE', 'Shimmer:APQ11', 'Shimmer:APQ3', 'HNR',
    'Jitter(Abs)', 'Jitter:PPQ5', 'Shimmer(dB)'
]
# Для фонации (79.13)
SELECTED_FEATURES_PHONATION = [
    'F2', 'F1', 'SHIMMER_LOCAL', 'JITTER_PPQ5', 'F0_RANGE',
    'INTENSITY_RANGE', 'HNR'
]


def load_and_preprocess_data(filename, target_column, name_column, random_state=42):
    """Загружает и предобрабатывает данные (совместимо с script.py)."""
    from sklearn.preprocessing import LabelEncoder
    try:
        df = pd.read_excel(filename)
        df = df.iloc[:, 1:]
        print("Данные загружены:", filename, "Размер:", df.shape)
    except Exception as e:
        print(f"Ошибка загрузки: {e}")
        raise

    df = df.dropna()
    if name_column not in df.columns:
        raise ValueError(f"Столбец '{name_column}' не найден.")

    unique_patients = df[name_column].unique()
    np.random.seed(random_state)
    shuffled_patients = np.random.permutation(unique_patients)
    shuffled_df = pd.concat([df[df[name_column] == patient] for patient in shuffled_patients])
    shuffled_df = shuffled_df.reset_index(drop=True)

    for column in shuffled_df.columns:
        if shuffled_df[column].dtype == 'object' and column != name_column:
            le = LabelEncoder()
            shuffled_df[column] = le.fit_transform(shuffled_df[column].astype(str))

    return shuffled_df


def get_available_features(df, requested_features):
    """Оставляет только признаки, которые есть в данных."""
    available = [f for f in requested_features if f in df.columns]
    missing = [f for f in requested_features if f not in df.columns]
    if missing:
        print(f"В данных отсутствуют признаки: {missing}")
    return available


def catboost_importance(model, feature_names):
    """Важность признаков из обученной модели CatBoost."""
    imp = model.get_feature_importance()
    return pd.Series(imp, index=feature_names).sort_values(ascending=False)


def permutation_importance_cv(
    df, target_column, name_column, feature_names, model, n_repeats=5,
    random_state=42, n_splits=5
):
    """
    Перестановочная важность с групповой кросс-валидацией:
    усреднение по фолдам и повторным перестановкам.
    """
    X = df[feature_names]
    y = df[target_column]
    groups = df[name_column].astype(str)
    group_kfold = GroupKFold(n_splits=n_splits)

    importances = []
    for fold, (train_idx, test_idx) in enumerate(group_kfold.split(X, y, groups), 1):
        X_train, X_test = X.iloc[train_idx], X.iloc[test_idx]
        y_train, y_test = y.iloc[train_idx], y.iloc[test_idx]

        fold_model = clone(model)
        fold_model.fit(
            X_train, y_train,
            eval_set=(X_test, y_test),
            use_best_model=True,
            verbose=False,
            early_stopping_rounds=200
        )

        result = permutation_importance(
            fold_model, X_test, y_test,
            n_repeats=n_repeats,
            random_state=random_state,
            scoring='roc_auc',
            n_jobs=-1
        )
        importances.append(result.importances_mean)

    mean_imp = np.mean(importances, axis=0)
    std_imp = np.std(importances, axis=0)
    return pd.Series(mean_imp, index=feature_names), pd.Series(std_imp, index=feature_names)


def drop_one_importance(
    df, target_column, name_column, feature_names, model, random_state=42, n_splits=5
):
    """
    Влияние признака через падение качества при его удалении (ablation).
    Возвращает изменение ROC AUC при удалении каждого признака.
    """
    X_full = df[feature_names]
    y = df[target_column]
    groups = df[name_column].astype(str)
    group_kfold = GroupKFold(n_splits=n_splits)

    def cv_roc_auc(X_data):
        aucs = []
        for train_idx, test_idx in group_kfold.split(X_data, y, groups):
            X_train = X_data.iloc[train_idx]
            X_test = X_data.iloc[test_idx]
            y_train, y_test = y.iloc[train_idx], y.iloc[test_idx]
            m = clone(model)
            m.fit(
                X_train, y_train,
                eval_set=(X_test, y_test),
                use_best_model=True,
                verbose=False,
                early_stopping_rounds=200
            )
            proba = m.predict_proba(X_test)[:, 1]
            if len(np.unique(y_test)) >= 2:
                aucs.append(roc_auc_score(y_test, proba))
        return np.mean(aucs) if aucs else np.nan

    baseline_auc = cv_roc_auc(X_full)
    drop_effects = {}
    for f in feature_names:
        cols = [c for c in feature_names if c != f]
        auc_without = cv_roc_auc(df[cols])
        drop_effects[f] = baseline_auc - auc_without  # положительное = признак полезен

    return pd.Series(drop_effects).sort_values(ascending=False), baseline_auc


def run_evaluation(
    filename,
    target_column='IS SICK',
    name_column='NAME',
    selected_features=None,
    use_saved_model=True,
    save_dir='saved_models',
    output_dir='feature_importance_results',
    random_state=42,
    n_perm_repeats=5,
    n_splits=5,
    run_permutation=True,
    run_ablation=False,
):
    """
    Запуск полной оценки важности признаков.
    """
    if selected_features is None:
        selected_features = SELECTED_FEATURES_22000.copy()

    os.makedirs(output_dir, exist_ok=True)

    print("Загрузка данных...")
    df = load_and_preprocess_data(filename, target_column, name_column, random_state)
    feature_names = get_available_features(df, selected_features)
    if len(feature_names) < 2:
        raise ValueError("В данных должно быть хотя бы 2 признака из списка.")

    model = CatBoostClassifier(
        iterations=1731,
        learning_rate=0.04541312179923037,
        depth=9,
        l2_leaf_reg=7.135909452154734,
        border_count=246,
        random_strength=6.281579072909145,
        bagging_temperature=5.948264766659993,
        loss_function='Logloss',
        eval_metric='AUC',
        random_seed=random_state,
        thread_count=max(1, (os.cpu_count() or 4) - 1),
        verbose=0
    )

    X = df[feature_names]
    y = df[target_column]
    trained_model = None

    if use_saved_model and save_dir:
        pkl_path = os.path.join(save_dir, 'catboost_model.pkl')
        joblib_path = os.path.join(save_dir, 'catboost_model.joblib')
        if os.path.isfile(pkl_path):
            with open(pkl_path, 'rb') as f:
                trained_model = pickle.load(f)
            print(f"Загружена сохранённая модель: {pkl_path}")
        elif os.path.isfile(joblib_path):
            trained_model = joblib.load(joblib_path)
            print(f"Загружена сохранённая модель: {joblib_path}")
        else:
            print("Сохранённая модель не найдена, обучаем новую на всех данных.")
            trained_model = clone(model)
            trained_model.fit(X, y, verbose=False)
    else:
        trained_model = clone(model)
        trained_model.fit(X, y, verbose=False)

    results = {}

    # 1. Важность CatBoost
    print("\n--- Важность признаков (CatBoost) ---")
    cb_imp = catboost_importance(trained_model, feature_names)
    results['catboost_importance'] = cb_imp
    for name, val in cb_imp.items():
        print(f"  {name}: {val:.2f}")

    # 2. Перестановочная важность (опционально)
    if run_permutation:
        print("\n--- Перестановочная важность (ROC AUC, CV) ---")
        perm_mean, perm_std = permutation_importance_cv(
            df, target_column, name_column, feature_names, model,
            n_repeats=n_perm_repeats, random_state=random_state, n_splits=n_splits
        )
        results['permutation_importance_mean'] = perm_mean
        results['permutation_importance_std'] = perm_std
        for name in perm_mean.index:
            print(f"  {name}: {perm_mean[name]:.4f} ± {perm_std[name]:.4f}")

    # 3. Ablation: влияние удаления признака (опционально)
    if run_ablation:
        print("\n--- Влияние удаления признака (ablation, ROC AUC) ---")
        drop_imp, baseline_auc = drop_one_importance(
            df, target_column, name_column, feature_names, model,
            random_state=random_state, n_splits=n_splits
        )
        results['ablation_drop_auc'] = drop_imp
        results['baseline_roc_auc'] = baseline_auc
        print(f"  Baseline ROC AUC: {baseline_auc:.4f}")
        for name, val in drop_imp.items():
            print(f"  Без '{name}': ΔAUC = {val:.4f}")

    # Сводная таблица
    report = pd.DataFrame(index=feature_names)
    report['catboost_importance'] = report.index.map(lambda x: results['catboost_importance'].get(x, np.nan))
    if run_permutation:
        report['permutation_importance'] = report.index.map(lambda x: results['permutation_importance_mean'].get(x, np.nan))
        report['permutation_std'] = report.index.map(lambda x: results['permutation_importance_std'].get(x, np.nan))
    if run_ablation:
        report['ablation_drop_auc'] = report.index.map(lambda x: results['ablation_drop_auc'].get(x, np.nan))

    report = report.sort_values('catboost_importance', ascending=False)
    report_path = os.path.join(output_dir, 'feature_importance_report.csv')
    report.to_csv(report_path, encoding='utf-8-sig')
    print(f"\nОтчёт сохранён: {report_path}")

    # Сохраняем также в Excel, если возможно
    excel_path = os.path.join(output_dir, 'feature_importance_report.xlsx')
    try:
        report.to_excel(excel_path)
        print(f"Отчёт сохранён (Excel): {excel_path}")
    except Exception as e:
        print(f"Excel не сохранён: {e}")

    return results, report


if __name__ == '__main__':
    filename = r'P:\PROJECTS\saved_models\data\2_speech.xlsx'
    target_column = 'IS SICK'
    name_column = 'NAME'
    random_state = 42

    # Выбор набора признаков
    selected_features = SELECTED_FEATURES_22000
    # selected_features = SELECTED_FEATURES_PHONATION

    results, report = run_evaluation(
        filename=filename,
        target_column=target_column,
        name_column=name_column,
        selected_features=selected_features,
        use_saved_model=True,
        save_dir='saved_models',
        output_dir='feature_importance_results',
        random_state=random_state,
        n_perm_repeats=5,
        n_splits=5,
        run_permutation=True,
        run_ablation=False,  # True для оценки через удаление признаков (дольше)
    )
