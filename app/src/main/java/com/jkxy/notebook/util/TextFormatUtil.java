package com.jkxy.notebook.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Think on 16-5-22.
 * <p/>
 * 格式化字符串的工具类
 */
public class TextFormatUtil {
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static String formatDate(Date date) {
        return dateFormat.format(date);
    }

    public static Date parseText(String text) throws ParseException {
        return dateFormat.parse(text);
    }

    public static String getNoteSummary(String content) {
        if (content.length() > 10) {
            if (content.startsWith("☆")) {
                return "图片";
            }

            String temp = content.substring(0, 10);

            if (temp.contains("☆")) {
                int index = temp.indexOf("☆");
                StringBuilder sb = new StringBuilder(temp.substring(0, index));
                return sb.append("...").toString();
            } else {
                StringBuilder sb = new StringBuilder(temp);
                return sb.append("...").toString();
            }


        }
        return content;
    }

}
