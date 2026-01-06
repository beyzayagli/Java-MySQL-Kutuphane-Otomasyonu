import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class GirisEkrani extends JFrame {
    private JTextField txtKullanici;
    private JPasswordField txtSifre;

    public GirisEkrani() {
        setTitle("Kütüphane Sistemi - Giriş");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(3, 2, 10, 10));

        add(new JLabel(" Kullanıcı Adı:"));
        txtKullanici = new JTextField();
        add(txtKullanici);

        add(new JLabel(" Şifre:"));
        txtSifre = new JPasswordField();
        add(txtSifre);

        JButton btnGiris = new JButton("Giriş Yap");
        add(btnGiris);

        btnGiris.addActionListener(e -> {
            String kullanici = txtKullanici.getText();
            String sifre = new String(txtSifre.getPassword());

            try (Connection conn = Veritabani.baglan()) {
                String sql = "SELECT * FROM KULLANICI WHERE KullaniciAdi = ? AND Sifre = ?";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, kullanici);
                pstmt.setString(2, sifre);

                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String rol = rs.getString("Rol");
                    String kullaniciAd = rs.getString("KullaniciAdi");

                    JOptionPane.showMessageDialog(this, "Giriş Başarılı! Rol: " + rol);

                    // Ana menüyü açıyoruz
                    new AnaMenu(kullaniciAd).setVisible(true);
                    this.dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Hata: Bilgiler yanlış!");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GirisEkrani().setVisible(true));
    }
}