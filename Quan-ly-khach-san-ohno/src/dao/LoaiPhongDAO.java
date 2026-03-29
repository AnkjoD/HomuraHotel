package dao;

import database.DatabaseConnection;
import entity.LoaiPhong;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LoaiPhongDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<LoaiPhong> getAll() {
        List<LoaiPhong> list = new ArrayList<>();
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM LoaiPhong ORDER BY tenLoaiPhong")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<LoaiPhong> getActive() {
        List<LoaiPhong> list = new ArrayList<>();
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM LoaiPhong ORDER BY tenLoaiPhong")) {
            while (rs.next()) {
                LoaiPhong lp = mapRow(rs);
                if (lp.isDangKinhDoanh()) list.add(lp);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public LoaiPhong getById(String maLoaiPhong) {
        try (PreparedStatement ps = getConn().prepareStatement("SELECT * FROM LoaiPhong WHERE maLoaiPhong=?")) {
            ps.setString(1, maLoaiPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public boolean insert(LoaiPhong lp) {
        String sql = "INSERT INTO LoaiPhong(maLoaiPhong, tenLoaiPhong, soNguoiToiDa, moTa, giaTheoNgay) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, lp.getMaLoaiPhong());
            ps.setString(2, lp.getTenLoaiPhong());
            ps.setInt(3, lp.getSucChua());
            ps.setString(4, lp.getMoTa());
            ps.setDouble(5, lp.getGiaTheoNgay());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean update(LoaiPhong lp) {
        String sql = "UPDATE LoaiPhong SET tenLoaiPhong=?, soNguoiToiDa=?, moTa=?, giaTheoNgay=? WHERE maLoaiPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, lp.getTenLoaiPhong());
            ps.setInt(2, lp.getSucChua());
            ps.setString(3, lp.getMoTa());
            ps.setDouble(4, lp.getGiaTheoNgay());
            ps.setString(5, lp.getMaLoaiPhong());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean delete(String maLoaiPhong) {
        try (PreparedStatement ps = getConn().prepareStatement("DELETE FROM LoaiPhong WHERE maLoaiPhong=?")) {
            ps.setString(1, maLoaiPhong);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /** Check if any phong references this loaiPhong */
    public int countPhongByLoai(String maLoaiPhong) {
        String sql = "SELECT COUNT(*) FROM Phong WHERE maLoaiPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maLoaiPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    /** Generate next maLoaiPhong like LP006, LP007... */
    public String generateMa() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT MAX(CAST(SUBSTRING(maLoaiPhong,3,10) AS INT)) FROM LoaiPhong WHERE maLoaiPhong LIKE 'LP%'")) {
            if (rs.next()) return String.format("LP%03d", rs.getInt(1) + 1);
        } catch (SQLException e) { e.printStackTrace(); }
        return "LP001";
    }

    private LoaiPhong mapRow(ResultSet rs) throws SQLException {
        LoaiPhong lp = new LoaiPhong();
        lp.setMaLoaiPhong(rs.getString("maLoaiPhong"));
        lp.setTenLoaiPhong(rs.getString("tenLoaiPhong"));
        lp.setSucChua(rs.getInt("soNguoiToiDa"));
        lp.setMoTa(rs.getString("moTa"));
        try { lp.setGiaTheoNgay(rs.getDouble("giaTheoNgay")); } catch (Exception e) {}
        return lp;
    }
}
