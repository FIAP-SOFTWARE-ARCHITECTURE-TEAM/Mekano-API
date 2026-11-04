package com.fiap.mekano.rest.filter;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fiap.mekano.rest.api.exception.ProblemDetail;

import io.quarkus.vertx.http.runtime.CurrentVertxRequest;
import io.vertx.ext.web.RoutingContext;

import java.util.Set;

/**
 * Protege {@code /auth/*} (brute force de login) e {@code /webhooks/evolution}
 * (flood) com janela fixa por IP — docs/ARCHITECTURE-REVIEW-2026-10-17.md item 5.
 *
 * <p>Roda ANTES da autenticação ({@link Priorities#AUTHENTICATION}) para que o
 * 429 vença o 401 e o hash de senha (bcrypt, caro) não seja executado sob ataque.
 *
 * <p>Configuração (api-config.yml):
 * <pre>
 * mekano.rate-limit.auth.limit / window-seconds      (default 10 / 60)
 * mekano.rate-limit.webhook.limit / window-seconds   (default 120 / 60)
 * </pre>
 */
@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION)
public class RateLimitFilter implements ContainerRequestFilter {

    private static final Set<String> PROTECTED_ROUTES = Set.of(
            "auth",
            "webhooks/evolution");

    @Inject
    FixedWindowRateLimiter limiter;

    @Inject
    CurrentVertxRequest currentVertxRequest;

    @ConfigProperty(name = "mekano.rate-limit.auth.limit", defaultValue = "10")
    int authLimit;

    @ConfigProperty(name = "mekano.rate-limit.auth.window-seconds", defaultValue = "60")
    int authWindowSeconds;

    @ConfigProperty(name = "mekano.rate-limit.webhook.limit", defaultValue = "120")
    int webhookLimit;

    @ConfigProperty(name = "mekano.rate-limit.webhook.window-seconds", defaultValue = "60")
    int webhookWindowSeconds;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String route = matchedRoute(ctx.getUriInfo().getPath());
        if (route == null) {
            return;
        }

        int limit = "auth".equals(route) ? authLimit : webhookLimit;
        int windowSeconds = "auth".equals(route) ? authWindowSeconds : webhookWindowSeconds;

        String key = route + ":" + remoteAddr();
        if (limiter.allow(key, limit, windowSeconds)) {
            return;
        }

        ctx.abortWith(Response
                .status(Response.Status.TOO_MANY_REQUESTS)
                .type("application/problem+json")
                .header("Retry-After", windowSeconds)
                .entity(ProblemDetail.of(429,
                        "Limite de requisicoes excedido. Tente novamente em " + windowSeconds + " segundos."))
                .build());
    }

    /**
     * IP do cliente via request Vert.x (Quarkus REST não expõe remoteAddr no
     * ContainerRequestContext). Atrás de proxy/Cloudflare é o IP do proxy —
     * limitação documentada na classe do limiter.
     */
    private String remoteAddr() {
        RoutingContext routing = currentVertxRequest.getCurrent();
        if (routing == null || routing.request() == null || routing.request().remoteAddress() == null) {
            return "unknown";
        }
        String host = routing.request().remoteAddress().host();
        return host != null ? host : "unknown";
    }

    /**
     * @return o prefixo de rota protegida que casa com o path, ou null se não protegida.
     *         O path chega relativo ao base da API (prefixo /api/v1 já removido pelo runtime).
     */
    private String matchedRoute(String path) {
        if (path == null) {
            return null;
        }
        String normalized = path.strip().replaceAll("^/+", "").replaceAll("/+$", "");
        for (String route : PROTECTED_ROUTES) {
            if (normalized.equals(route) || normalized.startsWith(route + "/")) {
                return route;
            }
        }
        return null;
    }
}
