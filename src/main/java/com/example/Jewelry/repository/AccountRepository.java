package com.example.Jewelry.repository;

import com.example.Jewelry.config.DBConnection;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class AccountRepository {

    private static final String LOGIN_SQL = "SELECT * FROM Account WHERE Username = ? AND PasswordHash = ? AND Status = 'ACTIVE'";

    public boolean login(String username, String password) {
        try (Connection connection = DBConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(LOGIN_SQL)) {

            statement.setString(1, username);
            statement.setString(2, password);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception exception) {
            // Return false so UI shows login error instead of throwing HTTP 500.
            return false;
        }
    }
}
