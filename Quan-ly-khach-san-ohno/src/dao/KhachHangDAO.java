package dao;

import database.DatabaseConnection;
import entity.KhachHang;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KhachHangDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<KhachHang> getAll() {
        List<KhachHang> list = new ArrayList<>();
        String sql = "SELECT * FROM KhachHang ORDER BY hoTen";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<KhachHang> timKiem(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return getAll();
        
        List<KhachHang> list = new ArrayList<>();
        String sql = "SELECT * FROM KhachHang WHERE hoTen LIKE ? OR sdt LIKE ? OR cccd LIKE ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            String kw = "%" + keyword.trim() + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public KhachHang getById(String maKhachHang) {
        String sql = "SELECT * FROM KhachHang WHERE maKhachHang=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maKhachHang);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public KhachHang getByPhone(String sdt) {
        String sql = "SELECT * FROM KhachHang WHERE sdt=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, sdt);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public KhachHang getByCCCD(String cccd) {
        String sql = "SELECT * FROM KhachHang WHERE cccd=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, cccd);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public boolean insert(KhachHang kh) {
        String sql = "INSERT INTO KhachHang(maKhachHang, hoTen, sdt, cccd) VALUES(?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, kh.getMaKhachHang());
            ps.setString(2, kh.getHoTen());
            ps.setString(3, kh.getSdt());
            ps.setString(4, kh.getCccd());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean update(KhachHang kh) {
        String sql = "UPDATE KhachHang SET hoTen=?, sdt=?, cccd=? WHERE maKhachHang=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, kh.getHoTen());
            ps.setString(2, kh.getSdt());
            ps.setString(3, kh.getCccd());
            ps.setString(4, kh.getMaKhachHang());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public String generateMaKH() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT MAX(CAST(SUBSTRING(maKhachHang,3,10) AS INT)) FROM KhachHang WHERE maKhachHang LIKE 'KH%'")) {
            if (rs.next()) return String.format("KH%03d", rs.getInt(1) + 1);
        } catch (SQLException e) { e.printStackTrace(); }
        return "KH001";
    }

    public boolean delete(String maKhachHang) {
        String sql = "DELETE FROM KhachHang WHERE maKhachHang=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maKhachHang);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    private KhachHang mapRow(ResultSet rs) throws SQLException {
        KhachHang kh = new KhachHang();
        kh.setMaKhachHang(rs.getString("maKhachHang"));
        kh.setHoTen(rs.getString("hoTen"));
        kh.setSdt(rs.getString("sdt"));
        kh.setCccd(rs.getString("cccd"));
        return kh;
    }
}
