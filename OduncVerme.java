import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class OduncVerme extends JFrame {
    private JTextField txtUyeID = new JTextField();
    private JTextField txtKitapID = new JTextField();
    private DefaultTableModel modelAktif;

    public OduncVerme() {
        setTitle("Kitap Ödünç Ver");
        setSize(400, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel pnlForm = new JPanel(new GridLayout(3, 2, 5, 5));
        pnlForm.add(new JLabel(" Üye ID:")); pnlForm.add(txtUyeID);
        pnlForm.add(new JLabel(" Kitap ID:")); pnlForm.add(txtKitapID);
        JButton btnOnayla = new JButton("Ödünç Ver");
        pnlForm.add(btnOnayla);
        add(pnlForm, BorderLayout.NORTH);

        modelAktif = new DefaultTableModel(new String[]{"Ödünç ID", "Kitap ID", "Son Teslim"}, 0);
        JTable tabloAktif = new JTable(modelAktif);
        add(new JScrollPane(tabloAktif), BorderLayout.CENTER);

        btnOnayla.addActionListener(e -> {
            if (txtUyeID.getText().trim().isEmpty() || txtKitapID.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Lütfen tüm alanları doldurunuz!", "Uyarı", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (Connection conn = Veritabani.baglan()) {
                CallableStatement cs = conn.prepareCall("{call sp_YeniOduncVer(?, ?, ?)}");
                cs.setInt(1, Integer.parseInt(txtUyeID.getText().trim()));
                cs.setInt(2, Integer.parseInt(txtKitapID.getText().trim()));
                cs.setInt(3, 1); // Admin ID

                cs.execute();

                JOptionPane.showMessageDialog(this, "İşlem Başarılı! Stok otomatik güncellendi.", "Başarılı", JOptionPane.INFORMATION_MESSAGE);

                txtKitapID.setText("");

            } catch (SQLException ex) {
                // Stok bittiğinde veya limit dolduğunda SQL'den gelen hata
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Sistem Engeli", JOptionPane.ERROR_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Lütfen ID alanlarına sayı giriniz!");
            }
        });
    }
}