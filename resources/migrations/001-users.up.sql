CREATE TABLE users
(id BIGINT AUTO_INCREMENT PRIMARY KEY,
 username VARCHAR(30) UNIQUE NOT NULL,
 password VARCHAR(100) NOT NULL);