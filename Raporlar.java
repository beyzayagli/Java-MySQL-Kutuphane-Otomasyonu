import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class Raporlar extends JFrame {
    private JTable tablo;
    private DefaultTableModel model;

    public Raporlar() {
        setTitle("Kütüphane İstatistik ve Raporları");
        setSize(750, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // buttons ve üye ozeti için giris alani
        JPanel pnlUst = new JPanel();
        JButton btnGecikenler = new JButton("Geciken Kitaplar");
        JButton btnEnCok = new JButton("En Çok Alınanlar");

        JTextField txtUyeID = new JTextField(5);
        JButton btnUyeOzet = new JButton("Üye Özeti Getir (ID)");

        pnlUst.add(btnGecikenler);
        pnlUst.add(btnEnCok);
        pnlUst.add(new JLabel("  |  Uye ID:"));
        pnlUst.add(txtUyeID);
        pnlUst.add(btnUyeOzet);

        add(pnlUst, BorderLayout.NORTH);

        // tablo kurulumu
        model = new DefaultTableModel();
        tablo = new JTable(model);
        add(new JScrollPane(tablo), BorderLayout.CENTER);

        // button events
        btnGecikenler.addActionListener(e -> gecikenleriGetir());
        btnEnCok.addActionListener(e -> enCokAlinanlar());

        // saklı yordamı çağıran button
        btnUyeOzet.addActionListener(e -> {
            if (!txtUyeID.getText().isEmpty()) {
                int id = Integer.parseInt(txtUyeID.getText());
                uyeOzetiniGetir(id);
            } else {
                JOptionPane.showMessageDialog(this, "Lütfen bir Üye ID girin!");
            }
        });
    }

    // sp_UyeOzetRapor saklı yordamını kullanan metod
    private void uyeOzetiniGetir(int uyeID) {
        model.setColumnIdentifiers(new String[]{"Üye Bilgisi", "Kitap Adı", "Ödünç Tarihi", "Toplam Borç"});
        model.setRowCount(0);

        try (Connection conn = Veritabani.baglan()) {
            // Üye adını ve toplam borcu getir
            String uyeSql = "SELECT Ad, Soyad, ToplamBorc FROM UYE WHERE UyeID = ?";
            PreparedStatement psUye = conn.prepareStatement(uyeSql);
            psUye.setInt(1, uyeID);
            ResultSet rsUye = psUye.executeQuery();

            String tamAd = "";
            String borc = "0.0 TL";
            if (rsUye.next()) {
                tamAd = rsUye.getString("Ad") + " " + rsUye.getString("Soyad");
                borc = rsUye.getString("ToplamBorc") + " TL";
            }

            // Elindeki kitapları getir
            String kitapSql = "SELECT k.KitapAdi, o.OduncTarihi FROM ODUNC o " +
                    "JOIN KITAP k ON o.KitapID = k.KitapID " +
                    "WHERE o.UyeID = ? AND o.TeslimTarihi IS NULL";
            PreparedStatement psKitap = conn.prepareStatement(kitapSql);
            psKitap.setInt(1, uyeID);
            ResultSet rsKitap = psKitap.executeQuery();

            boolean kitapVar = false;
            while (rsKitap.next()) {
                model.addRow(new Object[]{tamAd, rsKitap.getString("KitapAdi"), rsKitap.getDate("OduncTarihi"), borc});
                kitapVar = true;
            }

            if (!kitapVar) {
                model.addRow(new Object[]{tamAd, "Elde Kitap Yok", "-", borc});
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    // geciken kitaplar raporu
    private void gecikenleriGetir() {
        model.setColumnIdentifiers(new String[]{"Ödünç ID", "Üye Adı", "Kitap Adı", "Son Teslim", "Durum"});
        model.setRowCount(0);

        try (Connection conn = Veritabani.baglan()) {
            // SELECT ve JOIN yapısı
            String sql = "SELECT o.OduncID, u.Ad, k.KitapAdi, o.SonTeslimTarihi " +
                    "FROM ODUNC o " +
                    "JOIN UYE u ON o.UyeID = u.UyeID " +
                    "JOIN KITAP k ON o.KitapID = k.KitapID " +
                    "WHERE o.TeslimTarihi IS NULL AND o.SonTeslimTarihi < CURRENT_DATE";

            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("OduncID"),
                        rs.getString("Ad"),
                        rs.getString("KitapAdi"),
                        rs.getDate("SonTeslimTarihi"),
                        "GECİKTİ!"
                });
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void enCokAlinanlar() {
        model.setColumnIdentifiers(new String[]{"Kitap Adı", "Ödünç Sayısı"});
        model.setRowCount(0);

        try (Connection conn = Veritabani.baglan()) {
            String sql = "SELECT k.KitapAdi, COUNT(o.KitapID) as Sayi " +
                    "FROM ODUNC o " +
                    "JOIN KITAP k ON o.KitapID = k.KitapID " +
                    "GROUP BY k.KitapAdi " +
                    "ORDER BY Sayi DESC";

            ResultSet rs = conn.createStatement().executeQuery(sql);
            while (rs.next()) {
                model.addRow(new Object[]{rs.getString(1), rs.getInt(2)});
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }


}