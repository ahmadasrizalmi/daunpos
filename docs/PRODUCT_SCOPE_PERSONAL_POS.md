# DaunPOS — Product Scope Personal POS

## 1. Product objective

DaunPOS menjadi POS Android sederhana untuk pemilik usaha yang menjalankan kasir sendiri. Prioritas produk adalah transaksi yang konsisten, dapat berjalan offline, laporan yang dapat disimpan secara lokal, dan pengoperasian yang tidak membutuhkan akun tim atau backend untuk checkout.

## 2. Keputusan arsitektur

**Room/SQLite lokal adalah single source of truth.** Google Sheets bukan database operasional.

| Data | Penyimpanan utama | Catatan |
|---|---|---|
| Produk dan kategori | Room | Bisa dibaca tanpa internet |
| Stok | Room | Perubahan stok berada dalam transaksi database |
| Transaksi dan detail item | Room | Checkout harus tetap berhasil offline untuk pembayaran tunai |
| Pengguna pemilik dan PIN hash | Room | PIN tidak disimpan dalam bentuk plaintext |
| Pengaturan printer/struk | SharedPreferences | Lokal pada perangkat |
| Nomor invoice | SharedPreferences | Harus tetap unik pada satu instalasi |
| Laporan | Query Room | PDF/CSV diekspor melalui Storage Access Framework |
| Backup | File lokal | Cloud backup belum menjadi dependensi |

Sinkronisasi cloud, bila kelak dibutuhkan, harus menjadi lapisan tambahan. Konflik sinkronisasi tidak boleh menghambat checkout lokal.

## 3. Kategori usaha

### F&B
- Item: menu.
- Tujuan pesanan: meja atau takeaway.
- Stok: dilacak.
- Contoh kategori: kopi, non-kopi, makanan berat, snack, dessert.

### Retail
- Item: barang.
- Identitas tambahan: SKU/barcode.
- Stok: dilacak.
- Contoh kategori: kebutuhan rumah tangga, fashion, aksesori, elektronik.

### Jasa
- Item: layanan.
- Data tambahan: estimasi durasi dan catatan permintaan.
- Stok: tidak dilacak. Status aktif/nonaktif menjadi pengendali ketersediaan layanan.
- Contoh kategori: perawatan, perbaikan, dokumentasi, konsultasi.

## 4. Fitur yang tidak termasuk scope personal

- Login multi-role dan akun kasir.
- Manajemen tim/karyawan dan komisi.
- Shift kasir enterprise.
- Cloud database wajib.
- Google Sheets sebagai database transaksi.
- Sinkronisasi multi-device sebagai prasyarat penggunaan.

Tabel dan kolom lama yang menyimpan staf/shift tidak langsung dihapus dari database agar migrasi versi lama tidak merusak histori. UI personal tidak lagi menawarkan alur tersebut.

## 5. Ekspor laporan

Pada halaman laporan tersedia empat aksi:
1. Simpan PDF ke perangkat.
2. Bagikan PDF.
3. Simpan CSV ke perangkat.
4. Bagikan CSV.

Penyimpanan memakai Android Storage Access Framework sehingga pengguna menentukan lokasi dan nama file. Tidak diperlukan izin akses seluruh penyimpanan.

## 6. Rancangan WhatsApp otomatis

WhatsApp otomatis bukan bagian dari database transaksi dan tidak boleh menjadi syarat pembayaran.

### Alur yang disarankan

1. Kasir menyelesaikan transaksi dan Room menyimpan transaksi terlebih dahulu.
2. Aplikasi menambahkan pekerjaan notifikasi ke local outbox, menggunakan invoice ID sebagai idempotency key.
3. Jika internet tersedia, WorkManager mengirim pekerjaan ke backend.
4. Backend Node.js yang berjalan terus-menerus mengirim pesan melalui sesi Baileys yang sudah dipairing.
5. Backend mengembalikan status queued/sent/failed; kegagalan tidak membatalkan transaksi.
6. Aplikasi menyediakan retry terkontrol dan opsi berbagi manual sebagai fallback.

### Komponen backend

- Endpoint pairing QR dan status koneksi.
- Session store terenkripsi dan akses terbatas.
- Queue, retry dengan exponential backoff, dan deduplikasi invoice.
- Logging tanpa menyimpan PIN, token, atau isi sensitif yang tidak diperlukan.
- Health check dan monitoring.
- Pengaturan nomor tujuan pelanggan dalam format E.164.
- Persetujuan pelanggan untuk menerima struk/pemberitahuan WhatsApp.

Nomor yang dipairing adalah nomor pengirim milik usaha. Nomor pelanggan tetap diperlukan sebagai penerima pesan.

### Risiko dan keputusan

Baileys adalah integrasi tidak resmi. Sesi dapat terputus, protokol dapat berubah, dan akun dapat terkena pembatasan. Untuk kebutuhan komersial yang membutuhkan SLA dan kepatuhan lebih baik, WhatsApp Business Cloud API resmi lebih tepat. Pemilihan Baileys memerlukan persetujuan eksplisit atas risiko dan penentuan host selalu aktif (VPS atau mesin lokal).

## 7. Release gate

- Checkout tunai dan pengurangan stok tervalidasi.
- QRIS/transfer tidak pernah dianggap lunas hanya dari timer atau teks notifikasi yang belum diverifikasi.
- Pembatalan mengembalikan stok hanya bila transaksi sebelumnya mengurangi stok.
- Ekspor PDF/CSV berhasil disimpan melalui picker Android.
- Cetak 58 mm dan 80 mm diuji pada printer fisik.
- Seluruh halaman lolos audit visual ponsel dan tablet.
- Release APK ditandatangani dengan secret yang disimpan di luar repository.
