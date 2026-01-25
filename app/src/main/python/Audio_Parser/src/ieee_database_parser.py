from characteristic_extractor.abstract_characteristic_extractor_creator import AbstractCharacteristicExtractorCreator
from characteristic_saver.i_characteristic_saver import ICharacteristicSaver
from characteristic_processor.speech_characteristic_processor import SpeechCharacteristicProcessor
from characteristic_processor.voice_characteristic_processor import VoiceCharacteristicProcessor


class IEEEDatabaseParser():
    """Анализатор базы данных IEEE "Italian Parkinson's Voice and speech".
    """    
    def __init__(self, 
                characteristic_extractor_creator: AbstractCharacteristicExtractorCreator,
                characteristic_saver: ICharacteristicSaver):     
        self.__voice_characteristic_processor = VoiceCharacteristicProcessor(characteristic_extractor_creator, characteristic_saver)
        self.__speech_characteristic_processor = SpeechCharacteristicProcessor(characteristic_extractor_creator, characteristic_saver)

    def parse(self):
        """Для файлов из введённой папки с базой данных "Italian Parkinson's Voice and speech" будет проведён анализ.
        """        
        print("Enter Italian Parkinson's Voice and speech folder path: ")
        folder_path = input()
        print('Enter number of experiment: \n Voice experiment - 1 \n Speech experiment - 2')
        number_of_experiment = input()
        print('Enter output filename: ')
        output_filename = input()

        match number_of_experiment:
            case '1':
                self.__voice_characteristic_processor.process(folder_path, ["^VA", "^VA", "^VE", "^VI", "^VO", "^VU"], output_filename)

            case '2':
                self.__speech_characteristic_processor.process(folder_path, ["^B1", "^B2", "^FB", "^D1", "^D2"], output_filename)