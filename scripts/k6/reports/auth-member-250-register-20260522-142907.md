# Auth Member Register 250 VU

- Result: **FAIL**
- Checks: **34.17%** (2775/8121)
- HTTP failure rate: **-**
- HTTP p95: **15.40s**
- Requests: **2,707**
- Iterations: **2,707**

## Key Metrics

| Metric | Count/Rate | Avg | P90 | P95 | P99 | Max | Threshold |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| http_req_duration | 2,707 | 5.42s | 11.04s | 15.40s | 22.68s | 32.71s | FAIL p(95)<300 |
| http_req_failed | 0.66/s | - | - | - | - | - | FAIL rate<0.01 |
| http_reqs | 2,707 | - | - | - | - | - | - |
| checks | 0.34/s | - | - | - | - | - | - |
| iterations | 2,707 | - | - | - | - | - | - |
| auth_comm_failures | 1,782 | - | - | - | - | - | FAIL count<1 |
| auth_register_duration_ms | 2,707 | 5.42s | 11.04s | 15.40s | 22.68s | 32.71s | FAIL p(95)<400 |
| group_duration | 2,707 | 5.42s | 11.04s | 15.40s | 22.68s | 32.71s | - |
| http_req_duration{expected_response:true} | 925 | 2.45s | 5.00s | 7.70s | 15.09s | 20.70s | - |
| iteration_duration | 2,707 | 6.42s | 12.04s | 16.40s | 23.68s | 33.71s | - |

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
| ::auth register only probe::register probe status 200 | 925 | 1782 | 34.17% |
| ::auth register only probe::register probe success true | 925 | 1782 | 34.17% |
| ::auth register only probe::register probe has member id | 925 | 1782 | 34.17% |

