import numpy as np
import os
import pandas as pd
from sklearn.model_selection import GroupKFold
from sklearn.metrics import accuracy_score, roc_auc_score, f1_score
from sklearn.base import clone
from sklearn.preprocessing import LabelEncoder
from catboost import CatBoostClassifier
from sklearn.utils import shuffle
import pickle
import joblib

def load_and_preprocess_data(filename, target_column, name_column, random_state=42):
    """Загружает и предобрабатывает данные с групповым перемешиванием."""
    try:
        df = pd.read_excel(filename)
        df = df.iloc[:, 1:]
        print("Данные успешно загружены из файла:", filename)
        print("Исходный размер данных:", df.shape)
    except Exception as e:
        print(f"Ошибка: {e}")
        exit()

    df = df.dropna()
    print(f"Размер после удаления пропусков: {df.shape}")

    if name_column not in df.columns:
        print(f"Ошибка: Столбец '{name_column}' не найден.")
        exit()

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

def ten_fold_cross_validation(df, target_column, name_column, model, random_state=42):
    #ВОТ ЭТА ХЕРЯ ДЛЯ 22000 
    #selected_features = ['Jitter(%)', 'PPE', 'Shimmer:APQ11', 'Shimmer:APQ3', 'HNR', 'Jitter(Abs)', 'Jitter:PPQ5', 'Shimmer(dB)']

    #79.13
    selected_features = ['F2', 'F1', 'SHIMMER_LOCAL', 'JITTER_PPQ5', 'F0_RANGE', 'INTENSITY_RANGE','HNR']


    # selected_features = ["Jitter(%)", "Jitter(Abs)", "Jitter:RAP", "Jitter:PPQ5", 
    #                      "Shimmer", "Shimmer(dB)", "Shimmer:APQ3", "Shimmer:APQ5", 
    #                      "Shimmer:APQ11", "HNR", "PPE"]
    # selected_features = ['SHIMMER_LOCAL', 'JITTER_PPQ5','HNR']
    #selected_features = ['INTENSITY_STDEV', 'INTENSITY_MEAN', 'DURATION', 'F3', 'HNR', 'F0_MEAN', 'F0_MIN']
    # selected_features = ['JITTER_PPQ5', 'SHIMMER_LOCAL', 'HNR', 'F0_MIN', 'F0_RANGE', 
    #                     'F2', 'F4', 'INTENSITY_MEAN', 'INTENSITY_STDEV', 'DURATION']
    
    X = df[selected_features]
    y = df[target_column]
    groups = df[name_column]
    
    group_kfold = GroupKFold(n_splits=10)
    metrics = {
        'accuracy': [],
        'roc_auc': [],
        'f1_score': []
    }
    groups = groups.astype(str)
    for fold, (train_idx, test_idx) in enumerate(group_kfold.split(X, y, groups), 1):
        X_train, X_test = X.iloc[train_idx], X.iloc[test_idx]
        y_train, y_test = y.iloc[train_idx], y.iloc[test_idx]
        
        fold_model = clone(model)
        fold_model.fit(
            X_train,
            y_train,
            eval_set=(X_test, y_test),
            use_best_model=True,
            verbose=False,
            early_stopping_rounds=200
        )
        
        y_pred = fold_model.predict(X_test)
        y_proba = fold_model.predict_proba(X_test)[:, 1]
        
        metrics['accuracy'].append(accuracy_score(y_test, y_pred))
        metrics['f1_score'].append(f1_score(y_test, y_pred))
        
        if len(np.unique(y_test)) >= 2:
            metrics['roc_auc'].append(roc_auc_score(y_test, y_proba))
        else:
            metrics['roc_auc'].append(np.nan)
        
        print(f"Fold {fold}:")
        print(f"  Patients in train: {len(np.unique(groups.iloc[train_idx]))}")
        print(f"  Patients in test: {len(np.unique(groups.iloc[test_idx]))}")
        print(f"  Accuracy: {metrics['accuracy'][-1]:.4f}")
        print(f"  F1 Score: {metrics['f1_score'][-1]:.4f}")
        if not np.isnan(metrics['roc_auc'][-1]):
            print(f"  ROC AUC: {metrics['roc_auc'][-1]:.4f}")
        try:
            print(f"  Best iteration: {fold_model.get_best_iteration()}")
        except Exception:
            pass
        print("-" * 50)
    
    print("\nИтоговые метрики:")
    print(f"Accuracy: {np.nanmean(metrics['accuracy']):.4f} ± {np.nanstd(metrics['accuracy']):.4f}")
    print(f"ROC AUC: {np.nanmean(metrics['roc_auc']):.4f} ± {np.nanstd(metrics['roc_auc']):.4f}")
    print(f"F1 Score: {np.nanmean(metrics['f1_score']):.4f} ± {np.nanstd(metrics['f1_score']):.4f}")
    
    return metrics, selected_features

def train_and_save_final_model(df, target_column, selected_features, model, save_dir='saved_models', random_state=42):
    """Обучает финальную модель на всех данных и сохраняет в разных форматах."""
    X = df[selected_features]
    y = df[target_column]
    
    # Обучаем модель на всех данных
    print("\n" + "="*50)
    print("Обучение финальной модели на всех данных...")
    final_model = clone(model)
    final_model.fit(X, y, verbose=100)
    
    # Вычисляем точность на всех данных
    y_pred = final_model.predict(X)
    accuracy = accuracy_score(y, y_pred)
    y_proba = final_model.predict_proba(X)[:, 1]
    roc_auc = roc_auc_score(y, y_proba)
    f1 = f1_score(y, y_pred)
    
    print(f"\nТочность финальной модели на всех данных:")
    print(f"Accuracy: {accuracy:.4f}")
    print(f"ROC AUC: {roc_auc:.4f}")
    print(f"F1 Score: {f1:.4f}")
    
    # Создаем папку для сохранения
    os.makedirs(save_dir, exist_ok=True)
    
    # Сохраняем модель в разных форматах
    model_name_base = "catboost_model"
    
    # 1. CatBoost native format (.cbm)
    cbm_path = os.path.join(save_dir, f"{model_name_base}.cbm")
    final_model.save_model(cbm_path)
    print(f"\nМодель сохранена в формате CatBoost: {cbm_path}")
    
    # 2. Pickle format (.pkl)
    pkl_path = os.path.join(save_dir, f"{model_name_base}.pkl")
    with open(pkl_path, 'wb') as f:
        pickle.dump(final_model, f)
    print(f"Модель сохранена в формате Pickle: {pkl_path}")
    
    # 3. Joblib format (.joblib)
    joblib_path = os.path.join(save_dir, f"{model_name_base}.joblib")
    joblib.dump(final_model, joblib_path)
    print(f"Модель сохранена в формате Joblib: {joblib_path}")
    
    # 4. JSON format (CatBoost поддерживает)
    json_path = os.path.join(save_dir, f"{model_name_base}.json")
    final_model.save_model(json_path, format='json')
    print(f"Модель сохранена в формате JSON: {json_path}")
    
    # Сохраняем информацию о признаках
    features_info_path = os.path.join(save_dir, "selected_features.txt")
    with open(features_info_path, 'w', encoding='utf-8') as f:
        f.write("Использованные признаки:\n")
        for i, feature in enumerate(selected_features, 1):
            f.write(f"{i}. {feature}\n")
        f.write(f"\nТочность модели: {accuracy:.4f}\n")
        f.write(f"ROC AUC: {roc_auc:.4f}\n")
        f.write(f"F1 Score: {f1:.4f}\n")
    print(f"Информация о признаках сохранена: {features_info_path}")
    
    print("\n" + "="*50)
    print(f"Все модели успешно сохранены в папку: {save_dir}")
    print("="*50)
    
    return final_model, accuracy

if __name__ == '__main__':
    filename = 'C:\\Users\\Hot\\Downloads\\123\\result(2).xlsx'
    #filename = 'C:\\Users\\Hot\\Downloads\\123\\output.xlsx'
    target_column = 'IS SICK'
    name_column = 'NAME'
    random_state = 42
    
    df = load_and_preprocess_data(filename, target_column, name_column, random_state)

    class_counts = df.groupby(name_column)[target_column].first().value_counts()
    print("\nРаспределение по пациентам:")
    print(class_counts)
    
    model = CatBoostClassifier(
        iterations=1111,
        learning_rate=0.027358491976863103,
        depth=10,
        l2_leaf_reg=5.114960409364526,
        border_count=225, 
        random_seed=random_state,
        verbose=0
    )

    # model = CatBoostClassifier(
    #     iterations=1731,
    #     learning_rate=0.04541312179923037,
    #     depth=9,
    #     l2_leaf_reg=7.135909452154734,
    #     border_count=246,
    #     random_strength=6.281579072909145,
    #     bagging_temperature=5.948264766659993,
    #     loss_function='Logloss',
    #     eval_metric='AUC',
    #     random_seed=random_state,
    #     thread_count=max(1, (os.cpu_count() or 4) - 1),
    #     verbose=0
    # )
    
    metrics, selected_features = ten_fold_cross_validation(df, target_column, name_column, model, random_state)
    
    # Обучаем и сохраняем финальную модель
    final_model, final_accuracy = train_and_save_final_model(
        df, target_column, selected_features, model, 
        save_dir='saved_models', random_state=random_state
    )
    
    print(f"\n{'='*50}")
    print(f"ИТОГОВАЯ ТОЧНОСТЬ МОДЕЛИ: {final_accuracy:.4f}")
    print(f"{'='*50}")