package br.com.magnumbank.shared.domain.service;

import br.com.magnumbank.shared.domain.dto.ModelResponseDTO;
import br.com.magnumbank.shared.domain.entity.Brand;
import java.util.List;

public interface FipeClient {
    List<Brand> fetchBrands();
    ModelResponseDTO fetchModels(String brandCode);
}
