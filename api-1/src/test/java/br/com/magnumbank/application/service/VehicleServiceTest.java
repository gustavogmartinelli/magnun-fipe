package br.com.magnumbank.application.service;

import br.com.magnumbank.presentation.rest.dto.VehicleUpdateDTO;
import br.com.magnumbank.shared.domain.entity.Vehicle;
import br.com.magnumbank.shared.domain.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    VehicleRepository vehicleRepository;

    @InjectMocks
    VehicleService vehicleService;

    @Test
    void shouldGetVehiclesByBrand() {
        // Arrange
        String brandCode = "1";
        Vehicle v1 = new Vehicle();
        v1.codigo = "101";
        v1.modelo = "Uno";
        
        when(vehicleRepository.findByBrandCode(brandCode)).thenReturn(List.of(v1));

        // Act
        List<Vehicle> result = vehicleService.getVehiclesByBrand(brandCode);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Uno", result.get(0).modelo);
        verify(vehicleRepository).findByBrandCode(brandCode);
    }

    @Test
    void shouldUpdateVehicleWithSuccess() {
        // Arrange
        String vehicleCode = "101";
        String brandCode = "1";
        VehicleUpdateDTO updates = new VehicleUpdateDTO("Uno Novo", "Nova obs");
        
        Vehicle existingVehicle = new Vehicle();
        existingVehicle.codigo = vehicleCode;
        existingVehicle.modelo = "Uno Antigo";

        when(vehicleRepository.findByCodeAndBrandCode(vehicleCode, brandCode))
                .thenReturn(Optional.of(existingVehicle));

        // Act
        boolean result = vehicleService.updateVehicle(vehicleCode, brandCode, updates);

        // Assert
        assertTrue(result);
        assertEquals("Uno Novo", existingVehicle.modelo);
        assertEquals("Nova obs", existingVehicle.observacoes);
        verify(vehicleRepository).findByCodeAndBrandCode(vehicleCode, brandCode);
    }

    @Test
    void shouldReturnFalseWhenVehicleNotFoundForUpdate() {
        // Arrange
        String vehicleCode = "999";
        String brandCode = "1";
        VehicleUpdateDTO updates = new VehicleUpdateDTO("Modelo", "Obs");

        when(vehicleRepository.findByCodeAndBrandCode(vehicleCode, brandCode))
                .thenReturn(Optional.empty());

        // Act
        boolean result = vehicleService.updateVehicle(vehicleCode, brandCode, updates);

        // Assert
        assertFalse(result);
        verify(vehicleRepository).findByCodeAndBrandCode(vehicleCode, brandCode);
    }
}
