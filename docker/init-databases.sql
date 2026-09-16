CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS customer_db;
CREATE DATABASE IF NOT EXISTS loan_db;
CREATE DATABASE IF NOT EXISTS repayment_db;
CREATE DATABASE IF NOT EXISTS document_db;
CREATE DATABASE IF NOT EXISTS notification_db;
CREATE DATABASE IF NOT EXISTS audit_db;
CREATE DATABASE IF NOT EXISTS dashboard_db;

GRANT ALL PRIVILEGES ON auth_db.* TO 'loan_user'@'%';
GRANT ALL PRIVILEGES ON customer_db.* TO 'loan_user'@'%';
GRANT ALL PRIVILEGES ON loan_db.* TO 'loan_user'@'%';
GRANT ALL PRIVILEGES ON repayment_db.* TO 'loan_user'@'%';
GRANT ALL PRIVILEGES ON document_db.* TO 'loan_user'@'%';
GRANT ALL PRIVILEGES ON notification_db.* TO 'loan_user'@'%';
GRANT ALL PRIVILEGES ON audit_db.* TO 'loan_user'@'%';
GRANT ALL PRIVILEGES ON dashboard_db.* TO 'loan_user'@'%';
FLUSH PRIVILEGES;
