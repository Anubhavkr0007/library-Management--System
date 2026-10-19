CREATE DATABASE IF NOT EXISTS library_management;
USE library_management;

CREATE TABLE IF NOT EXISTS admin(
id INT PRIMARY KEY AUTO_INCREMENT,
username VARCHAR(50) UNIQUE,
password VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS books(
id INT PRIMARY KEY AUTO_INCREMENT,
title VARCHAR(100),
author VARCHAR(100),
quantity INT
);

CREATE TABLE IF NOT EXISTS members(
id INT PRIMARY KEY AUTO_INCREMENT,
name VARCHAR(100),
email VARCHAR(100),
phone VARCHAR(15)
);

CREATE TABLE IF NOT EXISTS issue_records(
id INT PRIMARY KEY AUTO_INCREMENT,
book_id INT,
member_id INT,
issue_date DATE,
due_date DATE,
return_date DATE
);

-- Default admin login: admin / admin123
INSERT IGNORE INTO admin(username, password) VALUES ('admin', 'admin123');
