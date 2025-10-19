-- SQL to create chat_history table for MySQL
CREATE TABLE IF NOT EXISTS chat_history (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(255) NOT NULL,
  message TEXT NOT NULL,
  timestamp DATETIME NOT NULL
);
