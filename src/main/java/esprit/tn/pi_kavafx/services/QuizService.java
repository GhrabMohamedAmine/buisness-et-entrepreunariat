package esprit.tn.pi_kavafx.services;

import esprit.tn.pi_kavafx.entities.Quiz;
import esprit.tn.pi_kavafx.utils.Mydatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuizService {

    Connection cnx = Mydatabase.getInstance().getConnection();
    public void update(Quiz q){

        String sql="""
        UPDATE quiz SET question=?, r1=?, r2=?, r3=?, correct=?
        WHERE id=?
    """;

        try(PreparedStatement ps=cnx.prepareStatement(sql)){

            ps.setString(1,q.getQuestion());
            ps.setString(2,q.getR1());
            ps.setString(3,q.getR2());
            ps.setString(4,q.getR3());
            ps.setInt(5,q.getCorrect());
            ps.setInt(6,q.getId());

            ps.executeUpdate();

        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public void add(Quiz q) {

        String sql = """
        INSERT INTO quiz(question, r1, r2, r3, image, formation_id, correct)
        VALUES (?, ?, ?, ?, ?, ?, ?)
    """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {

            ps.setString(1, q.getQuestion());
            ps.setString(2, q.getR1());
            ps.setString(3, q.getR2());
            ps.setString(4, q.getR3());
            ps.setString(5, q.getImage());
            ps.setInt(6, q.getFormationId());
            ps.setInt(7, q.getCorrect());

            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Quiz> getByFormationId(int formationId) {
        List<Quiz> list = new ArrayList<>();
        String sql = "SELECT * FROM quiz WHERE formation_id=? ORDER BY id DESC";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, formationId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {

                Quiz q = new Quiz(
                        rs.getInt("id"),
                        rs.getString("question"),
                        rs.getString("r1"),
                        rs.getString("r2"),
                        rs.getString("r3"),
                        rs.getString("image"),
                        rs.getInt("formation_id"),
                        rs.getInt("correct")
                );

                list.add(q);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }
    public void delete(int id){
        try{
            PreparedStatement ps = cnx.prepareStatement("DELETE FROM quiz WHERE id=?");
            ps.setInt(1,id);
            ps.executeUpdate();
        }catch(Exception e){ throw new RuntimeException(e);}
    }

}
