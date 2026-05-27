package scratch;

import database.DatabaseConnection;
import java.sql.*;

public class CheckCustomer {
    public static void main(String[] args) {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            String sql = "SELECT maKhachHang, hoTen, sdt, cccd, anhCCCD FROM KhachHang WHERE sdt='0358059277' OR cccd='052205012025'";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    System.out.println("ID: " + rs.getString("maKhachHang"));
                    System.out.println("Name: " + rs.getString("hoTen"));
                    System.out.println("Phone: " + rs.getString("sdt"));
                    System.out.println("CCCD: " + rs.getString("cccd"));
                    System.out.println("anhCCCD: " + rs.getString("anhCCCD"));
                } else {
                    System.out.println("Customer not found!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
