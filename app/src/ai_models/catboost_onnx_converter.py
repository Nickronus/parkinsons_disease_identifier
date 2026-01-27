from catboost import CatBoostClassifier

model = CatBoostClassifier()
model.load_model("P:\\PROJECTS\\saved_models\\voice\\catboost_model.cbm")

model.save_model("voice_model.onnx", format="onnx")