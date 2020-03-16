package com.github.bcanvural.lie;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class DateUtil {

    public static final String AMSTERDAM_ZONE_ID = "Europe/Paris";

    public static String formatCurrentDateToString(String dateFormat) {
        LocalDate localDate = LocalDate.now(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
        return localDate.format(formatter);
    }

    public static Date convertLocalDateToDateViaInstant(LocalDate dateToConvert) {
        return Date.from(dateToConvert.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

}
