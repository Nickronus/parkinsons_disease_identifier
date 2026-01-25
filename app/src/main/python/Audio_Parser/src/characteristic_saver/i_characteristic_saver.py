from abc import ABC, abstractmethod

from characteristic import Characteristic

class ICharacteristicSaver(ABC):
    """Интерфейс для классов, сохраняющих файлы.
    """    
    @abstractmethod
    def save(header_and_data: dict[str, list[str]], output_filename: str):
        """Сохранить файл.

        Args:
            header_and_data (dict[str, list[str]]): Словарь из заголовков и списка данных для них.
            output_filename (str): Полное имя сохраняемого фала.
        """        
        pass