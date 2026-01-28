# Модуль для вызова из Java (Chaquopy): парсинг WAV и получение признаков для ONNX-моделей.
import sys
import os

# Подключить путь к парсеру (Audio_Parser/src)
_parser_src = os.path.join(os.path.dirname(os.path.abspath(__file__)), "Audio_Parser", "src")
if _parser_src not in sys.path:
    sys.path.insert(0, _parser_src)

from file_parser import FileParser


def get_speech_features(file_path: str) -> dict:
    """
    Парсит WAV-файл как речь, извлекает признаки.
    Возвращает словарь {имя_признака: значение} для передачи в модель речи.
    """
    parser = FileParser()
    parser.parse_speech(file_path)
    return parser.get_data()


def get_voice_features(file_path: str) -> dict:
    """
    Парсит WAV-файл как голос (фоноваяция), извлекает признаки.
    Возвращает словарь {имя_признака: значение} для передачи в модель голоса.
    """
    parser = FileParser()
    parser.parse_voice(file_path)
    return parser.get_data()
