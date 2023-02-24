package ua.com.supersonic.android.notebook.experiments;

import android.text.*;
/*
import android.text.Editable;
import android.text.SpannableStringBuilder;
*/

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class Experiments {
    public static void main(String[] args) throws IOException {
        TimeZone curTZ = TimeZone.getDefault();
        System.out.println(curTZ.getOffset(new Date().getTime()));
        System.out.println(curTZ.getRawOffset());


//        String uriString = "https://www.dropbox.com/s/u2bhfjd6da7uard/1.txt?raw=1";
//        String uriString1 = "https://www.dropbox.com/s/u2bhfjd6da7uard/1.txt?raw=2";

//        System.out.println(readFromUri(uriString1));
//        writeToUri(uriString1);

/*
        Editable editable = new SpannableStringBuilder();
        editable.append("abc");
        editable.replace(0, 0, "o");
        System.out.println(editable);
*/
//        Date curDate = new Date();
//        System.out.println(curDate);
        /*DateFormat dateFormat = Utils.getDateFormatInstance(Utils.FormatType.DB_DATE_TIME);
        System.out.println(dateFormat.format(curDate));
        System.out.println("--------------");

        Date transformedDate = getMonthStartOf(curDate);
        System.out.println(dateFormat.format(curDate));
        System.out.println(dateFormat.format(transformedDate));
        System.out.println(dateFormat.format(curDate));*/

    }

    private static Date getMonthStartOf(Date inputDate) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(inputDate);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getActualMinimum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getActualMinimum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getActualMinimum(Calendar.SECOND));
        return calendar.getTime();
    }

    private static StringBuilder readFromUri(String uriString) {
        URI uri = URI.create(uriString);
        StringBuilder builder = new StringBuilder();
        try {
            URL url = uri.toURL();
            try (
                    InputStream stream = url.openStream();
                    BufferedReader bufReader = new BufferedReader(new InputStreamReader(stream))
            ) {

                String curLine;

                while ((curLine = bufReader.readLine()) != null) {
                    builder.append(curLine).append("\n");
                }
                if (builder.length() != 0) {
                    builder.delete(builder.length() - 1, builder.length());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return builder;
    }

    private static void writeToUri(String uriString) {
        URI uri = URI.create(uriString);
        try {
            URL url = uri.toURL();
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            try (
                    BufferedWriter bufWriter = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()))
            ) {
                bufWriter.write("New Line From App");
                bufWriter.newLine();
                bufWriter.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
