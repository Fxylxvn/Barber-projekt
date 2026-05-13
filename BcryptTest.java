import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("adminpass123: " + encoder.encode("adminpass123"));
        System.out.println("barberpass123: " + encoder.encode("barberpass123"));
        System.out.println("haslo123: " + encoder.encode("haslo123"));
    }
}
