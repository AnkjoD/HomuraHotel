package entity;

import java.util.Date;

public class DichVuThue {
    private int    id;
    private String maThue;
    private String maDV;
    private int    soLuong;
    private long   donGia;
    private long   thanhTien;
    private Date   ngayDung;
    private String ghiChu;

    // Join fields
    private String tenDichVu;
    private String donVi;

    public DichVuThue() {}

    public DichVuThue(String maThue, String maDV, int soLuong, long donGia) {
        this.maThue    = maThue;
        this.maDV      = maDV;
        this.soLuong   = soLuong;
        this.donGia    = donGia;
        this.thanhTien = donGia * soLuong;
    }

    public int    getId()               { return id; }
    public void   setId(int v)          { this.id = v; }
    public String getMaThue()           { return maThue; }
    public void   setMaThue(String v)   { this.maThue = v; }
    public String getMaDV()             { return maDV; }
    public void   setMaDV(String v)     { this.maDV = v; }
    public int    getSoLuong()          { return soLuong; }
    public void   setSoLuong(int v)     { this.soLuong = v; }
    public long   getDonGia()           { return donGia; }
    public void   setDonGia(long v)     { this.donGia = v; }
    public long   getThanhTien()        { return thanhTien; }
    public void   setThanhTien(long v)  { this.thanhTien = v; }
    public Date   getNgayDung()         { return ngayDung; }
    public void   setNgayDung(Date v)   { this.ngayDung = v; }
    public String getGhiChu()           { return ghiChu; }
    public void   setGhiChu(String v)   { this.ghiChu = v; }
    public String getTenDichVu()        { return tenDichVu; }
    public void   setTenDichVu(String v){ this.tenDichVu = v; }
    public String getDonVi()            { return donVi; }
    public void   setDonVi(String v)    { this.donVi = v; }
}
