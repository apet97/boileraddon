package com.example.rules;

/**
 * Factory for creating {@link ClockifyClient} instances scoped to a workspace.
 */
@FunctionalInterface
public interface ClockifyClientFactory {
    ClockifyClient create(String apiBaseUrl, String token);
}
