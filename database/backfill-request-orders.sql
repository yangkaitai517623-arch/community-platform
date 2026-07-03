-- Backfill missing service-order rows for existing repair/errand requests.
-- Use this once on an existing database if old completed/active requests were created
-- before repair_order / errand_order were wired into the business flow.

INSERT INTO repair_order (
    order_no,
    request_id,
    user_id,
    worker_id,
    amount,
    status,
    created_at,
    updated_at
)
SELECT
    CONCAT('RO', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), LPAD(rr.id, 8, '0')) AS order_no,
    rr.id AS request_id,
    rr.user_id,
    rr.worker_id,
    COALESCE(rr.actual_price, rr.estimated_price, 0) AS amount,
    CASE WHEN rr.status = 3 THEN 1 ELSE 0 END AS status,
    COALESCE(rr.created_at, NOW()) AS created_at,
    NOW() AS updated_at
FROM repair_request rr
WHERE rr.deleted = 0
  AND rr.status IN (1, 2, 3)
  AND rr.worker_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM repair_order ro WHERE ro.request_id = rr.id
  );

INSERT INTO errand_order (
    order_no,
    request_id,
    user_id,
    runner_id,
    amount,
    status,
    created_at,
    updated_at
)
SELECT
    CONCAT('EO', DATE_FORMAT(NOW(), '%Y%m%d%H%i%s'), LPAD(er.id, 8, '0')) AS order_no,
    er.id AS request_id,
    er.user_id,
    er.runner_id,
    COALESCE(er.reward, 0) AS amount,
    CASE WHEN er.status = 3 THEN 1 ELSE 0 END AS status,
    COALESCE(er.created_at, NOW()) AS created_at,
    NOW() AS updated_at
FROM errand_request er
WHERE er.deleted = 0
  AND er.status IN (1, 2, 3)
  AND er.runner_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM errand_order eo WHERE eo.request_id = er.id
  );
