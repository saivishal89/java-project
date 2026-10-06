-- ============================================================
-- Student Management System - Database Setup Script
-- ============================================================
-- This script creates the database, the students table,
-- and inserts 5 sample records for testing.
--
-- HOW TO RUN:
--   1. Open MySQL Workbench (or mysql CLI)
--   2. Copy-paste this entire script
--   3. Execute it
-- ============================================================

-- Step 1: Create the database
CREATE DATABASE IF NOT EXISTS student_db;

-- Step 2: Switch to the new database
USE student_db;

-- Step 3: Create the students table
CREATE TABLE IF NOT EXISTS students (
    id          INT             AUTO_INCREMENT  PRIMARY KEY,
    name        VARCHAR(100)    NOT NULL,
    email       VARCHAR(150)    NOT NULL        UNIQUE,
    phone       VARCHAR(15)     NOT NULL,
    age         INT             NOT NULL,
    gender      VARCHAR(10)     NOT NULL,
    course      VARCHAR(100)    NOT NULL,
    department  VARCHAR(100)    NOT NULL,
    semester    INT             NOT NULL,
    address     VARCHAR(255)    NOT NULL,
    created_at  TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- Step 4: Insert 5 sample students for testing
INSERT INTO students (name, email, phone, age, gender, course, department, semester, address)
VALUES
    ('Rahul Sharma',    'rahul.sharma@gmail.com',   '9876543210', 20, 'Male',   'Java Programming',     'CSE', 3, 'New Delhi, India'),
    ('Priya Patel',     'priya.patel@gmail.com',    '9876543211', 21, 'Female', 'Python Development',   'IT',  5, 'Mumbai, India'),
    ('Amit Kumar',      'amit.kumar@gmail.com',     '9876543212', 22, 'Male',   'Web Development',      'CSE', 7, 'Bangalore, India'),
    ('Sneha Gupta',     'sneha.gupta@gmail.com',    '9876543213', 19, 'Female', 'Data Structures',      'ECE', 2, 'Pune, India'),
    ('Vikram Singh',    'vikram.singh@gmail.com',   '9876543214', 23, 'Male',   'Machine Learning',     'CSE', 8, 'Hyderabad, India');

-- Verify the data
SELECT * FROM students;
