package org.telegram.messenger;

import java.util.Calendar;

public class JalaliCalendar {

    public static final String[] PERSIAN_MONTHS = {
            "فروردین", "اردیبهشت", "خرداد",
            "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر",
            "دی", "بهمن", "اسفند"
    };

    public static final String[] PERSIAN_WEEKDAYS = {
            "یکشنبه", "دوشنبه", "سه‌شنبه",
            "چهارشنبه", "پنجشنبه", "جمعه", "شنبه"
    };

    public static int[] gregorianToJalali(int gy, int gm, int gd) {
        int[] g_d_m = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
        int gy2 = (gm > 2) ? (gy + 1) : gy;
        int days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + g_d_m[gm - 1];
        int jy = -1595 + (33 * (days / 12053));
        days %= 12053;
        jy += 4 * (days / 1461);
        days %= 1461;
        if (days > 365) {
            jy += (days - 1) / 365;
            days = (days - 1) % 365;
        }
        int jm, jd;
        if (days < 186) {
            jm = 1 + (days / 31);
            jd = 1 + (days % 31);
        } else {
            jm = 7 + ((days - 186) / 30);
            jd = 1 + ((days - 186) % 30);
        }
        return new int[]{jy, jm, jd};
    }

    public static int[] fromMillis(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return gregorianToJalali(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
        );
    }

    public static String getMonthName(int jm) {
        if (jm < 1 || jm > 12) return "";
        return PERSIAN_MONTHS[jm - 1];
    }

    public static String getWeekdayName(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        int dow = cal.get(Calendar.DAY_OF_WEEK);
        return PERSIAN_WEEKDAYS[dow - 1];
    }

    public static String formatDayMonth(long millis) {
        int[] j = fromMillis(millis);
        return j[2] + " " + getMonthName(j[1]);
    }

    public static String formatDayMonthYear(long millis) {
        int[] j = fromMillis(millis);
        return j[2] + " " + getMonthName(j[1]) + " " + j[0];
    }

    public static String formatMonthYear(long millis) {
        int[] j = fromMillis(millis);
        return getMonthName(j[1]) + " " + j[0];
    }

    public static String formatNumeric(long millis) {
        int[] j = fromMillis(millis);
        return j[0] + "/" + (j[1] < 10 ? "0" + j[1] : j[1]) + "/" + (j[2] < 10 ? "0" + j[2] : j[2]);
    }

    public static boolean isSameJalaliYear(long millis1, long millis2) {
        return fromMillis(millis1)[0] == fromMillis(millis2)[0];
    }
}
