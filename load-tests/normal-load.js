import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, ADMIN_CREDENTIALS } from './config.js';
import { login, authHeaders } from './helpers.js';

// Custom metrics
const errorRate = new Rate('errors');
const dashboardDuration = new Trend('dashboard_duration');
const ordersListDuration = new Trend('orders_list_duration');
const channelsDuration = new Trend('channels_duration');
const orderDetailDuration = new Trend('order_detail_duration');

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '2m', target: 10 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.05'],
    errors: ['rate<0.05'],
  },
};

export function setup() {
  const token = login(ADMIN_CREDENTIALS);
  if (!token) {
    throw new Error('Setup failed: could not login');
  }

  // Fetch order IDs for the order detail scenario
  const ordersRes = http.get(`${BASE_URL}/api/orders?page=0&size=20`, {
    headers: authHeaders(token),
  });
  let orderIds = [];
  if (ordersRes.status === 200) {
    try {
      const body = JSON.parse(ordersRes.body);
      orderIds = (body.data?.content || []).map((o) => o.id).filter(Boolean);
    } catch (_) {}
  }

  return { token, orderIds };
}

export default function (data) {
  const { token, orderIds } = data;
  const headers = authHeaders(token);

  // Weighted random scenario: 40% dashboard, 30% orders, 15% channels, 15% order detail
  const r = Math.random();
  if (r < 0.4) {
    runDashboardScenario(headers);
  } else if (r < 0.7) {
    runOrdersListScenario(headers);
  } else if (r < 0.85) {
    runChannelsScenario(headers);
  } else {
    runOrderDetailScenario(headers, orderIds);
  }

  sleep(Math.random() * 2 + 1);
}

function runDashboardScenario(headers) {
  const res = http.get(`${BASE_URL}/api/dashboard/stats`, { headers });
  dashboardDuration.add(res.timings.duration);
  const ok = check(res, { 'dashboard status 200': (r) => r.status === 200 });
  errorRate.add(!ok);
}

function runOrdersListScenario(headers) {
  const res = http.get(`${BASE_URL}/api/orders?page=0&size=20`, { headers });
  ordersListDuration.add(res.timings.duration);
  const ok = check(res, { 'orders list status 200': (r) => r.status === 200 });
  errorRate.add(!ok);
}

function runChannelsScenario(headers) {
  const res = http.get(`${BASE_URL}/api/channels`, { headers });
  channelsDuration.add(res.timings.duration);
  const ok = check(res, { 'channels status 200': (r) => r.status === 200 });
  errorRate.add(!ok);
}

function runOrderDetailScenario(headers, orderIds) {
  if (orderIds.length === 0) {
    runOrdersListScenario(headers);
    return;
  }
  const id = orderIds[Math.floor(Math.random() * orderIds.length)];
  const res = http.get(`${BASE_URL}/api/orders/${id}`, { headers });
  orderDetailDuration.add(res.timings.duration);
  const ok = check(res, { 'order detail status 200': (r) => r.status === 200 });
  errorRate.add(!ok);
}
