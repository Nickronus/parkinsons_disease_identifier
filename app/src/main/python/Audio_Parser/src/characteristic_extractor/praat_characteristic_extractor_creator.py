from characteristic_extractor.abstract_characteristic_extractor_creator import AbstractCharacteristicExtractorCreator
from characteristic_extractor.i_characteristic_extractor import ICharacteristicExtractor
from characteristic_extractor.praat_characteristic_extractor import PraatCharacteristicExtractor

class PraatCharacteristicExtractorCreator(AbstractCharacteristicExtractorCreator):
    """Фабрика для создания извлекателей характеристик при помощи parselmouth (Praat).
    """    
    def create_characteristic_extractor(self, file_path: str) -> ICharacteristicExtractor:
        """Создать извлекателя характеристик аудиофайла.

        Args:
            file_path (str): Путь к аудиофайлу.

        Returns:
            ICharacteristicExtractor: Интерфейс для извлекателя характеристик.
        """   
        return PraatCharacteristicExtractor(file_path)

