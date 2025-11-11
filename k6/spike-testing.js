import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');
const requestCount = new Counter('request_count');

// Spike test configuration
export const options = {
  stages: [
    // Normal load
    { duration: '2m', target: 50 },   // 50 users for 2 minutes

    // Spike 1: Sudden increase
    { duration: '30s', target: 500 }, // Spike to 500 users in 30 seconds
    { duration: '1m', target: 500 },  // Stay at 500 users for 1 minute
    { duration: '30s', target: 50 },  // Back to 50 users in 30 seconds

    // Normal load
    { duration: '2m', target: 50 },   // 50 users for 2 minutes

    // Spike 2: Extreme spike
    { duration: '15s', target: 1000 }, // Spike to 1000 users in 15 seconds
    { duration: '45s', target: 1000 }, // Stay at 1000 users for 45 seconds
    { duration: '30s', target: 50 },   // Back to 50 users in 30 seconds

    // Normal load
    { duration: '2m', target: 50 },    // 50 users for 2 minutes

    // Spike 3: Gradual spike
    { duration: '1m', target: 300 },   // Ramp to 300 users in 1 minute
    { duration: '30s', target: 300 },  // Stay at 300 users
    { duration: '30s', target: 50 },   // Back to 50 users

    // Cool down
    { duration: '1m', target: 0 },     // Ramp down to 0
  ],
  thresholds: {
    http_req_duration: ['p(95)<800'],  // 95% of requests should be below 800ms during spikes
    http_req_failed: ['rate<0.03'],    // Error rate should be less than 3% during spikes
    errors: ['rate<0.03'],             // Custom error rate threshold
  },
};

// Test data
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const workspaceIds = [
  'workspace-spike-1',
  'workspace-spike-2',
  'workspace-spike-3'
];

// Headers for authenticated requests
const headers = {
  'Content-Type': 'application/json',
  'User-Agent': 'k6-spike-test/1.0',
};

export default function () {
  // Simulate different spike scenarios based on current VU count
  const currentVU = __VU;

  if (currentVU > 800) {
    // Extreme spike: Focus on critical endpoints only
    extremeSpikeScenario();
  } else if (currentVU > 400) {
    // High spike: Mixed critical and non-critical endpoints
    highSpikeScenario();
  } else if (currentVU > 100) {
    // Medium spike: Balanced endpoint mix
    mediumSpikeScenario();
  } else {
    // Normal load: Full endpoint mix
    normalLoadScenario();
  }

  // Think time varies with load
  const thinkTime = currentVU > 500 ? Math.random() * 0.2 : Math.random() * 1;
  sleep(thinkTime);
}

function extremeSpikeScenario() {
  // During extreme spikes, focus only on health checks and webhooks
  const scenarioType = Math.random();

  if (scenarioType < 0.7) {
    // 70% health checks
    healthCheck();
  } else {
    // 30% webhooks
    webhook();
  }
}

function highSpikeScenario() {
  const scenarioType = Math.random();

  if (scenarioType < 0.4) {
    healthCheck();
  } else if (scenarioType < 0.7) {
    webhook();
  } else if (scenarioType < 0.9) {
    settingsGet();
  } else {
    apiGet();
  }
}

function mediumSpikeScenario() {
  const scenarioType = Math.random();

  if (scenarioType < 0.3) {
    healthCheck();
  } else if (scenarioType < 0.5) {
    webhook();
  } else if (scenarioType < 0.7) {
    settingsGet();
  } else if (scenarioType < 0.9) {
    apiGet();
  } else {
    settingsPost();
  }
}

function normalLoadScenario() {
  const scenarioType = Math.random();

  if (scenarioType < 0.2) {
    healthCheck();
  } else if (scenarioType < 0.4) {
    webhook();
  } else if (scenarioType < 0.6) {
    settingsGet();
  } else if (scenarioType < 0.8) {
    apiGet();
  } else {
    settingsPost();
  }
}

function healthCheck() {
  const url = `${baseUrl}/health`;
  const params = { headers, tags: { scenario: 'spike_health_check' } };

  const response = http.get(url, params);

  const success = check(response, {
    'spike health check status is 200': (r) => r.status === 200,
  });

  recordMetrics(response, success, 'spike_health_check');
}

function webhook() {
  const url = `${baseUrl}/webhook`;
  const workspaceId = workspaceIds[Math.floor(Math.random() * workspaceIds.length)];

  const webhookData = JSON.stringify({
    workspaceId: workspaceId,
    eventType: 'TIME_ENTRY_CREATED',
    timestamp: new Date().toISOString(),
    data: {
      id: `spike-${Math.random().toString(36).substr(2, 12)}`,
      description: 'Spike test webhook payload',
      tags: [],
      projectId: `spike-project-${Math.random().toString(36).substr(2, 8)}`,
    },
  });

  const params = {
    headers: {
      ...headers,
      'Clockify-Webhook-Signature': 'spike-test-signature',
      'Clockify-Webhook-Timestamp': Date.now().toString(),
    },
    tags: { scenario: 'spike_webhook' }
  };

  const response = http.post(url, webhookData, params);

  const success = check(response, {
    'spike webhook status is 200': (r) => r.status === 200,
  });

  recordMetrics(response, success, 'spike_webhook');
}

function settingsGet() {
  const url = `${baseUrl}/settings`;
  const params = { headers, tags: { scenario: 'spike_settings_get' } };

  const response = http.get(url, params);

  const success = check(response, {
    'spike settings get status is 200': (r) => r.status === 200,
  });

  recordMetrics(response, success, 'spike_settings_get');
}

function settingsPost() {
  const workspaceId = workspaceIds[Math.floor(Math.random() * workspaceIds.length)];
  const url = `${baseUrl}/api/settings`;

  const settingsData = JSON.stringify({
    workspaceId: workspaceId,
    autoTag: Math.random() > 0.5,
    tagPrefix: 'spike-',
    keywords: 'spike,test,performance',
  });

  const params = {
    headers: { ...headers, 'X-CSRF-Token': 'spike-csrf-token' },
    tags: { scenario: 'spike_settings_post' }
  };

  const response = http.post(url, settingsData, params);

  const success = check(response, {
    'spike settings post status is 200': (r) => r.status === 200,
  });

  recordMetrics(response, success, 'spike_settings_post');
}

function apiGet() {
  const endpoints = [
    '/api/rules',
    '/api/rules?workspaceId=' + workspaceIds[Math.floor(Math.random() * workspaceIds.length)],
    '/manifest.json',
  ];

  const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];
  const url = `${baseUrl}${endpoint}`;
  const params = { headers, tags: { scenario: 'spike_api_get', endpoint } };

  const response = http.get(url, params);

  const success = check(response, {
    'spike api get status is 200': (r) => r.status === 200,
  });

  recordMetrics(response, success, 'spike_api_get');
}

function recordMetrics(response, success, scenario) {
  requestCount.add(1, { scenario });
  responseTime.add(response.timings.duration, { scenario });
  errorRate.add(!success, { scenario });
}