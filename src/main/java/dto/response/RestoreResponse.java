package dto.response;

import java.time.LocalDateTime;

/**
 * Response DTO representing the result of a restore operation.
 */
public class RestoreResponse {

    private String status;
    private String message;
    private int restoredFilesCount;
    private LocalDateTime timestamp;

    public RestoreResponse() {
    }

    public RestoreResponse(String status, String message, int restoredFilesCount) {
        this.status = status;
        this.message = message;
        this.restoredFilesCount = restoredFilesCount;
        this.timestamp = LocalDateTime.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getRestoredFilesCount() {
        return restoredFilesCount;
    }

    public void setRestoredFilesCount(int restoredFilesCount) {
        this.restoredFilesCount = restoredFilesCount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
