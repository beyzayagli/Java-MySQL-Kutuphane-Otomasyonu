import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class CezaGoruntuleme extends JFrame {
    public CezaGoruntuleme() {
        setTitle("Cezalar");
        setSize(500, 300);
        setLocationRelativeTo(null);

        DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Uye ID", "Ad Soyad", "Tutar"}, 0);
        JTable tablo = new JTable(model);
        add(new JScrollPane(tablo));

        try (Connection conn = Veritabani.baglan()) {
            String sql = "SELECT CEZA.*, UYE.Ad, UYE.Soyad FROM CEZA JOIN UYE ON CEZA.UyeID = UYE.UyeID";
            ResultSet rs = conn.createStatement().executeQuery(sql);

            while (rs.next()) {
                int id = rs.getInt("CezaID");
                int uyeId = rs.getInt("UyeID");
                String adSoyad = rs.getString("Ad") + " " + rs.getString("Soyad");
                double tutar = rs.getDouble("Tutar");

                model.addRow(new Object[]{id, uyeId, adSoyad, tutar});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
}