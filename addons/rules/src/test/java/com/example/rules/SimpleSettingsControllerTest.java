package com.example.rules;

import com.clockify.addon.sdk.HttpResponse;
import com.clockify.addon.sdk.middleware.SecurityHeadersFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SimpleSettingsControllerTest {

    @Test
    void templateIncludesTagPrefillDedupLogic() {
        SimpleSettingsController controller = new SimpleSettingsController("https://example.test/rules");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(SecurityHeadersFilter.CSP_NONCE_ATTR)).thenReturn("nonce");

        HttpResponse response = controller.handle(request);
        String body = response.getBody();

        assertTrue(body.contains("prefillTagIds"), "Template should read prefillTagIds from URL params");
        assertTrue(body.contains("new Set("), "Tag prefill must deduplicate IDs");
        assertTrue(body.contains("addCondition('hasTag'"), "Tag prefill must add hasTag conditions");
    }
}
