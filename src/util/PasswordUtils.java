package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

/** New passwords use PBKDF2-HMAC-SHA256; legacy SHA-256 hashes remain readable. */
public class PasswordUtils {

    private static final Logger LOGGER = Logger.getLogger(PasswordUtils.class.getName());
    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16;

    /**
     * Tạo salt ngẫu nhiên.
     */
    public static String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hash mật khẩu với salt.
     * Kết quả: salt$hash (dùng $ làm separator)
     */
    public static String hashPassword(String password) {
        byte[] salt = Base64.getDecoder().decode(generateSalt());
        int iterations = 210000;
        return "pbkdf2$" + iterations + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derive(password, salt, iterations));
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try { return javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (java.security.GeneralSecurityException e) { throw new IllegalStateException(e); }
        finally { spec.clearPassword(); }
    }

    /**
     * Hash mật khẩu với salt cho trước.
     */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORITHM);
            String salted = salt + password;
            byte[] hash = md.digest(salted.getBytes(StandardCharsets.UTF_8));
            String hashStr = Base64.getEncoder().encodeToString(hash);
            return salt + "$" + hashStr;
        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("Lỗi hash mật khẩu: " + e.getMessage());
            throw new RuntimeException("Không hỗ trợ thuật toán " + ALGORITHM, e);
        }
    }

    /**
     * Kiểm tra mật khẩu có khớp với hash đã lưu không.
     * @param password  Mật khẩu người dùng nhập
     * @param storedHash  Hash đã lưu trong DB (dạng salt$hash)
     * @return true nếu khớp
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) return false;
        if (storedHash.startsWith("pbkdf2$")) {
            try {
                String[] parts = storedHash.split("\\$");
                if (parts.length != 4) return false;
                int iterations = Integer.parseInt(parts[1]);
                if (iterations < 10000 || iterations > 2000000) return false;
                return MessageDigest.isEqual(Base64.getDecoder().decode(parts[3]),
                        derive(password, Base64.getDecoder().decode(parts[2]), iterations));
            } catch (IllegalArgumentException e) { return false; }
        }

        // Hỗ trợ legacy: nếu hash không chứa '$' thì đây là plaintext cũ → so sánh trực tiếp
        if (!storedHash.contains("$")) {
            return password.equals(storedHash);
        }

        String salt = storedHash.substring(0, storedHash.indexOf('$'));
        String newHash = hashPassword(password, salt);
        return newHash.equals(storedHash);
    }

    /**
     * Kiểm tra xem mật khẩu đã được hash chưa (có chứa '$' separator).
     */
    public static boolean isHashed(String password) {
        return password != null && password.contains("$") && password.length() > 30;
    }
}
