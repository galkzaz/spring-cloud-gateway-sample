package com.example.demogateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class DemogatewayApplication {

	@RequestMapping("/circuitbreakerfallback")
	public String circuitbreakerfallback() {
		return "This is a fallback";
	}

	@Bean
	public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
		//@formatter:off
		return builder.routes()
				.route("path_route", r -> r.path("/get")
						.uri("http://echo.jsontest.com/key/value/one/two"))
				.route("host_route", r -> r.host("*.myhost.org")
						.uri("http://echo.jsontest.com/key/value/one/two"))
				.route("rewrite_route", r -> r.host("*.rewrite.org")
						.filters(f -> f.rewritePath("/foo/(?<segment>.*)",
								"/${segment}"))
						.uri("http://echo.jsontest.com/key/value/one/two"))
				.route("circuitbreaker_route", r -> r.host("*.circuitbreaker.org")
						.filters(f -> f.circuitBreaker(c -> c.setName("slowcmd")))
								.uri("http://echo.jsontest.com/key/value/one/two"))
				.route("circuitbreaker_fallback_route", r -> r.host("*.circuitbreakerfallback.org")
						.filters(f -> f.circuitBreaker(c -> c.setName("slowcmd").setFallbackUri("forward:/circuitbreakerfallback")))
								.uri("http://echo.jsontest.com/key/value/one/two"))
				.route("limit_route", r -> r
					.host("*.limited.org").and().path("/anything/**")
						.filters(f -> f.requestRateLimiter(c -> c.setRateLimiter(redisRateLimiter())))
					.uri("http://echo.jsontest.com/key/value/one/two"))
				.route("websocket_route", r -> r.path("/echo")
					.uri("ws://localhost:9000"))
				.build();
		//@formatter:on
	}

	@Bean
	RedisRateLimiter redisRateLimiter() {
		return new RedisRateLimiter(1, 2);
	}

	@Bean
	SecurityWebFilterChain springWebFilterChain(ServerHttpSecurity http) throws Exception {
		return http.httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable()
                        .authorizeExchange(exchange -> exchange
                                .pathMatchers("/anything/**").authenticated()
                                .anyExchange().permitAll()))
                .build();
	}

	@Bean
	public MapReactiveUserDetailsService reactiveUserDetailsService() {
		UserDetails user = User.withDefaultPasswordEncoder().username("user").password("password").roles("USER").build();
		return new MapReactiveUserDetailsService(user);
	}

	public static void main(String[] args) {
		SpringApplication.run(DemogatewayApplication.class, args);
	}
}
