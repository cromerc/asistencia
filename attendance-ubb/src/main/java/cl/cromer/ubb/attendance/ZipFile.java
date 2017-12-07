package cl.cromer.ubb.attendance;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import cl.cromer.ubb.attendance.FileSystem;

public class ZipFile {

    public ZipFile() {}

    protected boolean unzip(InputStream inputStream, File androidDirectory) throws IOException {
        if (!FileSystem.isWritable()) {
            return false;
        }

        if (!androidDirectory.exists()) {
            if (!androidDirectory.mkdir()) {
                return false;
            }
        }

        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        ZipEntry zipEntry;
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
            String filePath = androidDirectory.toString() + File.separator + zipEntry.getName();
            if (!zipEntry.isDirectory()) {
                extractFile(zipInputStream, filePath);
            }
            else {
                File directory = new File(filePath);
                if (!directory.mkdir()) {
                    return false;
                }
            }
            zipInputStream.closeEntry();
        }
        zipInputStream.close();

        return true;
    }

    private void extractFile(ZipInputStream zipInputStream, String filePath) throws IOException {
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytes = new byte[4096];
        int read;
        while ((read = zipInputStream.read(bytes)) != -1) {
            bufferedOutputStream.write(bytes, 0, read);
        }
        bufferedOutputStream.close();
    }
}
