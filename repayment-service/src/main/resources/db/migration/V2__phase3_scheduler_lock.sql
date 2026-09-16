CREATE TABLE IF NOT EXISTS scheduler_locks (
    lock_name VARCHAR(100) PRIMARY KEY,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT IGNORE INTO scheduler_locks (lock_name) VALUES ('repayment-overdue-scheduler');
