package googleDriveSync;
import android.util.Log;

import com.google.common.base.Charsets;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class IOUtils {
    public static void writeToFile(String data, File file) throws IOException {
        writeToFile(data.getBytes(Charsets.UTF_8), file);
    }

    public static void writeToFile(byte[] data, File file) throws IOException {
        Throwable th;
        FileOutputStream os = null;
        try {
            FileOutputStream os2 = new FileOutputStream(file);
            try {
                os2.write(data);
                os2.flush();
                os2.getFD().sync();
                if (os2 != null) {
                    os2.close();
                }
            } catch (Throwable th2) {
                th = th2;
                os = os2;
                if (os != null) {
                    os.close();
                }
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            if (os != null) {
                os.close();
            }
            Log.e("Exeption", "Exception while activity", th);
        }
    }

    public static void writeToStream(String content, OutputStream os) throws IOException {
        Throwable th;
        BufferedWriter writer = null;
        try {
            BufferedWriter writer2 = new BufferedWriter(new OutputStreamWriter(os, Charsets.UTF_8));
            try {
                writer2.write(content);
                if (writer2 != null) {
                    writer2.close();
                }
            } catch (Throwable th2) {
                th = th2;
                writer = writer2;
                if (writer != null) {
                    writer.close();
                }
                throw th;
            }
        } catch (Throwable th3) {
            th = th3;
            if (writer != null) {
                writer.close();
            }
            Log.e("Exeption", "Exception while activity", th);
        }
    }

    public static String readFileAsString(File file) throws IOException {
        return readAsString(new FileInputStream(file));
    }

    public static String readAsString(InputStream is) throws IOException {
        Throwable th;
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader2 = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));
            while (true) {
                try {
                    String line = reader2.readLine();
                    if (line == null) {
                        break;
                    }
                    sb.append(line);
                } catch (Throwable th2) {
                    th = th2;
                    reader = reader2;
                }
            }
            if (reader2 != null) {
                reader2.close();
            }

        } catch (Throwable th3) {
            th = th3;
            if (reader != null) {
                reader.close();
            }
            Log.e("Exeption", "Exception while activity", th);
        }
        return sb.toString();
    }
}
