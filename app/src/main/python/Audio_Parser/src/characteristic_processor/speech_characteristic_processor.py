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
        return[characteristic_extractor.get_jitter_ppq5,
                characteristic_extractor.get_jitter_local,
                characteristic_extractor.get_shimmer_local,
                characteristic_extractor.get_hnr,
                characteristic_extractor.get_jitter_abs,
                characteristic_extractor.get_jitter_rap,
                characteristic_extractor.get_shimmer_db,
                characteristic_extractor.get_shimmer_apq3,
                characteristic_extractor.get_shimmer_apq5,
                characteristic_extractor.get_shimmer_apq11,
                characteristic_extractor.get_ppe]
