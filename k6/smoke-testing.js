import http from 'k6/http';
import { check } from 'k6';

// Smoke test configuration - quick validation that system is working
export const options = {
  vus: 1,           // 1 virtual user
  duration: '1m',   // 1 minute duration
  thresholds: {
    http_req_duration: ['p(95)<200'], // 95% of requests should be below 200ms
    http_req_failed: ['rate<0.01'],   // Error rate should be less than 1%
  },
};

// Test data
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

// Headers for authenticated requests
const headers = {
  'Content-Type': 'application/json',
  'User-Agent': 'k6-smoke-test/1.0',
};

export default function () {
  // Test all critical endpoints in sequence
  testHealthEndpoint();
  testManifestEndpoint();
  testSettingsEndpoint();
  testWebhookEndpoint();
  testApiRulesEndpoint();
}

function testHealthEndpoint() {
  const url = `${baseUrl}/health`;
  const params = { headers, tags: { endpoint: 'health' } };

  const response = http.get(url, params);

  check(response, {
    'health endpoint is accessible': (r) => r.status === 200,
    'health response time is acceptable': (r) => r.timings.duration < 100,
    'health response contains expected data': (r) => r.body.includes('status') || r.body.includes('ok'),
  });
}

function testManifestEndpoint() {
  const url = `${baseUrl}/manifest.json`;
  const params = { headers, tags: { endpoint: 'manifest' } };

  const response = http.get(url, params);

  check(response, {
    'manifest endpoint is accessible': (r) => r.status === 200,
    'manifest response is JSON': (r) => r.headers['Content-Type']?.includes('application/json'),
    'manifest contains required fields': (r) => {
      try {
        const manifest = JSON.parse(r.body);
        return manifest.name && manifest.version;
      } catch {
        return false;
      }
    },
  });
}

function testSettingsEndpoint() {
  const url = `${baseUrl}/settings`;
  const params = { headers, tags: { endpoint: 'settings' } };

  const response = http.get(url, params);

  check(response, {
    'settings endpoint is accessible': (r) => r.status === 200,
    'settings response is HTML': (r) => r.headers['Content-Type']?.includes('text/html'),
  });
}

function testWebhookEndpoint() {
  const url = `${baseUrl}/webhook`;
  const webhookData = JSON.stringify({
    workspaceId: 'smoke-test-workspace',
    eventType: 'TIME_ENTRY_CREATED',
    timestamp: new Date().toISOString(),
    data: {
      id: 'smoke-test-time-entry',
      description: 'Smoke test time entry',
      tags: [],
      projectId: 'smoke-test-project',
    },
  });

  const params = {
    headers: {
      ...headers,
      'Clockify-Webhook-Signature': 'smoke-test-signature',
      'Clockify-Webhook-Timestamp': Date.now().toString(),
    },
    tags: { endpoint: 'webhook' }
  };

  const response = http.post(url, webhookData, params);

  check(response, {
    'webhook endpoint is accessible': (r) => r.status === 200,
    'webhook response time is acceptable': (r) => r.timings.duration < 500,
  });
}

function testApiRulesEndpoint() {
  const url = `${baseUrl}/api/rules`;
  const params = { headers, tags: { endpoint: 'api_rules' } };

  const response = http.get(url, params);

  check(response, {
    'api rules endpoint is accessible': (r) => r.status === 200,
    'api rules response is JSON': (r) => r.headers['Content-Type']?.includes('application/json'),
  });
}