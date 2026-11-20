# DaunPOS — Audit Kategori, Layanan, Retail & Foto Produk
Tanggal: 20 November 2026
Perangkat uji: LDPlayer / ADB emulator-5554, 900 × 1600 px, 320 dpi
Status reset: `adb shell pm clear com.daunpos.app` berhasil; aplikasi kembali ke onboarding. Tidak ada data uji lama yang dipertahankan.

## Ringkasan

Audit kategori F&B, Retail & Toko, dan Jasa & Layanan dilakukan pada layar kategori yang sebenarnya di LDPlayer. Temuan visual kategori Retail/Jasa yang terlihat adalah subtitle terpotong saat badge "Terpilih" tampil dan chips fitur kartu Jasa tertutup footer sticky. Kedua masalah ini telah diperbaiki dan screenshot regresi baru menunjukkan ketiga kartu terlihat penuh.

Alur setelah onboarding (Main/Kasir/Produk/keranjang) belum bisa diuji live karena reset mengembalikan aplikasi ke onboarding dan PIN pemilik baru belum dibuat pada perangkat. Source code telah diperiksa untuk alur Retail, Jasa, ikon bottom bar, catatan, durasi, serta pemilihan gambar.

## Screenshot bukti

Folder: `design/audit/screenshots/phone/`

- `10-category-screen.png`: kategori awal sebelum perbaikan.
- `11-category-retail-selected.png`: Retail terpilih sebelum perbaikan.
- `12-category-service-selected.png`: Jasa terpilih sebelum perbaikan.
- `17-category-after-layout-fix.png`: ketiga kartu penuh sesudah perbaikan.
- `18-retail-selected-after-layout-fix.png`: Retail terpilih sesudah perbaikan.
- `19-service-selected-after-layout-fix.png`: Jasa terpilih sesudah perbaikan.

## Temuan per kategori

### 1. F&B
- Alur yang didesain: menu, kategori, meja/takeaway, stok menu, pembayaran dan struk.
- Kartu kategori paling stabil pada screenshot awal karena subtitle lebih pendek dan labelnya tidak terdorong badge.
- Belum dilakukan regresi transaksi F&B setelah reset.

### 2. Retail & Toko
- Data kategori bawaan mencakup Sembako, Makanan, Minuman, Kebutuhan Harian, Rumah Tangga, Fashion, Kecantikan, Petshop, Elektronik, Perlengkapan Bayi, Alat Tulis, Otomotif, Aksesori, dan Lainnya.
- Perilaku yang sudah ada di source: stok barang, harga modal, barcode/SKU, satuan jual, pencarian dan input kode barcode/SKU. POS mengaktifkan input kode untuk Retail.
- Bug visual terkonfirmasi: subtitle kartu terpotong ketika badge "Terpilih" tampil. Diperbaiki dengan mengizinkan subtitle dua baris.
- Catatan cakupan: belum ada varian produk (ukuran/warna), batch/expired date, supplier, dan mutasi stok masuk/keluar. Ini adalah gap fitur per vertikal, bukan crash visual.
- Input barcode saat ini berupa field untuk scanner keyboard wedge atau ketik SKU; bukan kamera scanner built-in.

### 3. Jasa & Layanan
- Data kategori bawaan: Salon & Grooming, Laundry, Perbaikan, Fotografi, Konsultasi, Kursus, Transportasi, Perawatan, dan Lainnya.
- Perilaku yang sudah ada di source:
  - stok barang tidak dilacak untuk kategori Jasa;
  - satuan transaksi per layanan bisa diatur, misalnya sesi/paket/orang/kunjungan;
  - estimasi durasi per produk disimpan sebagai menit dan UI menyediakan pilihan Menit/Jam/Hari;
  - deskripsi/detail layanan disimpan per produk;
  - catatan permintaan pelanggan tersedia per item di keranjang dan tersimpan ke snapshot transaksi.
- Bug visual terkonfirmasi pada kartu kategori Jasa: chips fitur bagian bawah tertutup footer sticky. Perbaikan spacing/padding membuat semua chips terlihat.
- UX form diperjelas: label satuan menjadi "Satuan layanan", label durasi menjadi "Estimasi durasi per layanan", dan helper menjelaskan jumlah kasir dihitung dari satuan layanan, bukan menit.
- Label detail produk Jasa diperjelas menjadi "Detail & catatan bawaan layanan".
- Bug data-entry ditemukan saat mengubah satuan durasi: mengganti Menit ke Jam/Hari sebelumnya dapat mengalikan angka tanpa mengonversi nilai sehingga durasi berubah diam-diam. Sekarang konversi hanya dilakukan jika hasilnya tepat; bila tidak, nilai dipertahankan dalam Menit agar durasi aktual tidak berubah.
- Bug state catatan ditemukan: dialog catatan menggunakan remember tanpa key, sehingga catatan item sebelumnya berpotensi terbawa ke item lain. Sekarang state dialog dikunci berdasarkan product ID.
- Gap fitur yang belum tersedia: kalender booking/slot jadwal, status pengerjaan, assignment teknisi/stylist, deposit/DP, dan pelacakan job untuk servis/laundry. Ini perlu diputuskan sebagai fitur lanjutan, tidak termasuk transaksi kasir dasar.

## Ikon bottom bar

Source `MainScreen.kt` sudah menghitung ikon tab item berdasarkan `storeProfile.businessCategory`:
- F&B → RestaurantMenu
- Retail → Storefront
- Jasa → ContentCut

Logika ini diterapkan untuk navigasi ponsel dan tablet. Dengan demikian, keluhan ikon sendok/garpu tidak berasal dari mapping source yang sekarang; kemungkinan berasal dari APK lama atau profil kategori yang belum ter-refresh. Karena reset membuat onboarding belum selesai, ikon pada halaman utama belum dapat diuji ulang secara live. Perlu verifikasi setelah setup Retail dan Jasa masing-masing.

## Foto produk offline

- Source sebelumnya sebenarnya sudah memiliki `ActivityResultContracts.OpenDocument()` untuk memilih `image/*`.
- Gambar diproses dengan disalin ke `context.filesDir/product-images`, lalu URI file lokal disimpan di kolom `imageUrl` Room. Jadi gambar tidak bergantung pada URL atau koneksi internet.
- Masalah UX: kontrol pilih foto berada di bagian paling bawah dialog setelah banyak field; pengguna mudah mengira hanya ada input URL.
- Perbaikan: pemilih galeri dipindahkan ke bagian atas form, diberi preview/placeholder dan CTA "Ambil dari galeri". URL dipindahkan ke opsi lanjutan.
- Batasan yang perlu ditangani sebelum production: gambar saat ini disalin apa adanya; belum ada resize/compression/limit ukuran. Perlu optimasi agar penyimpanan lokal tidak cepat membengkak.

## Perubahan yang diterapkan

1. `BusinessCategoryScreen.kt`: subtitle kartu dapat membungkus dua baris, padding/spacing dipadatkan agar seluruh kartu dan chips terlihat bersama footer.
2. `MenuManagementScreen.kt`: pemilih gambar lokal dipindah ke bagian atas form; URL menjadi opsi lanjutan; label satuan/durasi/detail Jasa diperjelas; pergantian unit durasi menjaga nilai menit tetap akurat.
3. `POSScreen.kt`: dialog catatan item sekarang mengikat state ke product ID agar catatan antar item tidak tertukar.

## Build & instalasi

Perintah: `gradlew.bat testDebugUnitTest assembleDebug --no-daemon`
Hasil: `BUILD SUCCESSFUL` (exit code 0).
APK debug berhasil di-install ulang ke LDPlayer tanpa reset tambahan.

## Belum diverifikasi live

- Setup toko selesai, login owner, dan masuk ke MainScreen setelah reset.
- Bottom bar icon untuk kategori Retail dan Jasa saat runtime.
- Menambah/mengedit barang Retail dan layanan Jasa secara live.
- Memilih gambar dari galeri dan memverifikasi thumbnail setelah app restart.
- Alur catatan layanan sampai nota dan riwayat.
- Layout tablet.

## Langkah QA berikutnya

1. Selesaikan onboarding pada LDPlayer dengan PIN pemilik yang dipilih pemilik perangkat.
2. Jalankan onboarding Retail, screenshot tab Kasir/Barang/Laporan/Pengaturan, tambah barang dengan foto lokal, lalu buat transaksi uji.
3. Reset data uji kembali, jalankan onboarding Jasa, tambah layanan dengan satuan paket/sesi dan durasi, catat instruksi per item, lalu uji nota.
4. Verifikasi icon bottom bar di kedua kategori dan tablet portrait/landscape.
5. Tambahkan image resize/compression dan test file retention sebelum rilis.
