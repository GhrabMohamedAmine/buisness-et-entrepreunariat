package services;



import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import entities.Resultat;
import utils.database;

public class ResultatService {

    Connection cnx = database.getInstance().getConnection();

    public void save(Resultat r) {

        String sql = """
            INSERT INTO resultat(formation_id, score, total)
            VALUES (?, ?, ?)
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setInt(1, r.getFormationId());
            ps.setInt(2, r.getScore());
            ps.setInt(3, r.getTotal());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Resultat> getAll(){

        List<Resultat> list = new ArrayList<>();

        String sql = "SELECT * FROM resultat ORDER BY date_passage DESC";

        try(var st = cnx.createStatement();
            ResultSet rs = st.executeQuery(sql)){

            while(rs.next()){
                list.add(new Resultat(
                        rs.getInt("id"),
                        rs.getInt("formation_id"),
                        rs.getInt("score"),
                        rs.getInt("total"),
                        rs.getTimestamp("date_passage")
                ));
            }

        }catch(Exception e){
            throw new RuntimeException(e);
        }

        return list;
    }
}
