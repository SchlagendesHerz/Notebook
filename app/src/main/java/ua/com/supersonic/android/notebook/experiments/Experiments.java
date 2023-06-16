package ua.com.supersonic.android.notebook.experiments;

import static ua.com.supersonic.android.notebook.utils.Utils.FormatType.DEFAULT_DATE_TIME;

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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class Experiments {
    static class A implements Runnable {
        private C c;

        public A(C c) {
            this.c = c;
        }

        @Override
        public void run() {
            System.out.println("A is about to pause");
            c.pause();
            System.out.println("A is out of pause");
        }
    }

    static class B implements Runnable {

        private C c;

        public B(C c) {
            this.c = c;
        }

        @Override
        public void run() {
            System.out.println("B is about to resume");
            c.resume();
            System.out.println("B is after resume");
        }
    }

    static class C {
        synchronized void pause() {
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        synchronized void resume() {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            notify();
        }
    }


    public static void main(String[] args) throws IOException, ParseException {
        C c = new C();
        Thread a = new Thread(new A(c));
        Thread b = new Thread(new B(c));
        a.start();
        b.start();
//        TimeZone curTZ = TimeZone.getDefault();
//        System.out.println(curTZ.getOffset(new Date().getTime()));
//        System.out.println(curTZ.getRawOffset());


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
