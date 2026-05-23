# Auth Member Register 250 VU

- Result: **FAIL**
- Checks: **37.03%** (3108/8394)
- HTTP failure rate: **-**
- HTTP p95: **15.02s**
- Requests: **2,798**
- Iterations: **2,798**

## Key Metrics

| Metric | Count/Rate | Avg | P90 | P95 | P99 | Max | Threshold |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| http_req_duration | 2,798 | 5.22s | 11.49s | 15.02s | 21.10s | 30.90s | FAIL p(95)<300 |
| http_req_failed | 0.63/s | - | - | - | - | - | FAIL rate<0.01 |
| http_reqs | 2,798 | - | - | - | - | - | - |
| checks | 0.37/s | - | - | - | - | - | - |
| iterations | 2,798 | - | - | - | - | - | - |
| auth_comm_failures | 1,762 | - | - | - | - | - | FAIL count<1 |
| auth_register_duration_ms | 2,798 | 5.22s | 11.49s | 15.02s | 21.10s | 30.90s | FAIL p(95)<400 |
| group_duration | 2,798 | 5.22s | 11.49s | 15.02s | 21.10s | 30.90s | - |
| http_req_duration{expected_response:true} | 1,036 | 2.52s | 5.20s | 8.53s | 15.40s | 24.09s | - |
| iteration_duration | 2,798 | 6.23s | 12.50s | 16.02s | 22.10s | 31.90s | - |

## Failed Thresholds

| Metric | Thresholds |
| --- | --- |
| auth_comm_failures | count<1 |
| auth_register_duration_ms | p(95)<400 |
| http_req_duration | p(95)<300 |
| http_req_failed | rate<0.01 |

## Checks

| Check | Pass | Fail | Success |
| --- | ---: | ---: | ---: |
| ::auth register only probe::register probe status 200 | 1036 | 1762 | 37.03% |
| ::auth register only probe::register probe success true | 1036 | 1762 | 37.03% |
| ::auth register only probe::register probe has member id | 1036 | 1762 | 37.03% |

