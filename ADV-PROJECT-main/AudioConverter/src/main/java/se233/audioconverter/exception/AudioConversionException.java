package se233.audioconverter.exception;

public class AudioConversionException extends Exception {
    private final String fileName;
    private final ErrorType errorType;

    public enum ErrorType {
        UNSUPPORTED_FORMAT("Unsupported audio format"),
        FILE_NOT_FOUND("File not found"),
        FFMPEG_ERROR("FFmpeg conversion error"),
        INVALID_SETTINGS("Invalid conversion settings"),
        IO_ERROR("Input/Output error");

        private final String message;

        ErrorType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public AudioConversionException(String fileName, ErrorType errorType, String details) {
        super(String.format("%s: %s - %s", errorType.getMessage(), fileName, details));
        this.fileName = fileName;
        this.errorType = errorType;
    }

    public AudioConversionException(String fileName, ErrorType errorType, Throwable cause) {
        super(String.format("%s: %s", errorType.getMessage(), fileName), cause);
        this.fileName = fileName;
        this.errorType = errorType;
    }

    public String getFileName() {
        return fileName;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getUserFriendlyMessage() {
        return String.format("Failed to convert '%s': %s", fileName, errorType.getMessage());
    }
}