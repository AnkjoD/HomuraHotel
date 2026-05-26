package scratch;

import dao.DatPhongDAO;
import dao.ChiTietDatPhongDAO;
import entity.DatPhong;
import entity.ChiTietDatPhong;
import database.DatabaseConnection;
import java.util.List;

public class CheckDB {
    public static void main(String[] args) {
        try {
            // Initialize connection
            DatabaseConnection.getInstance().getConnection();
            System.out.println("=== DAT PHONG ===");
            DatPhongDAO dpDAO = new DatPhongDAO();
            DatPhong dp = dpDAO.getById("DP-20260526-003");
            if (dp == null) {
                System.out.println("DatPhong DP-20260526-003 not found!");
            } else {
                System.out.println("MaDatPhong: " + dp.getMaDatPhong());
                System.out.println("TienDatCoc: " + dp.getTienDatCoc());
                System.out.println("TrangThai: " + dp.getTrangThai());
            }

            System.out.println("\n=== CHI TIET DAT PHONG ===");
            ChiTietDatPhongDAO ctdpDAO = new ChiTietDatPhongDAO();
            List<ChiTietDatPhong> list = ctdpDAO.getByDatPhong("DP-20260526-003");
            for (ChiTietDatPhong ct : list) {
                System.out.println("MaChiTiet: " + ct.getMaChiTiet() + 
                                   ", Phong: " + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "null") + 
                                   ", DaThanhToan: " + ct.isDaThanhToan() + 
                                   ", NgayTraThucTe: " + ct.getNgayTraThucTe());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
