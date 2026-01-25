from collections.abc import Callable

from characteristic import Characteristic
from characteristic_extractor.i_characteristic_extractor import ICharacteristicExtractor
from file_paths_extractor import FilePathsExtractor
from characteristic_processor.abstract_characteristic_processor import AbstractCharacteristicProcessor
from characteristic_extractor.abstract_characteristic_extractor_creator import AbstractCharacteristicExtractorCreator
from characteristic_saver.i_characteristic_saver import ICharacteristicSaver

class SpeechCharacteristicProcessor(AbstractCharacteristicProcessor):
    """Процессор характеристик для записей с речью.
    """    
    def __init__(self, 
                characteristic_extractor_creator: AbstractCharacteristicExtractorCreator,
                characteristic_saver: ICharacteristicSaver):
        AbstractCharacteristicProcessor.__init__(self, characteristic_extractor_creator, characteristic_saver)

    def _create_characteristic_extractor_methods_list(self, characteristic_extractor: ICharacteristicExtractor) ->list[Callable[[], dict[Characteristic: float]]]:
        return [characteristic_extractor.get_f0_mean,
                characteristic_extractor.get_no_pauses,
                characteristic_extractor.get_intensity_SD]
                
