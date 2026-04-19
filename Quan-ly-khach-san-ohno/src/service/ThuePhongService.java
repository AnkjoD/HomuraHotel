package service;

import dao.*;
import database.DatabaseConnection;
import entity.*;
import entity.enums.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThuePhongService {

    private static final Logger LOGGER = Logger.getLogger(ThuePhongService.class.getName());

    private final ChiTietDatPhongDAO ctdpDAO  = new ChiTietDatPhongDAO();
    private final PhongDAO           phongDAO = new PhongDAO();
    private final HoaDonDAO          hoaDonDAO = new HoaDonDAO();
    private final ChiTietHoaDonDAO   cthdDAO  = new ChiTietHoaDonDAO();
    private final SuDungDichVuDAO    sddvDAO  = new SuDungDichVuDAO();
    private final DatPhongDAO        datPhongDAO = new DatPhongDAO();
    private final NhanVienDAO        nhanVienDAO = new NhanVienDAO();
    private final DichVuDAO          dichVuDAO   = new DichVuDAO();
    private final BangGiaService     bangGiaService = new BangGiaService();
    private final KhachHangDAO       khachHangDAO = new KhachHangDAO();
    private final KhuyenMaiDAO       khuyenMaiDAO = new KhuyenMaiDAO();

    // ---- Check-in ----
    public String checkIn(ChiTietDatPhong ct) {
        if (ct == null || ct.getPhong() == null) return "Thông tin phòng không hợp lệ!";
        
        Phong phong = phongDAO.getById(ct.getPhong().getMaPhong());
        if (phong == null) return "Phòng không tồn tại!";
        
        if (phong.getTrangThai() != TrangThaiPhong.AVAILABLE) 
            return "Phòng " + ct.getPhong().getMaPhong() + " không sẵn sàng (đang: " + phong.getTrangThaiString() + ")!";

        if (ctdpDAO.hasActiveStay(phong.getMaPhong())) {
            return "Phòng " + ct.getPhong().getMaPhong() + " hiện đang có một lượt lưu trú chưa được trả phòng trên hệ thống. Vui lòng kiểm tra và trả phòng cũ trước!";
        }

        if (ct.getDatPhong() != null && phong.getLoaiPhong() != null) {
            int sucChua = phong.getLoaiPhong().getSucChua();
            int maxPax = sucChua + 2;
            if (ct.getDatPhong().getSoLuongKhach() > maxPax && sucChua > 0) {
                return "Số khách (" + ct.getDatPhong().getSoLuongKhach() + ") vượt mức tối đa tuyệt đối cho phép (" + maxPax + ")!";
            }
        }

        ChiTietDatPhong existingCt = null;
        if (ct.getDatPhong() != null && ct.getDatPhong().getMaDatPhong() != null) {
            for (ChiTietDatPhong act : ctdpDAO.getByDatPhong(ct.getDatPhong().getMaDatPhong())) {
                if (act.getPhong() != null && act.getPhong().getMaPhong().equals(phong.getMaPhong()) && act.getNgayTraThucTe() == null) {
                    existingCt = act;
                    break;
                }
            }
        }

        if (existingCt != null) {
            existingCt.setNgayNhanThucTe(LocalDateTime.now());
            if (ct.getGiaThucTeChot() > 0) existingCt.setGiaThucTeChot(ct.getGiaThucTeChot());
            ct.setMaChiTiet(existingCt.getMaChiTiet());
            if (!ctdpDAO.update(existingCt)) return "Lỗi cập nhật chi tiết đặt phòng!";
        } else {
            ct.setMaChiTiet(ctdpDAO.generateMaChiTiet());
            ct.setNgayNhanThucTe(LocalDateTime.now());
            if (ct.getGiaThucTeChot() <= 0) {
                ct.setGiaThucTeChot(bangGiaService.layGiaHienHanh(phong.getLoaiPhong().getMaLoaiPhong()));
            }
            if (!ctdpDAO.insert(ct)) return "Lỗi tạo chi tiết thuê phòng!";
        }

        phong.setTrangThai(TrangThaiPhong.OCCUPIED);
        phongDAO.update(phong);
        return null;
    }

    public String themDichVu(SuDungDichVu sddv) {
        if (sddv == null) return "Dữ liệu dịch vụ trống!";
        if (sddv.getDichVu() == null || sddv.getSoLuong() <= 0) 
            return "Thông tin dịch vụ hoặc số lượng không hợp lệ!";
        return sddvDAO.insert(sddv) ? null : "Lỗi lưu dịch vụ vào cơ sở dữ liệu!";
    }

    public String themDichVu(String maChiTiet, String maDV, int sl, double gia) {
        ChiTietDatPhong ct = ctdpDAO.getById(maChiTiet);
        DichVu dv = dichVuDAO.getById(maDV);
        if (dv == null) return "Dịch vụ không tồn tại!";
        SuDungDichVu s = new SuDungDichVu();
        if (ct == null) {
            ct = new ChiTietDatPhong();
            ct.setMaChiTiet(maChiTiet);
        }
        s.setCtdp(ct);
        s.setDichVu(dv);
        s.setSoLuong(sl);
        s.setDonGiaLuu(gia > 0 ? gia : dv.getDonGia());
        s.setThoiDiem(LocalDateTime.now());
        return themDichVu(s);
    }

    public String extendStay(String maChiTiet, LocalDateTime newLDT) {
        ChiTietDatPhong ct = ctdpDAO.getById(maChiTiet);
        if (ct == null || ct.getDatPhong() == null) return "Không tìm thấy thông tin thuê phòng!";
        DatPhong dp = ct.getDatPhong();
        if (newLDT.isBefore(dp.getNgayTraDuKien())) {
            return "Ngày trả mới không thể sớm hơn ngày trả hiện tại!";
        }
        LocalDateTime oldEnd = dp.getNgayTraDuKien();
        if (!phongDAO.isRoomAvailable(ct.getPhong().getMaPhong(), oldEnd, newLDT, dp.getMaDatPhong())) {
            return "Không thể gia hạn! Phòng " + ct.getPhong().getMaPhong() + " đã có khách khác đặt trước trong khoảng thời gian này.";
        }
        dp.setNgayTraDuKien(newLDT);
        boolean ok = datPhongDAO.update(dp);
        if (ok) {
            LOGGER.info("Gia hạn phòng " + ct.getPhong().getMaPhong() + " đến " + newLDT);
            return null;
        }
        return "Lỗi cập nhật CSDL!";
    }

    public String checkOut(String maChiTiet, String maNV) {
        return checkOut(maChiTiet, maNV, "PAID", null);
    }
    
    public String checkOut(String maChiTiet, String maNV, String status) {
        return checkOut(maChiTiet, maNV, status, null);
    }

    /**
     * Overload for legacy logic that only passes status (UNPAID/PAID)
     */
    public String checkOut(String maChiTiet, String maNV, String status, String voucherCode) {
        DatabaseConnection dbConn = DatabaseConnection.getInstance();
        try {
            dbConn.beginTransaction();

            ChiTietDatPhong mainCt = ctdpDAO.getById(maChiTiet);
            if (mainCt == null) {
                dbConn.rollbackTransaction();
                return "Không tìm thấy chi tiết thuê phòng!";
            }

            List<ChiTietDatPhong> segments = new ArrayList<>();
            if (mainCt.getDatPhong() != null) {
                String mainKhId = mainCt.getKhachHang() != null ? mainCt.getKhachHang().getMaKhachHang() : "";
                segments = ctdpDAO.getByDatPhong(mainCt.getDatPhong().getMaDatPhong()).stream()
                        .filter(c -> !c.isDaThanhToan())
                        .filter(c -> {
                            String currentKhId = c.getKhachHang() != null ? c.getKhachHang().getMaKhachHang() : "";
                            return currentKhId.equals(mainKhId);
                        })
                        .collect(java.util.stream.Collectors.toList());
            } else {
                segments.add(mainCt);
            }

            boolean foundMain = false;
            for (ChiTietDatPhong s : segments) if (s.getMaChiTiet().equals(maChiTiet)) foundMain = true;
            if (!foundMain) segments.add(mainCt);

            HoaDon hd = new HoaDon();
            hd.setMaHoaDon(hoaDonDAO.generateMaHD());
            hd.setDatPhong(mainCt.getDatPhong());
            hd.setNhanVien(nhanVienDAO.getById(maNV));
            hd.setNgayLap(LocalDateTime.now());
            
            double tongTienPhong = 0;
            double tongDichVu = 0;
            double tongPhuPhi = 0;
            List<ChiTietHoaDon> pendingItems = new ArrayList<>();

            for (ChiTietDatPhong ct : segments) {
                if (ct.getNgayTraThucTe() == null) ct.setNgayTraThucTe(LocalDateTime.now());
                
                LocalDateTime ngayNhanResolved = ct.getNgayNhanThucTe();
                if (ngayNhanResolved == null && ct.getDatPhong() != null) ngayNhanResolved = ct.getDatPhong().getNgayNhanDuKien();
                
                long soNgay = Math.max(1, tinhSoNgay(ngayNhanResolved, ct.getNgayTraThucTe()));
                double donGiaChot = ct.getGiaThucTeChot() > 0 ? ct.getGiaThucTeChot() : 400000;
                double tienPhongChang = donGiaChot * soNgay;
                tongTienPhong += tienPhongChang;

                ChiTietHoaDon cthdPhong = new ChiTietHoaDon();
                cthdPhong.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                cthdPhong.setHoaDon(hd);
                cthdPhong.setLoaiChiTiet(LoaiChiTietHoaDon.PHONG);
                cthdPhong.setSoLuong((int) soNgay);
                cthdPhong.setDonGia(donGiaChot);
                cthdPhong.setNoiDung("Phòng " + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?") + " (" + soNgay + " đêm)");
                cthdPhong.setThanhTien(tienPhongChang);
                pendingItems.add(cthdPhong);

                List<SuDungDichVu> dsDichVu = sddvDAO.getByChiTietDatPhong(ct.getMaChiTiet());
                for (SuDungDichVu sddv : dsDichVu) {
                    double itemTotal = sddv.getDonGiaLuu() * sddv.getSoLuong();
                    tongDichVu += itemTotal;
                    ChiTietHoaDon cthdDV = new ChiTietHoaDon();
                    cthdDV.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                    cthdDV.setHoaDon(hd);
                    cthdDV.setLoaiChiTiet(LoaiChiTietHoaDon.DICH_VU);
                    cthdDV.setSoLuong(sddv.getSoLuong());
                    cthdDV.setDonGia(sddv.getDonGiaLuu());
                    cthdDV.setNoiDung("[" + (ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?") + "] " + (sddv.getDichVu() != null ? sddv.getDichVu().getTenDV() : "Dịch vụ"));
                    cthdDV.setThanhTien(itemTotal);
                    pendingItems.add(cthdDV);
                }
                tongPhuPhi += ct.getPhuPhiPhatSinh();
                
                ct.setDaThanhToan(true);
                if (!ctdpDAO.update(ct)) { dbConn.rollbackTransaction(); return "Lỗi cập nhật chặng!"; }

                if (ct.getPhong() != null) {
                    Phong p = phongDAO.getById(ct.getPhong().getMaPhong());
                    if (p != null) {
                        p.setTrangThai(TrangThaiPhong.CLEANING);
                        p.setTenKhachHienTai(null);
                        phongDAO.update(p);
                    }
                }
            }
            
            hd.setTongTienPhong(tongTienPhong);
            hd.setTongTienDichVu(tongDichVu + tongPhuPhi);
            
            KhuyenMai chosenKM = null;
            double subTotal = tongTienPhong + hd.getTongTienDichVu();
            
            String selectedVoucher = (voucherCode != null && !voucherCode.trim().isEmpty()) 
                ? voucherCode.trim() 
                : (mainCt.getDatPhong() != null ? mainCt.getDatPhong().getMaKhuyenMai() : null);

            if (selectedVoucher != null && !selectedVoucher.trim().isEmpty()) {
                chosenKM = khuyenMaiDAO.getByVoucherCode(selectedVoucher.trim());
                if (chosenKM != null && !chosenKM.kiemTraHopLe(subTotal)) chosenKM = null;
            }
            
            if (chosenKM == null) chosenKM = getBestPromotion(subTotal);
            
            hd.setKhuyenMai(chosenKM);
            double tienGiam = 0;
            if (chosenKM != null) {
                tienGiam = chosenKM.tinhSoTienGiam(subTotal);
                chosenKM.setDaDung(chosenKM.getDaDung() + 1);
                khuyenMaiDAO.update(chosenKM);
            }
            hd.setTienGiamKhuyenMai(tienGiam);
            
            double tienCoc = 0;
            if (mainCt.getDatPhong() != null) {
                tienCoc = mainCt.getDatPhong().getTienDatCoc();
                if (tienCoc > 0) {
                    ChiTietHoaDon cthdCoc = new ChiTietHoaDon();
                    cthdCoc.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                    cthdCoc.setHoaDon(hd);
                    cthdCoc.setLoaiChiTiet(LoaiChiTietHoaDon.PHU_PHI);
                    cthdCoc.setSoLuong(1);
                    cthdCoc.setDonGia(-tienCoc);
                    cthdCoc.setNoiDung("Khấu trừ tiền cọc");
                    cthdCoc.setThanhTien(-tienCoc);
                    pendingItems.add(cthdCoc);
                }
            }
            hd.setTongThanhToan(subTotal - tienGiam - tienCoc);
            
            try { hd.setTrangThai(TrangThaiThanhToan.valueOf(status.toUpperCase())); } 
            catch (Exception e) { hd.setTrangThai(TrangThaiThanhToan.PAID); }
            
            if (!hoaDonDAO.insert(hd)) { dbConn.rollbackTransaction(); return "Lỗi tạo hóa đơn!"; }
            for (ChiTietHoaDon item : pendingItems) {
                if (!cthdDAO.insert(item)) { dbConn.rollbackTransaction(); return "Lỗi lưu chi tiết hóa đơn!"; }
            }

            if (mainCt.getDatPhong() != null) {
                DatPhong dp = datPhongDAO.getById(mainCt.getDatPhong().getMaDatPhong());
                if (dp != null) {
                    boolean allPaid = true;
                    for (ChiTietDatPhong c : ctdpDAO.getByDatPhong(dp.getMaDatPhong())) {
                        if (c.getNgayTraThucTe() == null) { allPaid = false; break; }
                    }
                    if (allPaid) { dp.setTrangThai(TrangThaiDatPhong.CHECKED_OUT); datPhongDAO.update(dp); }
                }
            }

            if (hd.getTrangThai() == TrangThaiThanhToan.PAID) {
                String targetMaKH = null;
                if (mainCt.getKhachHang() != null) {
                    targetMaKH = mainCt.getKhachHang().getMaKhachHang();
                } else if (mainCt.getDatPhong() != null && mainCt.getDatPhong().getKhachHang() != null) {
                    targetMaKH = mainCt.getDatPhong().getKhachHang().getMaKhachHang();
                }
                
                if (targetMaKH != null) {
                    khachHangDAO.updateCRMStats(targetMaKH, hd.getTongThanhToan(), true);
                }
            }

            dbConn.commitTransaction();
            return null;
        } catch (Exception e) {
            dbConn.rollbackTransaction();
            LOGGER.log(Level.SEVERE, "Lỗi check-out", e);
            return "Lỗi hệ thống: " + e.getMessage();
        }
    }

    public String checkOutWithoutBilling(String maChiTiet) {
        DatabaseConnection dbConn = DatabaseConnection.getInstance();
        try {
            dbConn.beginTransaction();
            ChiTietDatPhong ct = ctdpDAO.getById(maChiTiet);
            if (ct == null) { dbConn.rollbackTransaction(); return "Không tìm thấy chi tiết thuê phòng!"; }
            if (ct.getNgayTraThucTe() != null) { dbConn.rollbackTransaction(); return "Phòng này đã được trả!"; }
            ct.setNgayTraThucTe(LocalDateTime.now());
            ct.setDaThanhToan(false);
            if (!ctdpDAO.update(ct)) { dbConn.rollbackTransaction(); return "Lỗi cập nhật chi tiết đặt phòng!"; }
            if (ct.getDatPhong() != null) {
                DatPhong dp = datPhongDAO.getById(ct.getDatPhong().getMaDatPhong());
                if (dp != null) {
                    boolean allCheckedOut = true;
                    for (ChiTietDatPhong c : ctdpDAO.getByDatPhong(dp.getMaDatPhong())) {
                        if (c.getNgayTraThucTe() == null) { allCheckedOut = false; break; }
                    }
                    if (allCheckedOut) {
                        dp.setTrangThai(TrangThaiDatPhong.CHECKED_OUT);
                        datPhongDAO.update(dp);
                    }
                }
            }
            Phong phong = phongDAO.getById(ct.getPhong().getMaPhong());
            if (phong != null) { phong.setTrangThai(TrangThaiPhong.CLEANING); phongDAO.update(phong); }
            dbConn.commitTransaction();
            return null;
        } catch (Exception e) { dbConn.rollbackTransaction(); return "Lỗi hệ thống: " + e.getMessage(); }
    }

    public String checkOutMasterBill(String maDatPhong, String maNV) {
        DatabaseConnection dbConn = DatabaseConnection.getInstance();
        try {
            dbConn.beginTransaction();
            DatPhong dp = datPhongDAO.getById(maDatPhong);
            if (dp == null) { dbConn.rollbackTransaction(); return "Không tìm thấy đơn đặt phòng!"; }
            List<ChiTietDatPhong> allRooms = ctdpDAO.getByDatPhong(maDatPhong);
            if (allRooms.isEmpty()) { dbConn.rollbackTransaction(); return "Không có phòng nào trong đơn đặt này!"; }

            HoaDon hd = new HoaDon();
            hd.setMaHoaDon(hoaDonDAO.generateMaHD());
            hd.setDatPhong(dp);
            hd.setNhanVien(nhanVienDAO.getById(maNV));
            hd.setNgayLap(LocalDateTime.now());

            double tongTienPhong = 0;
            double tongTienDichVu = 0;
            double tongPhuPhi = 0;
            BangGia bgActive = null;
            try { bgActive = new BangGiaDAO().findActiveToday(); } catch (Exception ignored) {}
            List<ChiTietHoaDon> pendingCTHD = new ArrayList<>();

            for (ChiTietDatPhong ct : allRooms) {
                if (ct.getNgayTraThucTe() == null) ct.setNgayTraThucTe(LocalDateTime.now());
                LocalDateTime ngayNhanResolved = ct.getNgayNhanThucTe();
                if (ngayNhanResolved == null && dp.getNgayNhanDuKien() != null) ngayNhanResolved = dp.getNgayNhanDuKien();
                long soNgay = Math.max(1, tinhSoNgay(ngayNhanResolved, ct.getNgayTraThucTe()));
                double donGia = ct.getGiaThucTeChot() > 0 ? ct.getGiaThucTeChot() : 400000;
                double tienPhong = donGia * soNgay;
                tongTienPhong += tienPhong;

                double phuPhiTraTreCuaPhong = 50000;
                if (bgActive != null && ct.getPhong() != null && ct.getPhong().getLoaiPhong() != null) {
                    ChiTietBangGia ctbg = new ChiTietBangGiaDAO().findByBangGiaAndLoaiPhong(bgActive.getMaBangGia(), ct.getPhong().getLoaiPhong().getMaLoaiPhong());
                    if (ctbg != null && ctbg.getPhuPhiTraTre() > 0) phuPhiTraTreCuaPhong = ctbg.getPhuPhiTraTre();
                }

                double phuPhi = 0;
                if (ngayNhanResolved != null && ngayNhanResolved.getHour() < 14) phuPhi += phuPhiTraTreCuaPhong;
                if (ct.getNgayTraThucTe() != null && ct.getNgayTraThucTe().getHour() > 12) phuPhi += phuPhiTraTreCuaPhong;
                phuPhi += ct.getPhuPhiPhatSinh();
                tongPhuPhi += phuPhi;
                
                String tenPhong = ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?";
                ChiTietHoaDon cthdPhong = new ChiTietHoaDon();
                cthdPhong.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                cthdPhong.setHoaDon(hd);
                cthdPhong.setLoaiChiTiet(LoaiChiTietHoaDon.PHONG);
                cthdPhong.setSoLuong((int) soNgay);
                cthdPhong.setDonGia(donGia);
                cthdPhong.setNoiDung("Phòng " + tenPhong + " (" + soNgay + " đêm)");
                cthdPhong.setThanhTien(tienPhong);
                pendingCTHD.add(cthdPhong);

                if (phuPhi > 0) {
                    ChiTietHoaDon cthdPhuPhi = new ChiTietHoaDon();
                    cthdPhuPhi.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                    cthdPhuPhi.setHoaDon(hd);
                    cthdPhuPhi.setLoaiChiTiet(LoaiChiTietHoaDon.PHU_PHI);
                    cthdPhuPhi.setSoLuong(1);
                    cthdPhuPhi.setDonGia(phuPhi);
                    cthdPhuPhi.setNoiDung("Phụ phí phòng " + tenPhong);
                    cthdPhuPhi.setThanhTien(phuPhi);
                    pendingCTHD.add(cthdPhuPhi);
                }

                List<SuDungDichVu> dsDv = sddvDAO.getByChiTietDatPhong(ct.getMaChiTiet());
                for (SuDungDichVu sddv : dsDv) {
                    double tienDV = sddv.getDonGiaLuu() * sddv.getSoLuong();
                    tongTienDichVu += tienDV;
                    ChiTietHoaDon cthdDV = new ChiTietHoaDon();
                    cthdDV.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                    cthdDV.setHoaDon(hd);
                    cthdDV.setLoaiChiTiet(LoaiChiTietHoaDon.DICH_VU);
                    cthdDV.setSoLuong(sddv.getSoLuong());
                    cthdDV.setDonGia(sddv.getDonGiaLuu());
                    cthdDV.setNoiDung("[P." + tenPhong + "] " + (sddv.getDichVu() != null ? sddv.getDichVu().getTenDV() : "Dịch vụ"));
                    cthdDV.setThanhTien(tienDV);
                    pendingCTHD.add(cthdDV);
                }
                ct.setDaThanhToan(true);
                if (!ctdpDAO.update(ct)) { dbConn.rollbackTransaction(); return "Lỗi cập nhật chặng!"; }
                Phong phong = phongDAO.getById(ct.getPhong().getMaPhong());
                if (phong != null) { phong.setTrangThai(TrangThaiPhong.CLEANING); phongDAO.update(phong); }
            }

            hd.setTongTienPhong(tongTienPhong);
            hd.setTongTienDichVu(tongTienDichVu + tongPhuPhi);
            hd.setTienGiamKhuyenMai(0);
            double tienCocDoan = dp.getTienDatCoc();
            if (tienCocDoan > 0) {
                ChiTietHoaDon cthdCoc = new ChiTietHoaDon();
                cthdCoc.setMaChiTietHoaDon(cthdDAO.generateMaCTHD());
                cthdCoc.setHoaDon(hd);
                cthdCoc.setLoaiChiTiet(LoaiChiTietHoaDon.PHU_PHI);
                cthdCoc.setSoLuong(1);
                cthdCoc.setDonGia(-tienCocDoan);
                cthdCoc.setNoiDung("Khấu trừ tiền cọc (Đoàn)");
                cthdCoc.setThanhTien(-tienCocDoan);
                pendingCTHD.add(cthdCoc);
            }
            hd.setTongThanhToan(tongTienPhong + tongTienDichVu + tongPhuPhi - tienCocDoan);
            hd.setTrangThai(TrangThaiThanhToan.PAID);

            if (!hoaDonDAO.insert(hd)) { dbConn.rollbackTransaction(); return "Lỗi tạo hóa đơn!"; }
            for (ChiTietHoaDon p : pendingCTHD) { if (!cthdDAO.insert(p)) { dbConn.rollbackTransaction(); return "Lỗi lưu chi tiết hóa đơn!"; } }
            
            // --- Cập nhật thống kê CRM (Mới bổ sung) ---
            if (dp.getKhachHang() != null) {
                khachHangDAO.updateCRMStats(dp.getKhachHang().getMaKhachHang(), hd.getTongThanhToan(), true);
            }

            dp.setTrangThai(TrangThaiDatPhong.CHECKED_OUT);
            datPhongDAO.update(dp);
            dbConn.commitTransaction();
            return null;
        } catch (Exception e) { dbConn.rollbackTransaction(); return "Lỗi hệ thống: " + e.getMessage(); }
    }

    public List<ChiTietDatPhong> getUnpaidByDatPhong(String ma) { return ctdpDAO.getByDatPhong(ma).stream().filter(c -> !c.isDaThanhToan()).collect(java.util.stream.Collectors.toList()); }
    public ChiTietDatPhong getById(String ma) { return ctdpDAO.getById(ma); }
    public List<ChiTietDatPhong> getChiTietByDatPhong(String ma) { return ctdpDAO.getByDatPhong(ma); }
    public List<SuDungDichVu> getDichVuByChiTiet(String ma) { return sddvDAO.getByChiTietDatPhong(ma); }
    public ChiTietDatPhong getActiveByPhong(String p) { return ctdpDAO.getActiveStayByPhong(p); }

    public String updateRepresentative(String maDP, KhachHang newKh, String room) {
        try {
            DatPhong dp = datPhongDAO.getById(maDP);
            if (dp == null) return "Không tìm thấy!";
            dp.setKhachHang(newKh);
            datPhongDAO.update(dp);
            if (room != null) { Phong p = phongDAO.getById(room); if (p != null) { p.setTenKhachHienTai(newKh.getHoTen()); phongDAO.update(p); } }
            return null;
        } catch (Exception e) { return e.getMessage(); }
    }

    public String transferRoom(String maCT, String maP, boolean keepPrice) {
        DatabaseConnection dbConn = DatabaseConnection.getInstance();
        try {
            dbConn.beginTransaction();
            ChiTietDatPhong ctOld = ctdpDAO.getById(maCT);
            if (ctOld == null) { dbConn.rollbackTransaction(); return "Không tìm thấy!"; }
            Phong newRoom = phongDAO.getById(maP);
            if (newRoom == null || newRoom.getTrangThai() != TrangThaiPhong.AVAILABLE) { dbConn.rollbackTransaction(); return "Phòng không khả dụng!"; }

            ctOld.setNgayTraThucTe(LocalDateTime.now());
            ctOld.setDaThanhToan(false);
            ctdpDAO.update(ctOld);
            Phong oldR = ctOld.getPhong();
            if (oldR != null) { oldR.setTrangThai(TrangThaiPhong.CLEANING); phongDAO.update(oldR); }

            ChiTietDatPhong ctNew = new ChiTietDatPhong();
            ctNew.setMaChiTiet(ctdpDAO.generateMaChiTiet());
            ctNew.setDatPhong(ctOld.getDatPhong());
            ctNew.setPhong(newRoom);
            ctNew.setKhachHang(ctOld.getKhachHang());
            ctNew.setNgayNhanThucTe(LocalDateTime.now());
            double rate = keepPrice ? ctOld.getGiaThucTeChot() : bangGiaService.layGiaHienHanh(newRoom.getLoaiPhong().getMaLoaiPhong());
            ctNew.setGiaThucTeChot(rate);
            ctNew.setDaThanhToan(false);
            ctdpDAO.insert(ctNew);

            newRoom.setTrangThai(TrangThaiPhong.OCCUPIED);
            newRoom.setTenKhachHienTai(ctNew.getKhachHang() != null ? ctNew.getKhachHang().getHoTen() : "Khách");
            phongDAO.update(newRoom);
            dbConn.commitTransaction();
            return null;
        } catch (Exception e) { dbConn.rollbackTransaction(); return e.getMessage(); }
    }

    public KhuyenMai getBestPromotion(double total) {
        List<KhuyenMai> all = khuyenMaiDAO.getAll();
        KhuyenMai best = null; double maxD = 0;
        for (KhuyenMai km : all) {
            if (km.kiemTraHopLe(total)) {
                double d = km.tinhSoTienGiam(total);
                if (d > maxD) { maxD = d; best = km; }
            }
        }
        return best;
    }

    public long tinhSoNgay(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0;
        java.time.Duration d = java.time.Duration.between(start, end);
        long days = d.toDays();
        // Tính làm tròn: hơn 2 tiếng tính thêm 1 ngày
        return Math.max(1, days + (d.toHoursPart() > 2 ? 1 : 0));
    }

    /**
     * Overload for legacy code using java.util.Date
     */
    public long tinhSoNgay(java.util.Date start, java.util.Date end) {
        if (start == null || end == null) return 0;
        LocalDateTime s = start.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime e = end.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        return tinhSoNgay(s, e);
    }

    public double tinhPhuPhiEarlyCheckin(Phong p, java.time.LocalDateTime t) {
        if (p == null || p.getLoaiPhong() == null || t == null) return 0;
        int hour = t.getHour();
        double baseRate = bangGiaService.layGiaHienHanh(p.getLoaiPhong().getMaLoaiPhong());

        if (hour >= 14) return 0; // Standard check-in

        if (hour < 5) return baseRate; // 100% surcharge
        if (hour < 9) return baseRate * 0.5; // 50% surcharge
        if (hour < 12) return baseRate * 0.3; // 30% surcharge
        
        // 12h - 14h: Free (Grace period)
        return 0;
    }
}
