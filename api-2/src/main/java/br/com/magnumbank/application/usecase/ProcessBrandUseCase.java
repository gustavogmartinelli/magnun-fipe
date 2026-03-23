package br.com.magnumbank.application.usecase;

import br.com.magnumbank.shared.domain.dto.BrandDTO;
import br.com.magnumbank.shared.domain.dto.ModelResponseDTO;
import br.com.magnumbank.shared.domain.entity.Brand;

public interface ProcessBrandUseCase {
    void execute(BrandDTO brandDto);
    Brand saveBrand(BrandDTO brandDto);
    ModelResponseDTO fetchModelsFromFipe(Brand brand);
    void saveVehicleModels(ModelResponseDTO modelsResponse, Brand brand);
    void handleProcessingError(BrandDTO brandDto, Exception e);
    boolean isRetryable(Exception e);
}
