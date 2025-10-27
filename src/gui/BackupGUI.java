package gui;

import crypto.BackupUtils;
import crypto.CryptoUtils;

import javax.crypto.SecretKey;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class BackupGUI {
    private JFrame frame;
    private JPasswordField passwordField;
    private JProgressBar progressBar;
    private JComboBox<String> themeSelector;
    private JLabel statsLabel;
    private String currentUserRole = "viewer"; // default viewer role

    public BackupGUI() {
        setupLoginScreen();
    }

    private void setupLoginScreen() {
        JFrame loginFrame = new JFrame("Login");
        loginFrame.setSize(300, 200);
        loginFrame.setLayout(null);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setBounds(30, 30, 80, 25);
        loginFrame.add(userLabel);

        JTextField userText = new JTextField();
        userText.setBounds(120, 30, 130, 25);
        loginFrame.add(userText);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(30, 70, 80, 25);
        loginFrame.add(passwordLabel);

        JPasswordField passwordText = new JPasswordField();
        passwordText.setBounds(120, 70, 130, 25);
        loginFrame.add(passwordText);

        JButton loginButton = new JButton("Login");
        loginButton.setBounds(120, 110, 100, 30);
        loginFrame.add(loginButton);

        loginButton.addActionListener(e -> {
            String username = userText.getText();
            String password = new String(passwordText.getPassword());
            if ("admin".equals(username) && "adminpass".equals(password)) {
                currentUserRole = "admin";
                loginFrame.dispose();
                createMainGUI();
            } else if ("user".equals(username) && "userpass".equals(password)) {
                currentUserRole = "viewer";
                loginFrame.dispose();
                createMainGUI();
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Invalid credentials");
            }
        });

        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);
    }

    private void createMainGUI() {
        frame = new JFrame("Secure Backup & Restore Tool - Role: " + currentUserRole.toUpperCase());
        frame.setSize(570, 420);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);

        JLabel lbl = new JLabel("Password:");
        lbl.setBounds(20, 20, 80, 25);
        frame.add(lbl);

        passwordField = new JPasswordField();
        passwordField.setBounds(110, 20, 180, 25);
        frame.add(passwordField);

        JButton backupBtn = new JButton("Backup");
        backupBtn.setBounds(20, 60, 130, 30);
        frame.add(backupBtn);

        JButton restoreBtn = new JButton("Restore");
        restoreBtn.setBounds(180, 60, 130, 30);
        frame.add(restoreBtn);

        JButton previewBtn = new JButton("Preview Restore");
        previewBtn.setBounds(340, 60, 170, 30);
        previewBtn.setToolTipText("Preview files inside backup before restore");
        frame.add(previewBtn);

        JButton exportLogBtn = new JButton("Export Logs (CSV)");
        exportLogBtn.setBounds(20, 110, 150, 30);
        exportLogBtn.setToolTipText("Export backup/restore logs as CSV");
        frame.add(exportLogBtn);

        JButton infoBtn = new JButton("Cybersecurity Info");
        infoBtn.setBounds(190, 110, 170, 30);
        frame.add(infoBtn);

        JLabel themeLabel = new JLabel("Theme:");
        themeLabel.setBounds(370, 110, 50, 30);
        frame.add(themeLabel);

        themeSelector = new JComboBox<>(new String[] { "Light", "Dark" });
        themeSelector.setBounds(420, 110, 100, 30);
        frame.add(themeSelector);

        progressBar = new JProgressBar();
        progressBar.setBounds(20, 160, 490, 25);
        progressBar.setStringPainted(true);
        progressBar.setIndeterminate(false);
        frame.add(progressBar);

        statsLabel = new JLabel("");
        statsLabel.setBounds(20, 200, 490, 150);
        statsLabel.setVerticalAlignment(SwingConstants.TOP);
        frame.add(statsLabel);

        if (!"admin".equals(currentUserRole)) {
            exportLogBtn.setEnabled(false);
        }

        backupBtn.addActionListener(e -> backupAction());
        restoreBtn.addActionListener(e -> restoreAction());
        previewBtn.addActionListener(e -> previewRestoreAction());
        exportLogBtn.addActionListener(e -> exportLogsAsCSV());
        infoBtn.addActionListener(e -> displaySecurityAndLawInfo());

        themeSelector.addActionListener(e -> {
            String selected = (String) themeSelector.getSelectedItem();
            if ("Dark".equals(selected)) {
                setDarkTheme();
            } else {
                setLightTheme();
            }
        });

        updateStats();

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        setLightTheme();
    }

    private void displaySecurityAndLawInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== Mobile and Wireless Device Security Issues ===\n\n");
        info.append(
                "1. Mobile devices face threats like malware, phishing attacks, unsecured Wi-Fi networks, and SIM swapping.\n");
        info.append("2. Wireless data transmission can be intercepted without proper encryption.\n");
        info.append("3. Encrypt backups and use secure communication over wireless.\n\n");

        info.append("=== Cyber Laws Overview ===\n\n");
        info.append("1. Data Protection laws (e.g., GDPR) govern handling of personal data.\n");
        info.append("2. Cybercrime laws penalize unauthorized access, data tampering, identity theft.\n");
        info.append("3. Maintaining logs is critical for legal compliance.\n");
        info.append("4. Users must comply with laws during backup/restore.\n");

        JOptionPane.showMessageDialog(frame, info.toString(), "Cybersecurity & Cyber Law Info",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void backupAction() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            new Thread(() -> {
                try {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setIndeterminate(true);
                        progressBar.setString("Backing up...");
                    });

                    String password = new String(passwordField.getPassword());
                    byte[] salt = CryptoUtils.generateSalt();
                    Files.createDirectories(java.nio.file.Paths.get("metadata"));
                    Files.write(java.nio.file.Paths.get("metadata/salt.bin"), salt);

                    SecretKey key = CryptoUtils.getKeyFromPassword(password, salt);

                    File[] files = chooser.getSelectedFiles();
                    String[] filePaths = new String[files.length];
                    for (int i = 0; i < files.length; i++) {
                        filePaths[i] = files[i].getAbsolutePath();
                    }

                    BackupUtils.performBackup(filePaths, key);

                    SwingUtilities.invokeLater(() -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setString("");
                        showNotification("Backup completed successfully!");
                        updateStats();
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setString("");
                        showNotification("Backup failed: " + ex.getMessage());
                        JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
                    });
                    ex.printStackTrace();
                }
            }).start();
        }
    }

    private void restoreAction() {
        JFileChooser chooser = new JFileChooser("backups/");
        chooser.setMultiSelectionEnabled(false);
        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            new Thread(() -> {
                try {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setIndeterminate(true);
                        progressBar.setString("Restoring...");
                    });

                    String password = new String(passwordField.getPassword());
                    byte[] salt = Files.readAllBytes(java.nio.file.Paths.get("metadata/salt.bin"));
                    SecretKey key = CryptoUtils.getKeyFromPassword(password, salt);

                    File backupFile = chooser.getSelectedFile();

                    JFileChooser dirChooser = new JFileChooser();
                    dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                    dirChooser.setDialogTitle("Select Restore Directory");
                    int dirResult = dirChooser.showOpenDialog(frame);
                    if (dirResult == JFileChooser.APPROVE_OPTION) {
                        File restoreDir = dirChooser.getSelectedFile();

                        if (restoreDir.exists() && restoreDir.isDirectory() && restoreDir.list().length > 0) {
                            int overwrite = JOptionPane.showConfirmDialog(frame,
                                    "Restore directory is not empty. Overwrite existing files?",
                                    "Confirm Overwrite", JOptionPane.YES_NO_OPTION);
                            if (overwrite != JOptionPane.YES_OPTION) {
                                SwingUtilities.invokeLater(() -> {
                                    progressBar.setIndeterminate(false);
                                    progressBar.setString("");
                                    JOptionPane.showMessageDialog(frame, "Restore cancelled.");
                                });
                                return;
                            }
                        }
                        BackupUtils.performRestore(backupFile.getAbsolutePath(), key, restoreDir);

                        SwingUtilities.invokeLater(() -> {
                            progressBar.setIndeterminate(false);
                            progressBar.setString("");
                            showNotification("Restore completed successfully!");
                            JOptionPane.showMessageDialog(frame, "Restore completed successfully!");
                            updateStats();
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setIndeterminate(false);
                            progressBar.setString("");
                            JOptionPane.showMessageDialog(frame, "Restore directory selection cancelled.");
                        });
                    }
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setIndeterminate(false);
                        progressBar.setString("");
                        showNotification("Restore failed: " + ex.getMessage());
                        JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
                    });
                    ex.printStackTrace();
                }
            }).start();
        }
    }

    private void previewRestoreAction() {
        JFileChooser chooser = new JFileChooser("backups/");
        chooser.setMultiSelectionEnabled(false);
        int result = chooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File backupFile = chooser.getSelectedFile();
            try {
                List<String> entries = new ArrayList<>();
                try (ZipInputStream zis = new ZipInputStream(new FileInputStream(backupFile))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        entries.add(entry.getName());
                    }
                }
                showPreviewDialog(entries);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Error reading backup: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void showPreviewDialog(List<String> entries) {
        JDialog dialog = new JDialog(frame, "Backup Contents Preview", true);
        dialog.setSize(300, 400);
        dialog.setLayout(new BorderLayout());

        DefaultListModel<String> model = new DefaultListModel<>();
        entries.forEach(model::addElement);
        JList<String> fileList = new JList<>(model);
        JScrollPane scrollPane = new JScrollPane(fileList);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        dialog.add(closeBtn, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void exportLogsAsCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Logs CSV");
        int result = chooser.showSaveDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (BufferedReader br = Files.newBufferedReader(java.nio.file.Paths.get("logs/logs.txt"));
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                bw.write("Timestamp,Message\n"); // CSV header
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split(" - ", 2);
                    String timestamp = parts[0];
                    String message = parts.length > 1 ? parts[1] : "";
                    bw.write("\"" + timestamp + "\",\"" + message.replace("\"", "\"\"") + "\"\n");
                }
                JOptionPane.showMessageDialog(frame, "Logs exported successfully!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(frame, "Failed to export logs: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void setDarkTheme() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            SwingUtilities.updateComponentTreeUI(frame);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setLightTheme() {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            SwingUtilities.updateComponentTreeUI(frame);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showNotification(String message) {
        if (SystemTray.isSupported()) {
            try {
                SystemTray tray = SystemTray.getSystemTray();
                Image image = Toolkit.getDefaultToolkit().createImage("icon.png");
                TrayIcon trayIcon = new TrayIcon(image, "Backup Tool");
                trayIcon.setImageAutoSize(true);
                trayIcon.setToolTip("Backup Tool Notification");
                tray.add(trayIcon);
                trayIcon.displayMessage("Backup Tool", message, TrayIcon.MessageType.INFO);
                Thread.sleep(3000);
                tray.remove(trayIcon);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateStats() {
        try {
            long count = Files.lines(java.nio.file.Paths.get("logs/logs.txt")).count();
            statsLabel.setText("<html>Total backup/restore operations: " + count + "</html>");
        } catch (Exception e) {
            statsLabel.setText("No logs available.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(BackupGUI::new);
    }
}
