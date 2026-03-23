package br.com.magnumbank.shared.infrastructure.client;

import br.com.magnumbank.shared.domain.dto.ModelResponseDTO;
import br.com.magnumbank.shared.domain.entity.Brand;
import br.com.magnumbank.shared.domain.service.FipeClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class FipeClientImpl implements FipeClient {

    private final FipeRestClient fipeRestClient;

    public FipeClientImpl(@RestClient FipeRestClient fipeRestClient) {
        this.fipeRestClient = fipeRestClient;
    }

    @Override
    public List<Brand> fetchBrands() {
        return fipeRestClient.getBrands().stream()
                .map(dto -> {
                    Brand brand = new Brand();
                    brand.codigo = dto.codigo();
                    brand.nome = dto.nome();
                    return brand;
                })
                .collect(Collectors.toList());
    }

    @Override
    public ModelResponseDTO fetchModels(String brandCode) {
        return fipeRestClient.getModels(brandCode);
    }
}
