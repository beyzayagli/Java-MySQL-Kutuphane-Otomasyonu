import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class UyeYonetimi extends JFrame {
    private JTable tablo;
    private DefaultTableModel model;
    private JTextField txtAd, txtSoyad, txtTel, txtEmail;

    public UyeYonetimi() {
        setTitle("Üye Yönetim Paneli");
        setSize(800, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel pnlEkle = new JPanel(new GridLayout(5, 2, 5, 5));
        pnlEkle.setBorder(BorderFactory.createTitledBorder("Yeni Üye Kaydı"));

        pnlEkle.add(new JLabel("Ad:"));
        txtAd = new JTextField(); pnlEkle.add(txtAd);

        pnlEkle.add(new JLabel("Soyad:"));
        txtSoyad = new JTextField(); pnlEkle.add(txtSoyad);

        pnlEkle.add(new JLabel("Telefon:"));
        txtTel = new JTextField(); pnlEkle.add(txtTel);

        pnlEkle.add(new JLabel("E-posta:"));
        txtEmail = new JTextField(); pnlEkle.add(txtEmail);

        JButton btnKaydet = new JButton("Üyeyi Kaydet");
        pnlEkle.add(btnKaydet);
        add(pnlEkle, BorderLayout.NORTH);


        // ÜyeYönetimi ekranına bir panel daha ekler
        JPanel pnlAra = new JPanel();
        JTextField txtAra = new JTextField(10);
        JButton btnAra = new JButton("Üye Ara");
        pnlAra.add(new JLabel("Arama (Ad/Email):"));
        pnlAra.add(txtAra); pnlAra.add(btnAra);
        add(pnlAra, BorderLayout.SOUTH);

        btnAra.addActionListener(e -> {
            model.setRowCount(0);
            try (Connection conn = Veritabani.baglan()) {
                String kelime = txtAra.getText();
                String sql = "SELECT * FROM UYE WHERE Ad LIKE '%"+kelime+"%' OR Email LIKE '%"+kelime+"%'";
                ResultSet rs = conn.createStatement().executeQuery(sql);
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt("UyeID"), rs.getString("Ad"), rs.getString("Soyad"), rs.getString("Telefon"), rs.getString("Email"), rs.getBigDecimal("ToplamBorc")});
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });

        // member list
        model = new DefaultTableModel(new String[]{"ID", "Ad", "Soyad", "Telefon", "E-posta", "Borç"}, 0);
        tablo = new JTable(model);
        add(new JScrollPane(tablo), BorderLayout.CENTER);

        // silme islemi
        JButton btnSil = new JButton("Seçili Üyeyi Sil");
        add(btnSil, BorderLayout.SOUTH);

        listele();

        // buttons olaylari
        btnKaydet.addActionListener(e -> uyeEkle());
        btnSil.addActionListener(e -> uyeSil());
    }

    private void listele() {
        model.setRowCount(0);
        try (Connection conn = Veritabani.baglan()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM UYE");
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("UyeID"), rs.getString("Ad"), rs.getString("Soyad"),
                        rs.getString("Telefon"), rs.getString("Email"), rs.getBigDecimal("ToplamBorc")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void uyeEkle() {
        try (Connection conn = Veritabani.baglan()) {
            String sql = "INSERT INTO UYE (Ad, Soyad, Telefon, Email) VALUES (?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, txtAd.getText());
            pstmt.setString(2, txtSoyad.getText());
            pstmt.setString(3, txtTel.getText());
            pstmt.setString(4, txtEmail.getText());
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Üye başarıyla eklendi!");
            listele();
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Hata: " + e.getMessage()); }
    }

    private void uyeSil() {
        int satir = tablo.getSelectedRow();
        if (satir == -1) return;
        int id = (int) model.getValueAt(satir, 0);

        try (Connection conn = Veritabani.baglan()) {
            PreparedStatement pstmt = conn.prepareStatement("DELETE FROM UYE WHERE UyeID = ?");
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Üye silindi.");
            listele();
        } catch (SQLException e) {
            // TR_UYE_SILME_KONTROL
            JOptionPane.showMessageDialog(this, "Hata: " + e.getMessage());
        }
    }
}