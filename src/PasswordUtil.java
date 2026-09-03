
// ════════════════════════════════════════════════════════════════════
//  UTILITY — PasswordUtil  (SHA-256 hashing)
// ════════════════════════════════════════════════════════════════════

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
class PasswordUtil
{
    public static String hash(String password)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        }
        catch (Exception e) { return password; }  // fallback (should not happen)
    }

    public static boolean verify(String plainText, String hash)
    {
        return hash(plainText).equals(hash);
    }
}
