package esprit.tn.pi_kavafx.services;

import esprit.tn.pi_kavafx.entities.Formation;
import esprit.tn.pi_kavafx.utils.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FormationService {

    Connection cnx = Mydatabase.getInstance().getConnection();

    public void add(Formation f) {
        String sql = """
            INSERT INTO formation(titre, description, video1, video2, video3)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, f.getTitre());
            ps.setString(2, f.getDescription());
            ps.setString(3, f.getVideo1());
            ps.setString(4, f.getVideo2());
            ps.setString(5, f.getVideo3());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(Formation f) {
        String sql = """
            UPDATE formation
            SET titre=?, description=?, video1=?, video2=?, video3=?
            WHERE id=?
        """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, f.getTitre());
            ps.setString(2, f.getDescription());
            ps.setString(3, f.getVideo1());
            ps.setString(4, f.getVideo2());
            ps.setString(5, f.getVideo3());
            ps.setInt(6, f.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Formation getById(int id) {
        String sql = "SELECT * FROM formation WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Formation(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getString("video1"),
                        rs.getString("video2"),
                        rs.getString("video3")
                );
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Formation> getAll() {
        List<Formation> list = new ArrayList<>();
        String sql = "SELECT * FROM formation ORDER BY id DESC";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Formation(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getString("video1"),
                        rs.getString("video2"),
                        rs.getString("video3")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }
    public void delete(int id){
        String sql = "DELETE FROM formation WHERE id=?";
        try(PreparedStatement ps = cnx.prepareStatement(sql)){
            ps.setInt(1,id);
            ps.executeUpdate();
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

}
