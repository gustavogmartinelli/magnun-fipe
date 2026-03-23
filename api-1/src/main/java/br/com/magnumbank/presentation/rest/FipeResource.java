package br.com.magnumbank.presentation.rest;

import br.com.magnumbank.application.service.BrandService;
import br.com.magnumbank.application.service.VehicleService;
import br.com.magnumbank.presentation.rest.dto.VehicleUpdateDTO;
import br.com.magnumbank.presentation.rest.dto.ErrorResponse;
import br.com.magnumbank.shared.domain.entity.Brand;
import br.com.magnumbank.shared.domain.entity.Vehicle;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import java.util.List;

@Path("/fipe")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "FIPE API-1", description = "Endpoints para carga e consulta de dados FIPE")
@SecurityRequirement(name = "Authentication")
@RolesAllowed("user")
public class FipeResource {

    @Inject
    BrandService brandService;

    @Inject
    VehicleService vehicleService;

    @POST
    @Path("/load")
    @Operation(summary = "Acionar carga inicial", description = "Busca marcas na API FIPE e envia para fila RabbitMQ")
    public Response initialLoad() {
        brandService.initialLoad();
        return Response.accepted().build();
    }

    @GET
    @Path("/brands")
    @Operation(summary = "Listar marcas", description = "Busca as marcas armazenadas no banco de dados")
    public Response getBrands() {
        List<Brand> brands = brandService.getBrands();
        if (brands.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Nenhuma marca encontrada", "NOT_FOUND"))
                    .build();
        }
        return Response.ok(brands).build();
    }

    @GET
    @Path("/vehicles/{brandCode}")
    @Operation(summary = "Buscar veículos por marca", description = "Busca códigos, modelos e observações por marca no banco com cache Redis")
    public Response getVehiclesByBrand(@PathParam("brandCode") String brandCode) {
        List<Vehicle> vehicles = vehicleService.getVehiclesByBrand(brandCode);
        if (vehicles.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Marca não encontrada", "NOT_FOUND"))
                    .build();
        }
        return Response.ok(vehicles).build();
    }

    @PUT
    @Path("/vehicles/{brandCode}/{vehicleCode}")
    @Operation(summary = "Atualizar veículo", description = "Salva alterações de modelo e observações de um veículo")
    public Response updateVehicle(@PathParam("brandCode") String brandCode, @PathParam("vehicleCode") String vehicleCode, VehicleUpdateDTO vehicleUpdateDTO) {
        boolean updated = vehicleService.updateVehicle(vehicleCode, brandCode, vehicleUpdateDTO);
        if (!updated) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Veículo não encontrado para a marca informada", "NOT_FOUND"))
                    .build();
        }
        return Response.ok().build();
    }
}
