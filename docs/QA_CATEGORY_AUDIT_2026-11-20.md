# DaunPOS — Audit Kategori Retail & Jasa

Tanggal audit: 20 November 2026  
Perangkat uji: LDPlayer via ADB  
Mode ponsel: 900 × 1600 px, 320 dpi  
Mode tablet simulasi: 1600 × 1000 px, 240 dpi  
Database: SQLite / Room, offline-first

## Ringkasan

Audit dilakukan langsung pada aplikasi yang terpasang di LDPlayer. Data aplikasi direset dengan persetujuan pemilik sebelum pengujian. Screenshot disimpan di `design/audit/screenshots/phone/` dan `design/audit/screenshots/tablet/`.

Temuan awal terkonfirmasi:
- Badge "Terpilih" pada kartu kategori Retail/Jasa pecah atau tertekan oleh teks deskripsi.
- Tombol lanjut kategori berada di akhir konten scroll sehingga sebagian tertutup di layar ponsel; pada tablet kartu Jasa tertutup CTA.
- Ikon tab katalog selalu sendok-garpu meskipun kategori Retail/Jasa.
- Form produk Jasa hanya menerima durasi dalam menit dan tidak punya field detail layanan yang tersimpan per produk.
- Catatan per item ada di model keranjang, tetapi akses UI hanya ikon kecil.
- Form foto produk hanya menyediakan URL, tidak ada pemilih file lokal.

Semua temuan tersebut sudah ditangani pada source dan diuji ulang, kecuali item yang dinyatakan sebagai pekerjaan lanjutan di bawah.

## Perubahan implementasi

### 1. Halaman pilih kategori
- Kartu kategori menggunakan layout header yang membagi ruang judul/subjudul dan badge terpilih secara eksplisit.
- Judul/subjudul dibatasi satu baris dengan ellipsis; badge tidak boleh wrap.
- CTA "Lanjutkan ke Setup Profil" sekarang sticky di bagian bawah dan tidak ikut terscroll.
- Untuk layar lebar/tablet (smallest width >= 600dp), kartu memakai compact spacing sehingga ketiga kategori dapat terlihat bersamaan pada viewport 1600 × 1000.
- Screenshot sebelum/sesudah: `11-category-retail-selected.png`, `12-category-service-selected.png`, `13-category-fixed.png`, `14-category-retail-fixed.png`, `15-category-service-fixed.png`, `06-category-compact.png`.

### 2. Retail & Toko
- Ikon tab katalog dinamis menggunakan storefront, bukan ikon F&B.
- Kategori default diperluas: Sembako, Makanan, Minuman, Kebutuhan Harian, Rumah Tangga, Fashion, Kecantikan, Petshop, Elektronik, Perlengkapan Bayi, Alat Tulis, Otomotif, Aksesori, Lainnya.
- Satuan jual per produk tersimpan (contoh pcs, kg, botol, box) dan dipakai pada kartu produk, stok, keranjang, checkout, transaksi, serta struk.
- SKU/barcode tetap dapat dicari/diinput melalui field scan keyboard-wedge; inventory policy tetap aktif untuk Retail.
- Uji visual: ikon tab Retail menjadi storefront; form produk memuat SKU/barcode, stok, satuan, deskripsi.
- Screenshot: `20-retail-main.png`, `21-retail-menu.png`, `22-retail-add-product.png`, `23-retail-add-product-bottom.png`, `28-retail-pos-product.png`.

### 3. Jasa & Layanan
- Ikon tab katalog dinamis menggunakan gunting/layanan.
- Kategori default diperluas: Salon & Grooming, Laundry, Perbaikan, Fotografi, Konsultasi, Kursus, Transportasi, Perawatan, Lainnya.
- Setiap produk jasa kini memiliki deskripsi/detail layanan, satuan jual (default sesi), dan durasi individual.
- Durasi bisa dimasukkan sebagai Menit, Jam, atau Hari; disimpan secara kanonis dalam menit untuk perhitungan, lalu ditampilkan sebagai format manusiawi (contoh 1 jam, 1 hari 2 jam).
- Di keranjang, tombol teks "Tambah catatan / permintaan pelanggan" menggantikan ikon kecil; dialog menyesuaikan konteks Jasa.
- Kuantitas layanan mengalikan estimasi durasi total. Catatan, deskripsi, durasi, dan satuan disnapshot ke item transaksi sehingga tidak berubah jika katalog diedit di kemudian hari.
- Ringkasan pembayaran, detail transaksi, teks struk/WhatsApp, dan struk printer menampilkan detail, durasi, dan catatan.
- Screenshot: `31-service-main.png`, `32-service-add-product.png`, `33-service-duration-menu.png`, `37-service-pos-product.png`, `39-service-cart-expanded.png`, `40-service-note-dialog.png`, `44-service-payment-final.png`, `45-service-success.png`, `46-service-menu-final.png`.

### 4. Upload gambar lokal
- Form produk sekarang memakai Android Storage Access Framework (OpenDocument) dengan filter `image/*`.
- File yang dipilih disalin ke penyimpanan privat aplikasi di `files/product-images/`; database SQLite hanya menyimpan URI file lokal.
- URL gambar tetap tersedia sebagai opsi alternatif, bukan jalur utama.
- Tidak memerlukan koneksi internet atau izin akses seluruh penyimpanan.
- Uji LDPlayer: file dipilih melalui system picker, salinan lokal muncul di folder privat aplikasi, lalu thumbnail tampil di katalog/POS.
- Screenshot: `24-local-image-picker.png`, `25-local-image-selected.png`, `27-retail-product-saved.png`, `28-retail-pos-product.png`.

### 5. Penyimpanan dan kompatibilitas
- Room database dinaikkan ke versi 5 dengan migrasi 4→5 untuk kolom `description` dan `unitLabel` pada tabel produk.
- Data transaksi lama tetap dapat dibaca; properti snapshot baru memiliki default kompatibel untuk transaksi historis.
- Unit test ditambah untuk format durasi dan keberadaan detail layanan/catatan pada struk teks dan thermal.

## Hasil pengujian

| Area | Hasil |
|---|---|
| Kartu kategori Retail/Jasa di ponsel | Lulus setelah perbaikan |
| CTA kategori ponsel | Lulus; sticky dan terlihat |
| Kartu kategori dan CTA tablet 1600×1000 | Lulus; tiga kartu terlihat dalam compact mode |
| Ikon tab Retail | Lulus; storefront |
| Ikon tab Jasa | Lulus; scissors/service |
| Form Retail + satuan/stok/SKU | Diuji |
| Pemilih foto lokal | Lulus; file tersalin ke private app storage |
| Thumbnail foto lokal di katalog/POS | Lulus |
| Form Jasa: deskripsi, satuan, durasi menit/jam/hari | Diuji |
| Catatan layanan pada keranjang | Lulus |
| Kuantitas mengalikan total durasi | Lulus; 2 × 1 jam = 2 jam |
| Ringkasan pembayaran: unit, detail, durasi, catatan | Lulus |
| Transaksi dan struk: detail, durasi, catatan | Lulus |
| `testDebugUnitTest` | Lulus |
| `assembleDebug` | Lulus |
| Signed release APK | Belum; signing belum dikonfigurasi |

## Pekerjaan lanjutan / batas audit

1. **Release signing belum dikonfigurasi.** Project mencari `keystore/daunpos-release.jks` dan environment variables `DAUNPOS_STORE_PASSWORD`, `DAUNPOS_KEY_ALIAS`, `DAUNPOS_KEY_PASSWORD`. Tanpa ini hanya APK debug yang tervalidasi; jangan menyebut build ini siap distribusi produksi.
2. **Hardware scanner fisik** belum diuji. Jalur input SKU/barcode sudah tersedia untuk keyboard-wedge scanner, tetapi perlu tes dengan perangkat nyata.
3. **Printer thermal fisik** belum diuji pada sprint ini; struk diuji melalui builder/tes kode.
4. **Optimasi gambar besar** (resize/compress thumbnail) belum dibuat. File lokal sudah berfungsi offline, tetapi gambar resolusi tinggi dapat menggunakan ruang penyimpanan lebih besar.
5. Tablet diuji pada resolusi simulasi LDPlayer 1600 × 1000; tes pada tablet fisik dengan variasi density masih diperlukan.

## Status reset

Setelah pengujian, data demo dan transaksi QA akan dibersihkan lagi agar aplikasi kembali ke onboarding awal. Source code dan APK tidak dihapus.
