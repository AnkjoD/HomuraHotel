package test;

import dao.*;
import database.*;
import entity.*;
import entity.enums.*;
import service.*;
import java.time.*;
import java.util.*;

/** End-to-end checks. SQL mode is restricted to explicitly named isolated test databases. */
public class HomuraBusinessTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static DatPhong book(String room, LocalDateTime from, LocalDateTime to) {
        DatPhong dp = new DatPhong();
        dp.setKhachHang(new KhachHangDAO().getById("KH001"));
        dp.setNhanVien(new NhanVienDAO().getById("NV003"));
        dp.setNgayNhanDuKien(from); dp.setNgayTraDuKien(to); dp.setSoLuongKhach(1);
        dp.setTrangThai(TrangThaiDatPhong.CONFIRMED);
        ChiTietDatPhong ct = new ChiTietDatPhong(); ct.setPhong(new PhongDAO().getById(room));
        ct.setGiaThucTeChot(500000); ct.setKhachHang(dp.getKhachHang());
        dp.setDsChiTiet(new ArrayList<>(List.of(ct)));
        return dp;
    }
    public static void main(String[] args) throws Exception {
        boolean sql = args.length > 0 && "--sql".equals(args[0]);
        if (sql) {
            String url=System.getenv("DB_URL");
            check(url!=null && url.matches(".*databaseName=HomuraHotel_Test_[A-Za-z0-9_]+(?:;.*)?"), "SQL tests require a dedicated HomuraHotel_Test_ database");
            DatabaseConnection.setDemoMode(false);
            check(DatabaseConnection.getInstance().testConnection(), "SQL Server connection");
            try (var statement=DatabaseConnection.getInstance().getConnection().createStatement();
                 var database=statement.executeQuery("SELECT DB_NAME()")) {
                check(database.next() && database.getString(1).matches("HomuraHotel_Test_[A-Za-z0-9_]+"),
                        "Connected database is not an isolated SQL test database");
            }
        } else { DatabaseConnection.setDemoMode(true); DemoRepository.reset(); }
        check(AuthService.getInstance().dangNhap("manager", "123") != null, "Login");
        DatPhongService bookings = new DatPhongService(); ThuePhongService stays = new ThuePhongService();
        LocalDateTime now = LocalDateTime.now();
        DatPhong dp = book("P101", now.minusMinutes(5), now.plusDays(2));
        check(bookings.datPhong(dp) == null, "Create booking");
        DatPhong duplicate = book("P101", now, now.plusDays(1));
        check(bookings.datPhong(duplicate) != null, "Reject overlap");
        check(new DatPhongDAO().getById(duplicate.getMaDatPhong()) == null, "Failed booking rolled back");
        ChiTietDatPhong ct = new ChiTietDatPhongDAO().getByDatPhong(dp.getMaDatPhong()).get(0);
        check(stays.checkIn(ct) == null, "Check in");
        check(new PhongDAO().getById("P101").getTrangThai() == TrangThaiPhong.OCCUPIED, "Room occupied");
        check(stays.checkIn(ct) != null, "Reject repeated check-in");
        check(stays.themDichVu(ct.getMaChiTiet(), "DV001", 2, 15000) == null, "Order service");
        check(stays.transferRoom(ct.getMaChiTiet(), "P104", true) == null, "Transfer room");
        ChiTietDatPhong moved = stays.getActiveByPhong("P104");
        check(moved != null && moved.getGiaThucTeChot() == 500000, "Transfer preserves rate");
        check(stays.getDichVuByChiTiet(moved.getMaChiTiet()).size() == 1, "Service moved once");
        check(stays.getDichVuByChiTiet(ct.getMaChiTiet()).isEmpty(), "No duplicate service");
        check(stays.checkOut(moved.getMaChiTiet(), "NV003") == null, "Checkout");
        int invoices = new HoaDonDAO().getAll().size();
        check(invoices == 1, "One invoice");
        check(stays.checkOut(moved.getMaChiTiet(), "NV003") != null, "Reject repeated checkout");
        check(new HoaDonDAO().getAll().size() == invoices, "No duplicated invoice");
        check(new PhongDAO().getById("P104").getTrangThai() == TrangThaiPhong.CLEANING, "Room cleaning");

        DatabaseConnection db = DatabaseConnection.getInstance();
        db.beginTransaction();
        Phong room = new PhongDAO().getById("P201"); room.setTrangThai(TrangThaiPhong.OCCUPIED);
        check(new PhongDAO().update(room), "Write in transaction"); db.rollbackTransaction();
        check(new PhongDAO().getById("P201").getTrangThai() == TrangThaiPhong.AVAILABLE, "Rollback restores mutable data");

        DatPhong invalid = book("P201", now, now.plusDays(1));
        SuDungDichVu badService = new SuDungDichVu(); DichVu missing = new DichVu(); missing.setMaDichVu("MISSING");
        badService.setDichVu(missing); badService.setSoLuong(1); invalid.setDsDichVu(new ArrayList<>(List.of(badService)));
        check(bookings.datPhong(invalid)!=null, "Reject broken service reference");
        check(new DatPhongDAO().getById(invalid.getMaDatPhong())==null, "Rollback partially inserted booking");
        check(new ChiTietDatPhongDAO().getByDatPhong(invalid.getMaDatPhong()).isEmpty(), "Rollback booking details");

        DatPhong group=book("P201",now,now.plusDays(2));
        ChiTietDatPhong second=new ChiTietDatPhong();second.setPhong(new PhongDAO().getById("P204"));second.setGiaThucTeChot(500000);
        group.getDsChiTiet().add(second);group.setLoaiKhach("DOAN");group.setSoLuongKhach(2);group.setTienDatCoc(300000);
        check(bookings.datPhong(group)==null,"Group booking");
        for(ChiTietDatPhong c:new ChiTietDatPhongDAO().getByDatPhong(group.getMaDatPhong()))check(stays.checkIn(c)==null,"Group room check-in");
        check(stays.checkOutMasterBill(group.getMaDatPhong(),"NV003",null)==null,"Master bill");
        check(new DatPhongDAO().getById(group.getMaDatPhong()).getTienDatCoc()==0,"Deposit consumed once");
        check(stays.checkOutMasterBill(group.getMaDatPhong(),"NV003",null)!=null,"Reject repeated master bill");

        DatPhong canceled=book("P303",now.plusDays(4),now.plusDays(5));
        check(bookings.datPhong(canceled)==null,"Future booking");
        check(bookings.huyDatPhong(canceled.getMaDatPhong())==null,"Cancel booking");
        check(new PhongDAO().isRoomAvailable("P303",now.plusDays(4),now.plusDays(5)),"Cancel releases inventory");

        // Billing a historic segment must not change a room that another guest now occupies.
        Phong available=new PhongDAO().getById("P101");available.setTrangThai(TrangThaiPhong.AVAILABLE);new PhongDAO().update(available);
        Phong destination=new PhongDAO().getById("P104");destination.setTrangThai(TrangThaiPhong.AVAILABLE);new PhongDAO().update(destination);
        DatPhong overnight=book("P101",now.minusDays(1),now.plusDays(2));check(bookings.datPhong(overnight)==null,"Overnight booking");
        ChiTietDatPhong oldStay=new ChiTietDatPhongDAO().getByDatPhong(overnight.getMaDatPhong()).get(0);check(stays.checkIn(oldStay)==null,"Overnight check-in");
        oldStay=new ChiTietDatPhongDAO().getById(oldStay.getMaChiTiet());oldStay.setNgayNhanThucTe(now.minusDays(1));new ChiTietDatPhongDAO().update(oldStay);
        check(stays.transferRoom(oldStay.getMaChiTiet(),"P104",true)==null,"Overnight transfer");
        ChiTietDatPhong current=stays.getActiveByPhong("P104");check(stays.quotedRoomTotal(current.getMaChiTiet(),now.plusMinutes(1))==500000,"Same-day transfer does not add minimum night");
        Phong reused=new PhongDAO().getById("P101");reused.setTrangThai(TrangThaiPhong.OCCUPIED);new PhongDAO().update(reused);
        check(stays.checkOut(current.getMaChiTiet(),"NV003")==null,"Bill linked historic segments");
        check(new PhongDAO().getById("P101").getTrangThai()==TrangThaiPhong.OCCUPIED,"Historic room status retained");

        DatPhong competingA=book("P301",now,now.plusDays(2)), competingB=book("P301",now,now.plusDays(2));
        try (java.util.concurrent.ExecutorService pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.CountDownLatch start=new java.util.concurrent.CountDownLatch(1);
            var a=pool.submit(()->{start.await();return bookings.datPhong(competingA);});
            var b=pool.submit(()->{start.await();return bookings.datPhong(competingB);});start.countDown();
            int succeeded=(a.get(10,java.util.concurrent.TimeUnit.SECONDS)==null?1:0)+(b.get(10,java.util.concurrent.TimeUnit.SECONDS)==null?1:0);
            check(succeeded==1,"Concurrent bookings cannot reserve the same room twice");
        }

        LocalDateTime from=LocalDateTime.of(2026,10,13,14,0),to=from.plusDays(1).plusHours(4);
        check(stays.tinhSoNgay(from,to)==1, "Late hours do not become extra night");
        ChiTietDatPhong price = new ChiTietDatPhong(); price.setPhong(new PhongDAO().getById("P201"));
        price.setNgayNhanThucTe(from); price.setNgayTraThucTe(to); price.setGiaThucTeChot(500000);
        StandardRoomCalculator calc=new StandardRoomCalculator();
        check(calc.tinhTienPhong(price,List.of())==500000, "Locked price beats current rate");
        check(calc.tinhPhuPhi(price,from,to)==500000, "Late fee applied once");

        check(new TaiKhoanDAO().updateMatKhau("manager","NewPassword1!"), "Password update");
        AuthService.getInstance().dangXuat();
        check(AuthService.getInstance().dangNhap("manager","123")==null, "Old demo password rejected");
        check(AuthService.getInstance().dangNhap("manager","NewPassword1!")!=null, "Hash verification");
        if(sql) {
            check(new PhongDAO().delete("P501"),"SQL soft delete room");
            check(new PhongDAO().getById("P501").getTrangThai()==TrangThaiPhong.DELETED,"SQL deleted room status");
            HoaDon unpaid=new HoaDon();unpaid.setMaHoaDon(new HoaDonDAO().generateMaHD());
            unpaid.setDatPhong(canceled);unpaid.setNhanVien(new NhanVienDAO().getById("NV003"));
            unpaid.setTrangThai(TrangThaiThanhToan.UNPAID);unpaid.setPhuongThucThanhToan("CASH");
            check(new HoaDonDAO().insert(unpaid),"SQL unpaid invoice");
            check(new HoaDonDAO().delete(unpaid.getMaHoaDon()),"SQL cancel unpaid invoice");
            check(new HoaDonDAO().getById(unpaid.getMaHoaDon()).getTrangThai()==TrangThaiThanhToan.CANCELLED,"SQL canceled invoice status");
            check(!new HoaDonDAO().delete(new HoaDonDAO().getAll().get(0).getMaHoaDon()),"Keep paid invoice history");
        }
        long revenue=((Number)new ThongKeDAO().getDashboardStats().get("doanhThuHomNay")).longValue();
        check(revenue==Math.round(new HoaDonDAO().getAll().stream().mapToDouble(HoaDon::getTongThanhToan).sum()),"Report reflects invoices");
        System.out.println("PASS: booking, overlap, check-in, service, transfer, checkout, duplicate protection, rollback, locked rates and password change.");
    }
}
