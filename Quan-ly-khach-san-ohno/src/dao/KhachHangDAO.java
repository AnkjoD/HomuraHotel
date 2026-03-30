package dao;

import database.DatabaseConnection;
import entity.KhachHang;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.List;

public class KhachHangDAO {

    private static final Logger LOGGER = Logger.getLogger(KhachHangDAO.class.getName());

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<KhachHang> getAll() {
        List<KhachHang> list = new ArrayList<>();
        String sql = "SELECT * FROM KhachHang ORDER BY hoTen";
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in KhachHangDAO", e); }
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
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in KhachHangDAO", e); }
        return list;
    }

    public KhachHang getById(String maKhachHang) {
        String sql = "SELECT * FROM KhachHang WHERE maKhachHang=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maKhachHang);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in KhachHangDAO", e); }
        return null;
    }

    public KhachHang getByPhone(String sdt) {
        String sql = "SELECT * FROM KhachHang WHERE sdt=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, sdt);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in KhachHangDAO", e); }
        return null;
    }

    public KhachHang getByCCCD(String cccd) {
        String sql = "SELECT * FROM KhachHang WHERE cccd=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, cccd);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in KhachHangDAO", e); }
        return null;
    }

    public boolean insert(KhachHang kh) {
        String sql = "INSERT INTO KhachHang(maKhachHang, hoTen, sdt, cccd, ngaySinh, gioiTinh, quocTich) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, kh.getMaKhachHang());
            ps.setString(2, kh.getHoTen());
            ps.setString(3, kh.getSdt());
            ps.setString(4, kh.getCccd());
            ps.setDate(5, kh.getNgaySinh() != null ? java.sql.Date.valueOf(kh.getNgaySinh()) : null);
            ps.setString(6, kh.getGioiTinh());
            ps.setString(7, kh.getQuocTich());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { 
            // Fallback for older schema
            String sqlFallback = "INSERT INTO KhachHang(maKhachHang, hoTen, sdt, cccd) VALUES(?,?,?,?)";
            try (PreparedStatement ps = getConn().prepareStatement(sqlFallback)) {
                ps.setString(1, kh.getMaKhachHang());
                ps.setString(2, kh.getHoTen());
                ps.setString(3, kh.getSdt());
                ps.setString(4, kh.getCccd());
                return ps.executeUpdate() > 0;
            } catch (SQLException ex) { ex.printStackTrace(); return false; }
        }
    }

    public boolean update(KhachHang kh) {
        String sql = "UPDATE KhachHang SET hoTen=?, sdt=?, cccd=?, ngaySinh=?, gioiTinh=?, quocTich=? WHERE maKhachHang=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, kh.getHoTen());
            ps.setString(2, kh.getSdt());
            ps.setString(3, kh.getCccd());
            ps.setDate(4, kh.getNgaySinh() != null ? java.sql.Date.valueOf(kh.getNgaySinh()) : null);
            ps.setString(5, kh.getGioiTinh());
            ps.setString(6, kh.getQuocTich());
            ps.setString(7, kh.getMaKhachHang());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { 
            // Fallback for older schema
            String sqlFallback = "UPDATE KhachHang SET hoTen=?, sdt=?, cccd=? WHERE maKhachHang=?";
            try (PreparedStatement ps = getConn().prepareStatement(sqlFallback)) {
                ps.setString(1, kh.getHoTen());
                ps.setString(2, kh.getSdt());
                ps.setString(3, kh.getCccd());
                ps.setString(4, kh.getMaKhachHang());
                return ps.executeUpdate() > 0;
            } catch (SQLException ex) { ex.printStackTrace(); return false; }
        }
    }

    public String generateMaKH() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT MAX(CAST(SUBSTRING(maKhachHang,3,10) AS INT)) FROM KhachHang WHERE maKhachHang LIKE 'KH%'")) {
            if (rs.next()) return String.format("KH%03d", rs.getInt(1) + 1);
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in KhachHangDAO", e); }
        return "KH001";
    }

    public boolean delete(String maKhachHang) {
        String sql = "DELETE FROM KhachHang WHERE maKhachHang=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maKhachHang);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Database error in KhachHangDAO", e); return false; }
    }

    private KhachHang mapRow(ResultSet rs) throws SQLException {
        KhachHang kh = new KhachHang();
        kh.setMaKhachHang(rs.getString("maKhachHang"));
        kh.setHoTen(rs.getString("hoTen"));
        kh.setSdt(rs.getString("sdt"));
        kh.setCccd(rs.getString("cccd"));
        try {
            Date ns = rs.getDate("ngaySinh");
            if (ns != null) kh.setNgaySinh(ns.toLocalDate());
            kh.setGioiTinh(rs.getString("gioiTinh"));
            kh.setQuocTich(rs.getString("quocTich"));
        } catch (SQLException ignored) {
            // Ignore for backwards compatibility with old schema
        }
        return kh;
    }
}


