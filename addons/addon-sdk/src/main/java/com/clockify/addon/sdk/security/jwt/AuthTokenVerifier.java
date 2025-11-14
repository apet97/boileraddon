package com.clockify.addon.sdk.security.jwt;

/**
 * Minimal abstraction that allows components (controllers, filters) to accept any verifier
 * capable of validating a JWT and returning decoded payload data.
 */
public interface AuthTokenVerifier {

    JwtVerifier.DecodedJwt verify(String token) throws JwtVerifier.JwtVerificationException;
}
