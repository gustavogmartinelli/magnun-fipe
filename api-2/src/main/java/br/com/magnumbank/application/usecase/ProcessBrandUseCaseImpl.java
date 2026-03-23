package br.com.magnumbank.application.usecase;

import br.com.magnumbank.application.mapper.FipeMapper;
import br.com.magnumbank.shared.domain.dto.BrandDTO;
import br.com.magnumbank.shared.domain.dto.ModelResponseDTO;
import br.com.magnumbank.shared.domain.entity.Brand;
import br.com.magnumbank.shared.domain.entity.Vehicle;
import br.com.magnumbank.shared.domain.repository.BrandRepository;
import br.com.magnumbank.shared.domain.repository.VehicleRepository;
import br.com.magnumbank.shared.domain.service.FipeClient;
import io.quarkus.cache.CacheManager;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ProcessBrandUseCaseImpl implements ProcessBrandUseCase {

    private static final Logger LOG = Logger.getLogger(ProcessBrandUseCaseImpl.class);

    @Inject
    BrandRepository brandRepository;

    @Inject
    VehicleRepository vehicleRepository;

    @Inject
    FipeClient fipeClient;

    @Inject
    FipeMapper fipeMapper;

    @Inject
    CacheManager cacheManager;

    @Override
    public void execute(BrandDTO brandDto) {
        LOG.infof("Iniciando execução do ProcessBrandUseCase para a marca: %s - %s", 
                brandDto.codigo(), brandDto.nome());
        
        try {
            Brand savedBrand = saveBrand(brandDto);
            ModelResponseDTO modelsResponse = fetchModelsFromFipe(savedBrand);
            saveVehicleModels(modelsResponse, savedBrand);
            
            LOG.infof("Execução do ProcessBrandUseCase finalizada com sucesso para a marca: %s", 
                    brandDto.nome());
        } catch (Exception e) {
            handleProcessingError(brandDto, e);
        }
    }

    @Override
    public Brand saveBrand(BrandDTO brandDto) {
        LOG.infof("Salvando/Atualizando marca: %s", brandDto.nome());
        Brand brand = fipeMapper.toEntity(brandDto);
        Brand savedBrand = brandRepository.save(brand);
        LOG.infof("Brand %s persistida com sucesso. Invalidando cache de marcas.", savedBrand.nome);
        
        // Invalida o cache da lista de marcas
        cacheManager.getCache("brands-cache").ifPresent(cache -> {
            cache.invalidateAll().await().indefinitely();
        });
        
        return savedBrand;
    }

    @Override
    public ModelResponseDTO fetchModelsFromFipe(Brand brand) {
        LOG.infof("Buscando modelos na API FIPE para a marca: %s", brand.nome);
        return fipeClient.fetchModels(brand.codigo);
    }

    @Override
    public void saveVehicleModels(ModelResponseDTO modelsResponse, Brand brand) {
        if (modelsResponse != null && modelsResponse.modelos() != null) {
            LOG.infof("Salvando %d modelos para a marca %s em um lote transacional.", 
                    modelsResponse.modelos().size(), brand.nome);
            
            QuarkusTransaction.run(() -> {
                modelsResponse.modelos().forEach(modelDto -> {
                    Vehicle vehicle = fipeMapper.toEntity(modelDto, brand);
                    vehicleRepository.save(vehicle);
                });
            });
            
            LOG.infof("Todos os modelos da marca %s foram persistidos.", brand.nome);

            // Invalida o cache distribuído (Redis) para que a API-1 reflita os novos dados imediatamente
            cacheManager.getCache("vehicles-cache").ifPresent(cache -> {
                LOG.infof("Invalidando cache 'vehicles-cache' para a marca %s", brand.codigo);
                cache.invalidate(brand.codigo).await().indefinitely();
            });
        } else {
            LOG.warnf("Nenhum modelo encontrado para a marca %s na API FIPE.", brand.nome);
        }
    }

    @Override
    public void handleProcessingError(BrandDTO brandDto, Exception e) {
        LOG.errorf("Erro crítico no processamento da marca %s (%s): %s", 
                brandDto.nome(), brandDto.codigo(), e.getMessage());

        if (isRetryable(e)) {
            LOG.infof("O erro é recuperável. O processamento será enviado para retry (RabbitMQ). Motivo: %s", 
                    e.getClass().getSimpleName());
            throw new RuntimeException("Erro recuperável no processamento da marca " + brandDto.nome(), e);
        } else {
            LOG.errorf("O erro NÃO é recuperável. O processamento desta marca será CANCELADO (não haverá retry). Motivo: %s", 
                    e.getClass().getSimpleName());
            // Ao capturar a exceção e não relançar, o RabbitMQ entenderá como sucesso (ACK) 
            // e a mensagem não voltará para a fila de retry.
        }
    }

    @Override
    public boolean isRetryable(Exception e) {
        // Erros de timeout ou indisponibilidade de rede geralmente devem ser retentados
        if (e instanceof java.net.ConnectException || 
            e instanceof java.net.SocketTimeoutException ||
            e.getCause() instanceof java.net.ConnectException) {
            return true;
        }

        // Se for uma exceção da API REST (Quarkus/Rest Client)
        if (e instanceof jakarta.ws.rs.WebApplicationException webEx) {
            int status = webEx.getResponse().getStatus();
            // 5xx (Erro no Servidor) -> Retryable
            // 4xx (Erro no Cliente/Not Found/Bad Request) -> Non-Retryable
            return status >= 500;
        }

        // Por padrão, erros inesperados (como banco de dados fora do ar) são retentados
        return true;
    }
}
