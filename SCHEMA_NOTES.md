# Schema notes - what was kept, what changed, and why

Sources: Assessment 1 (Chen ER diagram), Assessment 2 (MySQL tables + 17 queries), Assessment 3 (normalization).

## 1. What the ER diagram (Assessment 1) says

| Item in the diagram | How it is implemented |
|---|---|
| Six entities: Alumni, Registration, Event, Attendance, Payment, Feedback | Six tables with the same names |
| Alumni **1 --- N** Registration ("Registers") | `Registration.Alumni_ID` -> `Alumni` (`FK_Alumni`) |
| Event **1 --- N** Registration ("Contains") | `Registration.Meet_ID` -> `Event` (`FK_Event`) |
| Attendance drawn as a **weak entity** (double box) with a double-lined "Records" relationship | `Attendance.Registration_ID` is `NOT NULL`, `UNIQUE` (one record per registration) and `ON DELETE CASCADE` (it cannot outlive its registration) |
| Registration "Makes" Payment, Registration "Gives" Feedback | `Payment.Registration_ID` and `Feedback.Registration_ID` -> `Registration` (see assumption A3) |
| `Age` drawn dashed = **derived** attribute | Not stored. The app computes it with `TIMESTAMPDIFF(YEAR, DOB, CURDATE())` |
| `Phone` drawn double-lined = **multivalued** | Assessment 2 implements a single `Mobile_No`, so one number is stored (see A2) |
| `Name` composite (First_Name, Last_Name) | Two columns, as in Assessment 2 |

## 2. Assessment 2 -> final schema (column by column)

All original table/column names are unchanged. `Phone` appears as `Mobile_No` because Assessment 2 Step 10 renamed it.
`LinkedIn_URL` was added in Step 8 and dropped in Step 11, so it is correctly absent.

| Table | Change | Reason |
|---|---|---|
| Registration | **Removed** `Attendance_Status`, `Check_In_Time`, `Check_Out_Time` | They move to the new `Attendance` table so the same fact is not stored twice |
| Attendance | **New table** (`Attendance_ID`, `Registration_ID`, `Attendance_Status`, `Check_In_Time`, `Check_Out_Time`) | Assessment 1 has six entities; Assessment 2 had implemented only five |
| Attendance | Times are `DATETIME`, not `TIME` | A check-in belongs to a calendar day; `TIME` alone cannot express an overnight stay or be ordered across days |
| Registration | `Registration_Status` is `NOT NULL DEFAULT 'Pending'` + CHECK (`Pending/Confirmed/Cancelled`) | Original Step 13 default kept; allowed values are an assumption (A4) |
| Registration | `UNIQUE (Alumni_ID, Meet_ID)` | Stops duplicate registration for the same event |
| Alumni | `Email` is `NOT NULL` | Assessment 2 Step 9 (`MODIFY Email VARCHAR(150)`) accidentally dropped NOT NULL; the first CREATE had it and the requirement says email is required |
| Alumni | CHECKs on `Gender` (M/F/O) and `Graduation_Year` (1950-2100) | Final enforcement of the Java validation |
| Event | `Capacity` is `NOT NULL` and `> 0` | The capacity rule needs a value for every event |
| Event | CHECK `End_Time > Start_Time` | Business rule from the requirements |
| Payment | `Transaction_ID`, `Amount`, `Payment_Mode` are `NOT NULL`; CHECK `Amount >= 0`; modes and statuses restricted | Same UNIQUE transaction ID as the original; stricter so bad rows cannot be stored |
| Payment | `FK_Registration_Payment` exists | Assessment 2 Step 15 **dropped** it only as a demonstration; the final design needs it |
| Feedback | `chk_rating CHECK (Rating BETWEEN 1 AND 5)`, `Rating NOT NULL` | Original Step 12 kept |
| Alumni | `idx_alumni_name` on `First_Name` | Original Step 14 kept |

### Referential actions

| Foreign key | ON DELETE | Meaning |
|---|---|---|
| `FK_Alumni`, `FK_Event` | RESTRICT | You cannot delete an alumnus or event that still has registrations |
| `FK_Registration_Payment` | RESTRICT | Payment records are financial history; delete the payment on purpose first |
| `FK_Registration_Attendance` | CASCADE | Attendance is a weak entity of Registration |
| `FK_Registration_Feedback` | CASCADE | Feedback has no meaning without its registration |

All foreign keys use `ON UPDATE CASCADE`.

## 3. Sample data

The INSERTs in Assessment 2 were inserted twice for some tables, and a few values would break the new rules, so the
sample data in `schema.sql` is a cleaned version of it (same people, events, transaction IDs and ratings):

* Alumni `DOB` values were in 2020-2024 (before or at graduation); replaced by plausible birth dates.
* Several events ended earlier than they started (e.g. `11:30 -> 02:30`, clearly meant as 14:30); corrected so `End_Time > Start_Time` holds.
* The duplicated "Annual Alumni Meet 2026" event and the duplicated registration rows were removed.
* Two extra alumni (Ananya Iyer, Vikram Singh) were added so the "alumni with no registration" report shows something.
* Attendance rows come from the old Registration attendance columns, with `TIME` combined with the event date.

## 4. Normalization (Assessment 3)

Assessment 3 shows `Registration_UNF` (one row per alumnus with several events and several payment modes in a single
cell) and `Registration_1NF` (one row per alumnus-event pair, one atomic value per cell). It stops at 1NF.
The explanation below continues from that point; the functional dependencies are **my assumptions**, not something the
PDF states.

Assumed functional dependencies:

* `Alumni_ID -> First_Name, Last_Name, Gender, DOB, Email, Mobile_No, Degree, Department, Graduation_Year`
* `Meet_ID -> Meet_Name, Meet_Date, Venue, Start_Time, End_Time, Capacity`
* `Registration_ID -> Alumni_ID, Meet_ID, Registration_Date, Registration_Status`, and `(Alumni_ID, Meet_ID) -> Registration_ID`
* `Registration_ID -> Attendance_Status, Check_In_Time, Check_Out_Time` (one attendance record per registration)
* `Payment_ID -> Registration_ID, Transaction_ID, Amount, Payment_Mode, Payment_Date, Payment_Status`
* `Feedback_ID -> Registration_ID, Rating, Comments, Feedback_Date`

How the design follows from them:

* **1NF** - every column is atomic (the multi-event and multi-payment-mode cells of the UNF table become separate rows in `Registration` / `Payment`).
* **2NF** - alumni details depend only on `Alumni_ID` and event details only on `Meet_ID`, so they live in `Alumni` and `Event` instead of being repeated for every registration row (the 1NF table repeats `Alumni_Name` and `Event_Name`).
* **3NF** - in each table every non-key column depends on the key only. `Age` is not stored because it is derivable from `DOB`. Attendance columns left `Registration` for the same reason: they describe the attendance, not the registration.

Note on a naming difference: in the Assessment 3 tables `Registration_ID` is the same for all rows of one alumnus
(1, 1, 1). In the final schema `Registration_ID` identifies one alumnus-event registration, as in Assessment 2.

## 5. Assumptions and unresolved points

* **A1** The ER diagram does not label cardinalities for Makes / Gives. The schema allows several payments per registration (instalments, retries) and several feedback rows; the original Assessment 2 had no UNIQUE on those foreign keys either. If your lecturer expects exactly one, add `UNIQUE (Registration_ID)` to `Payment` and/or `Feedback`.
* **A2** The multivalued `Phone` is stored as one `Mobile_No` (as in Assessment 2). A separate `Alumni_Phone` table would be needed for several numbers; it was not added because the brief says not to introduce entities.
* **A3** Payment and Feedback are modelled as 1:N from Registration.
* **A4** Allowed values for the status columns (registration, attendance, payment, payment mode) are my choice, based on the values appearing in Assessment 2's sample data.
* **A5** Cancelled registrations do not occupy a seat.
