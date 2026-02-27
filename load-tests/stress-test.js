import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, MANAGER_CREDENTIALS } from './config.js';
import { login, authHeaders } from './helpers.js';

// Custom metrics
const errorRate = new Rate('errors');
const dashboardDuration = new Trend('dashboard_duration');
const ordersListDuration = new Trend('orders_list_duration');
const channelsDuration = new Trend('channels_duration');
const orderDetailDuration = new Trend('order_detail_duration');
const createOrderDuration = new Trend('create_order_duration');
const updateStatusDuration = new Trend('update_status_duration');

const ORDER_STATUSES = ['PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED'];
const CHANNELS = ['WEBSITE', 'AMAZON', 'BLINKIT'];

function createOrderPayload() {
  const channel = CHANNELS[Math.floor(Math.random() * CHANNELS.length)];
  const ref = `LOAD-${Date.now()}-${Math.floor(Math.random() * 10000)}`;
  return JSON.stringify({
    channel,
    channelOrderRef: ref,
    customer: {
      name: 'Load Test Customer',
      email: `loadtest-${Date.now()}@example.com`,
      phone: '+91-9876543210',
    },
    items: [
      {
        productName: 'Organic Honey',
        sku: 'HON-001',
        quantity: 2,
        unitPrice: 450,
      },
    ],
    shippingAddress: {
      line1: '123 Test Street',
      city: 'Bangalore',
      state: 'Karnataka',
      pincode: '560001',
      country: 'India',
    },
    channelMetadata: {},
  });
}

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '30s', target: 50 },
    { duration: '30s', target: 100 },
    { duration: '30s', target: 50 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.2'],
    errors: ['rate<0.2'],
  },
};

export function setup() {
  const token = login(MANAGER_CREDENTIALS);
  if (!token) {
    throw new Error('Setup failed: could not login');
  }

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

  // 35% dashboard, 27% orders, 13% channels, 15% order detail, 5% create, 5% update
  const r = Math.random();
  if (r < 0.35) {
    runDashboardScenario(headers);
  } else if (r < 0.62) {
    runOrdersListScenario(headers);
  } else if (r < 0.75) {
    runChannelsScenario(headers);
  } else if (r < 0.9) {
    runOrderDetailScenario(headers, orderIds);
  } else if (r < 0.95) {
    runCreateOrderScenario(headers);
  } else {
    runUpdateStatusScenario(headers, orderIds);
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

function runCreateOrderScenario(headers) {
  const payload = createOrderPayload();
  const res = http.post(`${BASE_URL}/api/orders`, payload, { headers });
  createOrderDuration.add(res.timings.duration);
  const ok = check(res, { 'create order status 201': (r) => r.status === 201 });
  errorRate.add(!ok);
}

function runUpdateStatusScenario(headers, orderIds) {
  if (orderIds.length === 0) {
    runOrdersListScenario(headers);
    return;
  }
  const id = orderIds[Math.floor(Math.random() * orderIds.length)];
  const status = ORDER_STATUSES[Math.floor(Math.random() * ORDER_STATUSES.length)];
  const payload = JSON.stringify({ status, notes: 'Load test update' });
  const res = http.patch(`${BASE_URL}/api/orders/${id}/status`, payload, { headers });
  updateStatusDuration.add(res.timings.duration);
  const ok = check(res, { 'update status 200': (r) => r.status === 200 });
  errorRate.add(!ok);
}
