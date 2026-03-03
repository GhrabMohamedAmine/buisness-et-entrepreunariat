package services.ai;

import entities.Resource;

import java.util.List;

public class ResourceContextBuilder {

    public static String buildContext(List<Resource> resources) {

        StringBuilder sb = new StringBuilder();

        sb.append("Catalog (internal only, DO NOT display to user):\n");
        for (Resource r : resources) {
            if (r.getAvquant() <= 0) continue;

            sb.append("- ")
                    .append(r.getName())
                    .append(" (available: ")
                    .append((int) r.getAvquant())
                    .append(", price: ")
                    .append(r.getUnitcost())
                    .append(")\n");
        }


        return sb.toString();
    }
}