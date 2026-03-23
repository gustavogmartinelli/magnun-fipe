package br.com.magnumbank.application.service;

import br.com.magnumbank.shared.domain.dto.BrandDTO;
import br.com.magnumbank.shared.domain.entity.Brand;
import br.com.magnumbank.shared.domain.repository.BrandRepository;
import br.com.magnumbank.shared.domain.service.FipeClient;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    FipeClient fipeClient;

    @Mock
    BrandRepository brandRepository;

    @Mock
    Emitter<BrandDTO> brandEmitter;

    private BrandService brandService;

    @BeforeEach
    void setUp() {
        brandService = new BrandService(fipeClient, brandRepository, brandEmitter);
    }

    @Test
    void shouldExecuteInitialLoadWithSuccess() {
        // Arrange
        Brand brand1 = new Brand();
        brand1.codigo = "1";
        brand1.nome = "Fiat";
        Brand brand2 = new Brand();
        brand2.codigo = "2";
        brand2.nome = "Ford";
        List<Brand> brands = List.of(brand1, brand2);

        when(fipeClient.fetchBrands()).thenReturn(brands);

        // Act
        brandService.initialLoad();

        // Assert
        verify(fipeClient).fetchBrands();
        verify(brandEmitter, times(2)).send(ArgumentMatchers.<BrandDTO>any());
        
        ArgumentCaptor<BrandDTO> captor = ArgumentCaptor.forClass(BrandDTO.class);
        verify(brandEmitter, times(2)).send(captor.capture());
        
        List<BrandDTO> sentBrands = captor.getAllValues();
        assertEquals("1", sentBrands.get(0).codigo());
        assertEquals("Fiat", sentBrands.get(0).nome());
        assertEquals("2", sentBrands.get(1).codigo());
        assertEquals("Ford", sentBrands.get(1).nome());
    }

    @Test
    void shouldThrowExceptionWhenInitialLoadFails() {
        // Arrange
        RuntimeException expectedException = new RuntimeException("API Fipe Offline");
        when(fipeClient.fetchBrands()).thenThrow(expectedException);

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> brandService.initialLoad());
        assertEquals("API Fipe Offline", thrown.getMessage());
        
        verify(fipeClient).fetchBrands();
        verify(brandEmitter, never()).send(ArgumentMatchers.<BrandDTO>any());
    }

    @Test
    void shouldGetBrandsFromRepository() {
        // Arrange
        Brand brand1 = new Brand();
        brand1.codigo = "1";
        brand1.nome = "Fiat";
        List<Brand> expectedBrands = List.of(brand1);
        when(brandRepository.findAllBrands()).thenReturn(expectedBrands);

        // Act
        List<Brand> result = brandService.getBrands();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Fiat", result.get(0).nome);
        verify(brandRepository).findAllBrands();
    }
}
