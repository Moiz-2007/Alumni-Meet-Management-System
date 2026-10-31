package model;

import java.time.LocalDate;

public record Feedback(Integer id, int registrationId, int rating, String comments, LocalDate date,
                       String alumniName, String meetName) {}
