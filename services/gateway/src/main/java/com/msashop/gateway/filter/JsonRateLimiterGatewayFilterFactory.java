package com.msashop.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * gateway route에 붙는 Redis 기반 rate limit 필터 factory.
 *
 * 이 클래스가 필요한 이유:
 * - Spring Cloud Gateway 기본 RequestRateLimiter는 429 status는 쉽게 줄 수 있지만,
 *   프로젝트가 원하는 공통 JSON 에러 바디까지 맞추기는 애매하다.
 * - 그래서 기본 RedisRateLimiter를 그대로 사용하되,
 *   "거부 응답을 어떻게 내려줄지"만 직접 제어하는 필터를 만든다.
 *
 * 처리 흐름:
 * 1. application.yml route 설정에서 key-resolver / replenish-rate / burst-capacity 값을 받는다.
 * 2. 요청마다 KeyResolver로 rate limit key를 만든다.
 *    예: ip:127.0.0.1, user:1
 * 3. RedisRateLimiter로 현재 요청이 허용 가능한지 확인한다.
 * 4. 허용이면 다음 filter chain으로 넘긴다.
 * 5. 거부면 GatewayRateLimitResponseWriter가 429 JSON 응답을 작성한다.
 */
@Component
public class JsonRateLimiterGatewayFilterFactory extends AbstractGatewayFilterFactory<JsonRateLimiterGatewayFilterFactory.Config> {

    private final ApplicationContext applicationContext;
    private final GatewayRateLimitResponseWriter responseWriter;

    public JsonRateLimiterGatewayFilterFactory(
            ApplicationContext applicationContext,
            GatewayRateLimitResponseWriter responseWriter
    ) {
        super(Config.class);
        this.applicationContext = applicationContext;
        this.responseWriter = responseWriter;
    }

    @Override
    public GatewayFilter apply(Config config) {
        // route별 설정값으로 사용할 resolver / limiter를 먼저 준비한다.
        KeyResolver keyResolver = resolveKeyResolver(config);
        RedisRateLimiter rateLimiter = createRateLimiter(config);

        return (exchange, chain) -> resolveKey(keyResolver, exchange)
                .flatMap(key -> checkAllowed(rateLimiter, exchange, key))
                .flatMap(rateLimitResponse -> handleRateLimitResult(exchange, chain, rateLimitResponse));
    }

    private KeyResolver resolveKeyResolver(Config config) {
        // application.yml의 key-resolver-bean 이름으로 실제 KeyResolver bean을 찾는다.
        return applicationContext.getBean(config.getKeyResolverBean(), KeyResolver.class);
    }

    private RedisRateLimiter createRateLimiter(Config config) {
        // Spring Cloud Gateway가 제공하는 RedisRateLimiter를 그대로 사용한다.
        // requestedTokens=1이면 "요청 1건당 토큰 1개 소모"라는 의미다.
        RedisRateLimiter rateLimiter = new RedisRateLimiter(
                config.getReplenishRate(),
                config.getBurstCapacity(),
                config.getRequestedTokens()
        );
        rateLimiter.setApplicationContext(applicationContext);
        return rateLimiter;
    }

    private Mono<String> resolveKey(KeyResolver keyResolver, ServerWebExchange exchange) {
        // KeyResolver는 Mono<String>을 반환한다.
        // 값이 비어 있으면 unknown으로 fallback 해서 limiter 호출 시 null key를 피한다.
        return keyResolver.resolve(exchange).defaultIfEmpty("unknown");
    }

    private Mono<RateLimiter.Response> checkAllowed(
            RedisRateLimiter rateLimiter,
            ServerWebExchange exchange,
            String key
    ) {
        // RedisRateLimiter는 routeId + key 조합으로 버킷을 관리한다.
        // routeId까지 같이 주는 이유는 같은 IP라도 login / refresh / pay 정책을 분리하기 위해서다.
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        String routeId = route != null ? route.getId() : "json-rate-limit";
        return rateLimiter.isAllowed(routeId, key);
    }

    private Mono<Void> handleRateLimitResult(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            RateLimiter.Response rateLimitResponse
    ) {
        // RedisRateLimiter가 계산한 남은 토큰 등 표준 rate limit 헤더는 그대로 응답에 싣는다.
        applyRateLimitHeaders(exchange.getResponse(), rateLimitResponse.getHeaders());
        if (rateLimitResponse.isAllowed()) {
            // 허용이면 원래 목적지 서비스로 요청을 계속 보낸다.
            return chain.filter(exchange);
        }
        // 거부면 여기서 체인을 끊고 429 JSON 응답을 직접 작성한다.
        return responseWriter.write(exchange);
    }

    private void applyRateLimitHeaders(ServerHttpResponse response, Map<String, String> headers) {
        headers.forEach((name, value) -> response.getHeaders().add(name, value));
    }

    public static class Config {
        // route 설정에서 넘겨주는 KeyResolver bean 이름
        private String keyResolverBean;
        // 초당 몇 개 토큰을 다시 채울지
        private int replenishRate;
        // 한 번에 최대 몇 개 토큰까지 저장할지
        private int burstCapacity;
        // 요청 1건당 몇 개 토큰을 소모할지. 기본은 1
        private int requestedTokens = 1;

        public String getKeyResolverBean() {
            return keyResolverBean;
        }

        public void setKeyResolverBean(String keyResolverBean) {
            this.keyResolverBean = keyResolverBean;
        }

        public int getReplenishRate() {
            return replenishRate;
        }

        public void setReplenishRate(int replenishRate) {
            this.replenishRate = replenishRate;
        }

        public int getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(int burstCapacity) {
            this.burstCapacity = burstCapacity;
        }

        public int getRequestedTokens() {
            return requestedTokens;
        }

        public void setRequestedTokens(int requestedTokens) {
            this.requestedTokens = requestedTokens;
        }
    }
}
