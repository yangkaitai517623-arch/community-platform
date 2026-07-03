-- Check service request/order consistency before or after running backfill scripts.
-- This script is read-only.

SELECT 'repair_missing_order' AS issue, rr.id AS request_id, rr.status, rr.user_id, rr.worker_id
FROM repair_request rr
LEFT JOIN repair_order ro ON ro.request_id = rr.id
WHERE rr.deleted = 0
  AND rr.status IN (1, 2, 3)
  AND rr.worker_id IS NOT NULL
  AND ro.id IS NULL;

SELECT 'errand_missing_order' AS issue, er.id AS request_id, er.status, er.user_id, er.runner_id
FROM errand_request er
LEFT JOIN errand_order eo ON eo.request_id = er.id
WHERE er.deleted = 0
  AND er.status IN (1, 2, 3)
  AND er.runner_id IS NOT NULL
  AND eo.id IS NULL;

SELECT 'repair_active_without_worker' AS issue, rr.id AS request_id, rr.status, rr.user_id, rr.worker_id
FROM repair_request rr
WHERE rr.deleted = 0
  AND rr.status IN (1, 2)
  AND rr.worker_id IS NULL;

SELECT 'errand_active_without_runner' AS issue, er.id AS request_id, er.status, er.user_id, er.runner_id
FROM errand_request er
WHERE er.deleted = 0
  AND er.status IN (1, 2)
  AND er.runner_id IS NULL;

SELECT 'repair_duplicate_order' AS issue, request_id, COUNT(*) AS order_count
FROM repair_order
GROUP BY request_id
HAVING COUNT(*) > 1;

SELECT 'errand_duplicate_order' AS issue, request_id, COUNT(*) AS order_count
FROM errand_order
GROUP BY request_id
HAVING COUNT(*) > 1;

SELECT 'repair_worker_busy_multiple_tasks' AS issue, worker_id, COUNT(*) AS active_count
FROM repair_request
WHERE deleted = 0
  AND status IN (1, 2)
  AND worker_id IS NOT NULL
GROUP BY worker_id
HAVING COUNT(*) > 1;

SELECT 'errand_runner_busy_multiple_tasks' AS issue, runner_id, COUNT(*) AS active_count
FROM errand_request
WHERE deleted = 0
  AND status IN (1, 2)
  AND runner_id IS NOT NULL
GROUP BY runner_id
HAVING COUNT(*) > 1;
