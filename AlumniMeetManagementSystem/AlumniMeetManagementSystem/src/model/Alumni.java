package model;

import java.time.LocalDate;

/** age is derived (not stored): it is computed in SQL from DOB, as in the ER diagram's dashed Age attribute. */
public record Alumni(Integer id, String firstName, String lastName, String gender, LocalDate dob,
                     String email, String mobileNo, String degree, String department,
                     Integer graduationYear, Integer age) {
    public String fullName() { return firstName + " " + lastName; }
}
