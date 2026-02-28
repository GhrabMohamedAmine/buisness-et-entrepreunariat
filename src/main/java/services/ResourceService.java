package services;

import entities.Resource;
import utils.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResourceService implements Iservice<Resource> {

    private final Connection cnx;

    public ResourceService() {
        cnx = database.getInstance().getConnection();
    }

    @Override
    public void add(Resource r) throws SQLException {

        String sql = "INSERT INTO resources " +
                "(resource_code, resource_name, resource_type, unit_cost, total_quantity, available_quantity, status, image_path) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setInt(1, r.getCode());
        ps.setString(2, r.getName());
        ps.setString(3, r.getType());          // PHYSICAL / SOFTWARE
        ps.setDouble(4, r.getUnitcost());
        ps.setInt(5, r.getQuantity());
        ps.setDouble(6, r.getAvquant());

        // default status
        ps.setString(7, "AVAILABLE");

        // ✅ new image path (can be null)
        ps.setString(8, r.getImagePath());

        ps.executeUpdate();
    }

    @Override
    public void update(Resource r) throws SQLException {

        String sql = "UPDATE resources SET " +
                "resource_code=?, resource_name=?, resource_type=?, unit_cost=?, " +
                "total_quantity=?, available_quantity=?, image_path=? " +
                "WHERE resource_id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setInt(1, r.getCode());
        ps.setString(2, r.getName());
        ps.setString(3, r.getType());
        ps.setDouble(4, r.getUnitcost());
        ps.setInt(5, r.getQuantity());
        ps.setDouble(6, r.getAvquant());

        // ✅ update image path (can be null)
        ps.setString(7, r.getImagePath());

        ps.setInt(8, r.getId());

        ps.executeUpdate();
    }

    @Override
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM resources WHERE resource_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Resource deleted successfully!");
    }

    @Override
    public List<Resource> getAll() throws SQLException {

        List<Resource> list = new ArrayList<>();

        String sql = "SELECT * FROM resources";

        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Resource r = new Resource();
            r.setId(rs.getInt("resource_id"));

            // if resource_code is INT in DB, this is fine:
            // r.setCode(rs.getInt("resource_code"));
            // You wrote it as String->int, keeping your original behavior:
            r.setCode(Integer.parseInt(rs.getString("resource_code")));

            r.setName(rs.getString("resource_name"));
            r.setType(rs.getString("resource_type"));
            r.setUnitcost(rs.getDouble("unit_cost"));
            r.setAvquant(rs.getInt("available_quantity"));
            r.setQuantity(rs.getInt("total_quantity"));

            // ✅ new column
            try {
                r.setImagePath(rs.getString("image_path"));
            } catch (SQLException ignore) {
                // if column not yet created, avoid crash
                r.setImagePath(null);
            }

            list.add(r);
        }

        return list;
    }

    // =================== STATS METHODS ===================

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM resources";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        rs.next();
        return rs.getInt(1);
    }

    public int countByType(String type) throws SQLException {
        String sql = "SELECT COUNT(*) FROM resources WHERE resource_type = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, type);
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1);
    }

    public int countMaintenance() throws SQLException {
        String sql = "SELECT COUNT(*) FROM resources " +
                "WHERE UPPER(status) = 'MAINTENANCE' OR UPPER(status) = 'IN_MAINTENANCE'";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        rs.next();
        return rs.getInt(1);
    }
}