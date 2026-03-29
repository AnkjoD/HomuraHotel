package dao;

import database.DatabaseConnection;
import entity.ThuePhong;
import entity.DichVuThue;

import java.sql.*;
import java.util.*;

public class ThuePhongDAO {

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<ThuePhong> getAll() {
        List<ThuePhong> list = new ArrayList<>();
        String sql = "SELECT tp.*, kh.hoTen AS tenKhach, lp.tenLoai, nv.hoTen AS tenNV " +
                     "FROM THUE_PHONG tp " +
                     "JOIN KHACH_HANG kh ON tp.maKH=kh.maKH " +
                     "JOIN PHONG p ON tp.soPhong=p.soPhong " +
                     "JOIN LOAI_PHONG lp ON p.maLoai=lp.maLoai " +
                     "LEFT JOIN NHAN_VIEN nv ON tp.maNV=nv.maNV " +
                     "ORDER BY tp.ngayNhan DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("Error fetching all ThuePhong: " + e.getMessage()); }
        return list;
    }

    public ThuePhong getById(String maThue) {
        String sql = "SELECT tp.*, kh.hoTen AS tenKhach, lp.tenLoai, nv.hoTen AS tenNV " +
                     "FROM THUE_PHONG tp " +
                     "JOIN KHACH_HANG kh ON tp.maKH=kh.maKH " +
                     "JOIN PHONG p ON tp.soPhong=p.soPhong " +
                     "JOIN LOAI_PHONG lp ON p.maLoai=lp.maLoai " +
                     "LEFT JOIN NHAN_VIEN nv ON tp.maNV=nv.maNV " +
                     "WHERE tp.maThue=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maThue);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.err.println("Error fetching ThuePhong by ID: " + e.getMessage()); }
        return null;
    }

    public ThuePhong getActiveByPhong(String soPhong) {
        String sql = "SELECT tp.*, kh.hoTen AS tenKhach, lp.tenLoai, nv.hoTen AS tenNV " +
                     "FROM THUE_PHONG tp " +
                     "JOIN KHACH_HANG kh ON tp.maKH=kh.maKH " +
                     "JOIN PHONG p ON tp.soPhong=p.soPhong " +
                     "JOIN LOAI_PHONG lp ON p.maLoai=lp.maLoai " +
                     "LEFT JOIN NHAN_VIEN nv ON tp.maNV=nv.maNV " +
                     "WHERE tp.soPhong=? AND tp.trangThai=N'Đang thuê'";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, soPhong);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) { System.err.println("Error fetching active ThuePhong by room: " + e.getMessage()); }
        return null;
    }

    public boolean insert(ThuePhong tp) {
        String sql = "INSERT INTO THUE_PHONG(maThue,maKH,soPhong,maDatPhong,ngayNhan,ngayTraDK," +
                     "soKhach,tienPhong,tienDV,tongTien,trangThai,ghiChu,maNV) " +
                     "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, tp.getMaThue());
            ps.setString(2, tp.getMaKH());
            ps.setString(3, tp.getSoPhong());
            ps.setString(4, tp.getMaDatPhong());
            ps.setTimestamp(5, new Timestamp(tp.getNgayNhan().getTime()));
            ps.setTimestamp(6, new Timestamp(tp.getNgayTraDK().getTime()));
            ps.setInt(7, tp.getSoKhach());
            ps.setLong(8, tp.getTienPhong());
            ps.setLong(9, tp.getTienDV());
            ps.setLong(10, tp.getTongTien());
            ps.setString(11, tp.getTrangThai());
            ps.setString(12, tp.getGhiChu());
            ps.setString(13, tp.getMaNV());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error inserting ThuePhong: " + e.getMessage()); return false; }
    }

    public boolean updateTienDV(String maThue, long tienDV, long tongTien) {
        String sql = "UPDATE THUE_PHONG SET tienDV=?, tongTien=? WHERE maThue=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setLong(1, tienDV); ps.setLong(2, tongTien); ps.setString(3, maThue);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error updating DichVu fee: " + e.getMessage()); return false; }
    }

    public boolean checkOut(String maThue, java.util.Date ngayTraThuc, long tongTien) {
        String sql = "UPDATE THUE_PHONG SET ngayTraThuc=?,tongTien=?,trangThai=N'Đã trả' WHERE maThue=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setTimestamp(1, new Timestamp(ngayTraThuc.getTime()));
            ps.setLong(2, tongTien);
            ps.setString(3, maThue);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error updating checkout: " + e.getMessage()); return false; }
    }

    // ---- Dịch vụ trong đợt thuê ----
    public List<DichVuThue> getDichVuByThue(String maThue) {
        List<DichVuThue> list = new ArrayList<>();
        String sql = "SELECT dvt.*, dv.tenDV, dv.donVi FROM DICH_VU_THUE dvt " +
                     "JOIN DICH_VU dv ON dvt.maDV=dv.maDV WHERE dvt.maThue=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maThue);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                DichVuThue dvt = new DichVuThue();
                dvt.setId(rs.getInt("id"));
                dvt.setMaThue(rs.getString("maThue"));
                dvt.setMaDV(rs.getString("maDV"));
                dvt.setSoLuong(rs.getInt("soLuong"));
                dvt.setDonGia(rs.getLong("donGia"));
                dvt.setThanhTien(rs.getLong("thanhTien"));
                dvt.setNgayDung(rs.getTimestamp("ngayDung"));
                dvt.setGhiChu(rs.getString("ghiChu"));
                dvt.setTenDichVu(rs.getString("tenDV"));
                dvt.setDonVi(rs.getString("donVi"));
                list.add(dvt);
            }
        } catch (SQLException e) { System.err.println("Error fetching DichVuThue: " + e.getMessage()); }
        return list;
    }

    public boolean addDichVuThue(DichVuThue dvt) {
        String sql = "INSERT INTO DICH_VU_THUE(maThue,maDV,soLuong,donGia,thanhTien,ngayDung,ghiChu) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, dvt.getMaThue());
            ps.setString(2, dvt.getMaDV());
            ps.setInt(3, dvt.getSoLuong());
            ps.setLong(4, dvt.getDonGia());
            ps.setLong(5, dvt.getThanhTien());
            ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            ps.setString(7, dvt.getGhiChu());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { System.err.println("Error adding DichVuThue: " + e.getMessage()); return false; }
    }

    public String generateMaThue() {
        String sql = "SELECT MAX(CAST(SUBSTRING(maThue,3,10) AS INT)) FROM THUE_PHONG WHERE maThue LIKE 'TP%'";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return String.format("TP%04d", rs.getInt(1) + 1);
        } catch (SQLException e) { System.err.println("Error generating ThuePhong ID: " + e.getMessage()); }
        return "TP0001";
    }

    // ---- Thống kê ----
    public int countByTrangThai(String trangThai) {
        try (PreparedStatement ps = getConn().prepareStatement(
                "SELECT COUNT(*) FROM THUE_PHONG WHERE trangThai=?")) {
            ps.setString(1, trangThai);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println("Error counting ThuePhong by status: " + e.getMessage()); }
        return 0;
    }

    private ThuePhong mapRow(ResultSet rs) throws SQLException {
        ThuePhong tp = new ThuePhong();
        tp.setMaThue(rs.getString("maThue"));
        tp.setMaKH(rs.getString("maKH"));
        tp.setSoPhong(rs.getString("soPhong"));
        tp.setMaDatPhong(rs.getString("maDatPhong"));
        tp.setNgayNhan(rs.getTimestamp("ngayNhan"));
        tp.setNgayTraDK(rs.getTimestamp("ngayTraDK"));
        tp.setNgayTraThuc(rs.getTimestamp("ngayTraThuc"));
        tp.setSoKhach(rs.getInt("soKhach"));
        tp.setTienPhong(rs.getLong("tienPhong"));
        tp.setTienDV(rs.getLong("tienDV"));
        tp.setTongTien(rs.getLong("tongTien"));
        tp.setTrangThai(rs.getString("trangThai"));
        tp.setGhiChu(rs.getString("ghiChu"));
        tp.setMaNV(rs.getString("maNV"));
        tp.setTenKhachHang(rs.getString("tenKhach"));
        tp.setTenLoaiPhong(rs.getString("tenLoai"));
        tp.setTenNhanVien(rs.getString("tenNV"));
        return tp;
    }
}
