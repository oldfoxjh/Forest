package kr.go.forest.das.Log;

import android.os.Binder;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LogWrapper {
    private static final String TAG = "LogWrapper";
    private static final int LOG_FILE_SIZE_LIMIT = 512*1024;
    private static final int LOG_FILE_MAX_COUNT = 9;
    private static final String LOG_DIRECTORY = "DroneAppService/Log";
    private static final String LOG_FILE_NAME = "FileLog%g.txt";
    private static final SimpleDateFormat formatter =
            new SimpleDateFormat("MM-dd HH:mm:ss.SSS: ", Locale.getDefault());
    private static final Date date = new Date();
    private static Logger logger;
    private static FileHandler fileHandler;

    static {
        try {
            // Directory 체크
            String folderPath = Environment.getExternalStorageDirectory() + File.separator + LOG_DIRECTORY ;
            File folder = new File(folderPath);

            if(!folder.exists())
            {
                folder.mkdirs();
            }
            fileHandler = new FileHandler(folderPath + File.separator + LOG_FILE_NAME, LOG_FILE_SIZE_LIMIT, LOG_FILE_MAX_COUNT, true);

            fileHandler.setFormatter(new SimpleFormatter() {
                @Override
                public String format(LogRecord r) {
                    date.setTime(System.currentTimeMillis());
                    StringBuilder ret = new StringBuilder(80);
                    ret.append(formatter.format(date));
                    ret.append(r.getMessage());
                    return ret.toString();
                }
            });

            logger = Logger.getLogger(LogWrapper.class.getName());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
            logger.setUseParentHandlers(false);
            Log.d(TAG, "init success");
        } catch (IOException e) {
            Log.d(TAG, "init failure");
        }
    }

    public static void v(String tag, String msg) {
        if (logger != null) {
            logger.log(Level.INFO, String.format("V/%s(%d): %s\n",
                    tag, Binder.getCallingPid(), msg));
        }

        Log.v(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (logger != null) {
            logger.log(Level.INFO, String.format("I/%s(%d): %s\n",
                    tag, Binder.getCallingPid(), msg));
        }

        Log.i(tag, msg);
    }

    public static void d(String tag, String msg) {
        if (logger != null) {
            logger.log(Level.INFO, String.format("D/%s(%d): %s\n",
                    tag, Binder.getCallingPid(), msg));
        }

        Log.d(tag, msg);
    }

    public static void w(String tag, String msg) {
        if (logger != null) {
            logger.log(Level.INFO, String.format("W/%s(%d): %s\n",
                    tag, Binder.getCallingPid(), msg));
        }

        Log.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (logger != null) {
            logger.log(Level.INFO, String.format("E/%s(%d): %s\n",
                    tag, Binder.getCallingPid(), msg));
        }

        Log.e(tag, msg);
    }
}
