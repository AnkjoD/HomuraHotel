package dao;

import database.DemoRepository;

import database.DatabaseConnection;
import entity.GiaoCa;
import entity.NhanVien;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GiaoCaDAO {
    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public String generateMaGiaoCa() {
        return util.IdGenerator.next("GC");
    }

    public boolean insert(GiaoCa gc) {
        if (DatabaseConnection.isDemoMode()) { gc.setTrangThai("OPEN");return DemoRepository.insert(entity.GiaoCa.class,gc); }
        String sql = "INSERT INTO GiaoCa (maGiaoCa, maNhanVien, thoiGianBatDau, tienMatDauCa, trangThai, ghiChu) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, gc.getMaGiaoCa());
            ps.setString(2, gc.getNhanVien().getMaNhanVien());
            ps.setTimestamp(3, Timestamp.valueOf(gc.getThoiGianBatDau()));
            ps.setDouble(4, gc.getTienMatDauCa());
            ps.setString(5, "OPEN");
            ps.setString(6, gc.getGhiChu());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean closeShift(GiaoCa gc) {
        if (DatabaseConnection.isDemoMode()) { gc.setTrangThai("CLOSED");return DemoRepository.update(entity.GiaoCa.class,gc); }
        String sql = "UPDATE GiaoCa SET thoiGianKetThuc=?, tienMatThuTrongCa=?, tienMatBanGiao=?, tienMatChenhLech=?, maNhanVienNhan=?, ghiChu=?, trangThai='CLOSED' WHERE maGiaoCa=?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(gc.getThoiGianKetThuc()));
            ps.setDouble(2, gc.getTienMatThuTrongCa());
            ps.setDouble(3, gc.getTienMatBanGiao());
            ps.setDouble(4, gc.getTienMatChenhLech());
            ps.setString(5, gc.getMaNhanVienNhan());
            ps.setString(6, gc.getGhiChu());
            ps.setString(7, gc.getMaGiaoCa());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public GiaoCa findCurrentShift(String maNV) {
        if (DatabaseConnection.isDemoMode()) { return DemoRepository.find(entity.GiaoCa.class,x -> "OPEN".equals(x.getTrangThai()) && x.getNhanVien()!=null && maNV.equals(x.getNhanVien().getMaNhanVien())).stream().findFirst().orElse(null); }
        String sql = "SELECT * FROM GiaoCa WHERE maNhanVien=? AND trangThai='OPEN'";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maNV);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy ca CLOSED được bàn giao cho nhân viên chỉ định.
     * Ưu tiên ca có maNhanVienNhan trùng với người đang đăng nhập.
     * Nếu không có ca nào được chỉ định, fallback lấy ca CLOSED mới nhất.
     */
    public GiaoCa getLastClosedShift(String maNhanVienNhan) {
        if (DatabaseConnection.isDemoMode()) { return getAll().stream().filter(x->"CLOSED".equals(x.getTrangThai()) && java.util.Objects.equals(maNhanVienNhan,x.getMaNhanVienNhan())).max(java.util.Comparator.comparing(entity.GiaoCa::getThoiGianKetThuc)).orElse(null); }
        // Ưu tiên 1: Ca mới nhất được bàn giao đúng người
        String sql = "SELECT TOP 1 gc.*, nv.hoTen FROM GiaoCa gc " +
                     "JOIN NhanVien nv ON gc.maNhanVien = nv.maNhanVien " +
                     "WHERE gc.trangThai='CLOSED' AND gc.maNhanVienNhan=? " +
                     "ORDER BY gc.thoiGianKetThuc DESC";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maNhanVienNhan);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Ưu tiên 2: Fallback - Ca CLOSED mới nhất bất kỳ (dùng để hiển thị số tiền két)
        return getLastClosedShift();
    }

    /**
     * Fallback: Lấy ca CLOSED mới nhất bất kỳ (không lọc người nhận).
     */
    public GiaoCa getLastClosedShift() {
        if(DatabaseConnection.isDemoMode())return getAll().stream().filter(x->"CLOSED".equals(x.getTrangThai())).max(java.util.Comparator.comparing(entity.GiaoCa::getThoiGianKetThuc)).orElse(null);
        String sql = "SELECT TOP 1 gc.*, nv.hoTen FROM GiaoCa gc " +
                     "JOIN NhanVien nv ON gc.maNhanVien = nv.maNhanVien " +
                     "WHERE gc.trangThai='CLOSED' " +
                     "ORDER BY gc.thoiGianKetThuc DESC";
        try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return mapRow(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public double calculateRevenue(LocalDateTime start, LocalDateTime end) {
        if (DatabaseConnection.isDemoMode()) { return DemoRepository.find(entity.HoaDon.class,x -> x.getTrangThai()==entity.enums.TrangThaiThanhToan.PAID && x.getNgayLap()!=null && !x.getNgayLap().isBefore(start) && (end==null||!x.getNgayLap().isAfter(end))).stream().mapToDouble(entity.HoaDon::getTongThanhToan).sum(); }
        String sql = "SELECT SUM(tongThanhToan) FROM HoaDon WHERE ngayLap >= ? AND ngayLap <= ? AND trangThai='PAID'";
        return queryAmount(start, end, sql);
    }

    public double calculateCashRevenue(LocalDateTime start, LocalDateTime end) {
        if (DatabaseConnection.isDemoMode()) { return DemoRepository.find(entity.HoaDon.class,x -> x.getTrangThai()==entity.enums.TrangThaiThanhToan.PAID && "CASH".equals(x.getPhuongThucThanhToan()) && x.getNgayLap()!=null && !x.getNgayLap().isBefore(start) && (end==null||!x.getNgayLap().isAfter(end))).stream().mapToDouble(entity.HoaDon::getTongThanhToan).sum(); }
        String sql = "SELECT SUM(tongThanhToan) FROM HoaDon WHERE ngayLap >= ? AND ngayLap <= ? AND trangThai='PAID' AND phuongThucThanhToan='CASH'";
        return queryAmount(start, end, sql);
    }

    private double queryAmount(LocalDateTime start, LocalDateTime end, String sql) {
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(start));
            ps.setTimestamp(2, Timestamp.valueOf(end != null ? end : LocalDateTime.now()));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<GiaoCa> getAll() {
        return search(null, null);
    }

    public List<GiaoCa> search(String staffName, String status) {
        if (DatabaseConnection.isDemoMode()) { return DemoRepository.find(entity.GiaoCa.class,x -> (staffName==null||staffName.isBlank()||DemoRepository.matches(x.getMaGiaoCa(),staffName)||DemoRepository.matches(x.getNhanVien().getHoTen(),staffName)) && (status==null||status.isBlank()||status.equals(x.getTrangThai()))); }
        List<GiaoCa> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT gc.*, nv.hoTen FROM GiaoCa gc " +
            "JOIN NhanVien nv ON gc.maNhanVien = nv.maNhanVien " +
            "WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (staffName != null && !staffName.trim().isEmpty()) {
            sql.append(" AND (nv.hoTen LIKE ? OR gc.maGiaoCa LIKE ?)");
            String k = "%" + staffName.trim() + "%";
            params.add(k); params.add(k);
        }

        if (status != null && !status.equals("Tất cả")) {
            sql.append(" AND gc.trangThai = ?");
            params.add(status);
        }

        sql.append(" ORDER BY gc.thoiGianBatDau DESC");

        try (PreparedStatement ps = getConn().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private GiaoCa mapRow(ResultSet rs) throws SQLException {
        GiaoCa gc = new GiaoCa();
        gc.setMaGiaoCa(rs.getString("maGiaoCa"));

        NhanVien nv = new NhanVien();
        nv.setMaNhanVien(rs.getString("maNhanVien"));
        try { nv.setHoTen(rs.getString("hoTen")); } catch (Exception ignored) {}
        gc.setNhanVien(nv);

        gc.setThoiGianBatDau(rs.getTimestamp("thoiGianBatDau").toLocalDateTime());
        Timestamp end = rs.getTimestamp("thoiGianKetThuc");
        if (end != null) gc.setThoiGianKetThuc(end.toLocalDateTime());

        gc.setTienMatDauCa(rs.getDouble("tienMatDauCa"));
        gc.setTienMatThuTrongCa(rs.getDouble("tienMatThuTrongCa"));
        gc.setTienMatBanGiao(rs.getDouble("tienMatBanGiao"));
        gc.setTienMatChenhLech(rs.getDouble("tienMatChenhLech"));
        gc.setMaNhanVienNhan(rs.getString("maNhanVienNhan"));
        gc.setGhiChu(rs.getString("ghiChu"));
        gc.setTrangThai(rs.getString("trangThai"));
        return gc;
    }

    public boolean saveDenominations(String maGiaoCa, java.util.Map<Integer, Integer> map) {
        if (DatabaseConnection.isDemoMode()) { entity.GiaoCa x=DemoRepository.get(entity.GiaoCa.class,maGiaoCa);if(x==null)return false;x.setDemoDenominations(new java.util.HashMap<>(map));return DemoRepository.update(entity.GiaoCa.class,x); }
        String sql = "INSERT INTO ChiTietKiemTien (maGiaoCa, menhGia, soLuong) VALUES (?, ?, ?)";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            for (java.util.Map.Entry<Integer, Integer> entry : map.entrySet()) {
                ps.setString(1, maGiaoCa);
                ps.setInt(2, entry.getKey());
                ps.setInt(3, entry.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public java.util.Map<Integer, Integer> getDenominations(String maGiaoCa) {
        if (DatabaseConnection.isDemoMode()) { entity.GiaoCa x=DemoRepository.get(entity.GiaoCa.class,maGiaoCa);return x==null?new java.util.HashMap<>():x.getDemoDenominations(); }
        java.util.Map<Integer, Integer> map = new java.util.TreeMap<>(java.util.Collections.reverseOrder());
        String sql = "SELECT menhGia, soLuong FROM ChiTietKiemTien WHERE maGiaoCa = ?";
        try (PreparedStatement ps = getConn().prepareStatement(sql)) {
            ps.setString(1, maGiaoCa);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                map.put(rs.getInt("menhGia"), rs.getInt("soLuong"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }
}
