package services;

import entities.Participer;
import utils.database;

import java.sql.*;

public class ParticiperService {

    private final Connection cnx = database.getInstance().getConnection();

    // inscription automatique
    public void inscrire(int userId, int formationId){

        try{

            // vérifier s'il existe déjà
            String check = "SELECT id FROM participer WHERE user_id=? AND formation_id=?";
            PreparedStatement ps = cnx.prepareStatement(check);
            ps.setInt(1,userId);
            ps.setInt(2,formationId);
            ResultSet rs = ps.executeQuery();

            if(rs.next()) return; // déjà inscrit

            String sql = "INSERT INTO participer(user_id,formation_id) VALUES(?,?)";
            ps = cnx.prepareStatement(sql);
            ps.setInt(1,userId);
            ps.setInt(2,formationId);
            ps.executeUpdate();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // progression (quand il regarde une vidéo)
    public void updateProgress(int userId,int formationId,int progress){

        try{
            String sql="UPDATE participer SET progression=? WHERE user_id=? AND formation_id=?";
            PreparedStatement ps=cnx.prepareStatement(sql);
            ps.setInt(1,progress);
            ps.setInt(2,userId);
            ps.setInt(3,formationId);
            ps.executeUpdate();
        }catch(Exception e){e.printStackTrace();}
    }

    // résultat final
    public void finishFormation(int userId,int formationId,int score,int total){

        String statut = (score >= total*0.6) ? "REUSSI" : "ECHOUE";
        Participer p = getParticipation(userId,formationId);

        if(p != null && "REUSSI".equals(p.getStatut())){
            throw new RuntimeException("Formation déjà validée");
        }
        try{
            String sql="""
                    UPDATE participer
                    SET progression=100, statut=?
                    WHERE user_id=? AND formation_id=?
                    """;
            PreparedStatement ps=cnx.prepareStatement(sql);
            ps.setString(1,statut);
            ps.setInt(2,userId);
            ps.setInt(3,formationId);
            ps.executeUpdate();

        }catch(Exception e){e.printStackTrace();}
    }



    public Participer getParticipation(int userId, int formationId){

        try{
            String sql="SELECT * FROM participer WHERE user_id=? AND formation_id=?";
            PreparedStatement ps=cnx.prepareStatement(sql);
            ps.setInt(1,userId);
            ps.setInt(2,formationId);

            ResultSet rs=ps.executeQuery();

            if(rs.next()){
                return new Participer(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getInt("formation_id"),
                        rs.getTimestamp("date_inscription"),
                        rs.getInt("progression"),
                        rs.getString("statut")
                );
            }

        }catch(Exception e){e.printStackTrace();}
        return null;
    }




}
