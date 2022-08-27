package ua.com.supersonic.android.notebook.experiments;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import ua.com.supersonic.android.notebook.MainActivity;

public class DropboxExperiments {

    public static void main(String[] args) {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/app_notebook").build();
        DbxClientV2 client = new DbxClientV2(config, "");
        FullAccount account = null;
        try {
            account = client.users().getCurrentAccount();
            System.out.println(account.getName().getDisplayName());

            ListFolderResult result = client.files().listFolder("");
            while (true) {
                for (Metadata metadata : result.getEntries()) {
                    System.out.println(metadata.getPathLower());
                }

                if (!result.getHasMore()) {
                    break;
                }

                result = client.files().listFolderContinue(result.getCursor());
            }
        } catch (DbxException e) {
            e.printStackTrace();
        }

//        try (InputStream in = new FileInputStream(createTempFile())) {
//            FileMetadata metadata = client.files()
//                    .uploadBuilder("/two.txt")
//                    .withMode(WriteMode.OVERWRITE)
//                    .uploadAndFinish(in);
//        }
    }

    private static File createTempFile() {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("prefix", "");
            tempFile.deleteOnExit();
            try (BufferedWriter bufWriter = new BufferedWriter(new FileWriter(tempFile))) {
                bufWriter.write("one");
                bufWriter.newLine();
                bufWriter.write("two");
                bufWriter.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tempFile;
    }
}
