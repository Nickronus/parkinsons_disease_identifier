from characteristic_saver.i_characteristic_saver import ICharacteristicSaver
from characteristic import Characteristic

class IEEEDatabaseExcelCharacteristicSaver(ICharacteristicSaver):
    """Декоратор к ExcelCharacteristicSaver, добавляющий колонку с информацией о том, является ли человек больным и имя человека.
    Информация берётся из названия папок базы данных IEEE Italian Parkinson's Voice and speech.
    """    
    def __init__(self, excel_characteristic_saver: ICharacteristicSaver):
        self.__excel_characteristic_saver = excel_characteristic_saver

    def save(self, header_and_data: dict[str, list[str]], output_filename: str):
        """Сохранить файл.

        Args:
            header_and_data (dict[str, list[str]]): Словарь из заголовков и списка данных для них.
            output_filename (str): Полное имя сохраняемого фала.
        """ 
        is_sick = 'IS SICK'
        name = 'NAME'
        header_and_data.update({name: []})
        header_and_data.update({is_sick: []})
        for i in range(len(header_and_data[Characteristic.FILEPATH.name])):
            filepath = header_and_data[Characteristic.FILEPATH.name][i]
            if "28 People with Parkinson's disease" in filepath:
                header_and_data[is_sick].append('Yes')
            elif "22 Elderly Healthy Control" in filepath:
                header_and_data[is_sick].append('No')
            elif "15 Young Healthy Control" in filepath:
                header_and_data[is_sick].append('No')
            else:
               header_and_data[is_sick].append('-')
            

            header_and_data[Characteristic.FILEPATH.name][i] = filepath.rsplit("\\", 1)[-1]
            header_and_data[name].append(filepath.rsplit('\\', 2)[1])
            
        self.__excel_characteristic_saver.save(header_and_data, output_filename)