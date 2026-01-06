import javax.swing.*;
import java.awt.*;


public class AnaMenu extends JFrame {
    private String aktifKullanici;

    public AnaMenu(String kullaniciAdi) {
        this.aktifKullanici = kullaniciAdi;

        setTitle("Kütüphane Yönetim Sistemi - Ana Menü");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel pnlUst = new JPanel();
        pnlUst.setBackground(new Color(45, 45, 45));
        JLabel lblHosgeldin = new JLabel("Hoş geldiniz, [" + aktifKullanici + "]");
        lblHosgeldin.setForeground(Color.WHITE);
        lblHosgeldin.setFont(new Font("Arial", Font.BOLD, 16));
        pnlUst.add(lblHosgeldin);
        add(pnlUst, BorderLayout.NORTH);

        JPanel pnlButonlar = new JPanel(new GridLayout(4, 2, 10, 10));
        pnlButonlar.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton btnUye = new JButton("Üye Yönetimi");
        JButton btnKitap = new JButton("Kitap Yönetimi");
        JButton btnOduncVer = new JButton("Ödünç Verme İşlemi");
        JButton btnTeslimAl = new JButton("Kitap Teslim Alma");
        JButton btnCezalar = new JButton("Ceza Görüntüleme");
        JButton btnRaporlar = new JButton("İstatistik ve Raporlar");
        JButton btnDinamik = new JButton("Dinamik Sorgu Ekranı");
        JButton btnCikis = new JButton("Güvenli Çıkış");

        pnlButonlar.add(btnUye);
        pnlButonlar.add(btnKitap);
        pnlButonlar.add(btnOduncVer);
        pnlButonlar.add(btnTeslimAl);
        pnlButonlar.add(btnCezalar);
        pnlButonlar.add(btnRaporlar);
        pnlButonlar.add(btnDinamik);
        pnlButonlar.add(btnCikis);

        // button tasks
        btnUye.addActionListener(e ->
                new UyeYonetimi().setVisible(true));

        btnKitap.addActionListener(e ->
                new KitapYonetimi().setVisible(true));

        btnOduncVer.addActionListener(e ->
                new OduncVerme().setVisible(true));

        btnTeslimAl.addActionListener(e ->
                new KitapTeslim().setVisible(true));

        btnCezalar.addActionListener(e ->
                new CezaGoruntuleme().setVisible(true));

        btnRaporlar.addActionListener(e ->
                new Raporlar().setVisible(true));

        btnDinamik.addActionListener(e ->
                new DinamikSorgu().setVisible(true));

        btnCikis.addActionListener(e -> {
            new GirisEkrani().setVisible(true);
            this.dispose();
        });

        add(pnlButonlar, BorderLayout.CENTER);
    }
}