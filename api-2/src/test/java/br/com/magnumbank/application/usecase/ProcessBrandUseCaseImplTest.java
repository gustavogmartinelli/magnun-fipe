package br.com.magnumbank.application.usecase;

import br.com.magnumbank.application.mapper.FipeMapper;
import br.com.magnumbank.shared.domain.dto.BrandDTO;
import br.com.magnumbank.shared.domain.dto.ModelResponseDTO;
import br.com.magnumbank.shared.domain.dto.VehicleDTO;
import br.com.magnumbank.shared.domain.entity.Brand;
import br.com.magnumbank.shared.domain.entity.Vehicle;
import br.com.magnumbank.shared.domain.repository.BrandRepository;
import br.com.magnumbank.shared.domain.repository.VehicleRepository;
import br.com.magnumbank.shared.domain.service.FipeClient;
import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheManager;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessBrandUseCaseImplTest {

    @Mock
    BrandRepository brandRepository;

    @Mock
    VehicleRepository vehicleRepository;

    @Mock
    FipeClient fipeClient;

    @Mock
    FipeMapper fipeMapper;

    @Mock
    CacheManager cacheManager;

    @Mock
    Cache cache;

    @InjectMocks
    ProcessBrandUseCaseImpl processBrandUseCase;

    private BrandDTO brandDto;
    private Brand brand;

    @BeforeEach
    void setUp() {
        brandDto = new BrandDTO("1", "Fiat");
        brand = new Brand();
        brand.codigo = "1";
        brand.nome = "Fiat";
        
        lenient().when(cacheManager.getCache("vehicles-cache")).thenReturn(Optional.of(cache));
        lenient().when(cacheManager.getCache("brands-cache")).thenReturn(Optional.of(cache));
        lenient().when(cache.invalidate(anyString())).thenReturn(Uni.createFrom().voidItem());
        lenient().when(cache.invalidateAll()).thenReturn(Uni.createFrom().voidItem());
    }

    @Test
    void shouldExecuteWithSuccess() {
        // Arrange
        ModelResponseDTO modelsResponse = new ModelResponseDTO(Collections.emptyList());
        
        when(fipeMapper.toEntity(brandDto)).thenReturn(brand);
        when(brandRepository.save(brand)).thenReturn(brand);
        when(fipeClient.fetchModels("1")).thenReturn(modelsResponse);

        try (MockedStatic<QuarkusTransaction> quarkusTransactionMock = mockStatic(QuarkusTransaction.class)) {
            // Act
            processBrandUseCase.execute(brandDto);

            // Assert
            verify(brandRepository).save(brand);
            verify(fipeClient).fetchModels("1");
            verify(fipeMapper).toEntity(brandDto);
        }
    }

    @Test
    void shouldSaveBrand() {
        // Arrange
        when(fipeMapper.toEntity(brandDto)).thenReturn(brand);
        when(brandRepository.save(brand)).thenReturn(brand);

        // Act
        Brand result = processBrandUseCase.saveBrand(brandDto);

        // Assert
        assertNotNull(result);
        assertEquals("Fiat", result.nome);
        verify(brandRepository).save(brand);
    }

    @Test
    void shouldFetchModelsFromFipe() {
        // Arrange
        ModelResponseDTO expectedResponse = new ModelResponseDTO(Collections.emptyList());
        when(fipeClient.fetchModels("1")).thenReturn(expectedResponse);

        // Act
        ModelResponseDTO result = processBrandUseCase.fetchModelsFromFipe(brand);

        // Assert
        assertNotNull(result);
        verify(fipeClient).fetchModels("1");
    }

    @Test
    void shouldSaveVehicleModels() {
        // Arrange
        VehicleDTO vehicleDto = new VehicleDTO("101", "Uno");
        ModelResponseDTO modelsResponse = new ModelResponseDTO(List.of(vehicleDto));
        Vehicle vehicle = new Vehicle();
        vehicle.codigo = "101";
        vehicle.modelo = "Uno";

        when(fipeMapper.toEntity(vehicleDto, brand)).thenReturn(vehicle);

        try (MockedStatic<QuarkusTransaction> quarkusTransactionMock = mockStatic(QuarkusTransaction.class)) {
            quarkusTransactionMock.when(() -> QuarkusTransaction.run(any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        Runnable runnable = invocation.getArgument(0);
                        runnable.run();
                        return null;
                    });

            // Act
            processBrandUseCase.saveVehicleModels(modelsResponse, brand);

            // Assert
            verify(vehicleRepository).save(vehicle);
            verify(cache).invalidate(brand.codigo);
        }
    }

    @Test
    void shouldHandleRetryableError() {
        // Arrange
        Exception retryableException = new ConnectException("Connection refused");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> processBrandUseCase.handleProcessingError(brandDto, retryableException));
        
        assertTrue(exception.getMessage().contains("Erro recuperável"));
        assertEquals(retryableException, exception.getCause());
    }

    @Test
    void shouldHandleNonRetryableError() {
        // Arrange
        Response response = Response.status(404).build();
        Exception nonRetryableException = new WebApplicationException(response);

        // Act
        assertDoesNotThrow(() -> processBrandUseCase.handleProcessingError(brandDto, nonRetryableException));
    }

    @Test
    void testIsRetryable() {
        // Network errors
        assertTrue(processBrandUseCase.isRetryable(new ConnectException()));
        assertTrue(processBrandUseCase.isRetryable(new SocketTimeoutException()));
        assertTrue(processBrandUseCase.isRetryable(new RuntimeException(new ConnectException())));

        // REST errors
        assertTrue(processBrandUseCase.isRetryable(new WebApplicationException(Response.status(500).build())));
        assertFalse(processBrandUseCase.isRetryable(new WebApplicationException(Response.status(400).build())));

        // Generic errors
        assertTrue(processBrandUseCase.isRetryable(new RuntimeException()));
    }
}
