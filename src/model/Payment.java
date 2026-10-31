package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Payment(Integer id, int registrationId, String transactionId, BigDecimal amount,
                      String mode, LocalDate date, String status, String alumniName, String meetName) {}
