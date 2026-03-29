package dao;

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
        String maChiTiet = sddv.getCtdp() != null ? sddv.getCtdp().getMaChiTiet() : null;
        String maDV = sddv.getDichVu() != null ? sddv.getDichVu().getMaDichVu() : null;
        if (maChiTiet == null || maDV == null) return false;

        // 1) Legacy schemas using maChiTiet.
        String[] byChiTietStmts = {
            "INSERT INTO SU_DUNG_DICH_VU(maChiTiet, maDV, soLuong, donGiaLuu) VALUES(?,?,?,?)",
            "INSERT INTO SuDungDichVu(maChiTiet, maDV, soLuong, donGiaLuu) VALUES(?,?,?,?)",
            "INSERT INTO SuDungDichVu(maChiTiet, maDichVu, soLuong, donGiaLuu) VALUES(?,?,?,?)",
            "INSERT INTO SuDungDichVu(maChiTiet, maDichVu, soLuong, donGiaLucDung) VALUES(?,?,?,?)"
        };
        for (String sql : byChiTietStmts) {
            try (PreparedStatement ps = getConn().prepareStatement(sql)) {
                ps.setString(1, maChiTiet);
                ps.setString(2, maDV);
                ps.setInt(3, sddv.getSoLuong());
                ps.setDouble(4, sddv.getDonGiaLuu());
                return ps.executeUpdate() > 0;
            } catch (SQLException ignored) {
                // Try next compatible schema variant.
            }
        }

        // 2) New schema using maDatPhong (+ optional maSuDung PK).
        String maDatPhong = null;
        if (sddv.getCtdp() != null && sddv.getCtdp().getDatPhong() != null) {
            maDatPhong = sddv.getCtdp().getDatPhong().getMaDatPhong();
        }
        if (maDatPhong == null || maDatPhong.isBlank()) {
            ChiTietDatPhong ct = ctdpDAO.getById(maChiTiet);
            if (ct != null && ct.getDatPhong() != null) maDatPhong = ct.getDatPhong().getMaDatPhong();
        }
        if (maDatPhong != null && !maDatPhong.isBlank()) {
            String[] byDatPhongStmts = {
                "INSERT INTO SuDungDichVu(maDatPhong, maDichVu, soLuong, donGiaLucDung) VALUES(?,?,?,?)",
                "INSERT INTO SuDungDichVu(maDatPhong, maDichVu, soLuong, donGiaLuu) VALUES(?,?,?,?)",
                "INSERT INTO SuDungDichVu(maSuDung, maDatPhong, maDichVu, soLuong, donGiaLucDung) VALUES(?,?,?,?,?)",
                "INSERT INTO SuDungDichVu(maSuDung, maDatPhong, maDichVu, soLuong, donGiaLuu) VALUES(?,?,?,?,?)"
            };
            for (int i = 0; i < byDatPhongStmts.length; i++) {
                String sql = byDatPhongStmts[i];
                try (PreparedStatement ps = getConn().prepareStatement(sql)) {
                    if (i < 2) {
                        ps.setString(1, maDatPhong);
                        ps.setString(2, maDV);
                        ps.setInt(3, sddv.getSoLuong());
                        ps.setDouble(4, sddv.getDonGiaLuu());
                    } else {
                        ps.setString(1, generateMaSuDung());
                        ps.setString(2, maDatPhong);
                        ps.setString(3, maDV);
                        ps.setInt(4, sddv.getSoLuong());
                        ps.setDouble(5, sddv.getDonGiaLuu());
                    }
                    return ps.executeUpdate() > 0;
                } catch (SQLException ignored) {
                    // Try next compatible schema variant.
                }
            }
        }

        return false;
    }

    private SuDungDichVu mapRow(ResultSet rs) throws SQLException {
        SuDungDichVu sddv = new SuDungDichVu();
        
        // Load DichVu fully
        String maDV;
        try {
            maDV = rs.getString("maDV");
        } catch (SQLException ex) {
            maDV = rs.getString("maDichVu");
        }
        if (maDV != null) {
            DichVu dv = new DichVuDAO().getById(maDV);
            sddv.setDichVu(dv != null ? dv : new DichVu());
        }
        
        sddv.setSoLuong(rs.getInt("soLuong"));
        try {
            sddv.setDonGiaLuu(rs.getDouble("donGiaLuu"));
        } catch (SQLException ex) {
            sddv.setDonGiaLuu(rs.getDouble("donGiaLucDung"));
        }
        
        return sddv;
    }

    private String generateMaSuDung() {
        String[] queries = {
            "SELECT MAX(CAST(SUBSTRING(maSuDung,3,10) AS INT)) FROM SuDungDichVu WHERE maSuDung LIKE 'SD%'",
            "SELECT MAX(CAST(SUBSTRING(maSuDung,3,10) AS INT)) FROM SU_DUNG_DICH_VU WHERE maSuDung LIKE 'SD%'"
        };
        for (String sql : queries) {
            try (Statement st = getConn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) return String.format("SD%04d", rs.getInt(1) + 1);
            } catch (SQLException ignored) {
                // Try next compatible schema variant.
            }
        }
        return "SD0001";
    }
}
