package test;

import dao.*;
import database.DatabaseConnection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;

/** Read actual SQL data for all screens and fail on logged database errors. */
public class HomuraSqlReadTest {
    public static void main(String[] args) {
        if(System.getenv("DB_URL")==null)throw new IllegalStateException("Set DB_URL explicitly");
        DatabaseConnection.setDemoMode(false);
        AtomicInteger errors=new AtomicInteger();
        Handler handler=new Handler() {
            public void publish(LogRecord r){if(r.getThrown()!=null)errors.incrementAndGet();}
            public void flush(){} public void close(){}
        };
        Logger.getLogger("").addHandler(handler);
        java.io.PrintStream originalError=System.err;
        java.io.ByteArrayOutputStream reportErrors=new java.io.ByteArrayOutputStream();
        System.setErr(new java.io.PrintStream(reportErrors));
        try {
            HomuraBusinessTest.check(new PhongDAO().getAll().size()>=4,"SQL rooms");
            HomuraBusinessTest.check(new LoaiPhongDAO().getAll().size()>=5,"SQL room types");
            HomuraBusinessTest.check(new KhachHangDAO().getAll().size()>=2,"SQL customers");
            new NhanVienDAO().getAll();new TaiKhoanDAO().getAll();new DichVuDAO().getAll();
            new DatPhongDAO().getAll();new ChiTietDatPhongDAO().getAll();new HoaDonDAO().getAll();
            new BangGiaDAO().getAll();new KhuyenMaiDAO().getAll();new HuongNhinDAO().getAll();
            new TienNghiDAO().getAll();new KenhDatPhongDAO().getAll();new GiaoCaDAO().getAll();new LogDAO().getAll();
            ThongKeDAO reports=new ThongKeDAO();
            HomuraBusinessTest.check(reports.getDashboardStats().containsKey("tongPhong"),"Dashboard SQL data");
            HomuraBusinessTest.check(reports.getThongKeKy("thang").containsKey("doanhThu"),"Revenue SQL data");
            reports.getDoanhThu7Ngay();reports.getCheckinHomNay();reports.getCheckoutHomNay();reports.getHoatDongGanDay(10);
            reports.getAlerts();reports.getChiTietPhongStats("thang");reports.getChiTietDichVuStats("thang");
            reports.getPhanTichKenhStats("thang");reports.getPhanTichKhachStats("thang");
            HomuraBusinessTest.check(errors.get()==0,"DAO read errors: "+errors.get());
            HomuraBusinessTest.check(!reportErrors.toString().contains("ERROR"),"Reporting query failed: "+reportErrors);
            System.out.println("PASS: real SQL catalog, bookings, stays, invoices, shifts and reporting queries.");
        }finally{System.setErr(originalError);Logger.getLogger("").removeHandler(handler);DatabaseConnection.getInstance().closeConnection();}
    }
}
