package com.igtools.insta.videodownloader.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateUtils {


    public static final String FORMAT_YYYYMMDD = "yyyy-MM-dd";
    public static final String FORMAT_YYYYMMDDHHMMSS = "yyyy-MM-dd HH:mm:ss";

    public static Date getDate(String date) throws ParseException{
        Date date1 = new SimpleDateFormat(FORMAT_YYYYMMDD).parse(date);
        return date1;
    }

    public static String getDate(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat(FORMAT_YYYYMMDD);
        String dateString = formatter.format(date);
        return dateString;
    }

    public static Date getDateHHMMSS(String date) throws ParseException{
        Date date1 = new SimpleDateFormat(FORMAT_YYYYMMDDHHMMSS).parse(date);
        return date1;
    }

    /**
     * 获取指定beForDay之前的日期
     * @param beForDay
     * @return
     */
    public static Date getPreDay(Date date,int beForDay){
        Calendar calendarStart = Calendar.getInstance();
        calendarStart.setTime(date);
        calendarStart.add(Calendar.DAY_OF_YEAR, -beForDay);
        return calendarStart.getTime();
    }

    /**
     * 获取指定beForDay后一天的日期
     * @param beForDay
     * @return
     */
    public static Date getEndDay(Date date,int beForDay){
        Calendar calendarEnd = Calendar.getInstance();
        calendarEnd.setTime(date);
        calendarEnd.add(Calendar.DAY_OF_YEAR, -beForDay+1);
        return calendarEnd.getTime();
    }


    public static Date getDateYYMMDD00(String date) throws ParseException{
        Date parse = new SimpleDateFormat(FORMAT_YYYYMMDD).parse(date);
        return getPreDay(parse,0);
    }

    public static Date getDateYYMMDD59(String date) throws ParseException{
        Date parse = new SimpleDateFormat(FORMAT_YYYYMMDD).parse(date);
        return getEndDay(parse,0);
    }


    public static String getDateYYMMDD00(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat(FORMAT_YYYYMMDD);
        String dateString = formatter.format(date);
        return dateString+" 00:00:00";
    }

    public static String getDateYYMMDD59(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat(FORMAT_YYYYMMDD);
        String dateString = formatter.format(date);
        return dateString+" 23:59:59";
    }



}
