package entity;

import entity.enums.LoaiGiam;
import java.time.LocalDateTime;

public class KhuyenMai {
    private String maKhuyenMai;
    private String tenKhuyenMai;
    private LoaiGiam loaiGiam;
    private double giaTriGiam;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    
    // New fields
    private boolean trangThai = true; 
    private int soLuong = 999;
    private int daDung = 0;
    private double dieuKienToiThieu = 0;

    public KhuyenMai() {}

    public KhuyenMai(String maKhuyenMai, String tenKhuyenMai, LoaiGiam loaiGiam, double giaTriGiam, LocalDateTime ngayBatDau, LocalDateTime ngayKetThuc) {
        this.maKhuyenMai = maKhuyenMai;
        this.tenKhuyenMai = tenKhuyenMai;
        this.loaiGiam = loaiGiam;
        this.giaTriGiam = giaTriGiam;
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThuc = ngayKetThuc;
    }

    /**
     * Kiểm tra khuyến mãi có thể áp dụng cho hóa đơn này không.
     * @param tongBill Tổng tiền hóa đơn (trước giảm giá)
     * @return true nếu hợp lệ
     */
    public boolean kiemTraHopLe(double tongBill) {
        LocalDateTime now = LocalDateTime.now();
        if (!trangThai) return false;
        if (now.isBefore(ngayBatDau) || now.isAfter(ngayKetThuc)) return false;
        if (daDung >= soLuong) return false;
        if (tongBill < dieuKienToiThieu) return false;
        return true;
    }

    public double tinhSoTienGiam(double tongTien) {
        if (loaiGiam == LoaiGiam.PERCENT) {
            return tongTien * (giaTriGiam / 100.0);
        } else {
            return Math.min(tongTien, giaTriGiam);
        }
    }

    // Getters and Setters
    public String getMaKhuyenMai() { return maKhuyenMai; }
    public void setMaKhuyenMai(String maKhuyenMai) { this.maKhuyenMai = maKhuyenMai; }
    public String getMaKM() { return maKhuyenMai; } 
    public void setMaKM(String maKM) { this.maKhuyenMai = maKM; }

    public String getTenKhuyenMai() { return tenKhuyenMai; }
    public void setTenKhuyenMai(String tenKhuyenMai) { this.tenKhuyenMai = tenKhuyenMai; }
    public String getTenKM() { return tenKhuyenMai; }
    public void setTenKM(String tenKM) { this.tenKhuyenMai = tenKM; }

    public LoaiGiam getLoaiGiam() { return loaiGiam; }
    public void setLoaiGiam(LoaiGiam loaiGiam) { this.loaiGiam = loaiGiam; }

    public double getGiaTriGiam() { return giaTriGiam; }
    public void setGiaTriGiam(double giaTriGiam) { this.giaTriGiam = giaTriGiam; }

    public LocalDateTime getNgayBatDau() { return ngayBatDau; }
    public void setNgayBatDau(LocalDateTime ngayBatDau) { this.ngayBatDau = ngayBatDau; }

    public LocalDateTime getNgayKetThuc() { return ngayKetThuc; }
    public void setNgayKetThuc(LocalDateTime ngayKetThuc) { this.ngayKetThuc = ngayKetThuc; }

    public boolean isTrangThai() { return trangThai; }
    public void setTrangThai(boolean trangThai) { this.trangThai = trangThai; }

    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    public int getDaDung() { return daDung; }
    public void setDaDung(int daDung) { this.daDung = daDung; }

    public double getDieuKienToiThieu() { return dieuKienToiThieu; }
    public void setDieuKienToiThieu(double dieuKienToiThieu) { this.dieuKienToiThieu = dieuKienToiThieu; }
}
