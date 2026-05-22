package util;

import java.util.Arrays;
import java.util.List;

/**
 * Danh sách quốc gia dùng cho hệ thống khách sạn.
 * Bao gồm các nước có khách du lịch tới Việt Nam nhiều nhất.
 */
public class CountryList {

    public static final String VIETNAM = "Việt Nam";

    // Danh sách các nước hàng đầu du lịch tới VN (theo thống kê)
    private static final List<String> COUNTRIES = Arrays.asList(
            // Châu Á - Thái Bình Dương (lớn nhất)
            "Trung Quốc",
            "Thái Lan",
            "Nhật Bản",
            "Hàn Quốc",
            "Đài Loan",
            "Malaysia",
            "Sinh-ga-po",
            "In-đô-nê-xi-a",
            "Philippin",
            "Cambodge",
            "Lào",
            "Mia-ma",

            // Châu Âu
            "Anh",
            "Pháp",
            "Đức",
            "Ý",
            "Tây Ban Nha",
            "Hà Lan",
            "Thụy Sĩ",
            "Thụy Điển",
            "Người Đan Mạch",
            "Phần Lan",
            "Bỉ",
            "Áo",
            "Cộng hòa Séc",
            "Ba Lan",
            "Nga",
            "Ukraine",
            "Hy Lạp",
            "Bồ Đào Nha",

            // Bắc Mỹ
            "Mỹ",
            "Ca-na-đa",
            "Mê-xi-cô",

            // Nam Mỹ
            "Bra-xin",
            "Ác-hen-tin",
            "Chi-lê",
            "Colombia",
            "Perú",

            // Đại Dương / Châu Đại Dương
            "Úc",
            "New Zealand",

            // Trung Đông / Bắc Phi
            "U-ê",
            "Ả-rập Xê-út",
            "Ịch-lam",
            "Ai Cập",
            "Nam Phi",

            // Khác
            "Khác");

    /**
     * Lấy danh sách tất cả các nước cho dropdown.
     * Mục đầu là "Tất cả", sau đó là "Nội địa (Việt Nam)", rồi các nước khác
     */
    public static List<String> getFilterList() {
        java.util.ArrayList<String> list = new java.util.ArrayList<>(Arrays.asList(
                "Quốc tịch (Tất cả)",
                "Nội địa (Việt Nam)"));
        list.addAll(COUNTRIES);
        return list;
    }

    /**
     * Lấy danh sách nước để nhập liệu, bao gồm Việt Nam ở đầu
     */
    public static List<String> getCountriesForInput() {
        java.util.ArrayList<String> list = new java.util.ArrayList<>();
        list.add(VIETNAM);
        list.addAll(COUNTRIES);
        return list;
    }

    /**
     * Lấy danh sách nước để nhập liệu (không có "Tất cả")
     */
    public static List<String> getCountries() {
        return COUNTRIES;
    }

    /**
     * Kiểm tra xem có phải khách nội địa không
     */
    public static boolean isDomestic(String country) {
        if (country == null || country.isBlank())
            return true;
        return country.equalsIgnoreCase(VIETNAM) ||
                country.equalsIgnoreCase("VN") ||
                country.equalsIgnoreCase("Việt Nam");
    }

    /**
     * Kiểm tra xem có phải khách quốc tế không
     */
    public static boolean isInternational(String country) {
        return !isDomestic(country);
    }

    /**
     * Chuẩn hóa tên quốc tịch (nếu khách nhập sai)
     */
    public static String normalize(String country) {
        if (country == null || country.isBlank())
            return VIETNAM;

        String upper = country.trim().toUpperCase();

        // Kiểm tra một số biến thể
        if (upper.contains("VIET") || upper.contains("VN"))
            return VIETNAM;

        // Tìm trong danh sách chính
        for (String c : COUNTRIES) {
            if (c.equalsIgnoreCase(country.trim())) {
                return c;
            }
        }

        return country.trim();
    }
}
