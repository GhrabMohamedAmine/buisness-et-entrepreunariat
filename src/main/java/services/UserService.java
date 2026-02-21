package services;

import utils.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserService {

    private final Connection cnx = database.getInstance().getConnection();

    public String getPhoneByClientCode(String clientCode) throws SQLException {
        String sql = "SELECT telephone FROM utilisateurs WHERE client_code = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, clientCode);

        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getString("telephone");
        return null;
    }
}