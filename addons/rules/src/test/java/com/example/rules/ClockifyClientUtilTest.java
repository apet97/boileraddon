package com.example.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClockifyClientUtilTest {
    private static final ObjectMapper OM = new ObjectMapper();

    @Test
    void normalizeTagName_handlesNullWhitespaceAndCase() {
        assertNull(ClockifyClient.normalizeTagName(null));
        assertNull(ClockifyClient.normalizeTagName("   "));
        assertEquals("meeting-notes", ClockifyClient.normalizeTagName("  MEETING-NOTES  "));
    }

    @Test
    void mapTagsByNormalizedName_dedupesAndLowercases() {
        ArrayNode tags = OM.createArrayNode();
        tags.add(tag("T1", "Meeting"));
        tags.add(tag("T2", "meeting")); // duplicate normalized
        tags.add(tag("T3", "inbox"));

        var map = ClockifyClient.mapTagsByNormalizedName(tags);
        assertEquals(2, map.size());
        assertEquals("T1", map.get("meeting"));
        assertEquals("T3", map.get("inbox"));
    }

    @Test
    void ensureTagIdsArray_createsWhenMissingAndReturnsExistingWhenPresent() {
        ObjectNode entry = OM.createObjectNode();
        ArrayNode created = ClockifyClient.ensureTagIdsArray(entry, OM);
        assertNotNull(created);
        assertTrue(created.isArray());
        created.add("t1");

        ArrayNode again = ClockifyClient.ensureTagIdsArray(entry, OM);
        assertSame(created, again);
        assertEquals(1, again.size());
        assertEquals("t1", again.get(0).asText());
    }

    private static JsonNode tag(String id, String name) {
        ObjectNode t = OM.createObjectNode();
        t.put("id", id);
        t.put("name", name);
        return t;
    }
}

