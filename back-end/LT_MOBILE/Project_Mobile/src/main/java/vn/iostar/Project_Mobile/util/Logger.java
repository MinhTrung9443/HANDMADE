package vn.iostar.Project_Mobile.util;


import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
public class Logger {
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_LIGHT_YELLOW_BACKGROUND = "\u001B[48;5;226m";
    public static final String ANSI_RESET = "\u001B[0m"; // Reset màu


    public static void log(Object... message) {
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[2]; // Lấy thông tin stack trace của hàm gọi
        // Lấy tên class và dòng
        String className = stackTraceElement.getClassName();
        int lineNumber = stackTraceElement.getLineNumber();
        // In ra thông tin log với class và dòng
        for (Object o : message) {
            String msg = String.format("[%s:%d] - %s", className, lineNumber, o.toString());
            System.out.println(LocalDateTime.now()+ANSI_LIGHT_YELLOW_BACKGROUND + ANSI_BLACK +msg+ ANSI_RESET);
        }
    }
}