from collections.abc import Callable

from characteristic import Characteristic
from characteristic_extractor.i_characteristic_extractor import ICharacteristicExtractor
from file_paths_extractor import FilePathsExtractor
from characteristic_processor.abstract_characteristic_processor import AbstractCharacteristicProcessor
from characteristic_extractor.abstract_characteristic_extractor_creator import AbstractCharacteristicExtractorCreator
from characteristic_saver.i_characteristic_saver import ICharacteristicSaver

class VoiceCharacteristicProcessor(AbstractCharacteristicProcessor):
    """Процессор характеристик для записей с звуками.
    """    
    def __init__(self, 
                characteristic_extractor_creator: AbstractCharacteristicExtractorCreator,
                characteristic_saver: ICharacteristicSaver):
        AbstractCharacteristicProcessor.__init__(self, characteristic_extractor_creator, characteristic_saver)

    def _create_characteristic_extractor_methods_list(self, characteristic_extractor: ICharacteristicExtractor) ->list[Callable[[], dict[Characteristic: float]]]:
        return [characteristic_extractor.get_jitter_ppq5,
                characteristic_extractor.get_jitter_local,
                characteristic_extractor.get_shimmer_local,
            #    characteristic_extractor.get_nhr,
                characteristic_extractor.get_hnr,
                characteristic_extractor.get_f0_mean,
                characteristic_extractor.get_f0_stdev,
                characteristic_extractor.get_f0_min,
                characteristic_extractor.get_f0_max,
                characteristic_extractor.get_f0_range,
                characteristic_extractor.get_f1,
                characteristic_extractor.get_f2,
                characteristic_extractor.get_f3,
                characteristic_extractor.get_f4,
                characteristic_extractor.get_intensity_mean,
                characteristic_extractor.get_intensity_stdev,
                characteristic_extractor.get_intensity_range,
                characteristic_extractor.get_total_duration]