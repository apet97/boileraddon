import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter, Gauge } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');
const requestCount = new Counter('request_count');
const concurrentUsers = new Gauge('concurrent_users');

// Stress test configuration
export const options = {
  stages: [
    // Rapid ramp-up to stress levels
    { duration: '1m', target: 100 },  // 100 users in 1 minute
    { duration: '2m', target: 500 },  // 500 users in 2 minutes
    { duration: '3m', target: 1000 }, // 1000 users in 3 minutes
    { duration: '5m', target: 1000 }, // Stay at 1000 users for 5 minutes
    { duration: '2m', target: 2000 }, // Extreme load: 2000 users
    { duration: '3m', target: 2000 }, // Stay at 2000 users
    { duration: '2m', target: 1000 }, // Back to 1000 users
    { duration: '2m', target: 500 },  // Back to 500 users
    { duration: '1m', target: 100 },  // Back to 100 users
    { duration: '1m', target: 0 },    // Ramp down to 0
  ],
  thresholds: {
    http_req_duration: ['p(95)<1000'], // 95% of requests should be below 1000ms under stress
    http_req_failed: ['rate<0.05'],    // Error rate should be less than 5% under stress
    errors: ['rate<0.05'],             // Custom error rate threshold
  },
};

// Test data
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const workspaceIds = [
  'workspace-stress-1',
  'workspace-stress-2',
  'workspace-stress-3',
  'workspace-stress-4',
  'workspace-stress-5'
];

// Headers for authenticated requests
const headers = {
  'Content-Type': 'application/json',
  'User-Agent': 'k6-stress-test/1.0',
};

export default function () {
  // Update concurrent users gauge
  concurrentUsers.update(__VU);

  // Simulate different stress scenarios
  const scenarioType = Math.random();

  if (scenarioType < 0.4) {
    // Scenario 1: High-frequency health checks (40% of users)
    highFrequencyHealthChecks();
  } else if (scenarioType < 0.7) {
    // Scenario 2: Webhook flood (30% of users)
    webhookFlood();
  } else if (scenarioType < 0.9) {
    // Scenario 3: Settings spam (20% of users)
    settingsSpam();
  } else {
    // Scenario 4: Mixed API calls (10% of users)
    mixedApiCalls();
  }

  // Minimal think time for stress testing
  sleep(Math.random() * 0.5);
}

function highFrequencyHealthChecks() {
  const url = `${baseUrl}/health`;
  const params = { headers, tags: { scenario: 'stress_health_check' } };

  const response = http.get(url, params);

  const success = check(response, {
    'stress health check status is 200': (r) => r.status === 200,
  });

  recordMetrics(response, success, 'stress_health_check');
}

function webhookFlood() {
  const url = `${baseUrl}/webhook`;
  const workspaceId = workspaceIds[Math.floor(Math.random() * workspaceIds.length)];

  const eventTypes = [
    'TIME_ENTRY_CREATED',
    'TIME_ENTRY_UPDATED',
    'TIME_ENTRY_DELETED',
    'PROJECT_CREATED',
    'TAG_CREATED'
  ];

  const eventType = eventTypes[Math.floor(Math.random() * eventTypes.length)];

  const webhookData = JSON.stringify({
    workspaceId: workspaceId,
    eventType: eventType,
    timestamp: new Date().toISOString(),
    data: {
      id: `stress-${Math.random().toString(36).substr(2, 12)}`,
      description: 'Stress test webhook payload ' + Math.random().toString(36).substr(2, 8),
      tags: ['stress', 'test', 'load'],
      projectId: `stress-project-${Math.random().toString(36).substr(2, 8)}`,
    },
  });

  const params = {
    headers: {
      ...headers,
      'Clockify-Webhook-Signature': 'stress-test-signature-' + Math.random().toString(36).substr(2, 16),
      'Clockify-Webhook-Timestamp': Date.now().toString(),
    },
    tags: { scenario: 'stress_webhook', eventType }
  };

  const response = http.post(url, webhookData, params);

  const success = check(response, {
    'stress webhook status is 200': (r) => r.status === 200,
  });

  recordMetrics(response, success, 'stress_webhook');
}

function settingsSpam() {
  const workspaceId = workspaceIds[Math.floor(Math.random() * workspaceIds.length)];

  // GET settings
  const getUrl = `${baseUrl}/settings`;
  const getParams = { headers, tags: { scenario: 'stress_settings_get' } };

  const getResponse = http.get(getUrl, getParams);

  const getSuccess = check(getResponse, {
    'stress settings get status is 200': (r) => r.status === 200,
  });

  recordMetrics(getResponse, getSuccess, 'stress_settings_get');

  // POST settings with random data
  const postUrl = `${baseUrl}/api/settings`;
  const settingsData = JSON.stringify({
    workspaceId: workspaceId,
    autoTag: Math.random() > 0.5,
    tagPrefix: 'stress-' + Math.random().toString(36).substr(2, 4),
    keywords: Array.from({length: 10}, () => Math.random().toString(36).substr(2, 6)).join(','),
    notifyOnTag: Math.random() > 0.5,
  });

  const postParams = {
    headers: { ...headers, 'X-CSRF-Token': 'stress-csrf-token' },
    tags: { scenario: 'stress_settings_post' }
  };

  const postResponse = http.post(postUrl, settingsData, postParams);

  const postSuccess = check(postResponse, {
    'stress settings post status is 200': (r) => r.status === 200,
  });

  recordMetrics(postResponse, postSuccess, 'stress_settings_post');
}

function mixedApiCalls() {
  const endpoints = [
    '/api/rules',
    '/api/rules?workspaceId=' + workspaceIds[Math.floor(Math.random() * workspaceIds.length)],
    '/manifest.json',
    '/health',
    '/settings',
  ];

  const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];
  const url = `${baseUrl}${endpoint}`;

  // Mix of GET and occasional POST
  let response;
  if (endpoint.includes('/api/rules') && Math.random() < 0.3) {
    // POST to rules endpoint
    const postData = JSON.stringify({
      workspaceId: workspaceIds[Math.floor(Math.random() * workspaceIds.length)],
      name: 'Stress Rule ' + Math.random().toString(36).substr(2, 8),
      condition: 'description contains "stress"',
      action: 'addTag("stress-test")',
    });

    const postParams = {
      headers: { ...headers, 'X-CSRF-Token': 'stress-csrf-token' },
      tags: { scenario: 'stress_api_post', endpoint }
    };

    response = http.post(url, postData, postParams);
  } else {
    // GET request
    const getParams = { headers, tags: { scenario: 'stress_api_get', endpoint } };
    response = http.get(url, getParams);
  }

  const success = check(response, {
    'stress api status is 200': (r) => r.status === 200,
  });

  recordMetrics(response, success, 'stress_api');
}

function recordMetrics(response, success, scenario) {
  requestCount.add(1, { scenario });
  responseTime.add(response.timings.duration, { scenario });
  errorRate.add(!success, { scenario });
}