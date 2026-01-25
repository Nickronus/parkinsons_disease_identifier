import os, re

class FilePathsExtractor():
    """Извлекатель путей к файлам.
    """    
    def extract(folder_path: str, file_patterns: list[str]) -> list[str]:
        """Извлечь путь к файлам, соответствующим паттерну (регулярке), из переданной папки (включая все дочерние подпапки).

        Args:
            folder_path (str): Путь к папке с файлом.
            file_patterns (list[str]): Регулярное выражение для файла (берутся только файлы, удовлетворяющие регулярке).

        Returns:
            list[str]: Список файлов.
        """        
        filenames = []
        for root, _, files in os.walk(folder_path):
            for file in files:
                for pattern in file_patterns:
                    if re.match(pattern, file):
                        full_path = os.path.join(root, file)
                        filenames.append(full_path)
                        break
        
        print('Files quantity: ', len(filenames))
        return filenames