package ua.com.supersonic.android.notebook.experiments;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Arrays;

public class Experiments {
    public static void main(String[] args) throws IOException {
        String uriString = "https://www.dropbox.com/s/u2bhfjd6da7uard/1.txt?raw=1";
        String uriString1 = "https://www.dropbox.com/s/u2bhfjd6da7uard/1.txt?raw=2";

//        System.out.println(readFromUri(uriString1));
        writeToUri(uriString1);

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
