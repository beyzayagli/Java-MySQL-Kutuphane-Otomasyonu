-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Anamakine: 127.0.0.1
-- Üretim Zamanı: 01 Oca 2026, 00:02:05
-- Sunucu sürümü: 10.4.32-MariaDB
-- PHP Sürümü: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Veritabanı: `kutuphanesistemi`
--

DELIMITER $$
--
-- Yordamlar
--
DROP PROCEDURE IF EXISTS `sp_GecikenKitaplariGetir`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_GecikenKitaplariGetir` ()   BEGIN
    SELECT 
        o.OduncID,
        u.Ad, 
        k.KitapAdi, 
        o.SonTeslimTarihi, 
        'GECİKTİ!' AS Durum
    FROM ODUNC o
    JOIN UYE u ON o.UyeID = u.UyeID
    JOIN KITAP k ON o.KitapID = k.KitapID
    WHERE o.TeslimTarihi IS NULL AND o.SonTeslimTarihi < CURDATE();
END$$

DROP PROCEDURE IF EXISTS `sp_KitapAra`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_KitapAra` (IN `p_Kelime` VARCHAR(50))   BEGIN
    SELECT * FROM KITAP 
    WHERE KitapAdi LIKE CONCAT('%', p_Kelime, '%') 
    OR Yazar LIKE CONCAT('%', p_Kelime, '%');
END$$

DROP PROCEDURE IF EXISTS `sp_KitapEkleVeyaGuncelle`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_KitapEkleVeyaGuncelle` (IN `p_Ad` VARCHAR(100), IN `p_Yazar` VARCHAR(100), IN `p_Yil` INT, IN `p_Adet` INT)   BEGIN
    INSERT INTO KITAP (KitapAdi, Yazar, BasimYili, ToplamAdet, MevcutAdet) 
    VALUES (p_Ad, p_Yazar, p_Yil, p_Adet, p_Adet);
END$$

DROP PROCEDURE IF EXISTS `sp_KitapTeslimAl`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_KitapTeslimAl` (IN `p_OduncID` INT, IN `p_TeslimTarihi` DATE)   BEGIN
    DECLARE v_UyeID INT;
    DECLARE v_KitapID INT;
    DECLARE v_SonTeslim DATE;
    DECLARE v_MevcutTeslim DATE;
    DECLARE v_GunFark INT;
    DECLARE v_CezaTutar DECIMAL(10,2);

    -- Değişkenleri doğru şekilde çekiyoruz
    SELECT UyeID, KitapID, SonTeslimTarihi, TeslimTarihi 
    INTO v_UyeID, v_KitapID, v_SonTeslim, v_MevcutTeslim
    FROM ODUNC WHERE OduncID = p_OduncID;

    -- KONTROL: Eğer kitap zaten teslim edilmişse işlemi durdur
    IF v_MevcutTeslim IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'Hata: Bu kitap zaten teslim alinmis!';
    ELSE
        -- 1. Teslim tarihini tabloya işle
        UPDATE ODUNC SET TeslimTarihi = p_TeslimTarihi WHERE OduncID = p_OduncID;
        
        -- 2. Kitap stok adedini 1 artır
        UPDATE KITAP SET MevcutAdet = MevcutAdet + 1 WHERE KitapID = v_KitapID;

        -- 3. Ceza hesapla (Gün başına 10 TL)
        SET v_GunFark = DATEDIFF(p_TeslimTarihi, v_SonTeslim);
        
        IF v_GunFark > 0 THEN
            SET v_CezaTutar = v_GunFark * 10.00;
            
            INSERT INTO CEZA (OduncID, UyeID, Tutar) VALUES (p_OduncID, v_UyeID, v_CezaTutar);
            
            -- Üyenin toplam borç miktarını güncelle
            UPDATE UYE SET ToplamBorc = ToplamBorc + v_CezaTutar WHERE UyeID = v_UyeID;
        END IF;
    END IF;
END$$

DROP PROCEDURE IF EXISTS `sp_UyeOzetRapor`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_UyeOzetRapor` (IN `p_UyeID` INT)   BEGIN
    SELECT 
        (SELECT COUNT(*) FROM odunc WHERE UyeID = p_UyeID) AS ToplamAlinanKitap,
        (SELECT COUNT(*) FROM odunc WHERE UyeID = p_UyeID AND TeslimTarihi IS NULL) AS EldekiKitapSayisi,
        (SELECT IFNULL(SUM(Tutar), 0) FROM ceza WHERE UyeID = p_UyeID) AS ToplamCezaTutari;
END$$

DROP PROCEDURE IF EXISTS `sp_YeniOduncVer`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_YeniOduncVer` (IN `p_UyeID` INT, IN `p_KitapID` INT, IN `p_KullaniciID` INT)   BEGIN
    DECLARE aktif_kitap_sayisi INT;
    DECLARE stok_durumu INT;

    -- Üyenin elindeki kitap sayısını kontrol et
    SELECT COUNT(*) INTO aktif_kitap_sayisi 
    FROM ODUNC 
    WHERE UyeID = p_UyeID AND TeslimTarihi IS NULL;

    -- Kitabın stokta olup olmadığını kontrol et
    SELECT MevcutAdet INTO stok_durumu 
    FROM KITAP 
    WHERE KitapID = p_KitapID;

    -- KONTROLLER
    IF aktif_kitap_sayisi >= 5 THEN
        -- 5 kitap limiti kontrolü
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'HATA: Bu üye maksimum (5) kitap limitine ulaşmıştır!';
        
    ELSEIF stok_durumu <= 0 THEN
        -- Stok kontrolü
        SIGNAL SQLSTATE '45000' 
        SET MESSAGE_TEXT = 'HATA: Seçilen kitap kütüphane stoklarında kalmamıştır!';
        
    ELSE
        -- Her şey yolundaysa kaydı ekle
        INSERT INTO ODUNC (UyeID, KitapID, KullaniciID, OduncTarihi, SonTeslimTarihi)
        VALUES (p_UyeID, p_KitapID, p_KullaniciID, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 15 DAY));
        
        -- Not: Stok düşürme işlemini tetikleyicin (TR_ODUNC_INSERT) yapmaktadır.
    END IF;
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Tablo için tablo yapısı `ceza`
--

DROP TABLE IF EXISTS `ceza`;
CREATE TABLE `ceza` (
  `CezaID` int(11) NOT NULL,
  `OduncID` int(11) DEFAULT NULL,
  `UyeID` int(11) DEFAULT NULL,
  `Tutar` decimal(10,2) NOT NULL,
  `CezaTarihi` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Tablo döküm verisi `ceza`
--

INSERT INTO `ceza` (`CezaID`, `OduncID`, `UyeID`, `Tutar`, `CezaTarihi`) VALUES
(1, 2, 2, 60.00, '2025-12-31 17:14:13'),
(2, 22, 7, 50.00, '2025-12-31 17:25:36'),
(3, 5, 11, 360.00, '2025-12-31 17:25:41');

--
-- Tetikleyiciler `ceza`
--
DROP TRIGGER IF EXISTS `TR_CEZA_LOG`;
DELIMITER $$
CREATE TRIGGER `TR_CEZA_LOG` AFTER INSERT ON `ceza` FOR EACH ROW BEGIN
    -- Log tablosuna kayıt atılır
    INSERT INTO log_islem (TabloAdi, IslemTuru, Aciklama) 
    VALUES ('CEZA', 'INSERT', 'Yeni bir ceza kaydı eklendi.');
    
    -- Üyenin toplam borcu güncellenir
    UPDATE uye SET ToplamBorc = ToplamBorc + NEW.Tutar 
    WHERE UyeID = NEW.UyeID;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Tablo için tablo yapısı `kategori`
--

DROP TABLE IF EXISTS `kategori`;
CREATE TABLE `kategori` (
  `KategoriID` int(11) NOT NULL,
  `KategoriAd` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Tablo döküm verisi `kategori`
--

INSERT INTO `kategori` (`KategoriID`, `KategoriAd`) VALUES
(1, 'Bilim'),
(2, 'Edebiyat'),
(3, 'Tarih');

-- --------------------------------------------------------

--
-- Tablo için tablo yapısı `kitap`
--

DROP TABLE IF EXISTS `kitap`;
CREATE TABLE `kitap` (
  `KitapID` int(11) NOT NULL,
  `KitapAdi` varchar(100) NOT NULL,
  `Yazar` varchar(100) DEFAULT NULL,
  `KategoriID` int(11) DEFAULT NULL,
  `Yayinevi` varchar(100) DEFAULT NULL,
  `BasimYili` int(11) DEFAULT NULL,
  `ToplamAdet` int(11) NOT NULL,
  `MevcutAdet` int(11) NOT NULL,
  `Kategori` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Tablo döküm verisi `kitap`
--

INSERT INTO `kitap` (`KitapID`, `KitapAdi`, `Yazar`, `KategoriID`, `Yayinevi`, `BasimYili`, `ToplamAdet`, `MevcutAdet`, `Kategori`) VALUES
(1, 'Alleben Öyküleri', 'Ülkü Tamer', NULL, NULL, 1991, 10, 5, 'Edebiyat'),
(2, 'Yanardağın Üstündeki Kuş', 'Ülkü Tamer', NULL, NULL, 1994, 10, 9, 'Edebiyat'),
(3, 'Otuzların Kadını', 'Tomris Uyar', NULL, NULL, 1992, 20, 19, 'Edebiyat'),
(4, 'Canistan', 'Yusuf Atılgan', NULL, NULL, 2000, 10, 10, 'Edebiyat'),
(5, 'The Bell Jar', 'Sylvia Plath', NULL, NULL, 1963, 20, 20, 'Edebiyat'),
(6, 'Kendine Ait Bir Oda', 'Sylvia Plath', NULL, NULL, 1929, 10, 9, 'Edebiyat'),
(7, 'Bilgisayar Sistemleri Mimarisi', 'M. Morris Mano', NULL, NULL, 1976, 10, 8, 'Bilim'),
(8, 'Control Theory for Engineers', 'Michel De Lara', NULL, NULL, 2013, 8, 7, 'Bilim'),
(9, 'Topology Optimization Theory for Laminar Flow', 'Zhenyu Liu', NULL, NULL, 2018, 5, 5, 'Bilim'),
(10, 'Genel Topoloji', 'Şaziye Yüksel', NULL, NULL, 1998, 5, 0, 'Bilim'),
(11, 'Wildfell Konağı Kiracısı', 'Anne Bronte', NULL, NULL, 1848, 10, 10, 'Roman'),
(12, 'Herland', 'Charlotte Perkins Gilman', NULL, NULL, 1915, 10, 10, 'Roman'),
(13, 'Cadı: Garaib Faturası Külliyatı', 'Hüseyin Rahmi Gürpınar', NULL, NULL, 1912, 9, 8, 'Roman'),
(14, 'Gulyabani: Garaib Faturası Külliyatı', 'Hüseyin Rahmi Gürpınar', NULL, NULL, 1913, 9, 7, 'Roman');

-- --------------------------------------------------------

--
-- Tablo için tablo yapısı `kullanici`
--

DROP TABLE IF EXISTS `kullanici`;
CREATE TABLE `kullanici` (
  `KullaniciID` int(11) NOT NULL,
  `KullaniciAdi` varchar(50) NOT NULL,
  `Sifre` varchar(50) NOT NULL,
  `Rol` enum('Admin','Gorevli') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Tablo döküm verisi `kullanici`
--

INSERT INTO `kullanici` (`KullaniciID`, `KullaniciAdi`, `Sifre`, `Rol`) VALUES
(1, 'admin', '1234', 'Admin'),
(2, 'BeyzaY', '123', 'Gorevli'),
(3, 'BetulY', '123', 'Gorevli');

-- --------------------------------------------------------

--
-- Tablo için tablo yapısı `log_islem`
--

DROP TABLE IF EXISTS `log_islem`;
CREATE TABLE `log_islem` (
  `LogID` int(11) NOT NULL,
  `TabloAdi` varchar(50) DEFAULT NULL,
  `IslemTuru` varchar(20) DEFAULT NULL,
  `IslemZamani` timestamp NOT NULL DEFAULT current_timestamp(),
  `Aciklama` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Tablo döküm verisi `log_islem`
--

INSERT INTO `log_islem` (`LogID`, `TabloAdi`, `IslemTuru`, `IslemZamani`, `Aciklama`) VALUES
(1, 'ODUNC', 'INSERT', '2025-12-31 14:42:06', 'Uye 1 kitap aldi.'),
(2, 'ODUNC', 'INSERT', '2025-12-31 14:51:42', 'Uye 2 kitap aldi.'),
(3, 'ODUNC', 'INSERT', '2025-12-31 14:51:50', 'Uye 1 kitap aldi.'),
(4, 'ODUNC', 'INSERT', '2025-12-31 14:51:58', 'Uye 4 kitap aldi.'),
(5, 'ODUNC', 'INSERT', '2025-12-31 14:53:39', 'Uye 11 kitap aldi.'),
(6, 'ODUNC', 'INSERT', '2025-12-31 14:53:46', 'Uye 6 kitap aldi.'),
(7, 'ODUNC', 'INSERT', '2025-12-31 14:53:51', 'Uye 6 kitap aldi.'),
(8, 'ODUNC', 'INSERT', '2025-12-31 14:53:55', 'Uye 6 kitap aldi.'),
(9, 'ODUNC', 'INSERT', '2025-12-31 14:54:07', 'Uye 5 kitap aldi.'),
(10, 'ODUNC', 'INSERT', '2025-12-31 14:54:15', 'Uye 5 kitap aldi.'),
(11, 'ODUNC', 'INSERT', '2025-12-31 14:54:18', 'Uye 5 kitap aldi.'),
(12, 'ODUNC', 'INSERT', '2025-12-31 14:54:31', 'Uye 12 kitap aldi.'),
(13, 'ODUNC', 'INSERT', '2025-12-31 14:54:36', 'Uye 12 kitap aldi.'),
(14, 'ODUNC', 'INSERT', '2025-12-31 14:54:43', 'Uye 1 kitap aldi.'),
(15, 'ODUNC', 'INSERT', '2025-12-31 14:54:48', 'Uye 3 kitap aldi.'),
(16, 'ODUNC', 'INSERT', '2025-12-31 14:54:53', 'Uye 10 kitap aldi.'),
(17, 'ODUNC', 'INSERT', '2025-12-31 14:54:57', 'Uye 10 kitap aldi.'),
(18, 'ODUNC', 'INSERT', '2025-12-31 14:59:12', 'Uye 6 kitap aldi.'),
(19, 'ODUNC', 'INSERT', '2025-12-31 14:59:18', 'Uye 6 kitap aldi.'),
(20, 'ODUNC', 'INSERT', '2025-12-31 14:59:25', 'Uye 6 kitap aldi.'),
(21, 'ODUNC', 'INSERT', '2025-12-31 15:12:53', 'Uye 12 kitap aldi.'),
(22, 'ODUNC', 'INSERT', '2025-12-31 16:10:07', 'Uye 7 kitap aldi.'),
(23, 'ODUNC', 'UPDATE', '2025-12-31 17:14:13', 'ID 2 iade edildi.'),
(24, 'CEZA', 'INSERT', '2025-12-31 17:14:13', 'Yeni bir ceza kaydı eklendi.'),
(25, 'ODUNC', 'UPDATE', '2025-12-31 17:25:36', 'ID 22 iade edildi.'),
(26, 'CEZA', 'INSERT', '2025-12-31 17:25:36', 'Yeni bir ceza kaydı eklendi.'),
(27, 'ODUNC', 'UPDATE', '2025-12-31 17:25:41', 'ID 5 iade edildi.'),
(28, 'CEZA', 'INSERT', '2025-12-31 17:25:41', 'Yeni bir ceza kaydı eklendi.'),
(29, 'ODUNC', 'INSERT', '2025-12-31 17:39:23', 'Uye 2 kitap aldi.'),
(30, 'ODUNC', 'INSERT', '2025-12-31 17:39:56', 'Uye 1 kitap aldi.'),
(31, 'ODUNC', 'INSERT', '2025-12-31 17:40:30', 'Uye 5 kitap aldi.'),
(32, 'ODUNC', 'INSERT', '2025-12-31 17:40:48', 'Uye 7 kitap aldi.'),
(33, 'ODUNC', 'INSERT', '2025-12-31 17:40:58', 'Uye 8 kitap aldi.'),
(34, 'ODUNC', 'INSERT', '2025-12-31 17:41:06', 'Uye 9 kitap aldi.'),
(35, 'ODUNC', 'INSERT', '2025-12-31 17:43:52', 'Uye 9 kitap aldi.'),
(36, 'ODUNC', 'INSERT', '2025-12-31 17:52:14', 'Uye 1 kitap aldi.'),
(37, 'ODUNC', 'INSERT', '2025-12-31 17:52:24', 'Uye 2 kitap aldi.'),
(38, 'ODUNC', 'INSERT', '2025-12-31 17:52:31', 'Uye 8 kitap aldi.'),
(39, 'ODUNC', 'INSERT', '2025-12-31 17:52:39', 'Uye 9 kitap aldi.'),
(40, 'ODUNC', 'INSERT', '2025-12-31 17:52:49', 'Uye 7 kitap aldi.'),
(41, 'ODUNC', 'INSERT', '2025-12-31 17:53:19', 'Uye 11 kitap aldi.'),
(42, 'ODUNC', 'INSERT', '2025-12-31 18:01:21', 'Uye 1 kitap aldi.'),
(43, 'ODUNC', 'INSERT', '2025-12-31 18:01:25', 'Uye 2 kitap aldi.'),
(44, 'ODUNC', 'INSERT', '2025-12-31 18:01:31', 'Uye 8 kitap aldi.'),
(45, 'ODUNC', 'INSERT', '2025-12-31 18:01:38', 'Uye 7 kitap aldi.'),
(46, 'ODUNC', 'INSERT', '2025-12-31 18:14:27', 'Uye 1 kitap aldi.'),
(47, 'ODUNC', 'INSERT', '2025-12-31 18:14:52', 'Uye 1 kitap aldi.'),
(48, 'ODUNC', 'INSERT', '2025-12-31 18:14:59', 'Uye 2 kitap aldi.'),
(49, 'ODUNC', 'INSERT', '2025-12-31 18:15:06', 'Uye 7 kitap aldi.'),
(50, 'ODUNC', 'INSERT', '2025-12-31 18:15:13', 'Uye 8 kitap aldi.');

-- --------------------------------------------------------

--
-- Tablo için tablo yapısı `odunc`
--

DROP TABLE IF EXISTS `odunc`;
CREATE TABLE `odunc` (
  `OduncID` int(11) NOT NULL,
  `UyeID` int(11) DEFAULT NULL,
  `KitapID` int(11) DEFAULT NULL,
  `KullaniciID` int(11) DEFAULT NULL,
  `OduncTarihi` date NOT NULL,
  `SonTeslimTarihi` date NOT NULL,
  `TeslimTarihi` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Tablo döküm verisi `odunc`
--

INSERT INTO `odunc` (`OduncID`, `UyeID`, `KitapID`, `KullaniciID`, `OduncTarihi`, `SonTeslimTarihi`, `TeslimTarihi`) VALUES
(1, 1, 7, 1, '2025-12-31', '2026-01-15', NULL),
(2, 2, 7, 1, '2025-12-10', '2025-12-25', '2025-12-31'),
(3, 1, 5, 1, '2025-12-31', '2026-01-15', NULL),
(4, 4, 10, 1, '2025-12-31', '2026-01-15', NULL),
(5, 11, 9, 1, '2025-11-10', '2025-11-25', '2025-12-31'),
(6, 6, 9, 1, '2025-12-31', '2026-01-15', NULL),
(7, 6, 13, 1, '2025-12-31', '2026-01-15', NULL),
(8, 6, 14, 1, '2025-12-31', '2026-01-15', NULL),
(9, 5, 8, 1, '2025-12-31', '2026-01-15', NULL),
(10, 5, 7, 1, '2025-12-31', '2026-01-15', NULL),
(11, 5, 2, 1, '2025-12-31', '2026-01-15', NULL),
(12, 12, 3, 1, '2025-11-30', '2025-12-15', NULL),
(13, 12, 1, 1, '2025-12-31', '2026-01-15', NULL),
(14, 1, 1, 1, '2025-12-31', '2026-01-15', NULL),
(15, 3, 1, 1, '2025-12-31', '2026-01-15', NULL),
(16, 10, 1, 1, '2025-12-01', '2025-12-16', NULL),
(17, 10, 5, 1, '2025-12-31', '2026-01-15', NULL),
(18, 6, 1, 1, '2025-12-10', '2025-12-25', NULL),
(19, 6, 7, 1, '2025-12-31', '2026-01-15', NULL),
(22, 7, 5, 1, '2025-12-11', '2025-12-26', '2025-12-31'),
(40, 1, 6, 1, '2025-12-31', '2026-01-15', NULL),
(41, 1, 10, 1, '2025-12-31', '2026-01-15', NULL),
(42, 2, 10, 1, '2025-12-31', '2026-01-15', NULL),
(43, 7, 10, 1, '2025-12-31', '2026-01-15', NULL),
(44, 8, 10, 1, '2025-12-31', '2026-01-15', NULL);

--
-- Tetikleyiciler `odunc`
--
DROP TRIGGER IF EXISTS `TR_ODUNC_INSERT`;
DELIMITER $$
CREATE TRIGGER `TR_ODUNC_INSERT` AFTER INSERT ON `odunc` FOR EACH ROW BEGIN
    -- İşlem log kaydını oluştur
    INSERT INTO LOG_ISLEM (TabloAdi, IslemTuru, Aciklama) 
    VALUES ('ODUNC', 'INSERT', CONCAT('Uye ', NEW.UyeID, ' kitap aldi.'));

    -- kitabın stok sayısını otomatik 1 azalt
    UPDATE KITAP 
    SET MevcutAdet = MevcutAdet - 1 
    WHERE KitapID = NEW.KitapID;
END
$$
DELIMITER ;
DROP TRIGGER IF EXISTS `TR_ODUNC_UPDATE_TESLIM`;
DELIMITER $$
CREATE TRIGGER `TR_ODUNC_UPDATE_TESLIM` AFTER UPDATE ON `odunc` FOR EACH ROW BEGIN
    IF OLD.TeslimTarihi IS NULL AND NEW.TeslimTarihi IS NOT NULL THEN
        UPDATE KITAP SET MevcutAdet = MevcutAdet + 1 WHERE KitapID = NEW.KitapID;
        INSERT INTO LOG_ISLEM (TabloAdi, IslemTuru, Aciklama) 
        VALUES ('ODUNC', 'UPDATE', CONCAT('ID ', NEW.OduncID, ' iade edildi.'));
    END IF;
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Tablo için tablo yapısı `uye`
--

DROP TABLE IF EXISTS `uye`;
CREATE TABLE `uye` (
  `UyeID` int(11) NOT NULL,
  `Ad` varchar(50) NOT NULL,
  `Soyad` varchar(50) NOT NULL,
  `Telefon` varchar(15) DEFAULT NULL,
  `Email` varchar(100) DEFAULT NULL,
  `ToplamBorc` decimal(10,2) DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Tablo döküm verisi `uye`
--

INSERT INTO `uye` (`UyeID`, `Ad`, `Soyad`, `Telefon`, `Email`, `ToplamBorc`) VALUES
(1, 'Beyza', 'Yağlı', '555-055-00-55', 'yagli1@bil.omu.edu.tr', 0.00),
(2, 'Betül', 'Yağlı', '555-005-55-55', 'yagli2@bil.omu.edu.tr', 120.00),
(3, 'Burkut', 'Kum', '506-006-00-06', 'kum@bil.omu.edu.tr', 0.00),
(4, 'Şamil', 'Aykaç', '536-036-00-36', 'aykac@bil.omu.edu.tr', 0.00),
(5, 'Abrek', 'Yağlı', '519-019-19-19', 'yagli3@bil.omu.edu.tr', 0.00),
(6, 'Janberk', 'Öztürk', '536-003-36-36', 'ozturk@bil.omu.edu.tr', 0.00),
(7, 'Luca', 'Fritz', '534-034-00-34', 'fritz@eem.omu.edu.tr', 100.00),
(8, 'duru', 'eken', '542-042-42-42', 'eken@bil.omu.edu.tr', 0.00),
(9, 'Elanur', 'Demircioğlu', '534-334-34-34', 'demirciogl@bil.omu.edu.tr', 0.00),
(10, 'Merve Sıla', 'Akyol', '552-052-52-52', 'akyol@bil.omu.edu.tr', 0.00),
(11, 'Mustafa', 'Bozkurt', '557-057-00-57', 'bozkurt@bil.omu.edu.tr', 720.00),
(12, 'Sinemis', 'Kurtulmuş', '536-360-66-36', 'kurtulmus@bil.omu.edu.tr', 0.00);

--
-- Tetikleyiciler `uye`
--
DROP TRIGGER IF EXISTS `TR_UYE_SILME_KONTROL`;
DELIMITER $$
CREATE TRIGGER `TR_UYE_SILME_KONTROL` BEFORE DELETE ON `uye` FOR EACH ROW BEGIN
    IF OLD.ToplamBorc > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Hata: Borcu olan üye silinemez!';
    END IF;
END
$$
DELIMITER ;

--
-- Dökümü yapılmış tablolar için indeksler
--

--
-- Tablo için indeksler `ceza`
--
ALTER TABLE `ceza`
  ADD PRIMARY KEY (`CezaID`),
  ADD KEY `OduncID` (`OduncID`),
  ADD KEY `UyeID` (`UyeID`);

--
-- Tablo için indeksler `kategori`
--
ALTER TABLE `kategori`
  ADD PRIMARY KEY (`KategoriID`);

--
-- Tablo için indeksler `kitap`
--
ALTER TABLE `kitap`
  ADD PRIMARY KEY (`KitapID`),
  ADD KEY `KategoriID` (`KategoriID`);

--
-- Tablo için indeksler `kullanici`
--
ALTER TABLE `kullanici`
  ADD PRIMARY KEY (`KullaniciID`),
  ADD UNIQUE KEY `KullaniciAdi` (`KullaniciAdi`);

--
-- Tablo için indeksler `log_islem`
--
ALTER TABLE `log_islem`
  ADD PRIMARY KEY (`LogID`);

--
-- Tablo için indeksler `odunc`
--
ALTER TABLE `odunc`
  ADD PRIMARY KEY (`OduncID`),
  ADD KEY `UyeID` (`UyeID`),
  ADD KEY `KitapID` (`KitapID`),
  ADD KEY `KullaniciID` (`KullaniciID`);

--
-- Tablo için indeksler `uye`
--
ALTER TABLE `uye`
  ADD PRIMARY KEY (`UyeID`);

--
-- Dökümü yapılmış tablolar için AUTO_INCREMENT değeri
--

--
-- Tablo için AUTO_INCREMENT değeri `ceza`
--
ALTER TABLE `ceza`
  MODIFY `CezaID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- Tablo için AUTO_INCREMENT değeri `kategori`
--
ALTER TABLE `kategori`
  MODIFY `KategoriID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- Tablo için AUTO_INCREMENT değeri `kitap`
--
ALTER TABLE `kitap`
  MODIFY `KitapID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- Tablo için AUTO_INCREMENT değeri `kullanici`
--
ALTER TABLE `kullanici`
  MODIFY `KullaniciID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- Tablo için AUTO_INCREMENT değeri `log_islem`
--
ALTER TABLE `log_islem`
  MODIFY `LogID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=51;

--
-- Tablo için AUTO_INCREMENT değeri `odunc`
--
ALTER TABLE `odunc`
  MODIFY `OduncID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=45;

--
-- Tablo için AUTO_INCREMENT değeri `uye`
--
ALTER TABLE `uye`
  MODIFY `UyeID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- Dökümü yapılmış tablolar için kısıtlamalar
--

--
-- Tablo kısıtlamaları `ceza`
--
ALTER TABLE `ceza`
  ADD CONSTRAINT `ceza_ibfk_1` FOREIGN KEY (`OduncID`) REFERENCES `odunc` (`OduncID`),
  ADD CONSTRAINT `ceza_ibfk_2` FOREIGN KEY (`UyeID`) REFERENCES `uye` (`UyeID`);

--
-- Tablo kısıtlamaları `kitap`
--
ALTER TABLE `kitap`
  ADD CONSTRAINT `kitap_ibfk_1` FOREIGN KEY (`KategoriID`) REFERENCES `kategori` (`KategoriID`);

--
-- Tablo kısıtlamaları `odunc`
--
ALTER TABLE `odunc`
  ADD CONSTRAINT `odunc_ibfk_1` FOREIGN KEY (`UyeID`) REFERENCES `uye` (`UyeID`),
  ADD CONSTRAINT `odunc_ibfk_2` FOREIGN KEY (`KitapID`) REFERENCES `kitap` (`KitapID`),
  ADD CONSTRAINT `odunc_ibfk_3` FOREIGN KEY (`KullaniciID`) REFERENCES `kullanici` (`KullaniciID`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
