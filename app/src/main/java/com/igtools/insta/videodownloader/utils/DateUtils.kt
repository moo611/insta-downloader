package com.igtools.insta.videodownloader.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    const val FORMAT_YYYYMMDD = "yyyy-MM-dd"
    const val FORMAT_YYYYMMDDHHMMSS = "yyyy-MM-dd HH:mm:ss"

    @Throws(ParseException::class)
    fun getDate(date: String?): Date? {
        return SimpleDateFormat(FORMAT_YYYYMMDD).parse(date)
    }

    fun getDate(date: Date?): String {
        val formatter =
            SimpleDateFormat(FORMAT_YYYYMMDD)
        return formatter.format(date)
    }

    @Throws(ParseException::class)
    fun getDateHHMMSS(date: String?): Date? {
        return SimpleDateFormat(FORMAT_YYYYMMDDHHMMSS).parse(date)
    }

    /**
     * 获取指定beForDay之前的日期
     * @param beForDay
     * @return
     */
    private fun getPreDay(date: Date?, beForDay: Int): Date {
        val calendarStart = Calendar.getInstance()
        calendarStart.time = date
        calendarStart.add(Calendar.DAY_OF_YEAR, -beForDay)
        return calendarStart.time
    }

    /**
     * 获取指定beForDay后一天的日期
     * @param beForDay
     * @return
     */
    private fun getEndDay(date: Date?, beForDay: Int): Date {
        val calendarEnd = Calendar.getInstance()
        calendarEnd.time = date
        calendarEnd.add(Calendar.DAY_OF_YEAR, -beForDay + 1)
        return calendarEnd.time
    }

    @Throws(ParseException::class)
    fun getDateYYMMDD00(date: String?): Date {
        val parse = SimpleDateFormat(FORMAT_YYYYMMDD).parse(date)
        return getPreDay(parse, 0)
    }

    @Throws(ParseException::class)
    fun getDateYYMMDD59(date: String?): Date {
        val parse = SimpleDateFormat(FORMAT_YYYYMMDD).parse(date)
        return getEndDay(parse, 0)
    }

    fun getDateYYMMDD00(date: Date?): String {
        val formatter = SimpleDateFormat(FORMAT_YYYYMMDD)
        val dateString = formatter.format(date)
        return "$dateString 00:00:00"
    }

    fun getDateYYMMDD59(date: Date?): String {
        val formatter = SimpleDateFormat(FORMAT_YYYYMMDD)
        val dateString = formatter.format(date)
        return "$dateString 23:59:59"
    }
}