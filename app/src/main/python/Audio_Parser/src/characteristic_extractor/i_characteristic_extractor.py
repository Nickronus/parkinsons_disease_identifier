from abc import ABC, abstractmethod

from characteristic import Characteristic

class ICharacteristicExtractor(ABC):
    """Интерфейс для извлекателя характеристик.
    """    
    @abstractmethod
    def get_f0_mean(self) -> dict[Characteristic: float]:
        """Average value of the fundamental frequency (F0)."""
        pass

    @abstractmethod
    def get_f0_stdev(self) -> dict[Characteristic: float]:
        """Standard deviation of the fundamental frequency (F0)."""
        pass

    @abstractmethod
    def get_f0_min(self) -> dict[Characteristic: float]:
        """Minimum fundamental frequency (F0)."""
        pass

    @abstractmethod
    def get_f0_max(self) -> dict[Characteristic: float]:
        """Maximum fundamental frequency (F0)."""
        pass

    @abstractmethod
    def get_f0_range(self) -> dict[Characteristic: float]:
        """The range of the fundamental frequency (the difference between f0_max and f0_min)."""
        pass

    @abstractmethod
    def get_jitter_ppq5(self) -> dict[Characteristic: float]:
        """Five-point period perturbation quotient, the average absolute difference between a period 
        and the average of it and its four closest neighbors, divided by the average period."""
        pass

    @abstractmethod
    def get_jitter_local(self) -> dict[Characteristic: float]:
        """Relative deviation of the fundamental tone periods (frequency stability parameter)."""
        pass

    @abstractmethod
    def get_shimmer_local(self) -> dict[Characteristic: float]:
        """Average absolute difference between the amplitudes of consecutive periods, divided by the average amplitude."""
        pass

    @abstractmethod
    def get_nhr(self) -> dict[Characteristic: float]:
        """Noise-to-harmonics ratio, the amplitude of noise relative to tonal components."""
        pass

    @abstractmethod
    def get_hnr(self) -> dict[Characteristic: float]:
        """Harmonics-to-noise ratio, the amplitude of tonal relative to noise components."""
        pass

    @abstractmethod
    def get_no_pauses(self) -> dict[Characteristic: float]:
        """The number of all pauses compared to total time duration, after removing silence period not lasting more than 60 ms."""
        pass

    @abstractmethod
    def get_intensity_mean(self) -> dict[Characteristic: float]:
        """Average intensity (volume)."""
        pass

    @abstractmethod
    def get_intensity_stdev(self) -> dict[Characteristic: float]:
        """Standard deviation of intensity."""
        pass

    @abstractmethod
    def get_intensity_range(self) -> dict[Characteristic: float]:
        """Intensity range (the difference between maximum and minimum volume)."""
        pass

    @abstractmethod
    def get_f1(self) -> dict[Characteristic: float]:
        """Formant f1"""        
        pass

    @abstractmethod
    def get_f2(self) -> dict[Characteristic: float]:
        """Formant f2"""
        pass

    @abstractmethod
    def get_f3(self) -> dict[Characteristic: float]:
        """Formant f3"""
        pass

    @abstractmethod
    def get_f4(self) -> dict[Characteristic: float]:
        """Formant f4"""
        pass

    @abstractmethod
    def get_total_duration(self) -> dict[Characteristic: float]:
        """Duration"""
        pass

