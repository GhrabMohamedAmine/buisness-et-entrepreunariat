package services;

import entities.Resource;
import utils.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResourceService implements Iservice<Resource> {

    private Connection cnx;

    public ResourceService() {
        cnx = database.getInstance().getConnection();
    }

    @Override
    public void add(Resource r) throws SQLException {
        String sql = "INSERT INTO resources " +
                "(resource_code, resource_name, resource_type, unit_cost, total_quantity, available_quantity, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, r.getCode());
        ps.setString(2, r.getName());
        ps.setString(3, r.getType());          // PHYSICAL / SOFTWARE
        ps.setDouble(4, r.getUnitcost());
        ps.setInt(5, r.getQuantity());
        ps.setDouble(6, r.getAvquant());

        // default status (you can change later)
        ps.setString(7, "AVAILABLE");

        ps.executeUpdate();
    }

    @Override
    public void update(Resource r) throws SQLException {

        String sql = "UPDATE resources SET " +
                "resource_code=?, resource_name=?, resource_type=?, unit_cost=?, " +
                "total_quantity=?, available_quantity=? " +
                "WHERE resource_id=?";

        PreparedStatement ps = cnx.prepareStatement(sql);

        ps.setInt(1, r.getCode());
        ps.setString(2, r.getName());
        ps.setString(3, r.getType());
        ps.setDouble(4, r.getUnitcost());
        ps.setInt(5, r.getQuantity());
        ps.setDouble(6, r.getAvquant());
        ps.setInt(7, r.getId()); // id field holds resource_id value

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
            r.setCode(Integer.parseInt(rs.getString("resource_code"))); // if your code is varchar in DB
            r.setName(rs.getString("resource_name"));
            r.setType(rs.getString("resource_type"));
            r.setUnitcost(rs.getDouble("unit_cost"));
            r.setAvquant(rs.getInt("available_quantity"));
            r.setQuantity(rs.getInt("total_quantity"));

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
        ps.setString(1, type); // PHYSICAL or SOFTWARE
        ResultSet rs = ps.executeQuery();
        rs.next();
        return rs.getInt(1);
    }

    public int countMaintenance() throws SQLException {
        // Works if status contains MAINTENANCE / IN_MAINTENANCE
        String sql = "SELECT COUNT(*) FROM resources " +
                "WHERE UPPER(status) = 'MAINTENANCE' OR UPPER(status) = 'IN_MAINTENANCE'";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        rs.next();
        return rs.getInt(1);
    }
}
