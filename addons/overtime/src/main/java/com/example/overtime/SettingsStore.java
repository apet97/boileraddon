package com.example.overtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SettingsStore {
    public static class Settings {
        public double dailyHours = 8.0;
        public double weeklyHours = 40.0;
        public String tagName = "Overtime";
    }

    private final Map<String, Settings> byWorkspace = new ConcurrentHashMap<>();

    public Settings get(String workspaceId) {
        return byWorkspace.computeIfAbsent(workspaceId, k -> new Settings());
    }

    public void put(String workspaceId, Settings s) {
        byWorkspace.put(workspaceId, s);
    }
}

