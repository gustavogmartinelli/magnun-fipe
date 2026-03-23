package br.com.magnumbank.presentation.worker;

import br.com.magnumbank.application.usecase.ProcessBrandUseCase;
import br.com.magnumbank.shared.domain.dto.BrandDTO;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
public class BrandConsumer {

    private static final Logger LOG = Logger.getLogger(BrandConsumer.class);

    @Inject
    ProcessBrandUseCase processBrandUseCase;

    @Incoming("brands")
    @Blocking
    public void consume(JsonObject brandJson) {
        BrandDTO brandDto = brandJson.mapTo(BrandDTO.class);
        LOG.infof("Mensagem recebida para a marca: %s - %s. Iniciando processamento...", 
                brandDto.codigo(), brandDto.nome());
        
        processBrandUseCase.execute(brandDto);
        
        LOG.infof("Processamento da marca %s concluído com sucesso.", brandDto.nome());
    }
}
