package org.muralis.spaghetti.client.security;

import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.endpoint.NimbusJwtClientAuthenticationParametersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2RefreshTokenGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveRefreshTokenTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizedClientRepository;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    private final RSAKeyProperties jwtKeyProperties;

    public SecurityConfiguration(RSAKeyProperties rsaKeyProperties) {
        this.jwtKeyProperties = rsaKeyProperties;
    }

    @Bean
    SecurityWebFilterChain configure(ServerHttpSecurity serverHttpSecurity,
            final ReactiveClientRegistrationRepository clientRegistrationRepository,
            final ServerOAuth2AuthorizedClientRepository auth2AuthorizedClientRepository) {
        return serverHttpSecurity.authorizeExchange(
                (authorize) -> authorize.pathMatchers("/", "/**").permitAll().anyExchange().authenticated())
                .build();
    }

    @Bean
    JWKSource<SecurityContext> jwkSource() {
        JWK jwk = new RSAKey.Builder(jwtKeyProperties.publicKey()).privateKey(jwtKeyProperties.privateKey()).build();
        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return jwks;
    }

    @Bean
    Function<ClientRegistration, JWK> jwkResolver(JWKSource<SecurityContext> jwkSource) {
        JWKSelector jwkSelector = new JWKSelector(new JWKMatcher.Builder().privateOnly(true).build());

        return (registration) -> {
            JWKSet jwkSet = null;
            try {
                jwkSet = new JWKSet(jwkSource.get(jwkSelector, null));
            } catch (Exception e) {
            }
            return jwkSet != null ? jwkSet.getKeys().iterator().next() : null;

        };
    }

    @Bean
    ReactiveOAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> refreshTokenResponseClient(
            Function<ClientRegistration, JWK> jwkResolver) {
        WebClientReactiveRefreshTokenTokenResponseClient reactiveRefreshTokenResponseClient = new WebClientReactiveRefreshTokenTokenResponseClient();
        reactiveRefreshTokenResponseClient
                .addParametersConverter(new NimbusJwtClientAuthenticationParametersConverter<>(jwkResolver));
        return reactiveRefreshTokenResponseClient;
    }

    @Bean
    ReactiveOAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> accessTokenResponseClient(
            Function<ClientRegistration, JWK> jwkResolver) {
        WebClientReactiveClientCredentialsTokenResponseClient reactiveAccessTokenResponseClient = new WebClientReactiveClientCredentialsTokenResponseClient();
        reactiveAccessTokenResponseClient
                .addParametersConverter(new NimbusJwtClientAuthenticationParametersConverter<>(jwkResolver));
        return reactiveAccessTokenResponseClient;
    }

    @Bean
    ReactiveOAuth2AuthorizedClientManager auth2AuthorizedClientManager(
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            ServerOAuth2AuthorizedClientRepository serverOAuth2AuthorizedClientRepository,
            ReactiveOAuth2AccessTokenResponseClient<OAuth2RefreshTokenGrantRequest> reactiveOAuth2RefreshTokenResponseClient,
            ReactiveOAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> reactiveOAuth2AccessTokenResponseClient) {
        ReactiveOAuth2AuthorizedClientProvider oauth2AuthorizedClientProvider = ReactiveOAuth2AuthorizedClientProviderBuilder
                .builder()
                .refreshToken(refreshToken -> refreshToken
                        .accessTokenResponseClient(reactiveOAuth2RefreshTokenResponseClient))
                .clientCredentials(clientCredentials -> clientCredentials
                        .accessTokenResponseClient(reactiveOAuth2AccessTokenResponseClient))
                .build();

        DefaultReactiveOAuth2AuthorizedClientManager oauth2AuthorizedClientManager = new DefaultReactiveOAuth2AuthorizedClientManager(
                clientRegistrationRepository, serverOAuth2AuthorizedClientRepository);
        oauth2AuthorizedClientManager.setAuthorizedClientProvider(oauth2AuthorizedClientProvider);

        return oauth2AuthorizedClientManager;
    }

    @Bean
    WebClient webClient(ReactiveOAuth2AuthorizedClientManager oAuth2AuthorizedClientManager) {
        ServerOAuth2AuthorizedClientExchangeFilterFunction filter = new ServerOAuth2AuthorizedClientExchangeFilterFunction(
                oAuth2AuthorizedClientManager);
        filter.setDefaultOAuth2AuthorizedClient(true);
        return WebClient.builder().filter(filter).build();
    }
}