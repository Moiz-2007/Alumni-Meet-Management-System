package model;

import java.time.LocalDate;
import java.time.LocalTime;

/** registered = number of non-cancelled registrations (read-only, filled by queries). */
public record Event(Integer id, String name, LocalDate date, String venue, LocalTime startTime,
                    LocalTime endTime, int capacity, Integer registered) {}
