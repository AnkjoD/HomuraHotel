package entity;

import entity.enums.LoaiChiTietHoaDon;

public class ChiTietHoaDon {
    private String maChiTietHoaDon;
    private LoaiChiTietHoaDon loaiChiTiet;
    private String noiDung;
    private int soLuong;
    private double donGia;
    private double thanhTien;

    private HoaDon hoaDon;

    public ChiTietHoaDon() {}
    
    // Getters and Setters
    public String getMaChiTietHoaDon() { return maChiTietHoaDon; }
    public void setMaChiTietHoaDon(String maChiTietHoaDon) { this.maChiTietHoaDon = maChiTietHoaDon; }

    public ChiTietHoaDon(LoaiChiTietHoaDon loaiChiTiet, String noiDung, int soLuong, double donGia, double thanhTien, HoaDon hoaDon) {
        this.loaiChiTiet = loaiChiTiet;
        this.noiDung = noiDung;
        this.soLuong = soLuong;
        this.donGia = donGia;
        this.thanhTien = thanhTien;
        this.hoaDon = hoaDon;
    }

    // Getters and Setters
    public LoaiChiTietHoaDon getLoaiChiTiet() { return loaiChiTiet; }
    public void setLoaiChiTiet(LoaiChiTietHoaDon loaiChiTiet) { this.loaiChiTiet = loaiChiTiet; }

    public String getNoiDung() { return noiDung; }
    public void setNoiDung(String noiDung) { this.noiDung = noiDung; }

    public int getSoLuong() { return soLuong; }
    public void setSoLuong(int soLuong) { this.soLuong = soLuong; }

    public double getDonGia() { return donGia; }
    public void setDonGia(double donGia) { this.donGia = donGia; }

    public double getThanhTien() { return thanhTien; }

    public double tinhThanhTien() {
        this.thanhTien = soLuong * donGia;
        return this.thanhTien;
    }
    public void setThanhTien(double thanhTien) { this.thanhTien = thanhTien; }

    public HoaDon getHoaDon() { return hoaDon; }
    public void setHoaDon(HoaDon hoaDon) { this.hoaDon = hoaDon; }
}
