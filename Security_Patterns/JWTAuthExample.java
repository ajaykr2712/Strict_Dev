import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class JWTAuthExample {
    static String b64Url(byte[] bytes){ return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    static byte[] b64UrlDecode(String s){ return Base64.getUrlDecoder().decode(s); }
    static String hmacSha256(String data, String secret){
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return b64Url(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e){ throw new RuntimeException(e); }
    }
    static String toJson(Map<String, Object> map){
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var e : map.entrySet()){
            if (!first) sb.append(','); first = false;
            sb.append('"').append(e.getKey()).append('"').append(':');
            Object v = e.getValue();
            if (v instanceof Number || v instanceof Boolean) sb.append(v.toString());
            else sb.append('"').append(v.toString()).append('"');
        }
        sb.append('}');
        return sb.toString();
    }
    static String sign(Map<String, Object> header, Map<String, Object> payload, String secret){
        String h = b64Url(toJson(header).getBytes(StandardCharsets.UTF_8));
        String p = b64Url(toJson(payload).getBytes(StandardCharsets.UTF_8));
        String sig = hmacSha256(h + "." + p, secret);
        return h + "." + p + "." + sig;
    }
    static boolean verify(String token, String secret){
        String[] parts = token.split("\\."); if (parts.length != 3) return false;
        String expected = hmacSha256(parts[0] + "." + parts[1], secret);
        return expected.equals(parts[2]);
    }
    public static void main(String[] args){
        String secret = "super-secret"; // demo only
        String token = sign(Map.of("alg","HS256","typ","JWT"), Map.of("sub","user-1","admin", false, "iat", System.currentTimeMillis()/1000), secret);
        System.out.println("JWT: " + token);
        System.out.println("valid? " + verify(token, secret));
    }
}
