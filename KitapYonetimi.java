import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class KitapYonetimi extends JFrame {
    private JTable tablo;
    private DefaultTableModel model;
    private JTextField txtKitapAd, txtYazar, txtBasimYili, txtToplamAdet, txtKategori, txtAra;

    public KitapYonetimi() {
        setTitle("Kitap Yönetim Paneli");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());


        JPanel pnlEkle = new JPanel(new GridLayout(3, 4, 10, 10));
        pnlEkle.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        pnlEkle.add(new JLabel("Kitap Adı:")); txtKitapAd = new JTextField(); pnlEkle.add(txtKitapAd);
        pnlEkle.add(new JLabel("Yazar:")); txtYazar = new JTextField(); pnlEkle.add(txtYazar);
        pnlEkle.add(new JLabel("Basım Yılı:")); txtBasimYili = new JTextField(); pnlEkle.add(txtBasimYili);
        pnlEkle.add(new JLabel("Toplam Adet:")); txtToplamAdet = new JTextField(); pnlEkle.add(txtToplamAdet);
        pnlEkle.add(new JLabel("Kategori:")); txtKategori = new JTextField(); pnlEkle.add(txtKategori);

        JButton btnKaydet = new JButton("Kitabı Kaydet");
        pnlEkle.add(btnKaydet);
        add(pnlEkle, BorderLayout.NORTH);

        JPanel pnlOrta = new JPanel(new BorderLayout());

        JPanel pnlAra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlAra.add(new JLabel("Kitap Ara:"));
        txtAra = new JTextField(20);
        JButton btnAra = new JButton("Filtrele");
        pnlAra.add(txtAra); pnlAra.add(btnAra);
        pnlOrta.add(pnlAra, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"ID", "Kitap Adı", "Yazar", "Toplam", "Mevcut"}, 0);
        tablo = new JTable(model);
        pnlOrta.add(new JScrollPane(tablo), BorderLayout.CENTER);
        add(pnlOrta, BorderLayout.CENTER);

        JButton btnSil = new JButton("Seçili Kitabı Sil");
        add(btnSil, BorderLayout.SOUTH);

        listele();

        btnKaydet.addActionListener(e -> kitapEkle());
        btnAra.addActionListener(e -> kitapAra());
        btnSil.addActionListener(e -> kitapSil());
    }

    private void listele() {
        model.setRowCount(0);
        try (Connection conn = Veritabani.baglan()) {
            ResultSet rs = conn.createStatement().executeQuery("SELECT * FROM KITAP");
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("KitapID"), rs.getString("KitapAdi"), rs.getString("Yazar"), rs.getInt("ToplamAdet"), rs.getInt("MevcutAdet")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void kitapAra() {
        model.setRowCount(0);
        try (Connection conn = Veritabani.baglan()) {
            String sql = "SELECT * FROM KITAP WHERE KitapAdi LIKE ? OR Yazar LIKE ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "%" + txtAra.getText() + "%");
            pstmt.setString(2, "%" + txtAra.getText() + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("KitapID"), rs.getString("KitapAdi"), rs.getString("Yazar"), rs.getInt("ToplamAdet"), rs.getInt("MevcutAdet")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void kitapSil() {
        int satir = tablo.getSelectedRow();
        if (satir == -1) return;
        int id = (int) model.getValueAt(satir, 0);
        try (Connection conn = Veritabani.baglan()) {
            PreparedStatement pstmt = conn.prepareStatement("DELETE FROM KITAP WHERE KitapID = ?");
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            listele();
        } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Hata: Aktif ödünçteki kitap silinemez!"); }
    }

    private void kitapEkle() {
        if (txtKitapAd.getText().trim().isEmpty() || txtYazar.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Lütfen Kitap Adı ve Yazar alanlarını doldurun!");
            return;
        }

        try (Connection conn = Veritabani.baglan()) {
            // sp_KitapEkleVeyaGuncelle yordamini cagirir
            CallableStatement cs = conn.prepareCall("{call sp_KitapEkleVeyaGuncelle(?, ?, ?, ?, ?)}");
            cs.setString(1, txtKitapAd.getText().trim());
            cs.setString(2, txtYazar.getText().trim());
            cs.setString(3, txtKategori.getText().trim());
            cs.setInt(4, Integer.parseInt(txtBasimYili.getText().trim()));
            cs.setInt(5, Integer.parseInt(txtToplamAdet.getText().trim()));

            cs.execute();
            JOptionPane.showMessageDialog(this, "Kitap başarıyla eklendi veya güncellendi!");

            txtKitapAd.setText("");
            txtYazar.setText("");
            listele();

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Veritabanı Hatası: " + ex.getMessage());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Yıl ve Adet alanlarına sadece sayı giriniz!");
        }
    }
}