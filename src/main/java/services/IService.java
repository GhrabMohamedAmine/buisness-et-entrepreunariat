package services;
import java.sql.SQLException;

public interface IService<T> {
    long ajouter(T t) throws SQLException;
    void modifier(T t) throws SQLException;
    void supprimer(T t) throws SQLException;
}
