import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class KitapTeslim extends JFrame {
    private JTextField txtOduncID = new JTextField();

    public KitapTeslim() {
        setTitle("Kitap Teslim Al");
        setSize(300, 150);
        setLayout(new GridLayout(2, 2));
        setLocationRelativeTo(null);

        add(new JLabel("Ödünç ID:")); add(txtOduncID);
        JButton btnTeslim = new JButton("Teslim Al");
        add(btnTeslim);

        btnTeslim.addActionListener(e -> {
            try (Connection conn = Veritabani.baglan()) {
                CallableStatement cs = conn.prepareCall("{call sp_KitapTeslimAl(?, ?)}");
                cs.setInt(1, Integer.parseInt(txtOduncID.getText()));
                cs.setDate(2, new java.sql.Date(System.currentTimeMillis()));
                cs.execute();
                JOptionPane.showMessageDialog(this, "Kitap Teslim Alındı!");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Hata: " + ex.getMessage());
            }
        });
    }
}