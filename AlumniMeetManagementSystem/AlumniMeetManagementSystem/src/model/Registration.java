package model;

import java.time.LocalDate;

/** alumniName / meetName are read-only display values filled by JOIN queries. */
public record Registration(Integer id, int alumniId, int meetId, LocalDate date, String status,
                           String alumniName, String meetName) {}
