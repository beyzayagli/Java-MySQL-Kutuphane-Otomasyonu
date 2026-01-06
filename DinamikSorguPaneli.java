import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class DinamikSorguPaneli extends JFrame {
    private JTable tablo;
    private DefaultTableModel model;
    private JTextField txtArama;
    private JComboBox<String> comboKriter;

    public DinamikSorguPaneli() {
        setTitle("Dinamik Sorgu ve Raporlar");
        setSize(900, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel pnlArama = new JPanel();
        pnlArama.add(new JLabel("Arama Terimi:"));
        txtArama = new JTextField(15);
        pnlArama.add(txtArama);

        String[] kriterler = {"Kitap Adı", "Yazar", "Üye Adı"};
        comboKriter = new JComboBox<>(kriterler);
        pnlArama.add(comboKriter);

        JButton btnAra = new JButton("Sorgula");
        pnlArama.add(btnAra);
        add(pnlArama, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{"İşlem (Odunc) ID", "Üye", "Kitap", "Ödünç Tarihi", "Durum"}, 0);
        tablo = new JTable(model);
        add(new JScrollPane(tablo), BorderLayout.CENTER);

        JButton btnAktifOdunc = new JButton("Eldeki Kitapları Göster");
        add(btnAktifOdunc, BorderLayout.SOUTH);

        btnAra.addActionListener(e -> dinamikAra());
        btnAktifOdunc.addActionListener(e -> raporListele());

        raporListele(); // Açılışta mevcut ödünçleri göster
    }

    private void raporListele() {
        model.setRowCount(0);
        try (Connection conn = Veritabani.baglan()) {
            // JOIN kullanarak ilişkili tabloları birleştirme
            String sql = "SELECT o.OduncID, u.Ad, k.KitapAdi, o.OduncTarihi, o.TeslimTarihi " +
                    "FROM ODUNC o " +
                    "JOIN UYE u ON o.UyeID = u.UyeID " +
                    "JOIN KITAP k ON o.KitapID = k.KitapID " +
                    "ORDER BY o.OduncID DESC";

            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                String durum = rs.getDate("TeslimTarihi") == null ? "Teslim Edilmedi" : "İade Edildi";
                model.addRow(new Object[]{
                        rs.getInt("OduncID"), rs.getString("Ad"),
                        rs.getString("KitapAdi"), rs.getDate("OduncTarihi"), durum
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void dinamikAra() {
        // dinamik WHERE şartı oluştur
        String kriter = comboKriter.getSelectedItem().toString();
        String tabloSutun = kriter.equals("Kitap Adı") ? "k.KitapAdi" : (kriter.equals("Yazar") ? "k.Yazar" : "u.Ad");

        model.setRowCount(0);
        try (Connection conn = Veritabani.baglan()) {
            String sql = "SELECT o.OduncID, u.Ad, k.KitapAdi, o.OduncTarihi, o.TeslimTarihi " +
                    "FROM ODUNC o " +
                    "JOIN UYE u ON o.UyeID = u.UyeID " +
                    "JOIN KITAP k ON o.KitapID = k.KitapID " +
                    "WHERE " + tabloSutun + " LIKE ?";

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, "%" + txtArama.getText() + "%");

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String durum = rs.getDate("TeslimTarihi") == null ? "Teslim Edilmedi" : "İade Edildi";
                model.addRow(new Object[]{
                        rs.getInt("OduncID"), rs.getString("Ad"),
                        rs.getString("KitapAdi"), rs.getDate("OduncTarihi"), durum
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}