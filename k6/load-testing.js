import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');
const requestCount = new Counter('request_count');

// Test configuration
export const options = {
  stages: [
    // Ramp-up to normal load
    { duration: '2m', target: 50 }, // 50 users over 2 minutes
    { duration: '5m', target: 50 }, // Stay at 50 users for 5 minutes

    // Spike testing
    { duration: '1m', target: 200 }, // Spike to 200 users
    { duration: '2m', target: 200 }, // Stay at 200 users

    // Scale down
    { duration: '2m', target: 50 }, // Back to 50 users
    { duration: '2m', target: 0 },  // Ramp down to 0
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests should be below 500ms
    http_req_failed: ['rate<0.01'],   // Error rate should be less than 1%
    errors: ['rate<0.01'],            // Custom error rate threshold
  },
};

// Test data
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const workspaceId = __ENV.WORKSPACE_ID || 'test-workspace-123';

// Headers for authenticated requests
const headers = {
  'Content-Type': 'application/json',
  'User-Agent': 'k6-load-test/1.0',
};

export default function () {
  // Simulate different user scenarios
  const userType = Math.random();

  if (userType < 0.3) {
    // Scenario 1: Health check user (30% of users)
    healthCheckScenario();
  } else if (userType < 0.6) {
    // Scenario 2: Settings user (30% of users)
    settingsScenario();
  } else if (userType < 0.8) {
    // Scenario 3: Webhook user (20% of users)
    webhookScenario();
  } else {
    // Scenario 4: API user (20% of users)
    apiScenario();
  }

  // Add think time between requests
  sleep(Math.random() * 2);
}

function healthCheckScenario() {
  const url = `${baseUrl}/health`;
  const params = { headers, tags: { scenario: 'health_check' } };

  const response = http.get(url, params);

  const success = check(response, {
    'health check status is 200': (r) => r.status === 200,
    'health check response time acceptable': (r) => r.timings.duration < 1000,
  });

  recordMetrics(response, success, 'health_check');
}

function settingsScenario() {
  // GET settings
  const getUrl = `${baseUrl}/settings`;
  const getParams = { headers, tags: { scenario: 'settings_get' } };

  const getResponse = http.get(getUrl, getParams);

  const getSuccess = check(getResponse, {
    'settings get status is 200': (r) => r.status === 200,
  });

  recordMetrics(getResponse, getSuccess, 'settings_get');

  // POST settings (50% of settings users)
  if (Math.random() < 0.5) {
    const postUrl = `${baseUrl}/api/settings`;
    const settingsData = JSON.stringify({
      workspaceId: workspaceId,
      autoTag: Math.random() > 0.5,
      tagPrefix: 'test-',
      keywords: 'test,load,performance',
    });

    const postParams = {
      headers: { ...headers, 'X-CSRF-Token': 'test-csrf-token' },
      tags: { scenario: 'settings_post' }
    };

    const postResponse = http.post(postUrl, settingsData, postParams);

    const postSuccess = check(postResponse, {
      'settings post status is 200': (r) => r.status === 200,
    });

    recordMetrics(postResponse, postSuccess, 'settings_post');
  }
}

function webhookScenario() {
  const url = `${baseUrl}/webhook`;
  const webhookData = JSON.stringify({
    workspaceId: workspaceId,
    eventType: 'TIME_ENTRY_CREATED',
    timestamp: new Date().toISOString(),
    data: {
      id: `time-entry-${Math.random().toString(36).substr(2, 9)}`,
      description: 'Test time entry for load testing',
      tags: [],
      projectId: `project-${Math.random().toString(36).substr(2, 9)}`,
    },
  });

  const params = {
    headers: {
      ...headers,
      'Clockify-Webhook-Signature': 'test-signature',
      'Clockify-Webhook-Timestamp': Date.now().toString(),
    },
    tags: { scenario: 'webhook' }
  };

  const response = http.post(url, webhookData, params);

  const success = check(response, {
    'webhook status is 200': (r) => r.status === 200,
  });

  recordMetrics(response, success, 'webhook');
}

function apiScenario() {
  // Simulate API endpoint usage
  const endpoints = [
    '/api/rules',
    '/api/rules?workspaceId=' + workspaceId,
    '/api/health',
    '/manifest.json',
  ];

  const endpoint = endpoints[Math.floor(Math.random() * endpoints.length)];
  const url = `${baseUrl}${endpoint}`;
  const params = { headers, tags: { scenario: 'api', endpoint } };

  const response = http.get(url, params);

  const success = check(response, {
    'api status is 200': (r) => r.status === 200,
  });

  recordMetrics(response, success, 'api');
}

function recordMetrics(response, success, scenario) {
  requestCount.add(1, { scenario });
  responseTime.add(response.timings.duration, { scenario });
  errorRate.add(!success, { scenario });
}