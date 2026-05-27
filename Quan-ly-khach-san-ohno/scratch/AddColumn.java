package scratch;

import database.DatabaseConnection;
import java.sql.*;

public class AddColumn {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String checkSql = "SELECT COL_LENGTH('KhachHang', 'anhCCCD') AS ColumnLength";
            boolean columnExists = false;
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (rs.getObject("ColumnLength") != null) {
                        columnExists = true;
                    }
                }
            }

            if (!columnExists) {
                System.out.println("Column 'anhCCCD' does not exist in 'KhachHang'. Adding it now...");
                String alterSql = "ALTER TABLE KhachHang ADD anhCCCD NVARCHAR(MAX) NULL";
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(alterSql);
                    System.out.println("Column 'anhCCCD' has been added successfully to 'KhachHang' table!");
                }
            } else {
                System.out.println("Column 'anhCCCD' already exists in 'KhachHang' table.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
