package org.muralis.spaghetti.client.controller;

import java.net.URI;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@RestController
@RequiredArgsConstructor
@RequestMapping("/spaghetti")
@ConfigurationProperties("app")
@Slf4j
public class SpaghettiOrdersController {

    private Map<String,String> order;
    
    private final WebClient webClient;

    public void setOrder(Map<String,String> order) {
        this.order = order;
    }
    
    public Map<String,String> getOrder() {
        return this.order;
    }

    @GetMapping("/orderit")
    Mono<ResponseEntity<String>> orderIt() {
        log.info("Spaghetti order received...");

        // @formatter:off
        Mono<String> body = webClient
                .method(HttpMethod.POST)
                .uri(URI.create(order.get("url")))
                .attributes(clientRegistrationId(order.get("clientId")))
                .retrieve()
                .bodyToMono(String.class);
        // @formatter:on
        return body.map(bundle -> ResponseEntity.ok(bundle)).defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
