-- =====================================================================
--  Migration: Assessment 2 database (5 tables)  ->  final design (6 tables)
--  UNTESTED against a real MySQL server - read it, run it step by step, and take a
--  backup first:   mysqldump -u root -p AlumniMeetAppDB > alumnimeetappdb_backup.sql
--
--  Nothing here drops a table or a database. The last step (dropping the three
--  attendance columns from Registration) is commented out on purpose.
-- =====================================================================
USE AlumniMeetAppDB;

-- Step 1: create the Attendance table (same definition as schema.sql)
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

-- Step 2: copy existing attendance data. TIME values become DATETIME by combining them with the event date.
INSERT INTO Attendance (Registration_ID, Attendance_Status, Check_In_Time, Check_Out_Time)
SELECT R.Registration_ID,
       CASE WHEN R.Attendance_Status = 'Present' THEN 'Present' ELSE 'Absent' END,
       CASE WHEN R.Attendance_Status = 'Present' AND R.Check_In_Time  IS NOT NULL THEN TIMESTAMP(E.Meet_Date, R.Check_In_Time)  END,
       CASE WHEN R.Attendance_Status = 'Present' AND R.Check_In_Time  IS NOT NULL
             AND R.Check_Out_Time IS NOT NULL AND R.Check_Out_Time >= R.Check_In_Time THEN TIMESTAMP(E.Meet_Date, R.Check_Out_Time) END
FROM Registration R JOIN Event E ON E.Meet_ID = R.Meet_ID
WHERE R.Attendance_Status IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM Attendance T WHERE T.Registration_ID = R.Registration_ID)
  AND R.Registration_ID IN (SELECT MIN(Registration_ID) FROM Registration GROUP BY Alumni_ID, Meet_ID);

-- Step 3: check the copy before touching Registration
SELECT COUNT(*) AS registrations_with_attendance FROM Registration WHERE Attendance_Status IS NOT NULL;
SELECT COUNT(*) AS attendance_rows FROM Attendance;

-- Step 4 (optional, after you are happy): the app does not read the old columns, so you may keep them.
-- To finish the normalisation, remove them:
-- ALTER TABLE Registration DROP COLUMN Attendance_Status, DROP COLUMN Check_In_Time, DROP COLUMN Check_Out_Time;

-- Step 5 (optional): the Assessment 2 data contains duplicate (Alumni_ID, Meet_ID) registrations. If SELECT below
-- returns rows, resolve them by hand (delete extras whose payment/feedback you do not need) before adding the rule.
SELECT Alumni_ID, Meet_ID, COUNT(*) AS n FROM Registration GROUP BY Alumni_ID, Meet_ID HAVING COUNT(*) > 1;
-- ALTER TABLE Registration ADD CONSTRAINT uq_registration_alumni_event UNIQUE (Alumni_ID, Meet_ID);
-- ALTER TABLE Registration ADD CONSTRAINT chk_registration_status CHECK (Registration_Status IN ('Pending','Confirmed','Cancelled'));
-- ALTER TABLE Registration MODIFY Registration_Status VARCHAR(20) NOT NULL DEFAULT 'Pending';
-- ALTER TABLE Event MODIFY Capacity INT NOT NULL;   -- the app needs a capacity for every event
