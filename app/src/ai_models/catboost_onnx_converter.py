from catboost import CatBoostClassifier

model = CatBoostClassifier()
model.load_model("P:\\PROJECTS\\saved_models\\2_speech\\catboost_model.cbm")

model.save_model("2_speech_model.onnx", format="onnx")