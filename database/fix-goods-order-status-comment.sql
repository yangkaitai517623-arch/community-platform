-- Existing database migration: align goods_order status comment with application code.
ALTER TABLE goods_order
    MODIFY status TINYINT DEFAULT 0 COMMENT '状态: 0-待确认 1-已确认 2-已完成 4-已取消';
