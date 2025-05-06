package com.borisey.personal_finance.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FormatService {

    // Привожу строку с датой к формату LocalDateTime
    public static LocalDateTime formatDate(String date) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        return LocalDateTime.parse(date + " 00:00:00", dateTimeFormatter);
    }
}
