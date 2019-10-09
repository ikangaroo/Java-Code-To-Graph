package owaspbench;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * @ Author     ：wxkong
 */
public class DataBaseOp {
    public static void main(String[] args) {
        Connection connection;
        PreparedStatement preparedStatement;
        CallableStatement callableStatement;
        JdbcTemplate jdbcTemplate = new JdbcTemplate();
    }
}
