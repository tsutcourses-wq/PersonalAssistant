package com.personalassistant.mobile;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class Jalali {
    private Jalali() {}

    public static String today() {
        Calendar c = Calendar.getInstance();
        int[] j = gregorianToJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
        return String.format(Locale.US, "%04d/%02d/%02d", j[0], j[1], j[2]);
    }

    public static int currentYear() {
        Calendar c = Calendar.getInstance();
        return gregorianToJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))[0];
    }

    public static String nowTime() {
        return new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
    }

    public static String normalizeDigits(String s) {
        if (s == null) return "";
        String fa = "۰۱۲۳۴۵۶۷۸۹٠١٢٣٤٥٦٧٨٩";
        String en = "01234567890123456789";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            int idx = fa.indexOf(ch);
            out.append(idx >= 0 ? en.charAt(idx) : ch);
        }
        return out.toString().trim();
    }

    public static long toMillis(String jdate, String timeText) {
        String d = normalizeDigits(jdate).replace('-', '/').replace('.', '/');
        String t = normalizeDigits(timeText == null ? "00:00" : timeText).trim();
        int hh = 0;
        int mm = 0;
        try {
            String[] tp = t.split(":");
            hh = Integer.parseInt(tp[0].trim());
            if (tp.length > 1) mm = Integer.parseInt(tp[1].trim());
        } catch (Exception ignored) {}
        try {
            String[] p = d.split("/");
            int y = Integer.parseInt(p[0].trim());
            int m = Integer.parseInt(p[1].trim());
            int day = Integer.parseInt(p[2].trim());
            int[] g = y < 1700 ? jalaliToGregorian(y, m, day) : new int[]{y, m, day};
            Calendar c = Calendar.getInstance();
            c.set(Calendar.YEAR, g[0]);
            c.set(Calendar.MONTH, g[1] - 1);
            c.set(Calendar.DAY_OF_MONTH, g[2]);
            c.set(Calendar.HOUR_OF_DAY, hh);
            c.set(Calendar.MINUTE, mm);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            return c.getTimeInMillis();
        } catch (Exception ex) {
            return System.currentTimeMillis();
        }
    }

    public static String toIso(String jdate, String timeText) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(new Date(toMillis(jdate, timeText)));
    }

    public static String fromMillis(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        int[] j = gregorianToJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
        return String.format(Locale.US, "%04d/%02d/%02d", j[0], j[1], j[2]);
    }

    public static int[] gregorianToJalali(int gy, int gm, int gd) {
        int[] gdm = {0,31,59,90,120,151,181,212,243,273,304,334};
        int gy2 = (gm > 2) ? gy + 1 : gy;
        int days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gdm[gm - 1];
        int jy = -1595 + 33 * (days / 12053);
        days %= 12053;
        jy += 4 * (days / 1461);
        days %= 1461;
        if (days > 365) {
            jy += (days - 1) / 365;
            days = (days - 1) % 365;
        }
        int jm;
        int jd;
        if (days < 186) {
            jm = 1 + days / 31;
            jd = 1 + days % 31;
        } else {
            jm = 7 + (days - 186) / 30;
            jd = 1 + (days - 186) % 30;
        }
        return new int[]{jy, jm, jd};
    }

    public static int[] jalaliToGregorian(int jy, int jm, int jd) {
        jy += 1595;
        int days = -355668 + (365 * jy) + (jy / 33) * 8 + ((jy % 33 + 3) / 4) + jd;
        if (jm < 7) days += (jm - 1) * 31;
        else days += ((jm - 7) * 30) + 186;
        int gy = 400 * (days / 146097);
        days %= 146097;
        if (days > 36524) {
            gy += 100 * (--days / 36524);
            days %= 36524;
            if (days >= 365) days++;
        }
        gy += 4 * (days / 1461);
        days %= 1461;
        if (days > 365) {
            gy += (days - 1) / 365;
            days = (days - 1) % 365;
        }
        int gd = days + 1;
        int[] monthDays = {0,31, isGregorianLeap(gy) ? 29 : 28,31,30,31,30,31,31,30,31,30,31};
        int gm;
        for (gm = 1; gm <= 12 && gd > monthDays[gm]; gm++) gd -= monthDays[gm];
        return new int[]{gy, gm, gd};
    }

    public static boolean isJalaliLeap(int year) {
        int[] g1 = jalaliToGregorian(year, 1, 1);
        int[] g2 = jalaliToGregorian(year + 1, 1, 1);
        Calendar c1 = Calendar.getInstance();
        c1.clear();
        c1.set(g1[0], g1[1] - 1, g1[2]);
        Calendar c2 = Calendar.getInstance();
        c2.clear();
        c2.set(g2[0], g2[1] - 1, g2[2]);
        long days = (c2.getTimeInMillis() - c1.getTimeInMillis()) / 86400000L;
        return days == 366;
    }

    public static int daysInMonth(int year, int month) {
        if (month <= 6) return 31;
        if (month <= 11) return 30;
        return isJalaliLeap(year) ? 30 : 29;
    }

    private static boolean isGregorianLeap(int y) {
        return (y % 4 == 0 && y % 100 != 0) || (y % 400 == 0);
    }
}
