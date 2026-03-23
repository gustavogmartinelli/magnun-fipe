package br.com.magnumbank.presentation.rest.handler;

import br.com.magnumbank.presentation.rest.dto.ErrorResponse;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException webAppException) {
            int status = webAppException.getResponse().getStatus();
            
            // Não logamos 404 como erro grave para evitar poluição no console
            if (status != Response.Status.NOT_FOUND.getStatusCode()) {
                LOG.error("Erro capturado: " + exception.getMessage(), exception);
            }

            String message = exception.getMessage();
            String code = "API_ERROR";
            
            // Tratamento específico para erros do FipeRestClient ou similares
            if (status >= 400 && status < 500) {
                if (message == null || message.isBlank()) {
                    message = "Dados não encontrados ou requisição inválida.";
                }
                code = "NOT_FOUND_OR_BAD_REQUEST";
            } else if (status >= 500) {
                message = "Serviço indisponível no momento. Tente novamente mais tarde.";
                code = "SERVICE_UNAVAILABLE";
            }

            return Response.status(status)
                    .entity(new ErrorResponse(message, code))
                    .build();
        }

        if (exception instanceof ProcessingException) {
            LOG.error("Erro de processamento: " + exception.getMessage(), exception);
            Throwable cause = exception.getCause();
            if (cause instanceof ConnectException || cause instanceof SocketTimeoutException || 
                exception.getMessage().contains("timeout") || exception.getMessage().contains("Timeout")) {
                
                return Response.status(Response.Status.GATEWAY_TIMEOUT)
                        .entity(new ErrorResponse("Tempo esgotado ao conectar com a API Fipe. Tente novamente.", "FIPE_TIMEOUT_ERROR"))
                        .build();
            }
        }

        LOG.error("Erro interno não tratado: " + exception.getMessage(), exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Ocorreu um erro interno inesperado. Detalhes: " + exception.getMessage(), "INTERNAL_SERVER_ERROR"))
                .build();
    }
}
