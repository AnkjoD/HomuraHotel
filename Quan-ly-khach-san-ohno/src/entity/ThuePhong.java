package entity;

import java.util.Date;

public class ThuePhong {
    private String maThue;
    private String maKH;
    private String soPhong;
    private String maDatPhong;
    private Date   ngayNhan;
    private Date   ngayTraDK;
    private Date   ngayTraThuc;
    private int    soKhach;
    private long   tienPhong;
    private long   tienDV;
    private long   tongTien;
    private String trangThai;   // Đang thuê, Đã trả, Đã hủy
    private String ghiChu;
    private String maNV;

    // Join fields
    private String tenKhachHang;
    private String tenLoaiPhong;
    private String tenNhanVien;

    public ThuePhong() {}

    public String getMaThue()              { return maThue; }
    public void   setMaThue(String v)      { this.maThue = v; }
    public String getMaKH()                { return maKH; }
    public void   setMaKH(String v)        { this.maKH = v; }
    public String getSoPhong()             { return soPhong; }
    public void   setSoPhong(String v)     { this.soPhong = v; }
    public String getMaDatPhong()          { return maDatPhong; }
    public void   setMaDatPhong(String v)  { this.maDatPhong = v; }
    public Date   getNgayNhan()            { return ngayNhan; }
    public void   setNgayNhan(Date v)      { this.ngayNhan = v; }
    public Date   getNgayTraDK()           { return ngayTraDK; }
    public void   setNgayTraDK(Date v)     { this.ngayTraDK = v; }
    public Date   getNgayTraThuc()         { return ngayTraThuc; }
    public void   setNgayTraThuc(Date v)   { this.ngayTraThuc = v; }
    public int    getSoKhach()             { return soKhach; }
    public void   setSoKhach(int v)        { this.soKhach = v; }
    public long   getTienPhong()           { return tienPhong; }
    public void   setTienPhong(long v)     { this.tienPhong = v; }
    public long   getTienDV()              { return tienDV; }
    public void   setTienDV(long v)        { this.tienDV = v; }
    public long   getTongTien()            { return tongTien; }
    public void   setTongTien(long v)      { this.tongTien = v; }
    public String getTrangThai()           { return trangThai; }
    public void   setTrangThai(String v)   { this.trangThai = v; }
    public String getGhiChu()              { return ghiChu; }
    public void   setGhiChu(String v)      { this.ghiChu = v; }
    public String getMaNV()                { return maNV; }
    public void   setMaNV(String v)        { this.maNV = v; }
    public String getTenKhachHang()        { return tenKhachHang; }
    public void   setTenKhachHang(String v){ this.tenKhachHang = v; }
    public String getTenLoaiPhong()        { return tenLoaiPhong; }
    public void   setTenLoaiPhong(String v){ this.tenLoaiPhong = v; }
    public String getTenNhanVien()         { return tenNhanVien; }
    public void   setTenNhanVien(String v) { this.tenNhanVien = v; }
}
