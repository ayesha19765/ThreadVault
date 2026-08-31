package dto.request;

/**
 * Request payload for restoring backed-up files.
 */
public class RestoreRequest {

    private String targetDirectory;

    public RestoreRequest() {
    }

    public RestoreRequest(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }

    public String getTargetDirectory() {
        return targetDirectory;
    }

    public void setTargetDirectory(String targetDirectory) {
        this.targetDirectory = targetDirectory;
    }
}
