package com.timevale.drc.base.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 *
 */
public class DateUtils extends org.apache.commons.lang3.time.DateUtils {
    private static final Logger log = LoggerFactory.getLogger(DateUtils.class);

    public static final DateUtils INSTANCE = new DateUtils();
    public static final String YYYY_MM_DD = "yyyyMMdd";
    public static final String shortFormat3 = "yyyy/MM/dd";
    public static final String YYYY_MM_DD_HH = "yyyyMMddHH";
    public static final String webFormat = "yyyy-MM-dd";
    public static final String chineseDtFormat = "yyyy年MM月dd日";
    public static final String yearOnlyFormat = "yyyy";
    public static final String monthOnlyFormat = "MM";
    public static final String dayOnlyFormat = "dd";
    public static final String HH_MM_SS = "HHmmss";
    public static final String shortTimeFormat = "MM-dd HH:mm";
    public static final String chineseShortTimeFormat = "MM月dd日   HH:mm";
    public static final String chineseShortFormat = "MM月dd日";
    public static final String shortDateFormat = "MM-dd";
    public static final String YYYY_MM = "yyyyMM";
    public static final String YYYY_MM_DD_HH_MM_SS = "yyyyMMddHHmmss";
    public static final String newFormat = "yyyy-MM-dd HH:mm:ss";
    public static final String newFormat2 = "yyyy-M-d H:m:s";
    public static final String newFormat3 = "yyyy/MM/dd HH:mm:ss";
    public static final String noSecondFormat = "yyyy-MM-dd HH:mm";
    public static final String YYYY_MM_DD_WEEK_HH_MM = "yyyy-MM-dd E HH:mm";
    public static final String YYYY_MM_DD_WEEK_HH_MM_SS = "yyyy-MM-dd E HH:mm:ss";
    public static final String HH_MM = "HH:mm";
    public static final long ONE_DAY_SECONDS = 86400L;
    public static final long ONE_DAY_MILL_SECONDS = 86400000L;

    public DateUtils() {
    }

    public static String getYear() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy");
        return getDateString(new Date(), dateFormat);
    }

    public static boolean isToday(Date date) {
        Date now = new Date();
        return now.getYear() == date.getYear() && now.getMonth() == date.getMonth() && now.getDate() == date.getDate();
    }

    public static String getYear(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy");
        return getDateString(date, dateFormat);
    }

    public static String getHour() {
        return getHour(new Date());
    }

    public static String getHour(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int hour = c.get(11);
        return hour < 10 ? "0" + hour : Integer.toString(hour);
    }

    public static int getHourInt(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(11);
    }

    public static String getMinute() {
        return getMinute(new Date());
    }

    public static String getMinute(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int hour = c.get(12);
        return hour < 10 ? "0" + hour : Integer.toString(hour);
    }

    public static String getMonth() {
        DateFormat dateFormat = new SimpleDateFormat("MM");
        return getDateString(new Date(), dateFormat);
    }

    public static String getMonth(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("MM");
        return getDateString(date, dateFormat);
    }

    public static String getBeforeMinutes(int minutes) {
        return (new SimpleDateFormat("yyyy-MM-dd HH:mm")).format(Calendar.getInstance().getTimeInMillis() - (long)(minutes * 60 * 1000));
    }

    public static Date getBeforeMinutesForDate(int minutes) {
        Calendar calendar = Calendar.getInstance();
        int now = calendar.get(12);
        calendar.set(12, now - minutes);
        return calendar.getTime();
    }

    public static String getBeforeMinutesExt1(int minutes) {
        return (new SimpleDateFormat("HH:mm")).format(Calendar.getInstance().getTimeInMillis() - (long)(minutes * 60 * 1000));
    }

    public static String getDay() {
        DateFormat dateFormat = new SimpleDateFormat("dd");
        return getDateString(new Date(), dateFormat);
    }

    public static String getDay(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("dd");
        return getDateString(date, dateFormat);
    }

    public static DateFormat getNewDateFormat(String pattern) {
        DateFormat df = new SimpleDateFormat(pattern);
        df.setLenient(false);
        return df;
    }

    public static String format(Date date, String format) {
        return date == null ? null : (new SimpleDateFormat(format)).format(date);
    }

    public static Date parseDateNoTime3(String sDate) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
            return dateFormat.parse(sDate);
        } catch (ParseException var2) {
            log.error("解析日期错误,str=" + sDate);
            return null;
        }
    }

    public static Date parseDateNoTime(String sDate) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            if (sDate != null && sDate.length() >= "yyyyMMdd".length()) {
                if (!StringUtils.isNumeric(sDate)) {
                    throw new ParseException("not all digit", 0);
                } else {
                    return dateFormat.parse(sDate);
                }
            } else {
                throw new ParseException("length too little", 0);
            }
        } catch (Exception var3) {
            log.error("解析日期错误,str=" + sDate);
            return null;
        }
    }

    public static Date parseDate(String sDate, String dateFormatString) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(dateFormatString);
        if (sDate != null && sDate.length() >= dateFormatString.length()) {
            return dateFormat.parse(sDate);
        } else {
            throw new ParseException("length too little", 0);
        }
    }

    public static String getWeekDayByDate(Date date) {
        SimpleDateFormat formatD = new SimpleDateFormat("E");
        String weekDay = formatD.format(date);
        return weekDay;
    }

    public static Date parseDateNoTime(String sDate, String format) throws ParseException {
        if (StringUtils.isBlank(format)) {
            throw new ParseException("Null format. ", 0);
        } else {
            DateFormat dateFormat = new SimpleDateFormat(format);
            if (sDate != null && sDate.length() >= format.length()) {
                return dateFormat.parse(sDate);
            } else {
                throw new ParseException("length too little", 0);
            }
        }
    }

    public static Date parseDateNoTimeWithDelimit(String sDate, String delimit) throws ParseException {
        sDate = sDate.replaceAll(delimit, "");
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        if (sDate != null && sDate.length() == "yyyyMMdd".length()) {
            return dateFormat.parse(sDate);
        } else {
            throw new ParseException("length not match", 0);
        }
    }

    public static Date parseDateLongFormat(String sDate) {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date d = null;
        if (sDate != null && sDate.length() == "yyyyMMddHHmmss".length()) {
            try {
                d = dateFormat.parse(sDate);
            } catch (ParseException var4) {
                return null;
            }
        }

        return d;
    }

    public static Date parseDateNewFormat3(String sDate) {
        try {
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            return dateFormat.parse(sDate);
        } catch (ParseException var2) {
            log.error("解析日期错误,str=" + sDate);
            return null;
        }
    }

    public static Date parseDateNewFormat(String sDate) {
        Date d = null;
        if (sDate != null) {
            SimpleDateFormat dateFormat;
            if (sDate.length() == "yyyy-MM-dd HH:mm:ss".length()) {
                try {
                    dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    d = dateFormat.parse(sDate);
                } catch (ParseException var4) {
                    return null;
                }
            } else {
                try {
                    dateFormat = new SimpleDateFormat("yyyy-M-d H:m:s");
                    d = dateFormat.parse(sDate);
                } catch (ParseException var3) {
                    return null;
                }
            }
        }

        return d;
    }

    public static Date addHours(Date date, long hours) {
        return addMinutes(date, hours * 60L);
    }

    public static Date addMinutes(Date date, long minutes) {
        return addSeconds(date, minutes * 60L);
    }

    public static Date addSeconds(Date date1, long secs) {
        return new Date(date1.getTime() + secs * 1000L);
    }

    public static boolean isValidHour(String hourStr) {
        if (!StringUtils.isEmpty(hourStr) && StringUtils.isNumeric(hourStr)) {
            int hour = new Integer(hourStr);
            if (hour >= 0 && hour <= 23) {
                return true;
            }
        }

        return false;
    }

    public static boolean isValidMinuteOrSecond(String str) {
        if (!StringUtils.isEmpty(str) && StringUtils.isNumeric(str)) {
            int hour = new Integer(str);
            if (hour >= 0 && hour <= 59) {
                return true;
            }
        }

        return false;
    }

    public static Date addDays(Date date1, long days) {
        return addSeconds(date1, days * 86400L);
    }

    public static String getTomorrowDateString(String sDate) throws ParseException {
        Date aDate = parseDateNoTime(sDate);
        aDate = addSeconds(aDate, 86400L);
        return getDateString(aDate);
    }

    public static String getLongDateString(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        return getDateString(date, dateFormat);
    }

    public static String getNewFormatDateString(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return getDateString(date, dateFormat);
    }

    public static String getYearMonthDayWeekHourMinute(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd E HH:mm");
        return getDateString(date, dateFormat);
    }

    public static String getYearMonthDayWeekHourMinuteSecond(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd E HH:mm:ss");
        return getDateString(date, dateFormat);
    }

    public static String getDateString(Date date, DateFormat dateFormat) {
        return date != null && dateFormat != null ? dateFormat.format(date) : null;
    }

    public static String getYesterDayDateString(String sDate) throws ParseException {
        Date aDate = parseDateNoTime(sDate);
        aDate = addSeconds(aDate, -86400L);
        return getDateString(aDate);
    }

    public static String getDateString(Date date) {
        DateFormat df = getNewDateFormat("yyyyMMdd");
        return df.format(date);
    }

    public static String getDateyyyyMMddHHString(Date date) {
        DateFormat df = getNewDateFormat("yyyyMMddHH");
        return df.format(date);
    }

    public static String getShortDateString(Date date) {
        DateFormat df = getNewDateFormat("MM-dd");
        return df.format(date);
    }

    public static String getHourMinute(Date date) {
        DateFormat df = getNewDateFormat("HH:mm");
        return df.format(date);
    }

    public static String getDateDesc(Date date) {
        Date now = new Date();
        long seconds = getDiffSeconds(now, date);
        if (seconds < 60L) {
            return seconds + "秒";
        } else {
            long hours;
            if (seconds < 3600L) {
                hours = seconds / 60L;
                seconds -= hours * 60L;
                return seconds == 0L ? hours + "分" : hours + "分" + seconds + "秒";
            } else if (seconds < 43200L) {
                hours = seconds / 60L / 60L;
                long minutes = seconds / 60L - hours * 60L;
                return minutes == 0L ? hours + "小时" : hours + "小时" + minutes + "分";
            } else {
                return isToday(date) ? "今天" + getHourMinute(date) : getShortTimeString(date);
            }
        }
    }

    public static String getWebDateString(Date date) {
        DateFormat dateFormat = getNewDateFormat("yyyy-MM-dd");
        return getDateString(date, dateFormat);
    }

    public static String getNoSecond(Date date) {
        DateFormat dateFormat = getNewDateFormat("yyyy-MM-dd HH:mm");
        return getDateString(date, dateFormat);
    }

    public static String getChineseDateString(Date date) {
        DateFormat dateFormat = getNewDateFormat("yyyy年MM月dd日");
        return getDateString(date, dateFormat);
    }

    public static String getTodayString() {
        DateFormat dateFormat = getNewDateFormat("yyyyMMdd");
        return getDateString(new Date(), dateFormat);
    }

    public static String getTimeStringgetTimeStringgetTimeString(Date date) {
        DateFormat dateFormat = getNewDateFormat("HHmmss");
        return getDateString(date, dateFormat);
    }

    public static String getShortTimeString(Date date) {
        DateFormat dateFormat = getNewDateFormat("MM-dd HH:mm");
        return getDateString(date, dateFormat);
    }

    public static String getChineseShortTimeString(Date date) {
        DateFormat dateFormat = getNewDateFormat("MM月dd日   HH:mm");
        return getDateString(date, dateFormat);
    }

    public static String getChineseShortString(Date date) {
        DateFormat dateFormat = getNewDateFormat("MM月dd日");
        return getDateString(date, dateFormat);
    }

    public static String getBeforeDayString(int days) {
        Date date = new Date(System.currentTimeMillis() - 86400000L * (long)days);
        DateFormat dateFormat = getNewDateFormat("yyyyMMdd");
        return getDateString(date, dateFormat);
    }

    public static String getAddDayString(int days) {
        Date date = new Date(System.currentTimeMillis() + 86400000L * (long)days);
        DateFormat dateFormat = getNewDateFormat("yyyyMMdd");
        return getDateString(date, dateFormat);
    }

    public static long getDiffSeconds(Date one, Date two) {
        Calendar sysDate = new GregorianCalendar();
        sysDate.setTime(one);
        Calendar failDate = new GregorianCalendar();
        failDate.setTime(two);
        return (sysDate.getTimeInMillis() - failDate.getTimeInMillis()) / 1000L;
    }

    public static long getDiffMinutes(Date one, Date two) {
        Calendar sysDate = new GregorianCalendar();
        sysDate.setTime(one);
        Calendar failDate = new GregorianCalendar();
        failDate.setTime(two);
        return (sysDate.getTimeInMillis() - failDate.getTimeInMillis()) / 60000L;
    }

    public static long getDiffDays(Date one, Date two) {
        Calendar sysDate = new GregorianCalendar();
        sysDate.setTime(one);
        Calendar failDate = new GregorianCalendar();
        failDate.setTime(two);
        return (sysDate.getTimeInMillis() - failDate.getTimeInMillis()) / 86400000L;
    }

    public static long getDiffHours(Date one, Date two) {
        Calendar sysDate = new GregorianCalendar();
        sysDate.setTime(one);
        Calendar failDate = new GregorianCalendar();
        failDate.setTime(two);
        return (sysDate.getTimeInMillis() - failDate.getTimeInMillis()) / 3600000L;
    }

    public static String getBeforeDayString(String dateString, int days) {
        DateFormat df = getNewDateFormat("yyyyMMdd");

        Date date;
        try {
            date = df.parse(dateString);
        } catch (ParseException var5) {
            date = new Date();
        }

        date = new Date(date.getTime() - 86400000L * (long)days);
        return df.format(date);
    }

    public static boolean isValidShortDateFormat(String strDate) {
        if (strDate.length() != "yyyyMMdd".length()) {
            return false;
        } else {
            try {
                Integer.parseInt(strDate);
            } catch (Exception var4) {
                return false;
            }

            DateFormat df = getNewDateFormat("yyyyMMdd");

            try {
                df.parse(strDate);
                return true;
            } catch (ParseException var3) {
                return false;
            }
        }
    }

    public static boolean isValidShortDateFormat(String strDate, String delimiter) {
        String temp = strDate.replaceAll(delimiter, "");
        return isValidShortDateFormat(temp);
    }

    public static boolean isValidLongDateFormat(String strDate) {
        if (strDate.length() != "yyyyMMddHHmmss".length()) {
            return false;
        } else {
            try {
                Long.parseLong(strDate);
            } catch (Exception var4) {
                return false;
            }

            DateFormat df = getNewDateFormat("yyyyMMddHHmmss");

            try {
                df.parse(strDate);
                return true;
            } catch (ParseException var3) {
                return false;
            }
        }
    }

    public static boolean isValidLongDateFormat(String strDate, String delimiter) {
        String temp = strDate.replaceAll(delimiter, "");
        return isValidLongDateFormat(temp);
    }

    public static String getShortDateString(String strDate) {
        return getShortDateString(strDate, "-|/");
    }

    public static String getShortDateString(String strDate, String delimiter) {
        if (StringUtils.isBlank(strDate)) {
            return null;
        } else {
            String temp = strDate.replaceAll(delimiter, "");
            return isValidShortDateFormat(temp) ? temp : null;
        }
    }

    public static String getShortFirstDayOfMonth() {
        Calendar cal = Calendar.getInstance();
        Date dt = new Date();
        cal.setTime(dt);
        cal.set(5, 1);
        DateFormat df = getNewDateFormat("yyyyMMdd");
        return df.format(cal.getTime());
    }

    public static String getWebTodayString() {
        DateFormat df = getNewDateFormat("yyyy-MM-dd");
        return df.format(new Date());
    }

    public static String getWebFirstDayOfMonth() {
        Calendar cal = Calendar.getInstance();
        Date dt = new Date();
        cal.setTime(dt);
        cal.set(5, 1);
        DateFormat df = getNewDateFormat("yyyy-MM-dd");
        return df.format(cal.getTime());
    }

    public static String convert(String dateString, DateFormat formatIn, DateFormat formatOut) {
        try {
            Date date = formatIn.parse(dateString);
            return formatOut.format(date);
        } catch (ParseException var4) {
            log.warn("convert() --- orign date error: " + dateString);
            return "";
        }
    }

    public static String convert2WebFormat(String dateString) {
        DateFormat df1 = getNewDateFormat("yyyyMMdd");
        DateFormat df2 = getNewDateFormat("yyyy-MM-dd");
        return convert(dateString, df1, df2);
    }

    public static String convert2ChineseDtFormat(String dateString) {
        DateFormat df1 = getNewDateFormat("yyyyMMdd");
        DateFormat df2 = getNewDateFormat("yyyy年MM月dd日");
        return convert(dateString, df1, df2);
    }

    public static String convertFromWebFormat(String dateString) {
        DateFormat df1 = getNewDateFormat("yyyyMMdd");
        DateFormat df2 = getNewDateFormat("yyyy-MM-dd");
        return convert(dateString, df2, df1);
    }

    public static boolean webDateNotLessThan(String date1, String date2) {
        DateFormat df = getNewDateFormat("yyyy-MM-dd");
        return dateNotLessThan(date1, date2, df);
    }

    public static boolean dateNotLessThan(String date1, String date2, DateFormat format) {
        try {
            Date d1 = format.parse(date1);
            Date d2 = format.parse(date2);
            return !d1.before(d2);
        } catch (ParseException var5) {
            log.debug("dateNotLessThan() --- ParseException(" + date1 + ", " + date2 + ")");
            return false;
        }
    }

    public static String getEmailDate(Date today) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH:mm:ss");
        String todayStr = sdf.format(today);
        return todayStr;
    }

    public static String getSmsDate(Date today) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日HH:mm");
        String todayStr = sdf.format(today);
        return todayStr;
    }

    public static String formatTimeRange(Date startDate, Date endDate, String format) {
        if (endDate != null && startDate != null) {
            String rt = null;
            long range = endDate.getTime() - startDate.getTime();
            long day = range / 86400000L;
            long hour = range % 86400000L / 3600000L;
            long minute = range % 3600000L / 60000L;
            if (range < 0L) {
                day = 0L;
                hour = 0L;
                minute = 0L;
            }

            rt = format.replaceAll("dd", String.valueOf(day));
            rt = rt.replaceAll("hh", String.valueOf(hour));
            rt = rt.replaceAll("mm", String.valueOf(minute));
            return rt;
        } else {
            return null;
        }
    }

    public static String formatMonth(Date date) {
        return date == null ? null : (new SimpleDateFormat("yyyyMM")).format(date);
    }

    public static Date getBeforeDate() {
        Date date = new Date();
        return new Date(date.getTime() - 86400000L);
    }

    public static Date getBeforeDate(int days) {
        Date date = new Date();
        return new Date(date.getTime() - 86400000L * (long)days);
    }

    public static Date getDayBegin(Date date) {
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        df.setLenient(false);
        String dateString = df.format(date);

        try {
            return df.parse(dateString);
        } catch (ParseException var4) {
            return date;
        }
    }

    public static Date getDayEnd(Date date) {
        return date == null ? null : addSeconds(addDays(getDayBegin(date), 1), -1);
    }

    public static boolean dateLessThanNowAddMin(Date date, long min) {
        return addMinutes(date, min).before(new Date());
    }

    public static boolean isBeforeNow(Date date) {
        if (date == null) {
            return false;
        } else {
            return date.compareTo(new Date()) < 0;
        }
    }

    public static Date parseNoSecondFormat(String sDate) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        if (sDate != null && sDate.length() >= "yyyy-MM-dd HH:mm".length()) {
            if (!StringUtils.isNumeric(sDate)) {
                throw new ParseException("not all digit", 0);
            } else {
                return dateFormat.parse(sDate);
            }
        } else {
            throw new ParseException("length too little", 0);
        }
    }

    public static Date now() {
        return new Date();
    }

    public static Date parseWebFormat(String sDate) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        if (sDate != null && sDate.length() >= "yyyy-MM-dd".length()) {
            return dateFormat.parse(sDate);
        } else {
            throw new ParseException("length too little", 0);
        }
    }

    public static String getWeekDate(Date date, int week) {
        String strTemp = "";
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int dayofweek = c.get(7) - 1;
        if (dayofweek == 0) {
            dayofweek = 7;
        }

        c.add(5, -dayofweek + week);
        strTemp = c.get(1) + "-";
        if (c.get(2) + 1 < 10) {
            strTemp = strTemp + "0";
        }

        strTemp = strTemp + (c.get(2) + 1) + "-";
        if (c.get(5) < 10) {
            strTemp = strTemp + "0";
        }

        strTemp = strTemp + c.get(5);
        return strTemp;
    }

    public static Date getMonthFirstDay(Date sourceDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(sourceDate);
        calendar.set(5, calendar.getActualMinimum(5));
        return calendar.getTime();
    }

    public static Date getMonthLastDay(Date sourceDate) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(sourceDate);
        calendar.set(5, calendar.getActualMaximum(5));
        return calendar.getTime();
    }

    public static Date getNowMonthFirstDay() {
        return getMonthFirstDay(now());
    }

    public static Date getNowMonthLastDay() {
        return getMonthLastDay(now());
    }

    public static Date getWeekFirstDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        Date firstDay = addDays(date, c.get(7) == 1 ? -6 : -1 * c.get(7) + 2);
        return firstDay;
    }

    public static Date getWeekLastDay(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        Date lastDay = addDays(date, c.get(7) == 1 ? 0 : 8 - c.get(7));
        return lastDay;
    }

    public static Date getNowWeekFirstDay() {
        return getWeekFirstDay(now());
    }

    public static Date getDateWeekFirstDay(Date date) {
        return getWeekFirstDay(date);
    }

    public static Date getNowWeekLastDay() {
        return getWeekLastDay(now());
    }

    public static Date getDateWeekLastDay(Date date) {
        return getWeekLastDay(date);
    }

    public static String getWeekOfDate(Date dt) {
        String[] weekDays = new String[]{"日", "一", "二", "三", "四", "五", "六"};
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        int w = cal.get(7) - 1;
        if (w < 0) {
            w = 0;
        }

        return weekDays[w];
    }

    public static int getIntWeekOfDate(Date dt) {
        Integer[] weekDays = new Integer[]{7, 1, 2, 3, 4, 5, 6};
        Calendar cal = Calendar.getInstance();
        cal.setTime(dt);
        int w = cal.get(7) - 1;
        if (w < 0) {
            w = 0;
        }

        return weekDays[w];
    }

    public static String changeDateStyle(String dateStr, String originalFormat, String newFormat) {
        Date date = null;

        SimpleDateFormat format;
        try {
            format = new SimpleDateFormat(originalFormat);
            date = format.parse(dateStr);
        } catch (ParseException var5) {
            var5.printStackTrace();
        }

        format = new SimpleDateFormat(newFormat);
        return format.format(date);
    }
}
