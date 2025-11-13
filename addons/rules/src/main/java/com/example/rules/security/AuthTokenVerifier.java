package com.example.rules.security;

/**
 * Minimal abstraction that allows components (controllers, filters) to accept any verifier
 * capable of validating a JWT and returning decoded payload data. The production implementation
 * remains {@link JwtVerifier}, while tests can inject lightweight stubs.
 */
public interface AuthTokenVerifier {

    JwtVerifier.DecodedJwt verify(String token) throws JwtVerifier.JwtVerificationException;
}

