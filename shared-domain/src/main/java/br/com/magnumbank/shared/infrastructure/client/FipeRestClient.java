package br.com.magnumbank.shared.infrastructure.client;

import br.com.magnumbank.shared.domain.dto.BrandDTO;
import br.com.magnumbank.shared.domain.dto.ModelResponseDTO;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import java.util.List;

@Path("/carros/marcas")
@RegisterRestClient(configKey = "fipe-api")
public interface FipeRestClient {

    @GET
    List<BrandDTO> getBrands();

    @GET
    @Path("/{brandCode}/modelos")
    ModelResponseDTO getModels(@PathParam("brandCode") String brandCode);
}
