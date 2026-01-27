from abc import ABC, abstractmethod
from collections.abc import Callable

from characteristic import Characteristic
from characteristic_extractor.abstract_characteristic_extractor_creator import AbstractCharacteristicExtractorCreator
from characteristic_extractor.i_characteristic_extractor import ICharacteristicExtractor
from characteristic_saver.i_characteristic_saver import ICharacteristicSaver

class AbstractCharacteristicProcessor(ABC):
    """Абстрактный процессор характеристик аудиофайла.
    """    
    def __init__(self, 
                characteristic_extractor_creator: AbstractCharacteristicExtractorCreator,
                characteristic_saver: ICharacteristicSaver):
        self._characteristic_extractor_creator = characteristic_extractor_creator
        self._characteristic_saver = characteristic_saver

    def process(self, file_paths_list: list[str], output_filename: str):
        """Извлекает характеристики аудиофайлов (ICharacteristicExtractor),
        сохраняет данные (ICharacteristicSaver).

        Args:
            file_paths_list (list[str]): Список путей к обрабатываемым файлам.
            output_filename (str): Имя сохраняемого файла.
        """        
        header_and_data: dict[str, list[str]] = {}
        header_and_data_node = {Characteristic.FILEPATH.name: []}
        header_and_data.update(header_and_data_node)
        file_counter = 0
        print('Start processing.')
        for file_path in file_paths_list:
            try:
                characteristic_extractor: ICharacteristicExtractor = \
                    self._characteristic_extractor_creator.create_characteristic_extractor(file_path)
            except Exception as e:
                print(f"Error: cant create characteristic extractor to {file_path}. [{e}]")
                continue

            methods: list[Callable[[], dict[Characteristic: float]]] = \
                self._create_characteristic_extractor_methods_list(characteristic_extractor)
            
            try:
                characteristics_dict: dict[Characteristic: float] = {}
                for method in methods:
                    characteristics_dict.update(method())
            except Exception as e:
                print(f"Error: cant get characteristic. [{e}]")
                continue

            header_and_data[Characteristic.FILEPATH.name].append(file_path)
            for key, value in characteristics_dict.items():
                if key.name not in header_and_data:
                    header_and_data[key.name] = []

                header_and_data[key.name].append(value)

            file_counter += 1
            print(f"Processed files: {file_counter}", end='\r')

        print('\n')

        run: bool = True
        while run:
            try:
                self._characteristic_saver.save(header_and_data, output_filename)
                print('Saved successful.')
                run = False
            except Exception as e:
                print(f"Error: cant save characteristics file. [{e}]")
                print('Enter "q" to close program, or other key to try save again.')
                if input() == 'q':
                    run = False


    @abstractmethod
    def _create_characteristic_extractor_methods_list(self, characteristic_extractor: ICharacteristicExtractor) ->list[Callable[[], dict[Characteristic: float]]]:
        """Создать список вызываемых у ICharacteristicExtractor методов.
        Реализации статически закрепляют список характеристик, которые необходимо получить.
        """
        pass