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

    public KhuyenMai() {}

    public KhuyenMai(String maKhuyenMai, String tenKhuyenMai, LoaiGiam loaiGiam, double giaTriGiam, LocalDateTime ngayBatDau, LocalDateTime ngayKetThuc) {
        this.maKhuyenMai = maKhuyenMai;
        this.tenKhuyenMai = tenKhuyenMai;
        this.loaiGiam = loaiGiam;
        this.giaTriGiam = giaTriGiam;
        this.ngayBatDau = ngayBatDau;
        this.ngayKetThuc = ngayKetThuc;
    }

    public boolean kiemTraHopLe(LocalDateTime ngay) {
        return (ngay.isAfter(ngayBatDau) || ngay.isEqual(ngayBatDau)) && 
               (ngay.isBefore(ngayKetThuc) || ngay.isEqual(ngayKetThuc));
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

    public String getTenKhuyenMai() { return tenKhuyenMai; }
    public void setTenKhuyenMai(String tenKhuyenMai) { this.tenKhuyenMai = tenKhuyenMai; }

    public LoaiGiam getLoaiGiam() { return loaiGiam; }
    public void setLoaiGiam(LoaiGiam loaiGiam) { this.loaiGiam = loaiGiam; }

    public double getGiaTriGiam() { return giaTriGiam; }
    public void setGiaTriGiam(double giaTriGiam) { this.giaTriGiam = giaTriGiam; }

    public LocalDateTime getNgayBatDau() { return ngayBatDau; }
    public void setNgayBatDau(LocalDateTime ngayBatDau) { this.ngayBatDau = ngayBatDau; }

    public LocalDateTime getNgayKetThuc() { return ngayKetThuc; }
    public void setNgayKetThuc(LocalDateTime ngayKetThuc) { this.ngayKetThuc = ngayKetThuc; }
}
