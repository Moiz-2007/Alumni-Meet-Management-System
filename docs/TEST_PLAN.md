# Test plan

**Status legend** - *Executed*: I ran it in my sandbox (JDK 21, no MySQL server available). *You run*: needs MySQL; **not run by me**, so there is no pass/fail result yet.

## Already executed (no database needed)

| What | Result |
|---|---|
| Compile all 46 source files (55 class files) with `javac` (JDK 21) | Compiled without errors |
| 24 checks of Validator and service rules that run before any SQL: empty/invalid email, bad date/time, negative / 3-decimal / oversized amount, bad mobile, rating 0 and 6, end time <= start, capacity 0, absent with times, check-out before check-in, bad payment mode, bad graduation year, missing first name | 24 / 24 passed |
| MySQL error-code -> message mapping (duplicate email, FK delete blocked, CHECK violation) | Messages as intended |
| Start without the Connector/J JAR | Friendly "Cannot find the MySQL driver..." message |

Not executed: the SQL script, every JDBC query, and the Swing windows. Treat all of them as unverified until you run the tests below.

## You run (after `schema.sql` and starting the app)

| # | Test | Steps | Expected | Verify in MySQL |
|---|---|---|---|---|
| 1 | Connection works | Start the app | Dashboard footer: "Connected to ..." | - |
| 2 | Wrong credentials | Put a wrong password in `config/db.properties`, restart | Message "Cannot log in: wrong MySQL username or password" | - |
| 3 | Alumni CRUD | Add `Test User`, edit, search "test", delete | Each step succeeds; deleting alumnus 1 (has registrations) is refused with a clear message | `SELECT * FROM Alumni WHERE Email='test@example.com';` |
| 4 | Events | Add an event, edit, search by venue and by `2026-10`, delete | Works; Available = Capacity - Registered | `SELECT * FROM Event ORDER BY Meet_ID DESC LIMIT 1;` |
| 5 | Registration references | Registration form lists only real alumni/events | Drop-downs match tables | `SELECT COUNT(*) FROM Alumni; SELECT COUNT(*) FROM Event;` |
| 6 | Capacity | Add event "Tiny" capacity 1, register 2 alumni | 2nd attempt rejected "event is full"; cancelling the first frees the seat | `SELECT Capacity, (SELECT COUNT(*) FROM Registration r WHERE r.Meet_ID=e.Meet_ID AND r.Registration_Status<>'Cancelled') FROM Event e WHERE Meet_Name='Tiny';` |
| 7 | Attendance is independent | Create a record, Check-In Now, Check-Out Now; create a 2nd record for the same registration | Times saved; duplicate rejected | `SELECT * FROM Attendance;` and `DESCRIBE Registration;` (no attendance columns) |
| 8 | Payments persist | Add a payment, close and reopen the app | Row still there | `SELECT * FROM Payment ORDER BY Payment_ID DESC LIMIT 1;` |
| 9 | Rating range | Feedback form only offers 1-5; at SQL level: | Java rejects; MySQL error 3819 | `INSERT INTO Feedback (Registration_ID, Rating) VALUES (1, 6);` |
| 10 | Duplicates | Add alumnus with an existing email; add payment with `TXN2026001` | Friendly duplicate messages | `INSERT INTO Payment (Registration_ID,Transaction_ID,Amount,Payment_Mode) VALUES (2,'TXN2026001',1,'UPI');` -> error 1062 |
| 11 | Foreign keys | At SQL level: | 1452 and 1451 errors | `INSERT INTO Registration (Alumni_ID, Meet_ID) VALUES (999, 1);` and `DELETE FROM Registration WHERE Registration_ID = 1;` (payment exists) |
| 12 | Dashboard = SQL | Compare dashboard to counts | Identical | `SELECT (SELECT COUNT(*) FROM Alumni), (SELECT COUNT(*) FROM Event), (SELECT COUNT(*) FROM Registration), (SELECT COUNT(*) FROM Attendance), (SELECT COUNT(*) FROM Payment WHERE Payment_Status='Paid'), (SELECT COUNT(*) FROM Feedback);` |
| 13 | Empty database | Copy `schema.sql`, rename the database to `AlumniMeetAppDB_Empty`, delete the SAMPLE DATA block, run it, point `ALUMNI_DB_URL` at it | Dashboard shows zeros, every screen shows "No records yet", reports say "returned no rows", no crash | - |
| 14 | Restart | Add a record, close, reopen | Record still present | `SELECT COUNT(*) FROM Alumni;` |
| 15 | Modules together | Alumni -> Event -> Registration -> Attendance -> Payment -> Feedback -> Reports for one new alumnus | Report 1, 4, 5, 6 include the new data | Report queries are shown in the Reports screen |

Also check the reports against the sample data: Report 7 should list Ananya Iyer and Vikram Singh; Report 8 should list
registration 4 (Pending payment) and registrations 6, 7, 8 and 9 (no payment recorded).
