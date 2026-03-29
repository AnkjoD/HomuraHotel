package dao;

import database.DatabaseConnection;
import entity.KhuyenMai;
import entity.enums.LoaiGiam;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KhuyenMaiDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<KhuyenMai> getAll() {
        List<KhuyenMai> list = new ArrayList<>();
        String sql = "SELECT * FROM KHUYEN_MAI ORDER BY ngayBatDau DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public KhuyenMai getById(String maKM) {
        String sql = "SELECT * FROM KHUYEN_MAI WHERE maKM=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maKM);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public boolean insert(KhuyenMai km) {
        String sql = "INSERT INTO KHUYEN_MAI(maKM, tenKM, loaiGiam, giaTriGiam, ngayBatDau, ngayKetThuc) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, km.getMaKhuyenMai());
            ps.setString(2, km.getTenKhuyenMai());
            ps.setString(3, km.getLoaiGiam() != null ? km.getLoaiGiam().name() : null);
            ps.setDouble(4, km.getGiaTriGiam());
            ps.setTimestamp(5, km.getNgayBatDau() != null ? Timestamp.valueOf(km.getNgayBatDau()) : null);
            ps.setTimestamp(6, km.getNgayKetThuc() != null ? Timestamp.valueOf(km.getNgayKetThuc()) : null);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    private KhuyenMai mapRow(ResultSet rs) throws SQLException {
        KhuyenMai km = new KhuyenMai();
        km.setMaKhuyenMai(rs.getString("maKM"));
        km.setTenKhuyenMai(rs.getString("tenKM"));
        
        String loaiStr = rs.getString("loaiGiam");
        if (loaiStr != null) {
            try {
                km.setLoaiGiam(LoaiGiam.valueOf(loaiStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                km.setLoaiGiam(LoaiGiam.PERCENT);
            }
        }
        
        km.setGiaTriGiam(rs.getDouble("giaTriGiam"));
        
        Timestamp start = rs.getTimestamp("ngayBatDau");
        if (start != null) km.setNgayBatDau(start.toLocalDateTime());
        
        Timestamp end = rs.getTimestamp("ngayKetThuc");
        if (end != null) km.setNgayKetThuc(end.toLocalDateTime());
        
        return km;
    }
}
