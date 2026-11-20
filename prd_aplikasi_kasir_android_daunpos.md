# Product Requirements Document (PRD): DaunPOS (Aplikasi Kasir Cafe Sederhana)

> Dokumen ini adalah PRD awal dengan fokus F&B. Scope produk terbaru untuk POS personal multi-kategori dan offline-first berada di docs/PRODUCT_SCOPE_PERSONAL_POS.md. Jika terdapat perbedaan, dokumen scope terbaru menjadi acuan.

## 1. Gambaran Umum Produk (Overview)
**DaunPOS** adalah aplikasi kasir berbasis Android yang dirancang khusus untuk operasional kafe kecil dan menengah. Aplikasi ini mengutamakan kecepatan transaksi, tampilan antarmuka (UI/UX) yang bersih, modern, dan ramah pengguna (user-friendly), serta dilengkapi dengan fitur utama laporan penjualan yang mendalam dan integrasi pencetakan struk yang mudah.

*   **Platform:** Android (Smartphone & Tablet)
*   **Gaya Desain UI/UX:** Modern, minimalis ala kafe kekinian (Clean & Cozy).
*   **Warna Utama (Primary Color):** Hijau Muda (Sage Green / Mint Green, e.g., `#81C784` atau `#A8E6CF` dengan aksen putih dan abu-abu terang).

---

## 2. Target Pengguna (Target Users)
1.  **Kasir / Barista:** Membutuhkan pencatatan pesanan yang cepat, minim kesalahan saat jam sibuk, serta kemudahan mencetak struk pesanan.
2.  **Pemilik Kafe (Owner):** Membutuhkan laporan keuangan dan penjualan harian/bulanan yang akurat serta mudah dibaca untuk memantau performa bisnis.

---

## 3. Fitur Utama (Core Features)
1.  **Sistem Kasir (POS / Order Management):** Pemilihan kategori, menu, keranjang belanja, dan kalkulasi total otomatis.
2.  **Laporan Transaksi Penjualan:** Grafik ringkasan pendapatan, riwayat transaksi, filter berdasarkan tanggal, dan detail produk terjual.
3.  **Manajemen Produk/Menu:** Tambah, ubah, hapus menu, serta pengaturan status stok (Tersedia/Habis).
4.  **Pembayaran Multi-Metode:** Tunai, QRIS, dan Transfer Bank.
5.  **Integrasi & Pencetakan Struk (Thermal Printer):** Koneksi printer via Bluetooth/USB/LAN, kustomisasi header/footer struk, cetak otomatis, dan cetak ulang (reprint) dari riwayat transaksi.

---

## 4. Detail UI/UX Tiap Halaman

### Halaman 1: Halaman Utama Kasir (POS Screen)
Halaman ini adalah pusat operasional bagi kasir atau barista untuk melayani pesanan pelanggan.

*   **Layout & Komponen UI:**
    *   **Header:** Menampilkan logo kafe "DaunPOS", nama kasir yang bertugas, indikator status koneksi printer (ikon printer hijau jika terhubung, merah jika terputus), dan tombol profil/pengaturan di sudut kanan atas.
    *   **Kategori Produk (Tab Horizontal):** Di bawah header terdapat tab kategori yang bisa digeser (Scrollable: *Semua, Kopi, Non-Kopi, Makanan Berat, Snack, Dessert*). Berwarna latar hijau muda lembut dengan teks tebal saat aktif.
    *   **Grid Menu Produk (Area Utama Kiri/Atas):** Menampilkan daftar menu dalam bentuk kartu grid (2-3 kolom di HP, 4 kolom di Tablet). Setiap kartu berisi:
        *   Foto produk (thumbnail persegi dengan sudut melengkung).
        *   Nama produk (maksimal 2 baris).
        *   Harga (format mata uang, misal: Rp25.000).
        *   Indikator stok habis (overlay abu-abu transparan jika stok 0).
    *   **Panel Keranjang Belanja (Area Kanan / Bottom Sheet):**
        *   Daftar item yang dipilih beserta kuantitas (`+` / `-`) dan tombol hapus (`🗑️`).
        *   Rincian pembayaran: Subtotal, Pajak/Ppn (jika ada), dan Total Akhir (ditandai dengan teks tebal dan warna hijau mencolok).
        *   Tombol Aksi Utama: Tombol besar berwarna hijau muda penuh bertuliskan **"Bayar Rp [Total]"**.

*   **Interaksi UX:**
    *   *Tap* pada kartu menu akan langsung memasukkannya ke keranjang atau menambah kuantitas jika sudah ada.
    *   *Tap* pada ikon status printer di header akan mengarahkan langsung ke halaman pengaturan printer jika belum terhubung.
    *   Animasi halus (micro-interaction) saat item ditambahkan ke keranjang.

---

### Halaman 2: Halaman Pembayaran / Checkout (Payment Modal)
Muncul sebagai modal/pop-up interaktif saat kasir menekan tombol "Bayar".

*   **Layout & Komponen UI:**
    *   **Ringkasan Tagihan:** Menampilkan total belanja di bagian atas dengan font besar.
    *   **Pilihan Metode Pembayaran (Tabs/Cards):**
        *   Tunai (Cash)
        *   QRIS (Dynamic QR Code)
        *   Transfer Bank
    *   **Bagian Tunai (Jika dipilih):**
        *   Input nominal uang dari pelanggan.
        *   Tombol cepat nominal uang (Uang Pas, Rp20.000, Rp50.000, Rp100.000) untuk mempercepat input.
        *   Informasi kembalian secara *real-time*.
    *   **Tombol Konfirmasi:** Tombol "Selesaikan Transaksi" berwarna hijau muda penuh di bagian bawah.

*   **Interaksi UX:**
    *   Setelah transaksi sukses, muncul animasi *Success Checkmark* hijau yang memuaskan, diikuti opsi **"Cetak Struk"** (jika cetak otomatis dimatikan) atau **"Kirim Struk Digital (WhatsApp)"**.

---

### Halaman 3: Halaman Laporan Transaksi Penjualan (Sales Report)
Fitur utama bagi pemilik kafe untuk memantau performa keuangan.

*   **Layout & Komponen UI:**
    *   **Filter Tanggal:** Dropdown atau pemilih tanggal di bagian atas (*Hari Ini, Minggu Ini, Bulan Ini, Kustom*).
    *   **Kartu Ringkasan (Summary Cards):** 3 kartu metrik utama dengan latar belakang putih berbayang tipis dan aksen hijau muda:
        1.  Total Pendapatan (Rp)
        2.  Jumlah Transaksi (Pesanan)
        3.  Menu Terlaris (Best Seller)
    *   **Grafik Penjualan:** Grafik batang/garis interaktif yang menunjukkan tren penjualan harian.
    *   **Daftar Riwayat Transaksi (Transaction History List):**
        *   List daftar transaksi terbaru di bawah grafik.
        *   Setiap baris berisi: Nomor Order (#INV-001), Waktu, Metode Pembayaran, dan Total Harga dengan badge status (Hijau = Sukses, Merah = Dibatalkan).

*   **Interaksi UX:**
    *   *Tap* pada salah satu riwayat transaksi akan membuka **Detail Transaksi Modal** yang berisi rincian item pesanan serta tombol **"Cetak Ulang Struk (Reprint)"** di bagian bawah modal.
    *   Tombol ekspor laporan (ikon panah unduh) di sudut kanan atas untuk mengunduh laporan dalam format PDF atau Excel.

---

### Halaman 4: Halaman Manajemen Menu (Product Management)
Digunakan oleh pemilik atau admin untuk mengelola produk kafe.

*   **Layout & Komponen UI:**
    *   **Search Bar:** Kolom pencarian menu di bagian atas untuk mencari nama produk dengan cepat.
    *   **Daftar Produk (List View):**
        *   Thumbnail foto produk kecil di kiri.
        *   Nama produk dan kategori.
        *   Harga jual.
        *   Toggle Switch (Tombol Geser) untuk status **Tersedia / Stok Habis**.
    *   **Floating Action Button (FAB):** Tombol bulat mengambang (`+`) di pojok kanan bawah berwarna hijau muda dengan ikon plus putih untuk menambah menu baru.

*   **Interaksi UX:**
    *   Menggeser toggle stok langsung mengubah status menu secara *real-time* di halaman kasir utama.

---

### Halaman 5: Halaman Pengaturan Printer & Struk (Printer & Receipt Settings)
Halaman khusus untuk menghubungkan perangkat kasir dengan mesin printer thermal dan mengatur tampilan struk belanja.

*   **Layout & Komponen UI:**
    *   **Kartu Status Koneksi:** Menampilkan nama printer yang sedang terhubung (misal: *RPP02N Bluetooth Printer*) dengan badge status hijau "Terhubung" dan tombol "Putuskan".
    *   **Pemindai Perangkat (Device Scanner):**
        *   Tab pilihan tipe koneksi: **Bluetooth**, **USB**, atau **LAN/Wi-Fi**.
        *   Tombol **"Cari Perangkat"** berwarna hijau muda dengan animasi *loading* radar saat memindai printer di sekitar.
        *   Daftar perangkat yang ditemukan beserta tombol "Hubungkan" di setiap baris.
    *   **Pengaturan Ukuran Kertas & Pencetakan:**
        *   Pilihan ukuran kertas thermal: **58mm** atau **80mm** (Radio button / Segmented control).
        *   Toggle Switch untuk **"Cetak Struk Otomatis"** setelah pembayaran berhasil.
        *   Toggle Switch untuk **"Buka Laci Kasir (Cash Drawer)"** otomatis saat mencetak struk tunai.
    *   **Kustomisasi Tampilan Struk (Receipt Layout):**
        *   Upload Logo Kafe (hitam-putih/monokrom).
        *   Input teks **Header Struk** (Nama Kafe, Alamat, No. Telepon).
        *   Input teks **Footer Struk** (Catatan tambahan, misal: *"Terima kasih! Password Wi-Fi: kopi123"*).
    *   **Tombol Uji Coba:** Tombol sekunder dengan garis tepi hijau muda bertuliskan **"Tes Cetak Struk (Test Print)"** di bagian bawah halaman.

*   **Interaksi UX:**
    *   Menekan tombol **"Tes Cetak Struk"** akan langsung mengirimkan perintah cetak struk contoh ke printer untuk memastikan koneksi dan ukuran kertas sudah sesuai.
    *   Tersedia **Live Preview Struk** (tampilan pratinjau struk di sisi kanan pada mode tablet atau pop-up pada mode HP) sehingga pengguna bisa melihat hasil perubahan header/footer secara langsung.

---

## 5. Alur Pengguna (User Flow Utama)
1. **Login & Koneksi Printer:** Kasir masuk menggunakan PIN/Email -> Memastikan ikon printer di header berwarna hijau (jika belum, masuk ke Pengaturan Printer -> Scan Bluetooth -> Hubungkan -> Tes Cetak).
2. **Membuat Pesanan:** Kasir memilih kategori -> Tap produk sesuai pesanan pelanggan -> Produk masuk ke keranjang.
3. **Pembayaran & Cetak Struk:** Kasir tap "Bayar" -> Pilih metode pembayaran -> Masukkan nominal bayar -> Konfirmasi -> Struk otomatis tercetak (atau tap tombol "Cetak Struk").
4. **Cek Laporan & Cetak Ulang:** Pemilik kafe membuka menu "Laporan" -> Memilih filter tanggal -> Melihat grafik pendapatan -> Tap transaksi tertentu jika perlu melakukan cetak ulang struk (*reprint*).