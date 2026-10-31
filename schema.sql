-- =====================================================================
--  Alumni Meet Management System  -  final schema (six tables)
--  Requires MySQL 8.0.16 or newer (CHECK constraints are enforced from 8.0.16).
--
--  SAFE BY DEFAULT: nothing here drops or empties anything.
--    * CREATE DATABASE / CREATE TABLE use IF NOT EXISTS.
--    * Sample data is inserted only when ALL six tables are empty.
--
--  If AlumniMeetDB already holds your Assessment 2 tables (5 tables, attendance
--  columns inside Registration), IF NOT EXISTS will keep the OLD tables and the
--  Attendance table will be added next to them. Run
--  database/migrate_assessment2_to_final.sql instead - see README.
--
--  Run:  mysql -u root -p < database/schema.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS AlumniMeetDB CHARACTER SET utf8mb4;
USE AlumniMeetDB;

-- ---------- 1. Alumni (parent) ----------
CREATE TABLE IF NOT EXISTS Alumni (
    Alumni_ID       INT AUTO_INCREMENT PRIMARY KEY,
    First_Name      VARCHAR(50)  NOT NULL,
    Last_Name       VARCHAR(50)  NOT NULL,
    Gender          CHAR(1),
    DOB             DATE,
    Email           VARCHAR(150) NOT NULL,
    Mobile_No       VARCHAR(15),
    Degree          VARCHAR(50),
    Department      VARCHAR(50),
    Graduation_Year INT,
    CONSTRAINT uq_alumni_email       UNIQUE (Email),
    CONSTRAINT chk_alumni_gender     CHECK (Gender IN ('M', 'F', 'O')),
    CONSTRAINT chk_alumni_grad_year  CHECK (Graduation_Year BETWEEN 1950 AND 2100),
    INDEX idx_alumni_name (First_Name)                       -- original Step 14 index
) ENGINE=InnoDB;

-- ---------- 2. Event (parent) ----------
CREATE TABLE IF NOT EXISTS Event (
    Meet_ID    INT AUTO_INCREMENT PRIMARY KEY,
    Meet_Name  VARCHAR(100) NOT NULL,
    Meet_Date  DATE         NOT NULL,
    Venue      VARCHAR(100),
    Start_Time TIME,
    End_Time   TIME,
    Capacity   INT          NOT NULL,
    CONSTRAINT chk_event_capacity CHECK (Capacity > 0),
    CONSTRAINT chk_event_times    CHECK (Start_Time IS NULL OR End_Time IS NULL OR End_Time > Start_Time)
) ENGINE=InnoDB;

-- ---------- 3. Registration (Alumni N : 1 ... Event N : 1) ----------
CREATE TABLE IF NOT EXISTS Registration (
    Registration_ID     INT AUTO_INCREMENT PRIMARY KEY,
    Alumni_ID           INT         NOT NULL,
    Meet_ID             INT         NOT NULL,
    Registration_Date   DATE        NOT NULL DEFAULT (CURRENT_DATE),
    Registration_Status VARCHAR(20) NOT NULL DEFAULT 'Pending',   -- original Step 13 default
    CONSTRAINT FK_Alumni FOREIGN KEY (Alumni_ID) REFERENCES Alumni (Alumni_ID)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT FK_Event  FOREIGN KEY (Meet_ID)   REFERENCES Event (Meet_ID)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT uq_registration_alumni_event UNIQUE (Alumni_ID, Meet_ID),
    CONSTRAINT chk_registration_status CHECK (Registration_Status IN ('Pending', 'Confirmed', 'Cancelled'))
) ENGINE=InnoDB;

-- ---------- 4. Attendance (weak entity of Registration, 1 : 1) ----------
-- The ER diagram draws Attendance as a weak entity (double border) with a double-lined
-- "Records" relationship, so it cannot exist without its Registration: one record per
-- registration (UNIQUE) and it disappears with the registration (ON DELETE CASCADE).
CREATE TABLE IF NOT EXISTS Attendance (
    Attendance_ID     INT AUTO_INCREMENT PRIMARY KEY,
    Registration_ID   INT         NOT NULL,
    Attendance_Status VARCHAR(20) NOT NULL DEFAULT 'Absent',
    Check_In_Time     DATETIME NULL,
    Check_Out_Time    DATETIME NULL,
    CONSTRAINT uq_attendance_registration UNIQUE (Registration_ID),
    CONSTRAINT FK_Registration_Attendance FOREIGN KEY (Registration_ID) REFERENCES Registration (Registration_ID)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT chk_attendance_status CHECK (Attendance_Status IN ('Present', 'Absent')),
    CONSTRAINT chk_attendance_times  CHECK (Check_Out_Time IS NULL
                                            OR (Check_In_Time IS NOT NULL AND Check_Out_Time >= Check_In_Time)),
    CONSTRAINT chk_attendance_absent CHECK (Attendance_Status = 'Present'
                                            OR (Check_In_Time IS NULL AND Check_Out_Time IS NULL))
) ENGINE=InnoDB;

-- ---------- 5. Payment ----------
CREATE TABLE IF NOT EXISTS Payment (
    Payment_ID      INT AUTO_INCREMENT PRIMARY KEY,
    Registration_ID INT           NOT NULL,
    Transaction_ID  VARCHAR(50)   NOT NULL,
    Amount          DECIMAL(8,2)  NOT NULL,
    Payment_Mode    VARCHAR(20)   NOT NULL,
    Payment_Date    DATE          NOT NULL DEFAULT (CURRENT_DATE),
    Payment_Status  VARCHAR(20)   NOT NULL DEFAULT 'Pending',
    CONSTRAINT uq_payment_transaction UNIQUE (Transaction_ID),
    CONSTRAINT FK_Registration_Payment FOREIGN KEY (Registration_ID) REFERENCES Registration (Registration_ID)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_payment_amount CHECK (Amount >= 0),
    CONSTRAINT chk_payment_mode   CHECK (Payment_Mode IN ('UPI', 'Card', 'Cash', 'Net Banking')),
    CONSTRAINT chk_payment_status CHECK (Payment_Status IN ('Paid', 'Pending', 'Failed', 'Refunded'))
) ENGINE=InnoDB;

-- ---------- 6. Feedback ----------
CREATE TABLE IF NOT EXISTS Feedback (
    Feedback_ID     INT AUTO_INCREMENT PRIMARY KEY,
    Registration_ID INT          NOT NULL,
    Rating          INT          NOT NULL,
    Comments        VARCHAR(255),
    Feedback_Date   DATE         NOT NULL DEFAULT (CURRENT_DATE),
    CONSTRAINT chk_rating CHECK (Rating BETWEEN 1 AND 5),         -- original Step 12
    CONSTRAINT FK_Registration_Feedback FOREIGN KEY (Registration_ID) REFERENCES Registration (Registration_ID)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =====================================================================
--  SAMPLE DATA  (parents first, then dependents; only if every table is empty)
--  Derived from the INSERT queries in Assessment 2, cleaned so it satisfies the new
--  constraints - see database/SCHEMA_NOTES.md for what changed and why.
-- =====================================================================
SET @seed_data = ((SELECT COUNT(*) FROM Alumni) + (SELECT COUNT(*) FROM Event) + (SELECT COUNT(*) FROM Registration)
                + (SELECT COUNT(*) FROM Attendance) + (SELECT COUNT(*) FROM Payment) + (SELECT COUNT(*) FROM Feedback)) = 0;

INSERT INTO Alumni (Alumni_ID, First_Name, Last_Name, Gender, DOB, Email, Mobile_No, Degree, Department, Graduation_Year)
SELECT * FROM (
    SELECT 1 AS Alumni_ID, 'Abdul' AS First_Name, 'Moiz' AS Last_Name, 'M' AS Gender, '2002-05-15' AS DOB,
           'abdul.moiz@gmail.com' AS Email, '9876543210' AS Mobile_No, 'B.Tech' AS Degree,
           'Computer Science' AS Department, 2024 AS Graduation_Year
    UNION ALL SELECT 2, 'Rahul',  'Sharma', 'M', '2001-08-12', 'rahul@gmail.com',  '9123456780', 'B.Tech', 'Mechanical',      2023
    UNION ALL SELECT 3, 'Priya',  'Patel',  'F', '2000-04-18', 'priya@gmail.com',  '9988776655', 'B.Tech', 'Electronics',     2022
    UNION ALL SELECT 4, 'Sneha',  'Reddy',  'F', '1998-11-20', 'sneha@gmail.com',  '9871234567', 'M.Tech', 'Computer Science', 2021
    UNION ALL SELECT 5, 'Arjun',  'Kumar',  'M', '1996-01-25', 'arjun@gmail.com',  '9765432109', 'MBA',    'Management',      2020
    UNION ALL SELECT 6, 'Ananya', 'Iyer',   'F', '1997-03-09', 'ananya.iyer@gmail.com',  '9845012345', 'B.Tech', 'Civil',      2019
    UNION ALL SELECT 7, 'Vikram', 'Singh',  'M', '1995-07-30', 'vikram.singh@gmail.com', '9811122233', 'B.Tech', 'Mechanical', 2018
) AS seed WHERE @seed_data = 1;

INSERT INTO Event (Meet_ID, Meet_Name, Meet_Date, Venue, Start_Time, End_Time, Capacity)
SELECT * FROM (
    SELECT 1 AS Meet_ID, 'Annual Alumni Meet' AS Meet_Name, '2026-08-15' AS Meet_Date, 'VIT Chennai Auditorium' AS Venue,
           '09:00:00' AS Start_Time, '17:00:00' AS End_Time, 500 AS Capacity
    UNION ALL SELECT 2, 'Tech Connect 2026',               '2026-09-20', 'Seminar Hall A',        '10:00:00', '15:00:00',  300
    UNION ALL SELECT 3, 'Career Networking',               '2026-10-05', 'Convention Centre',     '11:00:00', '16:00:00',  250
    UNION ALL SELECT 4, 'Entrepreneur Summit',             '2026-11-18', 'Innovation Lab',        '09:30:00', '14:30:00',  200
    UNION ALL SELECT 5, 'Homecoming 2026',                 '2026-12-10', 'Main Ground',           '10:00:00', '18:00:00', 1000
    UNION ALL SELECT 6, 'GreenOS Sustainability Meetup',   '2026-09-12', 'GreenOS',               '10:00:00', '15:00:00',  300
    UNION ALL SELECT 7, 'Networking Lunch',                '2026-10-05', 'Foodys',                '11:30:00', '14:30:00',  250
    UNION ALL SELECT 8, 'Sports & Cultural Reunion',       '2026-11-08', 'Silver Jubilee Grounds','09:00:00', '16:00:00',  800
    UNION ALL SELECT 9, 'Research & Innovation Connect',   '2026-12-02', 'PRP Block',             '10:00:00', '17:00:00',  500
) AS seed WHERE @seed_data = 1;

INSERT INTO Registration (Registration_ID, Alumni_ID, Meet_ID, Registration_Date, Registration_Status)
SELECT * FROM (
    SELECT 1 AS Registration_ID, 1 AS Alumni_ID, 1 AS Meet_ID, '2026-08-01' AS Registration_Date, 'Confirmed' AS Registration_Status
    UNION ALL SELECT 2, 1, 6, '2026-09-01', 'Confirmed'
    UNION ALL SELECT 3, 1, 7, '2026-09-28', 'Confirmed'
    UNION ALL SELECT 4, 2, 9, '2026-10-20', 'Confirmed'
    UNION ALL SELECT 5, 2, 8, '2026-10-25', 'Confirmed'
    UNION ALL SELECT 6, 3, 7, '2026-09-28', 'Pending'
    UNION ALL SELECT 7, 3, 1, '2026-08-05', 'Pending'
    UNION ALL SELECT 8, 4, 2, '2026-09-05', 'Confirmed'
    UNION ALL SELECT 9, 5, 3, '2026-09-28', 'Confirmed'
) AS seed WHERE @seed_data = 1;

INSERT INTO Attendance (Attendance_ID, Registration_ID, Attendance_Status, Check_In_Time, Check_Out_Time)
SELECT * FROM (
    SELECT 1 AS Attendance_ID, 1 AS Registration_ID, 'Present' AS Attendance_Status,
           '2026-08-15 08:50:00' AS Check_In_Time, '2026-08-15 16:55:00' AS Check_Out_Time
    UNION ALL SELECT 2, 2, 'Present', '2026-09-12 09:55:00', '2026-09-12 14:10:00'
    UNION ALL SELECT 3, 3, 'Present', '2026-10-05 11:20:00', '2026-10-05 14:15:00'
    UNION ALL SELECT 4, 6, 'Absent',  NULL, NULL
    UNION ALL SELECT 5, 8, 'Present', '2026-09-20 10:05:00', '2026-09-20 15:00:00'
) AS seed WHERE @seed_data = 1;

INSERT INTO Payment (Payment_ID, Registration_ID, Transaction_ID, Amount, Payment_Mode, Payment_Date, Payment_Status)
SELECT * FROM (
    SELECT 1 AS Payment_ID, 1 AS Registration_ID, 'TXN2026001' AS Transaction_ID, 1000.00 AS Amount,
           'UPI' AS Payment_Mode, '2026-08-02' AS Payment_Date, 'Paid' AS Payment_Status
    UNION ALL SELECT 2, 2, 'TXN2026002',  500.00, 'Card',        '2026-09-02', 'Paid'
    UNION ALL SELECT 3, 3, 'TXN2026003',  300.00, 'Cash',        '2026-09-29', 'Paid'
    UNION ALL SELECT 4, 4, 'TXN2026004',  800.00, 'UPI',         '2026-10-21', 'Pending'
    UNION ALL SELECT 5, 5, 'TXN2026005', 1200.00, 'Net Banking', '2026-10-26', 'Paid'
) AS seed WHERE @seed_data = 1;

INSERT INTO Feedback (Feedback_ID, Registration_ID, Rating, Comments, Feedback_Date)
SELECT * FROM (
    SELECT 1 AS Feedback_ID, 1 AS Registration_ID, 5 AS Rating,
           'Excellent reunion and well organized.' AS Comments, '2026-08-15' AS Feedback_Date
    UNION ALL SELECT 2, 2, 4, 'GreenOS session was informative and engaging.',        '2026-09-12'
    UNION ALL SELECT 3, 3, 5, 'Foodys networking lunch was a great experience.',      '2026-10-05'
    UNION ALL SELECT 4, 8, 4, 'Tech Connect talks were useful.',                      '2026-09-20'
) AS seed WHERE @seed_data = 1;

-- =====================================================================
--  VERIFICATION QUERIES  (read-only)
-- =====================================================================
SHOW TABLES;                                        -- expect 6 tables

SELECT 'Alumni' AS Tbl, COUNT(*) AS Row_Count FROM Alumni
UNION ALL SELECT 'Event',        COUNT(*) FROM Event
UNION ALL SELECT 'Registration', COUNT(*) FROM Registration
UNION ALL SELECT 'Attendance',   COUNT(*) FROM Attendance
UNION ALL SELECT 'Payment',      COUNT(*) FROM Payment
UNION ALL SELECT 'Feedback',     COUNT(*) FROM Feedback;

DESCRIBE Attendance;                                -- the sixth table
DESCRIBE Registration;                              -- must NOT contain Attendance_Status / Check_In_Time / Check_Out_Time

-- Foreign keys and the referential actions in force
SELECT rc.CONSTRAINT_NAME, rc.TABLE_NAME, kcu.COLUMN_NAME, rc.REFERENCED_TABLE_NAME, rc.DELETE_RULE, rc.UPDATE_RULE
FROM information_schema.REFERENTIAL_CONSTRAINTS rc
JOIN information_schema.KEY_COLUMN_USAGE kcu
  ON kcu.CONSTRAINT_SCHEMA = rc.CONSTRAINT_SCHEMA AND kcu.CONSTRAINT_NAME = rc.CONSTRAINT_NAME AND kcu.TABLE_NAME = rc.TABLE_NAME
WHERE rc.CONSTRAINT_SCHEMA = 'AlumniMeetDB'
ORDER BY rc.TABLE_NAME;

-- CHECK constraints in force
SELECT TABLE_NAME, CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = 'AlumniMeetDB' AND CONSTRAINT_TYPE = 'CHECK' ORDER BY TABLE_NAME;

-- Orphan checks: every query below must return 0 rows
SELECT * FROM Registration R LEFT JOIN Alumni A ON A.Alumni_ID = R.Alumni_ID WHERE A.Alumni_ID IS NULL;
SELECT * FROM Registration R LEFT JOIN Event  E ON E.Meet_ID   = R.Meet_ID   WHERE E.Meet_ID   IS NULL;
SELECT * FROM Attendance T LEFT JOIN Registration R ON R.Registration_ID = T.Registration_ID WHERE R.Registration_ID IS NULL;

-- Joined view of the whole model
SELECT R.Registration_ID, CONCAT(A.First_Name, ' ', A.Last_Name) AS Alumnus, E.Meet_Name, R.Registration_Status,
       T.Attendance_Status, T.Check_In_Time, T.Check_Out_Time, P.Transaction_ID, P.Payment_Status, F.Rating
FROM Registration R
JOIN Alumni A ON A.Alumni_ID = R.Alumni_ID
JOIN Event  E ON E.Meet_ID   = R.Meet_ID
LEFT JOIN Attendance T ON T.Registration_ID = R.Registration_ID
LEFT JOIN Payment    P ON P.Registration_ID = R.Registration_ID
LEFT JOIN Feedback   F ON F.Registration_ID = R.Registration_ID
ORDER BY R.Registration_ID;
