package dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload to initiate a backup operation.
 */
public class BackupRequest {

    @NotBlank(message = "Source directory path cannot be blank")
    private String source;

    private String destination;

    private Integer workers;

    public BackupRequest() {
    }

    public BackupRequest(String source) {
        this.source = source;
    }

    public BackupRequest(String source, String destination, Integer workers) {
        this.source = source;
        this.destination = destination;
        this.workers = workers;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Integer getWorkers() {
        return workers;
    }

    public void setWorkers(Integer workers) {
        this.workers = workers;
    }
}
