# DAU Dashboard Notes

## Purpose

이 문서는 DB 기반 DAU 수집을 운영 대시보드에서 확인할 때 사용할 기본 SQL/PromQL을 정리한다.
Grafana provisioning 파일은 아직 저장소에 두지 않고, 운영 Grafana datasource와 권한 정책이 정해지면 별도 PR로 추가한다.

## Source Of Truth

- 최근 raw DAU:
  `member_daily_activity`
- 장기 추세:
  `daily_active_user_summary`
- 수집 건강도:
  `dau.activity.record.total`

## SQL Panels

오늘 DAU:

```sql
SELECT COUNT(*) AS dau
  FROM member_daily_activity
 WHERE activity_date = DATE(UTC_TIMESTAMP() + INTERVAL 9 HOUR);
```

어제 DAU:

```sql
SELECT COUNT(*) AS dau
  FROM member_daily_activity
 WHERE activity_date = DATE(UTC_TIMESTAMP() + INTERVAL 9 HOUR) - INTERVAL 1 DAY;
```

최근 30일 추세:

```sql
SELECT activity_date, COUNT(*) AS dau
  FROM member_daily_activity
 WHERE activity_date >= DATE(UTC_TIMESTAMP() + INTERVAL 9 HOUR) - INTERVAL 29 DAY
 GROUP BY activity_date
 ORDER BY activity_date;
```

장기 snapshot 추세:

```sql
SELECT activity_date, active_user_count
  FROM daily_active_user_summary
 WHERE activity_date >= DATE(UTC_TIMESTAMP() + INTERVAL 9 HOUR) - INTERVAL 400 DAY
 ORDER BY activity_date;
```

## PromQL Panels

DAU 기록 실패율:

```promql
sum(rate(dau_activity_record_total{result="failure"}[5m]))
/
sum(rate(dau_activity_record_total[5m]))
```

source별 DAU 기록량:

```promql
sum by (source, result) (rate(dau_activity_record_total[5m]))
```

## Privacy Guardrails

- Prometheus label에 `member_id`, `account`, `email`, `phone`, JWT subject를 넣지 않는다.
- Grafana 기본 패널은 aggregate query만 사용한다.
- 회원별 raw row 조회는 운영 DB 권한으로 제한한다.
