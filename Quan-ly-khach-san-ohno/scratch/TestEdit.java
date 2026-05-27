package scratch;

import dao.PhongDAO;
import entity.Phong;

public class TestEdit {
    public static void main(String[] args) {
        try {
            PhongDAO dao = new PhongDAO();
            Phong p = dao.getById("P101");
            if (p == null) {
                System.out.println("Room P101 not found!");
                return;
            }
            System.out.println("Found room P101: " + p.getSoPhong() + ", floor=" + p.getTang() + ", type=" + p.getTenLoaiPhong() + ", view=" + p.getView());
            
            // Try updating it
            boolean success = dao.update(p);
            System.out.println("Update execution result: " + success);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
