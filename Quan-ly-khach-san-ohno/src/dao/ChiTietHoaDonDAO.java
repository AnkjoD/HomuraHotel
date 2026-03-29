package dao;

import database.DatabaseConnection;
import entity.ChiTietHoaDon;
import entity.enums.LoaiChiTietHoaDon;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChiTietHoaDonDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<ChiTietHoaDon> getByHoaDon(String maHoaDon) {
        List<ChiTietHoaDon> list = new ArrayList<>();
        String sql = "SELECT * FROM CHI_TIET_HOA_DON WHERE maHD=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maHoaDon);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean insert(ChiTietHoaDon ct) {
        String sql = "INSERT INTO CHI_TIET_HOA_DON(maCTHD, maHD, loaiChiTiet, tenDichVuPhong, soLuong, donGia, thanhTien) " +
                     "VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, ct.getMaChiTietHoaDon());
            ps.setString(2, ct.getHoaDon().getMaHoaDon());
            ps.setString(3, ct.getLoaiChiTiet() != null ? ct.getLoaiChiTiet().name() : null);
            ps.setString(4, ct.getNoiDung());
            ps.setInt(5, ct.getSoLuong());
            ps.setDouble(6, ct.getDonGia());
            ps.setDouble(7, ct.getThanhTien());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public String generateMaCTHD() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT MAX(CAST(SUBSTRING(maCTHD,5,10) AS INT)) FROM CHI_TIET_HOA_DON WHERE maCTHD LIKE 'CTHD%'")) {
            if (rs.next()) return String.format("CTHD%03d", rs.getInt(1) + 1);
        } catch (SQLException e) { e.printStackTrace(); }
        return "CTHD001";
    }

    private ChiTietHoaDon mapRow(ResultSet rs) throws SQLException {
        ChiTietHoaDon ct = new ChiTietHoaDon();
        ct.setMaChiTietHoaDon(rs.getString("maCTHD"));
        
        String loaiStr = rs.getString("loaiChiTiet");
        if (loaiStr != null) {
            try {
                ct.setLoaiChiTiet(LoaiChiTietHoaDon.valueOf(loaiStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                ct.setLoaiChiTiet(LoaiChiTietHoaDon.PHONG);
            }
        }
        
        ct.setNoiDung(rs.getString("tenDichVuPhong"));
        ct.setSoLuong(rs.getInt("soLuong"));
        ct.setDonGia(rs.getDouble("donGia"));
        ct.setThanhTien(rs.getDouble("thanhTien"));
        
        return ct;
    }
}
