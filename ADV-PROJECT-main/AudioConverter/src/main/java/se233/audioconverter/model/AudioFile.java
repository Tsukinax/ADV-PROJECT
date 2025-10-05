package se233.audioconverter.model;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AudioFile {
    private String name;
    private String filePath;
    private String format;
    private long fileSize;
    private ConversionStatus status;

    public enum ConversionStatus {
        PENDING("Pending"),
        PROCESSING("Processing..."),
        COMPLETED("Completed"),
        FAILED("Failed");

        private final String displayName;

        ConversionStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public AudioFile(String filePath) {
        this.filePath = filePath;
        Path path = Paths.get(filePath);
        this.name = path.getFileName().toString();
        this.format = getFileExtension(name);
        File file = new File(filePath);
        this.fileSize = file.length();
        this.status = ConversionStatus.PENDING;
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public ConversionStatus getStatus() {
        return status;
    }

    public void setStatus(ConversionStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return String.format("%s [%s] - %s", name, format.toUpperCase(), status.getDisplayName());
    }
}