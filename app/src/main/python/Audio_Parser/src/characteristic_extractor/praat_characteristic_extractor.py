import parselmouth
import statistics
import numpy as np

from characteristic_extractor.i_characteristic_extractor import ICharacteristicExtractor
from characteristic import Characteristic

class PraatCharacteristicExtractor(ICharacteristicExtractor):
    """Извлекатель характеристик аудиофайла. Работает с библиотекой parselmouth (Praat).
    Основная часть скриптов была взята тут https://github.com/drfeinberg/PraatScripts
    """    
    def __init__(self, file_path: str):
        self.__sound = parselmouth.Sound(file_path)
        self.__pitch = None
        self.__point_process = None
        self.__f0_min = None
        self.__f0_max = None

        self.__f1 = None
        self.__f2 = None
        self.__f3 = None
        self.__f4 = None

        self.__intensity = None

    def get_f0_mean(self) -> dict[Characteristic: float]:
        """Average value of the fundamental frequency (F0)."""
        if not self.__pitch:
            self.__make_pitch()

        f0_mean = parselmouth.praat.call(self.__pitch, "Get mean", 0, 0, 'Hertz')
        return {Characteristic.F0_MEAN: f0_mean}
    
    def get_f0_stdev(self) -> dict[Characteristic: float]:
        """Standard deviation of the fundamental frequency (F0)."""
        if not self.__pitch:
            self.__make_pitch()

        f0_stdev = parselmouth.praat.call(self.__pitch, "Get standard deviation", 0 ,0, 'Hertz')
        return {Characteristic.F0_STDEV: f0_stdev}
    
    def get_f0_min(self) -> dict[Characteristic: float]:
        """Minimum fundamental frequency (F0)."""
        if not self.__f0_min:
            self.__extract_f0min_and_f0max()

        return {Characteristic.F0_MIN: self.__f0_min}

    def get_f0_max(self) -> dict[Characteristic: float]:
        """Maximum fundamental frequency (F0)."""
        if not self.__f0_max:
            self.__extract_f0min_and_f0max()

        return {Characteristic.F0_MAX: self.__f0_max}

    def get_f0_range(self) -> dict[Characteristic: float]:
        """The range of the fundamental frequency (the difference between f0_max and f0_min)."""
        if not self.__f0_min:
            self.__extract_f0min_and_f0max()

        f0_range = self.__f0_max - self.__f0_min
        return {Characteristic.F0_RANGE: f0_range}

    def get_jitter_ppq5(self) -> dict[Characteristic: float]:
        """Five-point period perturbation quotient, the average absolute difference between a period 
        and the average of it and its four closest neighbors, divided by the average period."""
        if not self.__point_process:
            self.__make_point_process()

        ppq5_jitter = parselmouth.praat.call(self.__point_process, "Get jitter (ppq5)", 0, 0, 0.0001, 0.02, 1.3)

        return {Characteristic.JITTER_PPQ5: ppq5_jitter}
    
    def get_jitter_local(self) -> dict[Characteristic: float]:
        """Relative deviation of the fundamental tone periods (frequency stability parameter)."""
        if not self.__point_process:
            self.__make_point_process()

        local_jitter =  parselmouth.praat.call(self.__point_process, "Get jitter (local)", 0, 0, 0.0001, 0.02, 1.3)
        return {Characteristic.JITTER_LOCAL: local_jitter}

    def get_shimmer_local(self) -> dict[Characteristic: float]:
        """Average absolute difference between the amplitudes of consecutive periods, divided by the average amplitude."""
        if not self.__point_process:
            self.__make_point_process()

        local_shimmer =  parselmouth.praat.call([self.__sound, self.__point_process], "Get shimmer (local)", 0, 0, 0.0001, 0.02, 1.3, 1.6)
        return {Characteristic.SHIMMER_LOCAL: local_shimmer}

    def get_nhr(self) -> dict[Characteristic: float]:
        """Noise-to-harmonics ratio, the amplitude of noise relative to tonal components."""
        pass
        #TODO Если надо, реализовать. По идее, тоже самое, что и HNR.

    def get_hnr(self) -> dict[Characteristic: float]:
        """Harmonics-to-noise ratio, the amplitude of tonal relative to noise components."""
        if not self.__f0_min:
            self.__extract_f0min_and_f0max()

        harmonicity = parselmouth.praat.call(self.__sound, "To Harmonicity (cc)", 0.01, self.__f0_min, 0.1, 1.0)
        hnr = parselmouth.praat.call(harmonicity, "Get mean", 0, 0)
        return {Characteristic.HNR: hnr}

    def get_no_pauses(self) -> dict[Characteristic: float]:
        """The number of all pauses compared to total time duration, after removing silence period not lasting more than 60 ms."""
        pass

    def get_intensity_mean(self) -> dict[Characteristic: float]:
        """Average intensity (volume)."""
        if not self.__intensity:
            self.__intensity = self.__sound.to_intensity()

        intensity_mean = parselmouth.praat.call(self.__intensity, "Get mean", 0, 0)
        return {Characteristic.INTENSITY_MEAN: intensity_mean}

    def get_intensity_stdev(self) -> dict[Characteristic: float]:
        """Standard deviation of intensity."""
        if not self.__intensity:
            self.__intensity = self.__sound.to_intensity()

        intensity_stdev = parselmouth.praat.call(self.__intensity, "Get standard deviation", 0 ,0)
        return {Characteristic.INTENSITY_STDEV: intensity_stdev}

    def get_intensity_range(self) -> dict[Characteristic: float]:
        """Intensity range (the difference between maximum and minimum volume)."""
        if not self.__intensity:
            self.__intensity = self.__sound.to_intensity()

        min_intensity = parselmouth.praat.call(self.__intensity, "Get minimum", 0, 0, "Parabolic")
        #max_intensity = call(intensity, "Get maximum", 0, 0, "Parabolic")
        max_99_intensity = parselmouth.praat.call(self.__intensity, "Get quantile", 0, 0, 0.99)
        i_range = max_99_intensity - min_intensity
        return {Characteristic.INTENSITY_RANGE: i_range}

    def get_f1(self) -> dict[Characteristic: float]:
        """Formant f1"""        
        if not self.__f1:
            self.__extract_formants()
        
        return {Characteristic.F1: self.__f1}

    def get_f2(self) -> dict[Characteristic: float]:
        """Formant f2"""
        if not self.__f2:
            self.__extract_formants()
        
        return {Characteristic.F2: self.__f2}

    def get_f3(self) -> dict[Characteristic: float]:
        """Formant f3"""
        if not self.__f3:
            self.__extract_formants()
        
        return {Characteristic.F3: self.__f3}

    def get_f4(self) -> dict[Characteristic: float]:
        """Formant f4"""
        if not self.__f4:
            self.__extract_formants()
        
        return {Characteristic.F4: self.__f4}
    
    def get_total_duration(self) -> dict[Characteristic: float]:
        """Duration"""
        duration = self.__sound.get_total_duration()
        return {Characteristic.DURATION: duration}

    def __make_point_process(self):     
        if not self.__f0_min:
            self.__extract_f0min_and_f0max()

        self.__point_process = parselmouth.praat.call(self.__sound, "To PointProcess (periodic, cc)", self.__f0_min, self.__f0_max)

    def __make_pitch(self):
        #self.__pitch = parselmouth.praat.call(self.__sound, "To Pitch", 0.0, self.__f0_min, self.__f0_max)
        self.__pitch = self.__sound.to_pitch()

    def __extract_formants(self):
        if not self.__pitch:
            self.__make_pitch()

        if not self.__f0_min:
            self.__extract_f0min_and_f0max()

        pointProcess = parselmouth.praat.call(self.__sound, "To PointProcess (periodic, cc)", self.__f0_min, self.__f0_max)
        
        formants = parselmouth.praat.call(self.__sound, "To Formant (burg)", 0.0025, 5, 5000, 0.025, 50)
        numPoints = parselmouth.praat.call(pointProcess, "Get number of points")

        f1_list = []
        f2_list = []
        f3_list = []
        f4_list = []
        
        # Measure formants only at glottal pulses
        for point in range(0, numPoints):
            point += 1
            t = parselmouth.praat.call(pointProcess, "Get time from index", point)
            f1 = parselmouth.praat.call(formants, "Get value at time", 1, t, 'Hertz', 'Linear')
            f2 = parselmouth.praat.call(formants, "Get value at time", 2, t, 'Hertz', 'Linear')
            f3 = parselmouth.praat.call(formants, "Get value at time", 3, t, 'Hertz', 'Linear')
            f4 = parselmouth.praat.call(formants, "Get value at time", 4, t, 'Hertz', 'Linear')
            f1_list.append(f1)
            f2_list.append(f2)
            f3_list.append(f3)
            f4_list.append(f4)
        
        f1_list = [f1 for f1 in f1_list if str(f1) != 'nan']
        f2_list = [f2 for f2 in f2_list if str(f2) != 'nan']
        f3_list = [f3 for f3 in f3_list if str(f3) != 'nan']
        f4_list = [f4 for f4 in f4_list if str(f4) != 'nan']
        
        # calculate mean formants across pulses
        f1_mean = statistics.mean(f1_list)
        f2_mean = statistics.mean(f2_list)
        f3_mean = statistics.mean(f3_list)
        f4_mean = statistics.mean(f4_list)
        
        # calculate median formants across pulses, this is what is used in all subsequent calcualtions
        # you can use mean if you want, just edit the code in the boxes below to replace median with mean
        f1_median = statistics.median(f1_list)
        f2_median = statistics.median(f2_list)
        f3_median = statistics.median(f3_list)
        f4_median = statistics.median(f4_list)
        
        self.__f1 = f1_median
        self.__f2 = f2_median
        self.__f3 = f3_median
        self.__f4 = f4_median

    def __extract_f0min_and_f0max(self):
        if not self.__pitch:
            self.__make_pitch()

        f0_values_with_Nan = self.__pitch.selected_array['frequency']
        #f0_values = f0_values_with_Nan[~np.isnan(f0_values_with_Nan)] # Удаление NaN значений
        mask = ~np.isnan(f0_values_with_Nan) & (f0_values_with_Nan != 0)
        f0_values = f0_values_with_Nan[mask]

        # Определение min и max F0
        self.__f0_min = np.min(f0_values)
        self.__f0_max = np.max(f0_values)