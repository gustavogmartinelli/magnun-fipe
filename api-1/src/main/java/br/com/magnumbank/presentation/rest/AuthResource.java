package br.com.magnumbank.presentation.rest;

import io.smallrye.jwt.build.Jwt;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.HashSet;
import java.util.Set;

@Path("/auth")
@Tag(name = "Autenticação", description = "Endpoints para geração de tokens JWT")
public class AuthResource {

    @POST
    @Path("/token")
    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Gerar token de teste", description = "Gera um token JWT válido por 30 minutos com papel 'user'")
    public String generateToken() {
        Set<String> roles = new HashSet<>();
        roles.add("user");

        return Jwt.issuer("https://magnumbank.com.br")
                .upn("test-user@magnumbank.com.br")
                .groups(roles)
                .expiresIn(1800) // 30 minutos
                .sign();
    }
}
