package entities;

import java.sql.Date;

public class ResourceAssignment {

    private int assignmentId;
    private int resourceId;

    // 🔹 NEW: FK to utilisateur.id
    private int userId;

    // For display (JOIN with resources)
    private String resourceName;
    private String resourceType;
    private String resourceImagePath;

    private String projectCode;

    private int quantity;
    private Date assignmentDate;
    private Date returnDate;

    private double totalCost;
    private String status;

    public ResourceAssignment() {}

    public int getAssignmentId() { return assignmentId; }
    public void setAssignmentId(int assignmentId) { this.assignmentId = assignmentId; }

    public int getResourceId() { return resourceId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }

    // ✅ NEW USER ID
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResourceImagePath() { return resourceImagePath; }
    public void setResourceImagePath(String resourceImagePath) { this.resourceImagePath = resourceImagePath; }

    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public Date getAssignmentDate() { return assignmentDate; }
    public void setAssignmentDate(Date assignmentDate) { this.assignmentDate = assignmentDate; }

    public Date getReturnDate() { return returnDate; }
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}