package services.ai;

import entities.Resource;

import java.util.List;

public class ResourceContextBuilder {

    public static String buildContext(List<Resource> resources) {
        StringBuilder sb = new StringBuilder();
        sb.append("Here is the CURRENT resource catalog from the database.\n")
                .append("Use it as the single source of truth.\n")
                .append("Never recommend unavailable items (avquant <= 0).\n\n");

        sb.append("RESOURCES:\n");
        for (Resource r : resources) {
            sb.append("- id=").append(r.getId())
                    .append(", code=").append(r.getCode())
                    .append(", name=").append(r.getName())
                    .append(", type=").append(r.getType())
                    .append(", unitCost=").append(r.getUnitcost())
                    .append(", totalQty=").append(r.getQuantity())
                    .append(", available=").append(r.getAvquant())
                    .append("\n");
        }

        sb.append("\nWhen you answer:\n")
                .append("1) Ask 1-2 clarification questions if needed (project type, quantity, duration).\n")
                .append("2) Give top 3 recommendations with suggested quantity.\n")
                .append("3) Provide rough cost estimation = qty * unitCost.\n")
                .append("4) If user chooses a resource, respond with: SELECT_RESOURCE: <id> QTY: <number>\n");

        return sb.toString();
    }
}