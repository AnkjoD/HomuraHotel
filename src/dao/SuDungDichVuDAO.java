package dao;

import database.DemoRepository;

import database.DatabaseConnection;
import entity.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SuDungDichVuDAO {

    private final ChiTietDatPhongDAO ctdpDAO = new ChiTietDatPhongDAO();

    private Connection getConn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    public List<SuDungDichVu> getByChiTietDatPhong(String maChiTiet) {
        if (DatabaseConnection.isDemoMode()) { return DemoRepository.find(SuDungDichVu.class, x -> x.getCtdp()!=null && maChiTiet.equals(x.getCtdp().getMaChiTiet())); }
        List<SuDungDichVu> list = new ArrayList<>();
        String[] queries = {
            "SELECT * FROM SU_DUNG_DICH_VU WHERE maChiTiet=?",
            "SELECT * FROM SuDungDichVu WHERE maChiTiet=?"
        };
        for (String sql : queries) {
            try (PreparedStatement ps = getConn().prepareStatement(sql)) {
                ps.setString(1, maChiTiet);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            } catch (SQLException ignored) {
                // Try next compatible schema variant.
            }
        }

        // Fallback for schema where SuDungDichVu references maDatPhong (not maChiTiet).
        String maDatPhong = null;
        ChiTietDatPhong ct = ctdpDAO.getById(maChiTiet);
        if (ct != null && ct.getDatPhong() != null) {
            maDatPhong = ct.getDatPhong().getMaDatPhong();
        }
        if (maDatPhong == null || maDatPhong.isBlank()) return list;

        String[] byDatPhongQueries = {
            "SELECT * FROM SuDungDichVu WHERE maDatPhong=?",
            "SELECT * FROM SU_DUNG_DICH_VU WHERE maDatPhong=?"
        };
        for (String sql : byDatPhongQueries) {
            try (PreparedStatement ps = getConn().prepareStatement(sql)) {
                ps.setString(1, maDatPhong);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            } catch (SQLException ignored) {
                // Try next compatible schema variant.
            }
        }
        return list;
    }

    public boolean insert(SuDungDichVu sddv) {
        if (DatabaseConnection.isDemoMode()) { if(sddv.getMaSuDung()==null) sddv.setMaSuDung(DemoRepository.nextId("DV")); if(sddv.getThoiDiem()==null) sddv.setThoiDiem(java.time.LocalDateTime.now()); return DemoRepository.insert(SuDungDichVu.class,sddv); }
        String maDV = sddv.getDichVu() != null ? sddv.getDichVu().getMaDichVu() : null;
        if (maDV == null) return false;

        String maChiTiet = sddv.getCtdp() != null ? sddv.getCtdp().getMaChiTiet() : null;

        Timestamp ts = sddv.getThoiDiem() != null ? Timestamp.valueOf(sddv.getThoiDiem()) : Timestamp.valueOf(java.time.LocalDateTime.now());
        String maSD = generateMaSuDung();
        SQLException lastEx = null;

        String[] queries = {
            "INSERT INTO SuDungDichVu(maSuDung, maChiTiet, maDichVu, soLuong, donGiaLucDung, thoiGianDung) VALUES(?,?,?,?,?,?)",
            "INSERT INTO SU_DUNG_DICH_VU(maSuDung, maChiTiet, maDichVu, soLuong, donGiaLucDung, thoiGianDung) VALUES(?,?,?,?,?,?)",
            "INSERT INTO SuDungDichVu(maSuDung, maChiTiet, maDV, soLuong, donGiaLuu, thoiDiem) VALUES(?,?,?,?,?,?)",
            "INSERT INTO SuDungDichVu(maChiTiet, maDichVu, soLuong, donGiaLucDung) VALUES(?,?,?,?)",
            "INSERT INTO SuDungDichVu(maChiTiet, maDV, soLuong, donGiaLuu) VALUES(?,?,?,?)"
        };

        if (maChiTiet != null && !maChiTiet.isBlank()) {
            for (String sql : queries) {
                try (PreparedStatement ps = getConn().prepareStatement(sql)) {
                    if (sql.contains("maSuDung")) {
                        ps.setString(1, maSD);
                        ps.setString(2, maChiTiet);
                        ps.setString(3, maDV);
                        ps.setInt(4, sddv.getSoLuong());
                        ps.setDouble(5, sddv.getDonGiaLuu());
                        ps.setTimestamp(6, ts);
                    } else {
                        ps.setString(1, maChiTiet);
                        ps.setString(2, maDV);
                        ps.setInt(3, sddv.getSoLuong());
                        ps.setDouble(4, sddv.getDonGiaLuu());
                    }
                    if (ps.executeUpdate() > 0) return true;
                } catch (SQLException e) {
                    lastEx = e;
                }
            }
        }

        if (lastEx != null) {
            java.util.logging.Logger.getLogger(SuDungDichVuDAO.class.getName())
                .log(java.util.logging.Level.SEVERE, "Lỗi lưu dịch vụ (maDV=" + maDV + ", maChiTiet=" + maChiTiet + ")", lastEx);
        }
        return false;
    }

    private SuDungDichVu mapRow(ResultSet rs) throws SQLException {
        SuDungDichVu sddv = new SuDungDichVu();

        // Đọc maSuDung nếu có
        try { sddv.setMaSuDung(rs.getString("maSuDung")); } catch (SQLException ignored) {}

        String maDV;
        try {
            maDV = rs.getString("maDichVu");
        } catch (SQLException ex) {
            maDV = rs.getString("maDV");
        }

        DichVu dv = null;
        if (maDV != null) {
            dv = new DichVuDAO().getById(maDV);
        }
        if (dv == null) {
            dv = new DichVu();
            dv.setMaDichVu(maDV);
            dv.setTenDichVu("Dịch vụ " + maDV);
        }
        sddv.setDichVu(dv);

        try {
            sddv.setDonGiaLuu(rs.getDouble("donGiaLucDung"));
        } catch (SQLException ex) {
            sddv.setDonGiaLuu(rs.getDouble("donGiaLuu"));
        }

        sddv.setSoLuong(rs.getInt("soLuong"));

        Timestamp ts = null;
        try {
            ts = rs.getTimestamp("thoiGianDung");
        } catch (SQLException e1) {
            try {
                ts = rs.getTimestamp("thoiDiem");
            } catch (SQLException e2) {
                try {
                    ts = rs.getTimestamp("ngaySuDung");
                } catch (SQLException ignored) {}
            }
        }
        if (ts != null) sddv.setThoiDiem(ts.toLocalDateTime());

        return sddv;
    }

    /**
     * Xóa 1 dòng dịch vụ theo maSuDung (primary key).
     * Nếu không có maSuDung, fallback xóa theo maChiTiet + maDV + soLuong.
     */
    public boolean deleteOne(SuDungDichVu sddv) {
        if (DatabaseConnection.isDemoMode()) { return DemoRepository.remove(SuDungDichVu.class,x -> java.util.Objects.equals(x.getMaSuDung(),sddv.getMaSuDung())); }
        // Thử xóa bằng maSuDung trước (chính xác nhất)
        if (sddv.getMaSuDung() != null && !sddv.getMaSuDung().isBlank()) {
            String[] queries = {
                "DELETE FROM SuDungDichVu WHERE maSuDung=?",
                "DELETE FROM SU_DUNG_DICH_VU WHERE maSuDung=?"
            };
            for (String sql : queries) {
                try (PreparedStatement ps = getConn().prepareStatement(sql)) {
                    ps.setString(1, sddv.getMaSuDung());
                    if (ps.executeUpdate() > 0) return true;
                } catch (SQLException ignored) {}
            }
        }
        // Fallback: xóa theo maChiTiet + maDV (xóa 1 hàng TOP 1)
        String maChiTiet = sddv.getCtdp() != null ? sddv.getCtdp().getMaChiTiet() : null;
        String maDV = sddv.getDichVu() != null ? sddv.getDichVu().getMaDichVu() : null;
        if (maChiTiet == null || maDV == null) return false;
        String[] fallbacks = {
            "DELETE TOP(1) FROM SuDungDichVu WHERE maChiTiet=? AND maDichVu=?",
            "DELETE TOP(1) FROM SU_DUNG_DICH_VU WHERE maChiTiet=? AND maDichVu=?",
            "DELETE TOP(1) FROM SuDungDichVu WHERE maChiTiet=? AND maDV=?",
        };
        for (String sql : fallbacks) {
            try (PreparedStatement ps = getConn().prepareStatement(sql)) {
                ps.setString(1, maChiTiet);
                ps.setString(2, maDV);
                if (ps.executeUpdate() > 0) return true;
            } catch (SQLException ignored) {}
        }
        return false;
    }

    /**
     * Cập nhật số lượng cho 1 dòng dịch vụ.
     */
    public boolean updateSoLuong(SuDungDichVu sddv, int newSoLuong) {
        if (DatabaseConnection.isDemoMode()) { sddv.setSoLuong(newSoLuong); return DemoRepository.update(SuDungDichVu.class,sddv); }
        if (sddv.getMaSuDung() != null && !sddv.getMaSuDung().isBlank()) {
            String[] queries = {
                "UPDATE SuDungDichVu SET soLuong=? WHERE maSuDung=?",
                "UPDATE SU_DUNG_DICH_VU SET soLuong=? WHERE maSuDung=?"
            };
            for (String sql : queries) {
                try (PreparedStatement ps = getConn().prepareStatement(sql)) {
                    ps.setInt(1, newSoLuong);
                    ps.setString(2, sddv.getMaSuDung());
                    if (ps.executeUpdate() > 0) return true;
                } catch (SQLException ignored) {}
            }
        }
        // Fallback theo maChiTiet + maDV
        String maChiTiet = sddv.getCtdp() != null ? sddv.getCtdp().getMaChiTiet() : null;
        String maDV = sddv.getDichVu() != null ? sddv.getDichVu().getMaDichVu() : null;
        if (maChiTiet == null || maDV == null) return false;
        String[] fallbacks = {
            "UPDATE TOP(1) SuDungDichVu SET soLuong=? WHERE maChiTiet=? AND maDichVu=?",
            "UPDATE TOP(1) SU_DUNG_DICH_VU SET soLuong=? WHERE maChiTiet=? AND maDichVu=?",
            "UPDATE TOP(1) SuDungDichVu SET soLuong=? WHERE maChiTiet=? AND maDV=?",
        };
        for (String sql : fallbacks) {
            try (PreparedStatement ps = getConn().prepareStatement(sql)) {
                ps.setInt(1, newSoLuong);
                ps.setString(2, maChiTiet);
                ps.setString(3, maDV);
                if (ps.executeUpdate() > 0) return true;
            } catch (SQLException ignored) {}
        }
        return false;
    }

    private String generateMaSuDung() {
        String[] queries = {
            "SELECT MAX(CAST(SUBSTRING(maSuDung,3,10) AS INT)) FROM SuDungDichVu",
            "SELECT MAX(CAST(SUBSTRING(maSuDung,3,10) AS INT)) FROM SU_DUNG_DICH_VU"
        };
        for (String sql : queries) {
            try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) {
                    int nextId = rs.getInt(1) + 1;
                    return String.format("SD%04d", nextId);
                }
            } catch (SQLException ignored) {}
        }
        return "SD0001";
    }

    public boolean deleteByDatPhong(String ma) {
        if (DatabaseConnection.isDemoMode()) { return DemoRepository.remove(SuDungDichVu.class,x -> x.getCtdp()!=null && ma.equals(x.getCtdp().getMaDatPhong())); }
        String[] queries = {
            "DELETE FROM SuDungDichVu WHERE maDatPhong=?",
            "DELETE FROM SU_DUNG_DICH_VU WHERE maDatPhong=?",
            "DELETE FROM SuDungDichVu WHERE maChiTiet IN (SELECT maChiTiet FROM ChiTietDatPhong WHERE maDatPhong=?)",
            "DELETE FROM SU_DUNG_DICH_VU WHERE maChiTiet IN (SELECT maChiTiet FROM ChiTietDatPhong WHERE maDatPhong=?)"
        };
        for (String sql : queries) {
            try (PreparedStatement ps = getConn().prepareStatement(sql)) {
                ps.setString(1, ma);
                ps.executeUpdate();
            } catch (SQLException ignored) {}
        }
        return true;
    }
}
