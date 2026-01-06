import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.*;

public class DinamikSorgu extends JFrame {
    private JTextField txtKitapAd, txtYazar, txtYilMin, txtYilMax;
    private JComboBox<String> cmbKategori;
    private JCheckBox chkSadeceMevcut;
    private JTable tablo;
    private DefaultTableModel model;

    public DinamikSorgu() {
        setTitle("Dinamik Kitap Arama ve Raporlama");
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel pnlFiltre = new JPanel(new GridLayout(3, 4, 5, 5));
        pnlFiltre.setBorder(BorderFactory.createTitledBorder("Arama Filtreleri"));

        pnlFiltre.add(new JLabel("Kitap Adı:")); txtKitapAd = new JTextField(); pnlFiltre.add(txtKitapAd);
        pnlFiltre.add(new JLabel("Yazar:")); txtYazar = new JTextField(); pnlFiltre.add(txtYazar);

        pnlFiltre.add(new JLabel("Kategori:"));
        cmbKategori = new JComboBox<>(new String[]{"Hepsi", "Roman", "Bilim", "Tarih", "Edebiyat"});
        pnlFiltre.add(cmbKategori);

        pnlFiltre.add(new JLabel("Basım Yılı (Min):")); txtYilMin = new JTextField(); pnlFiltre.add(txtYilMin);
        pnlFiltre.add(new JLabel("Basım Yılı (Max):")); txtYilMax = new JTextField(); pnlFiltre.add(txtYilMax);

        chkSadeceMevcut = new JCheckBox("Sadece Mevcut Kitaplar");
        pnlFiltre.add(chkSadeceMevcut);

        JButton btnAra = new JButton("Dinamik Sorgula");
        pnlFiltre.add(btnAra);

        JButton btnYazdir = new JButton("Raporu Yazdır (PDF)");
        pnlFiltre.add(btnYazdir);

        add(pnlFiltre, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"ID", "Kitap Adı", "Yazar", "Kategori", "Yıl", "Mevcut Stok"}, 0);
        tablo = new JTable(model);
        add(new JScrollPane(tablo), BorderLayout.CENTER);

        btnAra.addActionListener(e -> ara());
        btnYazdir.addActionListener(e -> {
            try { tablo.print(); } catch (Exception ex) { ex.printStackTrace(); }
        });
    }

    private void ara() {
        model.setRowCount(0);
        // dnamik sql, bos alanlar WHERE'e eklenmez
        String sql = "SELECT * FROM KITAP WHERE 1=1";

        if (!txtKitapAd.getText().trim().isEmpty()) {
            sql += " AND KitapAdi LIKE '%" + txtKitapAd.getText() + "%'";
        }
        if (!txtYazar.getText().trim().isEmpty()) {
            sql += " AND Yazar LIKE '%" + txtYazar.getText() + "%'";
        }
        if (cmbKategori.getSelectedIndex() > 0) {
            sql += " AND Kategori = '" + cmbKategori.getSelectedItem().toString() + "'";
        }
        if (!txtYilMin.getText().trim().isEmpty()) {
            sql += " AND BasimYili >= " + txtYilMin.getText();
        }
        if (!txtYilMax.getText().trim().isEmpty()) {
            sql += " AND BasimYili <= " + txtYilMax.getText();
        }
        if (chkSadeceMevcut.isSelected()) {
            sql += " AND MevcutAdet > 0";
        }

        try (Connection conn = Veritabani.baglan();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("KitapID"), rs.getString("KitapAdi"),
                        rs.getString("Yazar"), rs.getString("Kategori"),
                        rs.getInt("BasimYili"), rs.getInt("MevcutAdet")
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }
}