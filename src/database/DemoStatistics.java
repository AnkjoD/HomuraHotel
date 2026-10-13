package database;

import dao.ChiTietDatPhongDAO;
import entity.*;
import entity.enums.*;
import java.time.*;
import java.util.*;

/** Demo reports are derived from the same bookings and invoices as the workflows. */
public final class DemoStatistics {
    private DemoStatistics() {}
    public static LocalDate[] period(String key, boolean previous) {
        if(key!=null && key.startsWith("yoy:")) {
            LocalDate[] p=period(key.substring(4),false);return new LocalDate[]{p[0].minusYears(1),p[1].minusYears(1)};
        }
        if(key!=null && key.startsWith("custom:")) {
            String[] parts=key.split(":");LocalDate from=LocalDate.parse(parts[1]),to=LocalDate.parse(parts[2]).plusDays(1);
            if(!to.isAfter(from))throw new IllegalArgumentException("Invalid report dates");
            if(previous){long days=java.time.temporal.ChronoUnit.DAYS.between(from,to);return new LocalDate[]{from.minusDays(days),from};}
            return new LocalDate[]{from,to};
        }
        LocalDate today=LocalDate.now(), start, end;
        switch(key == null ? "thang" : key) {
            case "7ngay": start=today.minusDays(6); end=today.plusDays(1); break;
            case "quy": start=LocalDate.of(today.getYear(), ((today.getMonthValue()-1)/3)*3+1, 1); end=start.plusMonths(3); break;
            case "nam": start=today.withDayOfYear(1); end=start.plusYears(1); break;
            default: start=today.withDayOfMonth(1); end=start.plusMonths(1);
        }
        if(previous) { long days=java.time.temporal.ChronoUnit.DAYS.between(start,end); end=start; start=start.minusDays(days); }
        return new LocalDate[]{start,end};
    }
    public static List<HoaDon> invoices(LocalDate start, LocalDate end) {
        return DemoRepository.find(HoaDon.class, h -> h.getTrangThai()==TrangThaiThanhToan.PAID && h.getNgayLap()!=null
                && !h.getNgayLap().toLocalDate().isBefore(start) && h.getNgayLap().toLocalDate().isBefore(end));
    }
    public static Map<String,Object> dashboard() {
        Map<String,Object> result=new LinkedHashMap<>(); List<Phong> rooms=DemoRepository.all(Phong.class);
        result.put("tongPhong",(int)rooms.stream().filter(p->p.getTrangThai()!=TrangThaiPhong.DELETED).count());
        result.put("phongTrong",(int)rooms.stream().filter(p->p.getTrangThai()==TrangThaiPhong.AVAILABLE).count());
        result.put("dangO",(int)rooms.stream().filter(p->p.getTrangThai()==TrangThaiPhong.OCCUPIED).count());
        result.put("daDat",(int)new ChiTietDatPhongDAO().getAll().stream().filter(c->c.getNgayNhanThucTe()==null && c.getDatPhong().getTrangThai()==TrangThaiDatPhong.CONFIRMED).count());
        result.put("checkinHomNay",todayStays(false).size()); result.put("checkoutHomNay",todayStays(true).size());
        result.put("doanhThuHomNay",Math.round(invoices(LocalDate.now(),LocalDate.now().plusDays(1)).stream().mapToDouble(HoaDon::getTongThanhToan).sum()));
        return result;
    }
    public static Map<String,Object> stats(String key, boolean previous) {
        LocalDate[] p=period(key,previous); List<HoaDon> bills=invoices(p[0],p[1]);
        Map<String,Object> m=new LinkedHashMap<>();
        long revenue=Math.round(bills.stream().mapToDouble(HoaDon::getTongThanhToan).sum());
        long roomRevenue=Math.round(bills.stream().mapToDouble(HoaDon::getTongTienPhong).sum());
        long serviceRevenue=Math.round(bills.stream().mapToDouble(HoaDon::getTongTienDichVu).sum());
        long cost=Math.round(DemoRepository.find(ChiPhi.class,c->c.getThoiGian()!=null && !c.getThoiGian().toLocalDate().isBefore(p[0]) && c.getThoiGian().toLocalDate().isBefore(p[1])).stream().mapToDouble(ChiPhi::getSoTien).sum());
        int days=(int)java.time.temporal.ChronoUnit.DAYS.between(p[0],p[1]);
        int rooms=(int)DemoRepository.all(Phong.class).stream().filter(r->r.getTrangThai()!=TrangThaiPhong.DELETED).count();
        int bookings=(int)DemoRepository.all(DatPhong.class).stream().filter(d->d.getNgayDat()!=null && !d.getNgayDat().toLocalDate().isBefore(p[0]) && d.getNgayDat().toLocalDate().isBefore(p[1])).count();
        long occupied=0;
        for(ChiTietDatPhong c:new ChiTietDatPhongDAO().getAll()) if(c.getNgayNhanThucTe()!=null) {
            LocalDate from=c.getNgayNhanThucTe().toLocalDate().isBefore(p[0])?p[0]:c.getNgayNhanThucTe().toLocalDate();
            LocalDate actualEnd=c.getNgayTraThucTe()!=null?c.getNgayTraThucTe().toLocalDate():LocalDate.now().plusDays(1);
            LocalDate to=actualEnd.isAfter(p[1])?p[1]:actualEnd;
            occupied+=Math.max(0,java.time.temporal.ChronoUnit.DAYS.between(from,to));
        }
        long available=(long)rooms*days;
        m.put("days",days); m.put("tongPhong",rooms); m.put("doanhThu",revenue); m.put("doanhThuPhong",roomRevenue); m.put("doanhThuDV",serviceRevenue);
        m.put("operatingCost",cost); m.put("netProfit",revenue-cost); m.put("netProfitMargin",revenue==0?0.0:(revenue-cost)*100.0/revenue);
        m.put("luotDatPhong",bookings); m.put("occupiedRoomNights",occupied); m.put("availableRoomNights",available);
        m.put("occupancyRate",available==0?0.0:occupied*100.0/available); m.put("adr",occupied==0?0.0:roomRevenue/(double)occupied);
        m.put("revpar",available==0?0.0:roomRevenue/(double)available); m.put("trevpar",available==0?0.0:revenue/(double)available);
        m.put("alos",bookings==0?0.0:occupied/(double)bookings); m.put("phongDangThue",dashboard().get("dangO"));
        m.putAll(segments()); m.put("khachMoi",m.get("totalGuests"));
        return m;
    }
    public static Map<String,Object> segments() {
        Map<String,Object> m=new LinkedHashMap<>(); List<KhachHang> guests=DemoRepository.all(KhachHang.class);
        int repeat=(int)guests.stream().filter(g->g.getSoLanO()>1).count();
        m.put("totalGuests",guests.size()); m.put("repeatGuests",repeat); m.put("repeatRate",guests.isEmpty()?0.0:repeat*100.0/guests.size());
        m.put("vipGuests",(int)guests.stream().filter(g->g.getHangKhachHang().contains("VIP")).count());
        m.put("internationalGuests",(int)guests.stream().filter(g->!"Việt Nam".equals(g.getQuocTich())).count());
        m.put("corporateBookings",0); m.put("directBookings",DemoRepository.all(DatPhong.class).size());
        m.put("serviceAttachmentRate",0.0); m.put("totalBookings",DemoRepository.all(DatPhong.class).size()); return m;
    }
    public static List<long[]> revenue(String key) {
        LocalDate[] p=period(key,false); List<long[]> rows=new ArrayList<>();
        for(LocalDate day=p[0];day.isBefore(p[1]);day=day.plusDays(1)) {
            long amount=Math.round(invoices(day,day.plusDays(1)).stream().mapToDouble(HoaDon::getTongThanhToan).sum());
            rows.add(new long[]{day.getDayOfMonth(),amount,day.toEpochDay()});
        } return rows;
    }
    public static List<long[]> revenue7() {
        List<long[]> rows=new ArrayList<>();
        for(int ago=6;ago>=0;ago--) {
            LocalDate day=LocalDate.now().minusDays(ago);
            rows.add(new long[]{ago,Math.round(invoices(day,day.plusDays(1)).stream().mapToDouble(HoaDon::getTongThanhToan).sum())});
        } return rows;
    }
    public static List<Map<String,Object>> todayStays(boolean checkout) {
        List<Map<String,Object>> rows=new ArrayList<>();
        for(ChiTietDatPhong c:new ChiTietDatPhongDAO().getAll()) {
            LocalDateTime when=checkout?c.getNgayTraThucTe():c.getNgayNhanThucTe();
            if(when==null) when=checkout?c.getNgayTraDuKien():c.getNgayNhanDuKien();
            if(when==null || !when.toLocalDate().equals(LocalDate.now())) continue;
            Map<String,Object> row=new LinkedHashMap<>(); row.put("soPhong",c.getSoPhong()); row.put("hoTen",c.getDatPhong().getTenKhachHang());
            row.put("tenLoai",c.getTenLoaiPhong()); row.put(checkout?"ngayTraDK":"ngayNhan",java.sql.Timestamp.valueOf(when));
            row.put("trangThai",checkout?(c.getNgayTraThucTe()!=null?"Đã trả":"Chưa trả"):(c.getNgayNhanThucTe()!=null?"Đã nhận":"Chờ nhận"));rows.add(row);
        }return rows;
    }
    public static List<Map<String,Object>> activities(int limit) {
        List<Map<String,Object>> rows=new ArrayList<>();
        for(ChiTietDatPhong c:new ChiTietDatPhongDAO().getAll()) for(boolean out:new boolean[]{false,true}) {
            LocalDateTime when=out?c.getNgayTraThucTe():c.getNgayNhanThucTe(); if(when==null)continue;
            rows.add(new LinkedHashMap<>(Map.of("loai",out?"Trả phòng":"Nhận phòng","soPhong",c.getSoPhong(),"hoTen",c.getDatPhong().getTenKhachHang(),"thoiGian",java.sql.Timestamp.valueOf(when))));
        } rows.sort((a,b)->((java.sql.Timestamp)b.get("thoiGian")).compareTo((java.sql.Timestamp)a.get("thoiGian")));
        return new ArrayList<>(rows.subList(0,Math.min(Math.max(0,limit),rows.size())));
    }
    public static List<Map<String,Object>> alerts() {
        List<Map<String,Object>> rows=new ArrayList<>();
        for(Phong p:DemoRepository.all(Phong.class)) if(p.getTrangThai()==TrangThaiPhong.CLEANING || p.getTrangThai()==TrangThaiPhong.MAINTENANCE)
            rows.add(new LinkedHashMap<>(Map.of("id",p.getTrangThai()+"_"+p.getMaPhong(),"title",p.getMaPhong()+" – "+p.getTrangThaiString(),"desc","Cần kiểm tra trước khi cho thuê","type","warning")));
        return rows;
    }
    public static List<Map<String,Object>> roomRows(String key) {
        LocalDate[] p=period(key,false); List<Map<String,Object>> rows=new ArrayList<>();
        for(Phong room:DemoRepository.all(Phong.class)) {
            long nights=0, revenue=0;
            for(HoaDon bill:invoices(p[0],p[1])) for(ChiTietHoaDon item:DemoRepository.all(ChiTietHoaDon.class))
                if(item.getHoaDon()!=null && bill.getMaHoaDon().equals(item.getHoaDon().getMaHoaDon()) && item.getLoaiChiTiet()==LoaiChiTietHoaDon.PHONG
                        && item.getNoiDung()!=null && item.getNoiDung().startsWith("Phòng "+room.getMaPhong()+" (")) {
                    nights+=item.getSoLuong(); revenue+=Math.round(item.getThanhTien());
                }
            Map<String,Object> r=new LinkedHashMap<>(); r.put("soPhong",room.getMaPhong());r.put("tenLoai",room.getTenLoaiPhong());
            r.put("occupiedNights",nights); r.put("revenue",revenue);r.put("doanhThu",revenue);r.put("luot",(int)nights);
            r.put("adr",nights==0?0.0:revenue/(double)nights);r.put("revpar",revenue/(double)java.time.temporal.ChronoUnit.DAYS.between(p[0],p[1]));rows.add(r);
        }return rows;
    }
    public static List<Map<String,Object>> serviceRows(String key) {
        LocalDate[] p=period(key,false);List<Map<String,Object>> rows=new ArrayList<>();
        for(DichVu service:DemoRepository.all(DichVu.class)) {
            int quantity=0; double revenue=0;
            for(SuDungDichVu s:DemoRepository.all(SuDungDichVu.class)) if(s.getDichVu()!=null && service.getMaDichVu().equals(s.getDichVu().getMaDichVu()) && s.getThoiDiem()!=null
                    && !s.getThoiDiem().toLocalDate().isBefore(p[0]) && s.getThoiDiem().toLocalDate().isBefore(p[1])) {quantity+=s.getSoLuong();revenue+=s.getDonGiaLuu()*s.getSoLuong();}
            rows.add(new LinkedHashMap<>(Map.of("tenDV",service.getTenDichVu(),"unitPrice",service.getDonGia(),"quantity",quantity,"soLan",quantity,"revenue",revenue,"doanhThu",Math.round(revenue))));
        }return rows;
    }
    public static List<Map<String,Object>> channelRows(String key) {
        LocalDate[] p=period(key,false);List<Map<String,Object>> rows=new ArrayList<>();
        for(KenhDatPhong channel:DemoRepository.all(KenhDatPhong.class)) {
            double revenue=0; int count=0;
            for(DatPhong d:DemoRepository.all(DatPhong.class)) {
                String id=d.getKenhDatPhong()!=null?d.getKenhDatPhong().getMaKenh():"DIRECT";
                if(!channel.getMaKenh().equals(id))continue;
                if(d.getNgayDat()!=null && !d.getNgayDat().toLocalDate().isBefore(p[0]) && d.getNgayDat().toLocalDate().isBefore(p[1]))count++;
                for(HoaDon h:invoices(p[0],p[1])) if(h.getDatPhong()!=null && d.getMaDatPhong().equals(h.getDatPhong().getMaDatPhong()))revenue+=h.getTongTienPhong();
            }
            double commission=revenue*channel.getHeSoHoaHong()/100;
            rows.add(new LinkedHashMap<>(Map.of("maKenh",channel.getMaKenh(),"tenKenh",channel.getTenKenh(),"bookingsCount",count,"roomRevenue",revenue,"commission",commission,"netRevenue",revenue-commission)));
        }return rows;
    }
    public static List<Map<String,Object>> guestRows(String key) {
        LocalDate[] p=period(key,false);List<Map<String,Object>> rows=new ArrayList<>();
        for(KhachHang guest:DemoRepository.all(KhachHang.class)) {
            double spent=0;int visits=0;
            for(HoaDon h:invoices(p[0],p[1])) {
                DatPhong d=h.getDatPhong()!=null?DemoRepository.booking(h.getDatPhong().getMaDatPhong(),false):null;
                if(d!=null && d.getKhachHang()!=null && guest.getMaKhachHang().equals(d.getKhachHang().getMaKhachHang())) {spent+=h.getTongThanhToan();visits++;}
            }
            Map<String,Object> r=new LinkedHashMap<>();r.put("hoTen",guest.getHoTen());r.put("sdt",guest.getSdt());r.put("cccd",guest.getCccd());
            r.put("quocTich",guest.getQuocTich());r.put("vipLevel",guest.getVipLevel());r.put("visits",visits);r.put("totalSpent",spent);rows.add(r);
        }return rows;
    }
}
