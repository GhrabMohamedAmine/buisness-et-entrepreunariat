package entities;

public class ProjectFile {
    private final int id;
    private final int projectId;
    private final int uploadedBy;
    private final String originalName;
    private final String publicId;
    private final String resourceType;
    private final String format;
    private final long bytes;
    private final String secureUrl;
    private final String createdAt;

    public ProjectFile(int id, int projectId, int uploadedBy, String originalName, String publicId,
                       String resourceType, String format, long bytes, String secureUrl, String createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.uploadedBy = uploadedBy;
        this.originalName = originalName;
        this.publicId = publicId;
        this.resourceType = resourceType;
        this.format = format;
        this.bytes = bytes;
        this.secureUrl = secureUrl;
        this.createdAt = createdAt;
    }

    public ProjectFile(int projectId, int uploadedBy, String originalName, String publicId,
                       String resourceType, String format, long bytes, String secureUrl, String createdAt) {
        this(0, projectId, uploadedBy, originalName, publicId, resourceType, format, bytes, secureUrl, createdAt);
    }

    public int getId() { return id; }
    public int getProjectId() { return projectId; }
    public int getUploadedBy() { return uploadedBy; }
    public String getOriginalName() { return originalName; }
    public String getFileName() { return originalName; }
    public String getPublicId() { return publicId; }
    public String getResourceType() { return resourceType; }
    public String getFormat() { return format; }
    public long getBytes() { return bytes; }
    public String getSecureUrl() { return secureUrl; }
    public String getCreatedAt() { return createdAt; }
    public String getUploadedAt() { return createdAt; }
}
