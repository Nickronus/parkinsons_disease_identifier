from characteristic_extractor.abstract_characteristic_extractor_creator import AbstractCharacteristicExtractorCreator
from characteristic_extractor.praat_characteristic_extractor_creator import PraatCharacteristicExtractorCreator
from characteristic_saver.file_characteristic_saver import FileCharacteristicSaver
from characteristic_saver.i_characteristic_saver import ICharacteristicSaver
from characteristic_processor.speech_characteristic_processor import SpeechCharacteristicProcessor
from characteristic_processor.voice_characteristic_processor import VoiceCharacteristicProcessor


class FileParser:
    """Парсер одного аудиофайла (голос или речь) с сохранением характеристик в память.
    """    
    def __init__(self):
        characteristic_extractor_creator: AbstractCharacteristicExtractorCreator = PraatCharacteristicExtractorCreator()
        characteristic_saver: ICharacteristicSaver = FileCharacteristicSaver()
        self.__voice_characteristic_processor = VoiceCharacteristicProcessor(characteristic_extractor_creator, characteristic_saver)
        self.__speech_characteristic_processor = SpeechCharacteristicProcessor(characteristic_extractor_creator, characteristic_saver)

    def parse_voice(self, file_path: str):    
        output_filename = ""
        self.__voice_characteristic_processor.process([file_path], output_filename)


    def parse_speech(self, file_path: str):    
        output_filename = ""
        self.__speech_characteristic_processor.process([file_path], output_filename)