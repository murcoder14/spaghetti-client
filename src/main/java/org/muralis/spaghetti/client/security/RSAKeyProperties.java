package org.muralis.spaghetti.client.security;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationProperties(prefix ="jwt")
@ConfigurationPropertiesScan
public record RSAKeyProperties(RSAPublicKey publicKey,RSAPrivateKey privateKey) {
} 
