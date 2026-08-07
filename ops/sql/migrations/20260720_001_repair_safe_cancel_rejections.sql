BEGIN;

-- Historical terminal requests attempted to cancel order 52 after it had
-- already settled. Older code classified that safe precondition rejection as
-- a recovery failure and changed the completed order to EXCEPTION. Restore
-- only the mechanically provable settled shape and preserve the operation
-- audit trail as FAILED, not silently completed.
UPDATE market_order
SET order_status = 'FILLED',
    updated_at = now()
WHERE order_id = 52
  AND order_status = 'EXCEPTION'
  AND open_quantity = 0
  AND filled_quantity > 0;

UPDATE market_operation_log
SET operation_status = 'FAILED',
    updated_at = now()
WHERE operation_type = 'SELL_ORDER_CANCEL'
  AND operation_status = 'RECOVERY_REQUIRED'
  AND related_order_id = 52
  AND message = 'order is not cancellable in current status';

COMMIT;
