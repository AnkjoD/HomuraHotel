package dao;

import database.DatabaseConnection;
import entity.ChiTietDatPhong;
import entity.DatPhong;
import entity.Phong;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChiTietDatPhongDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<ChiTietDatPhong> getByDatPhong(String maDatPhong) {
        List<ChiTietDatPhong> list = new ArrayList<>();
        String sql = "SELECT * FROM ChiTietDatPhong WHERE maDatPhong=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maDatPhong);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean insert(ChiTietDatPhong ct) {
        String sql = "INSERT INTO ChiTietDatPhong(maChiTiet, maDatPhong, maPhong, ngayNhanThucTe, ngayTraThucTe, giaThucTeChot, phuPhiPhatSinh) " +
                     "VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, ct.getMaChiTiet());
            ps.setString(2, ct.getDatPhong() != null ? ct.getDatPhong().getMaDatPhong() : null);
            ps.setString(3, ct.getPhong() != null ? ct.getPhong().getMaPhong() : null);
            ps.setTimestamp(4, ct.getNgayNhanThucTe() != null ? Timestamp.valueOf(ct.getNgayNhanThucTe()) : null);
            ps.setTimestamp(5, ct.getNgayTraThucTe() != null ? Timestamp.valueOf(ct.getNgayTraThucTe()) : null);
            ps.setDouble(6, ct.getGiaThucTeChot());
            ps.setDouble(7, ct.getPhuPhiPhatSinh());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    public boolean update(ChiTietDatPhong ct) {
        String sql = "UPDATE ChiTietDatPhong SET maDatPhong=?, maPhong=?, ngayNhanThucTe=?, ngayTraThucTe=?, giaThucTeChot=?, phuPhiPhatSinh=? WHERE maChiTiet=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, ct.getDatPhong() != null ? ct.getDatPhong().getMaDatPhong() : null);
            ps.setString(2, ct.getPhong() != null ? ct.getPhong().getMaPhong() : null);
            ps.setTimestamp(3, ct.getNgayNhanThucTe() != null ? Timestamp.valueOf(ct.getNgayNhanThucTe()) : null);
            ps.setTimestamp(4, ct.getNgayTraThucTe() != null ? Timestamp.valueOf(ct.getNgayTraThucTe()) : null);
            ps.setDouble(5, ct.getGiaThucTeChot());
            ps.setDouble(6, ct.getPhuPhiPhatSinh());
            ps.setString(7, ct.getMaChiTiet());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    private ChiTietDatPhong mapRow(ResultSet rs) throws SQLException {
        ChiTietDatPhong ct = new ChiTietDatPhong();
        ct.setMaChiTiet(rs.getString("maChiTiet"));
        
        // Load Phong with full data
        String soPhong = rs.getString("maPhong");
        if (soPhong != null) {
            Phong p = new PhongDAO().getById(soPhong);
            ct.setPhong(p != null ? p : new Phong());
        }
        
        // Load DatPhong with full data including KhachHang
        String maDatPhong = rs.getString("maDatPhong");
        if (maDatPhong != null) {
            DatPhong dp = new DatPhongDAO().getByIdBasic(maDatPhong);
            ct.setDatPhong(dp);
        }
        
        Timestamp checkin = rs.getTimestamp("ngayNhanThucTe");
        if (checkin != null) ct.setNgayNhanThucTe(checkin.toLocalDateTime());
        
        Timestamp checkout = rs.getTimestamp("ngayTraThucTe");
        if (checkout != null) ct.setNgayTraThucTe(checkout.toLocalDateTime());
        
        ct.setGiaThucTeChot(rs.getDouble("giaThucTeChot"));
        ct.setPhuPhiPhatSinh(rs.getDouble("phuPhiPhatSinh"));
        
        return ct;
    }

    public String generateMaChiTiet() {
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT MAX(CAST(SUBSTRING(maChiTiet,3,10) AS INT)) FROM ChiTietDatPhong")) {
            if (rs.next()) return String.format("CT%04d", rs.getInt(1) + 1);
        } catch (SQLException e) { e.printStackTrace(); }
        return "CT0001";
    }

    public ChiTietDatPhong getById(String id) {
        try (PreparedStatement ps = getConn().prepareStatement("SELECT * FROM ChiTietDatPhong WHERE maChiTiet=?")) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    public List<ChiTietDatPhong> getAll() {
        List<ChiTietDatPhong> list = new ArrayList<>();
        try (Statement st = getConn().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM ChiTietDatPhong")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}
