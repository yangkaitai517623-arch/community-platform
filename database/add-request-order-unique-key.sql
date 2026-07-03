-- Ensure each repair/errand request has at most one generated service order.
-- This script is safe to run more than once on MySQL.

DELETE ro1 FROM repair_order ro1
JOIN repair_order ro2
  ON ro1.request_id = ro2.request_id
 AND ro1.id > ro2.id;

SET @repair_order_request_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'repair_order'
      AND index_name = 'uk_repair_order_request'
);

SET @repair_order_request_index_sql = IF(
    @repair_order_request_index_exists = 0,
    'ALTER TABLE repair_order ADD UNIQUE KEY uk_repair_order_request (request_id)',
    'SELECT ''uk_repair_order_request already exists'' AS message'
);

PREPARE repair_order_request_index_stmt FROM @repair_order_request_index_sql;
EXECUTE repair_order_request_index_stmt;
DEALLOCATE PREPARE repair_order_request_index_stmt;

DELETE eo1 FROM errand_order eo1
JOIN errand_order eo2
  ON eo1.request_id = eo2.request_id
 AND eo1.id > eo2.id;

SET @errand_order_request_index_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'errand_order'
      AND index_name = 'uk_errand_order_request'
);

SET @errand_order_request_index_sql = IF(
    @errand_order_request_index_exists = 0,
    'ALTER TABLE errand_order ADD UNIQUE KEY uk_errand_order_request (request_id)',
    'SELECT ''uk_errand_order_request already exists'' AS message'
);

PREPARE errand_order_request_index_stmt FROM @errand_order_request_index_sql;
EXECUTE errand_order_request_index_stmt;
DEALLOCATE PREPARE errand_order_request_index_stmt;
