# Alumni Meet Management System

Java Swing desktop application + JDBC + MySQL. Manages alumni, events, registrations, attendance, payments and feedback
for the DBMS semester project (Assessments 1-3). No frameworks, no web server: Swing talks directly to MySQL.

> Payments are **tracked records only** (an academic project). Nothing here talks to a bank or UPI.
> There is no login screen because the assessments define no authentication requirement.

## 1. Requirements

| Tool | Version |
|---|---|
| JDK (not just a JRE) | 17 or newer (the code compiled with JDK 21) |
| MySQL Server | **8.0.16 or newer** (CHECK constraints and `DEFAULT (CURRENT_DATE)` need it) |
| MySQL Connector/J | 8.0 or newer (platform independent ZIP) |
| VS Code | with the "Extension Pack for Java" |

Check Java (PowerShell): `java -version` and `javac -version` - both must print 17 or higher.
If `javac` is not found, install a JDK (e.g. Temurin 17/21) and reopen the terminal.

## 2. Project layout

```
AlumniMeetManagementSystem/
  src/Main.java              entry point
  src/database/DBConnection  JDBC connection + config loading
  src/model/                 one record per entity (+ DashboardStats)
  src/dao/                   JDBC code, PreparedStatement only
  src/service/               validation, business rules, transactions
  src/ui/                    Swing screens
  src/util/                  Validator, error mapping, helpers
  database/schema.sql        six tables + sample data + verification queries
  database/SCHEMA_NOTES.md   what changed from Assessment 2 and why
  database/migrate_assessment2_to_final.sql
  config/db.properties.example
  docs/TEST_PLAN.md
  lib/                       put the Connector/J JAR here
```

## 3. Install Connector/J

1. Download the **Platform Independent** ZIP from https://dev.mysql.com/downloads/connector/j/
2. Extract it and copy `mysql-connector-j-<version>.jar` into the project's `lib` folder.

## 4. Configure the database login

```powershell
Copy-Item config\db.properties.example config\db.properties
notepad config\db.properties      # set db.user and db.password
```

`config/db.properties` is git-ignored. Alternatively set environment variables
`ALUMNI_DB_URL`, `ALUMNI_DB_USER`, `ALUMNI_DB_PASSWORD` (they override the file).
The password is never printed or logged.

## 5. Create the database

```powershell
mysql -u root -p < database\schema.sql
```

(or open `schema.sql` in MySQL Workbench / MySQL Shell and run it). The script never drops anything and only inserts
sample data when all six tables are empty. The last part of the script runs verification queries.

**If you already have the `AlumniMeetDB` from Assessment 2**, `CREATE TABLE IF NOT EXISTS` will keep your old five tables.
Take a backup (`mysqldump -u root -p AlumniMeetDB > backup.sql`), then follow `database/migrate_assessment2_to_final.sql`
step by step instead (that script is untested - read it first). The simplest alternative is to create the project in a
fresh database name: replace `AlumniMeetDB` with another name in `schema.sql` and in `db.url`.

## 6. Compile and run (Windows PowerShell, from the project folder)

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -cp "lib/*" -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp "out;lib/*" Main
```

macOS / Linux:

```bash
mkdir -p out && javac -cp "lib/*" -d out $(find src -name '*.java')
java -cp "out:lib/*" Main
```

The Dashboard should say "Connected to jdbc:mysql://localhost:3306/AlumniMeetDB as root" and show live counts.

## 7. Using each module

| Screen | What to try |
|---|---|
| Dashboard | Six live counts; **Refresh** reloads them |
| Alumni | Fill the form -> **Add**; click a row -> edit -> **Update**; **Delete** asks for confirmation; search by name/email/department/year. Age is derived from DOB |
| Events | Add/update events; table shows Registered and Available seats |
| Registrations | Pick an alumnus and an event from the drop-downs -> **Add**. Full events and duplicate registrations are rejected. **Update Status** changes Pending/Confirmed/Cancelled |
| Attendance | Pick a registration -> **Create Record**; select a row then **Check-In Now / Check-Out Now** (stored with the MySQL clock) or type times and **Update** |
| Payments | Record transaction ID, amount, mode, date, status; duplicate transaction IDs are rejected |
| Feedback | Rating 1-5 and comments for a registration |
| Reports | Eight SQL reports; the query is shown above the result so you can explain it in the viva |

## 8. Common problems

| Message | Fix |
|---|---|
| `Cannot find the MySQL driver` | The JAR is not in `lib`, or you ran `java` without `-cp "out;lib/*"` |
| `Cannot log in: wrong MySQL username or password` | Fix `config/db.properties` |
| `Cannot find database AlumniMeetDB` | Run `database/schema.sql` |
| `Cannot connect to MySQL` | Start the MySQL service; check the port in `db.url` |
| `javac is not recognized` | Install a JDK and reopen the terminal |
| `error: invalid source release` / records not supported | JDK older than 17 |
| `Check constraint ... is not recognized` / syntax error near `CHECK` | MySQL older than 8.0.16 |
| `Unknown column 'Check_In_Time'` in Registration | You are on the old Assessment 2 tables - see section 5 |

## 9. Design in one paragraph (for the viva)

`ui` (Swing) calls `service` (validation + business rules + transactions), which calls `dao` (JDBC with
`PreparedStatement`, try-with-resources), which gets connections from `DBConnection`. Database work runs in a
`SwingWorker` so the window never freezes. Validation in Java gives friendly messages first; MySQL constraints
(PK, FK, UNIQUE, CHECK, NOT NULL) are the last line of defence. Registration uses one transaction with
`SELECT ... FOR UPDATE` on the event row so two people cannot take the last seat.

See `database/SCHEMA_NOTES.md` for the schema decisions and `docs/TEST_PLAN.md` for the test plan.
