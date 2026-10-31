package model;

import java.time.LocalDateTime;

public record Attendance(Integer id, int registrationId, String status, LocalDateTime checkIn,
                         LocalDateTime checkOut, String alumniName, String meetName) {}
