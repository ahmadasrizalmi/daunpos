# DaunPOS — Brand, Product & UI System

**Versi dokumen:** 1.0  
**Status:** Baseline implementasi UI/UX  
**Platform:** Android phone dan tablet  
**Target:** Penjual rumahan, reseller, usaha mikro, dan penjual online di Indonesia

## 1. Filosofi merek

DaunPOS mengambil filosofi dari kata *daun*: tumbuh secara bertahap, sederhana, segar, dan dekat dengan aktivitas sehari-hari. Simbol daun mewakili pertumbuhan usaha; simbol mesin kasir dan kertas nota mewakili pencatatan transaksi yang rapi.

DaunPOS bukan sistem ERP atau POS enterprise. Produk ini membantu satu pemilik usaha menjawab tiga pertanyaan operasional: berapa penjualan yang terjadi, berapa biaya pokok barang yang terjual, dan berapa hasil usaha yang tersisa.

**Janji produk:** jualan tercatat, nota rapi, omzet dan profit mudah dipantau.

## 2. Posisi produk dan pengguna

### Pengguna utama
- Pemilik warung atau toko rumahan.
- Penjual makanan/minuman dari rumah.
- Reseller dan dropshipper yang membutuhkan pencatatan transaksi.
- Penjual melalui WhatsApp, marketplace, atau media sosial.
- Usaha jasa kecil yang memerlukan invoice/nota.

### Masalah yang diselesaikan
- Penjualan tersebar di catatan manual dan chat.
- Pemilik sulit menghitung omzet harian/bulanan.
- Harga jual terlihat, tetapi HPP dan laba kotor tidak tercatat konsisten.
- Nota sulit dibuat ulang ketika pelanggan meminta salinan.
- Rekap penjualan membutuhkan pekerjaan manual.

### Batasan produk
DaunPOS berfokus pada satu perangkat Android dan satu pemilik. Tidak ada ketergantungan pada akun cloud, spreadsheet, multi-cabang, manajemen staf, shift, komisi, atau integrasi laci kasir otomatis.

## 3. Prinsip produk

1. **Offline-first:** transaksi, produk, dan laporan inti tersedia tanpa internet.
2. **Single-owner:** satu pemilik, satu ruang kerja, tanpa alur team management.
3. **Low-friction:** kasir dapat digunakan tanpa memahami istilah enterprise.
4. **Financial clarity:** omzet, HPP, dan profit ditampilkan sebagai metrik berbeda.
5. **Data integrity:** HPP disimpan sebagai snapshot saat checkout; perubahan harga modal berikutnya tidak mengubah profit transaksi lama.
6. **Export ownership:** pemilik dapat menyimpan PDF/CSV ke perangkat dan membagikannya.
7. **Optional hardware:** printer thermal merupakan pilihan, bukan prasyarat operasional.

## 4. Identitas visual

### Palet warna

| Token | Hex | Fungsi |
|---|---|---|
| Hijau Tua | `#2F7D32` | CTA utama, status berhasil, navigasi aktif |
| Hijau Segar | `#93C83E` | Aksen merek, highlight, gradient |
| Hijau Lembut | `#DDECC8` | Selected state dan panel bantuan |
| Putih | `#FFFFFF` | Card, form, area konten |
| Abu Lembut | `#F5F6F7` | Background halaman |
| Outline Gray | `#D9DDE2` | Batas input, card, divider |
| Teks Gelap | `#1F2937` | Heading dan body utama |
| Teks Sekunder | `#667085` | Deskripsi, label, metadata |
| Error | `#DC4C4C` | Error validasi dan kegagalan yang memerlukan tindakan |

Hijau digunakan sebagai sinyal tindakan utama, bukan sebagai warna semua elemen. Status printer yang belum tersambung memakai abu netral karena bukan kesalahan transaksi.

### Logo
Logo menggabungkan motif daun dengan perangkat kasir/nota. Logo mark harus tetap terbaca pada ukuran aplikasi, halaman login, navigasi, dan dokumen. Jangan meregangkan logo, mengubah proporsi, atau menambahkan efek yang mengurangi keterbacaan.

### Tipografi
- Heading: sans-serif modern dengan bobot semibold/bold.
- Body: sans-serif regular dengan line-height nyaman.
- Label dan metadata: ukuran lebih kecil, kontras tetap memadai.
- Angka uang: gunakan format Rupiah konsisten dan hindari pemotongan angka penting.

## 5. Komponen antarmuka

### Tombol
- Primary: hijau tua, teks putih, tinggi sentuh minimum 48 dp, sudut rounded.
- Secondary: putih, outline abu, teks gelap/hijau.
- Destructive: merah hanya untuk hapus/batal permanen.
- Disabled: latar abu/hijau lembut dan teks abu; jangan menyerupai tombol aktif.
- Loading: tampilkan indikator dan cegah submit ganda.

### Input
- Background putih.
- Outline `#D9DDE2` dengan label/placeholder yang jelas.
- Focus state memakai outline hijau tua.
- Error state memakai merah dan pesan spesifik.
- Nilai mata uang harus memakai keyboard numerik dan validasi batas nilai.

### Card dan layout
- Card putih, border 1 dp abu lembut, radius 16–20 dp.
- Shadow tipis hanya untuk elemen yang mengambang.
- Jarak antarkelompok konsisten; hindari card bersarang tanpa kebutuhan.
- Empty state menjelaskan langkah berikutnya, bukan hanya menampilkan ikon.

## 6. Peta halaman dan alur

| Halaman | Tujuan | Elemen inti |
|---|---|---|
| Onboarding | Menjelaskan manfaat dan memulai setup | Brand, manfaat, kategori usaha |
| Kategori usaha | Menentukan label dan alur sesuai usaha | Retail, F&B, jasa, kategori lain |
| Profil toko | Identitas yang dicetak pada nota | Nama, kontak, alamat, QRIS opsional |
| Login PIN | Melindungi akses pemilik | PIN keypad, validasi, ganti PIN |
| Kasir | Mencatat penjualan | Pencarian, kategori, katalog, keranjang |
| Keranjang | Memeriksa item sebelum pembayaran | Kuantitas, catatan, diskon, total |
| Pembayaran | Menyelesaikan transaksi | Tunai, transfer, QRIS, nominal dan validasi |
| Transaksi berhasil | Memberi konfirmasi dan akses nota | Ringkasan, preview nota, cetak/bagikan |
| Riwayat/detail transaksi | Meninjau dan membagikan nota lama | Status, item, total, aksi |
| Laporan | Memantau hasil usaha | Omzet, profit kotor, transaksi, tren |
| Produk/menu | Mengelola katalog dan HPP | Nama, harga jual, harga modal, stok, kategori |
| Pengaturan printer | Menghubungkan printer opsional | Bluetooth, kertas, tes cetak |
| Kustomisasi nota | Mengatur identitas dan footer nota | Header, footer, logo, preview |

## 7. Perhitungan keuangan dan definisi metrik

- **Omzet penjualan:** subtotal transaksi sukses setelah diskon, sebelum pajak yang dipungut atas nama pihak lain.
- **HPP:** harga modal per unit yang disimpan pada item transaksi ketika checkout dikalikan kuantitas.
- **Profit kotor:** omzet setelah diskon dikurangi HPP barang yang terjual.
- **Biaya operasional:** belum termasuk profit kotor kecuali aplikasi menyediakan pencatatan biaya tersendiri.
- **Transaksi batal/pending:** tidak masuk omzet dan profit penjualan sukses.

Profit tidak boleh dihitung menggunakan harga modal produk saat ini untuk transaksi lama karena harga modal dapat berubah. Jika satu transaksi historis tidak memiliki snapshot HPP, laporan menampilkan status belum lengkap, bukan angka profit yang direkayasa. Untuk transaksi baru, pemilik perlu mengisi harga modal pada produk; gunakan 0 hanya bila memang tidak ada biaya modal langsung.

## 8. Adaptasi smartphone dan tablet

### Smartphone
- Navigasi utama di bawah: Kasir, Laporan, Menu, Pengaturan.
- Kasir memakai katalog satu layar dan ringkasan keranjang ringkas di atas navigasi.
- Keranjang lengkap dibuka sebagai bottom sheet agar tidak menutupi katalog.
- Form panjang memakai scroll vertikal dan CTA tidak menimpa field.
- Nota memiliki lebar terbatas dan berada di tengah layar.

### Tablet
- Navigasi utama horizontal di bagian atas.
- Kasir memakai split pane: katalog di kiri dan keranjang tetap di kanan.
- Laporan menggunakan empat KPI dalam satu baris bila lebar memadai.
- Pengaturan dan form dapat menggunakan dua kolom.
- Konten dibatasi lebar bacanya; tablet tidak sekadar memperbesar font atau card smartphone.
- Semua aksi penting tetap dapat dijangkau tanpa horizontal scrolling yang tidak disengaja.

## 9. Fitur yang sengaja tidak menjadi bagian produk

- Pembukaan laci kasir otomatis/ESC-POS kick pulse.
- Notifikasi atau status laci kasir.
- Team/staff management, shift, komisi, dan multi-user.
- Cloud database wajib dan sinkronisasi spreadsheet sebagai sumber data utama.
- Alur setup enterprise yang tidak diperlukan pemilik tunggal.

Printer thermal tetap opsional untuk mencetak nota. Berbagi nota melalui aplikasi Android tetap tersedia. Pengiriman WhatsApp otomatis melalui bridge tidak boleh dianggap tersedia sebelum pairing, backend, antrean, retry, autentikasi, dan pengujian end-to-end selesai.

## 10. Kualitas, aksesibilitas, dan kriteria penerimaan

- Kontras teks dan status harus jelas; jangan mengandalkan warna saja.
- Target area sentuh minimal 48 dp.
- Field memiliki label yang dapat dipahami pengguna awam.
- Tombol disabled berbeda jelas dari tombol aktif.
- Nominal, tanggal, status transaksi, dan total harus konsisten antara kasir, nota, laporan, PDF, dan CSV.
- Ekspor PDF/CSV harus dapat disimpan melalui Android Storage Access Framework dan dapat dibuka di aplikasi lain.
- Uji phone portrait, tablet portrait/landscape, ukuran font sistem besar, dan keyboard terbuka.
- Uji alur transaksi dari tambah item sampai nota, termasuk pembayaran tunai, transfer, QRIS, pembatalan, dan kondisi tanpa printer.
- Uji database upgrade tanpa menghilangkan transaksi pengguna.
- Release hanya dinyatakan siap setelah lint, unit/instrumentation tests, pengujian perangkat, dan signing selesai.

## 11. Status implementasi saat dokumen dibuat

Sistem warna, logo mark vektor aplikasi, tampilan ringkasan omzet/profit, snapshot HPP transaksi, dan layout dasar phone/tablet sudah mulai diterapkan. Screenshot emulator menunjukkan beberapa halaman utama sudah dapat dirender. Pemeriksaan menyeluruh terhadap semua alur, ekspor pada perangkat fisik, printer, dan rilis signed masih harus diselesaikan.

Dokumen ini adalah pedoman desain dan produk; status fitur produksi harus ditentukan dari hasil pengujian aktual, bukan dari mockup.
