# Commerce Platform Load Tests

k6 load test scripts for the commerce-platform API.

## Prerequisites

### Install k6

**macOS (Homebrew):**
```bash
brew install k6
```

**Linux:**
```bash
# Debian/Ubuntu
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6

# Or use the binary release
sudo gpg --dearmor -o /usr/share/keyrings/k6-archive-keyring.gpg https://dl.k6.io/keyrings/k6-archive-keyring.gpg
```

**Windows:**
```powershell
choco install k6
```

**Verify installation:**
```bash
k6 version
```

### Application Requirements

- The commerce-platform backend must be running (default: `http://localhost:8080`)
- Database must be seeded with test users:
  - `admin@commerce.com` / `admin123`
  - `manager@commerce.com` / `manager123`

## Running the Tests

### Normal Load Test

Simulates 10 concurrent users for ~2.5 minutes (30s ramp-up + 2min hold).

```bash
k6 run load-tests/normal-load.js
```

**Custom base URL:**
```bash
k6 run -e BASE_URL=http://staging.example.com:8080 load-tests/normal-load.js
```

### Peak Load Test

Ramps 0→30 users in 30s, holds 30 users for 2min, ramps down in 30s.

```bash
k6 run load-tests/peak-load.js
```

### Stress Test

Ramps 0→10→50→100→50→0 users over 5 stages (30s each). Includes write operations (create order, update status).

```bash
k6 run load-tests/stress-test.js
```

## Expected Output Interpretation

### Summary Metrics

| Metric | Description |
|--------|-------------|
| `http_reqs` | Total HTTP requests made |
| `http_req_duration` | Request latency (avg, min, max, p90, p95) |
| `http_req_failed` | Failed request rate (0 = 0%, 1 = 100%) |
| `iterations` | Completed VU iterations |
| `vus` | Virtual users (concurrent) |
| `vus_max` | Max VUs during the test |

### Custom Metrics

- `dashboard_duration` — Time for GET /api/dashboard/stats
- `orders_list_duration` — Time for GET /api/orders
- `channels_duration` — Time for GET /api/channels
- `order_detail_duration` — Time for GET /api/orders/{id}
- `create_order_duration` — Time for POST /api/orders (stress test only)
- `update_status_duration` — Time for PATCH /api/orders/{id}/status (stress test only)
- `errors` — Rate of failed checks (non-200 responses)

### Thresholds

**Normal Load:**
- `http_req_duration` p(95) < 500ms
- `http_req_failed` rate < 5%

**Peak Load:**
- `http_req_duration` p(95) < 1000ms
- `http_req_failed` rate < 10%

**Stress Test:**
- `http_req_duration` p(95) < 2000ms
- `http_req_failed` rate < 20%

### Interpreting Results

- **PASS** — All thresholds met; system handled the load within SLA.
- **FAIL** — One or more thresholds exceeded; review slow endpoints and error rates.

If the test fails:
1. Check `http_req_failed` — high values indicate auth issues, 5xx errors, or timeouts.
2. Check `http_req_duration` p(95) — identify which custom metric (e.g. `dashboard_duration`) is slow.
3. Ensure the backend and database are running and seeded.
4. Run with fewer VUs or longer stages to narrow down capacity limits.
