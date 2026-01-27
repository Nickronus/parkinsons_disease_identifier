from characteristic_saver.i_characteristic_saver import ICharacteristicSaver


class FileCharacteristicSaver(ICharacteristicSaver):
    """Сохранитель характеристик в памяти (одна строка данных для последующего получения через get_data).
    """

    def __init__(self):
        self.__data: dict[str, str | float] = {}

    def save(self, header_and_data: dict[str, list[str]], output_filename: str):
        """Сохранить первую строку данных в память (output_filename не используется).

        Args:
            header_and_data (dict[str, list[str]]): Словарь из заголовков и списка данных для них.
            output_filename (str): Не используется.
        """
        self.__data = {k: v[0] for k, v in header_and_data.items()}

    def get_data(self) -> dict[str, str | float]:
        """Вернуть последнюю сохранённую строку характеристик."""
        return self.__data