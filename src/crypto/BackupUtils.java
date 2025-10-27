package crypto;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

public class BackupUtils {

    private static final String BACKUP_DIR = "backups/";
    private static final String META_DIR = "metadata/";
    private static final String LOG_FILE = "logs/logs.txt";

    // Backup process
    public static void performBackup(String[] files, SecretKey key) throws Exception {
        // Create directories if missing
        Files.createDirectories(Paths.get(BACKUP_DIR));
        Files.createDirectories(Paths.get(META_DIR));
        Files.createDirectories(Paths.get("logs"));

        String zipFile = BACKUP_DIR + "backup.zip";
        String encFile = BACKUP_DIR + "backup.enc";

        // Create Zip archive
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            for (String file : files) {
                File f = new File(file);
                try (FileInputStream fis = new FileInputStream(f)) {
                    zos.putNextEntry(new ZipEntry(f.getName()));
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                    zos.closeEntry();
                }
            }
        }

        // Encrypt the zip archive
        CryptoUtils.encryptFile(zipFile, encFile, key);
        Files.delete(Paths.get(zipFile)); // Remove plain zip

        // Save hash metadata
        String hash = CryptoUtils.calculateSHA256(encFile);
        Files.write(Paths.get(META_DIR + "metadata.txt"), hash.getBytes());

        writeLog("Backup created: " + encFile + " | Hash: " + hash);
    }

    // Restore process
    public static void performRestore(String backupFile, SecretKey key, File restoreDir) throws Exception {
        String decryptedZip = BACKUP_DIR + "restored.zip";

        // Decrypt encrypted backup file
        CryptoUtils.decryptFile(backupFile, decryptedZip, key);

        // Verify hash
        String storedHash = new String(Files.readAllBytes(Paths.get(META_DIR + "metadata.txt")));
        String currentHash = CryptoUtils.calculateSHA256(backupFile);

        if (!storedHash.equals(currentHash)) {
            throw new SecurityException("Hash mismatch! File may be tampered.");
        }

        // Extract zip to user chosen directory
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(decryptedZip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(restoreDir, entry.getName());
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
            }
        }
        Files.delete(Paths.get(decryptedZip));

        writeLog("Restore completed: " + backupFile);
    }

    // Logging
    public static void writeLog(String message) {
        try {
            Files.createDirectories(Paths.get("logs"));
            try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
                fw.write(java.time.LocalDateTime.now() + " - " + message + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
