# DaunPOS — Sprint Plan Review & QA Report

**Tanggal audit:** 20 November 2026  
**Basis audit:** source code, unit tests, Gradle build, dan LDPlayer pada 900×1600 serta 1600×1200.  
**Catatan rencana:** repository memiliki PRD dan APP_BUILD, tetapi tidak ditemukan dokumen sprint plan terpisah. Tabel di bawah adalah baseline sprint yang dibentuk dari PRD dan kondisi implementasi saat audit, bukan klaim bahwa sprint lama telah selesai.

## 1. Ringkasan progres

**Estimasi progres keseluruhan: 65%** (weighted completion).

Angka ini adalah estimasi bobot deliverable, bukan persentase baris kode atau test coverage. WhatsApp automation, visual QA seluruh halaman, printer fisik, dan release signing masih menjadi pekerjaan besar.

| Sprint | Bobot | Progres | Kontribusi | Review |
|---|---:|---:|---:|---|
| S1 — Core POS offline & transaksi | 20% | 90% | 18.0% | Room lokal, alur checkout, koreksi route invoice, dan kebijakan stok kategori jasa sudah masuk. Perlu uji perangkat nyata dan recovery edge cases. |
| S2 — Scope personal & kategori usaha | 15% | 90% | 13.5% | UI tim/staf dihapus dari alur personal, login pemilik saja, kategori dan stok jasa disesuaikan. Data historis staf tetap dipertahankan untuk kompatibilitas. |
| S3 — Laporan dan ekspor | 20% | 80% | 16.0% | PDF/CSV, save/share, agregasi Long, grafik tanggal kalender dan status pending sudah diperbaiki. SAF picker belum dapat diuji end-to-end karena sesi emulator masih terkunci. |
| S4 — Struk, QRIS, printer | 15% | 70% | 10.5% | Preview struk dipusatkan dan tutorial QRIS dirapikan. Cetak fisik 58/80 mm, laci kasir, dan validasi merchant QRIS belum diuji pada perangkat nyata. |
| S5 — WhatsApp otomatis | 15% | 0% | 0.0% | Belum ada pairing, backend Baileys, outbox, queue, retry, atau nomor penerima pelanggan. Perlu keputusan host dan persetujuan risiko Baileys. |
| S6 — QA, tablet, release | 15% | 45% | 6.75% | Build debug dan unit tests lulus; login diuji pada phone dan tablet. Audit visual tab yang memerlukan PIN, printer fisik, lint cleanup, dan release signing belum selesai. |
| **Total** | **100%** |  | **64.75% ≈ 65%** | |

## 2. Perubahan yang masuk pada audit ini

### Laporan
- Menambahkan alur Save PDF dan Save CSV menggunakan Android Storage Access Framework.
- Memisahkan tindakan simpan lokal dan share.
- Menambahkan writer stream yang mempertahankan byte PDF dan UTF-8 CSV.
- Memperbaiki label jumlah item agar netral untuk F&B, retail, dan jasa.
- Memisahkan status transaksi sukses, batal, dan menunggu pada riwayat.

### Kategori usaha dan inventaris
- Menyediakan label item dan kategori default per jenis usaha.
- Kategori jasa tidak lagi memerlukan stok positif dan tidak mengurangi stok ketika checkout.
- Status aktif/nonaktif tetap mengendalikan ketersediaan jasa.
- SKU/barcode hanya ditampilkan pada kategori retail; durasi hanya pada kategori jasa.

### Personal use
- Menghilangkan pemilihan akun tim pada onboarding/login.
- Menghilangkan pemilihan staf pada keranjang dan menggantinya dengan catatan item.
- Mengubah penyelesaian setup menjadi "Simpan & Mulai Kasir".
- Menyamakan input PIN menjadi 4–6 digit.

### Tampilan dan alur
- Memusatkan preview struk.
- Mengubah tutorial QRIS menjadi layout yang membungkus teks dan badge, serta menghapus instruksi tombol demo yang tidak tersedia.
- Memperbaiki parameter layout tablet pada konten pembayaran.
- Menghapus orientasi layar yang dipatok eksplisit agar rotasi dan form factor tidak dibatasi.

## 3. Audit halaman

Status "source review" berarti alur dan komponen telah diperiksa dari source code; status tersebut **bukan** bukti bahwa screenshot akhir halaman sudah diverifikasi.

| Halaman | Hasil source review | Visual QA |
|---|---|---|
| Login PIN | Owner-only, PIN 4–6 digit, tombol submit eksplisit | Phone dan tablet: lulus |
| Pilih kategori usaha | Deskripsi fitur disesuaikan; feature tags wrap pada lebar sempit | Menunggu akses halaman |
| Setup profil usaha | Akun tim dihapus; QRIS opsional; CTA disederhanakan | Menunggu akses halaman |
| POS / Kasir | Kategori dinamis; jasa tidak memakai stok; assignment staf dihapus | Menunggu akses halaman |
| Pembayaran | Tunai/QRIS/transfer; laci hanya diinformasikan bila dikonfigurasi; parameter tablet diteruskan | Menunggu akses halaman |
| Tutorial QRIS | Layout step responsif; instruksi demo yang tidak ada dihapus | Menunggu akses halaman |
| Transaksi berhasil / nota | Route ID di-encode; preview receipt dipusatkan | Menunggu akses halaman |
| Laporan | Simpan PDF/CSV dan share dipisah; status pending dibedakan | Menunggu akses halaman dan picker SAF |
| Detail transaksi | PIN pemilik 4–6 digit; route detail memakai encoding | Menunggu akses halaman |
| Manajemen item | Kategori default per bisnis; stok jasa disembunyikan; SKU/durasi kondisional | Menunggu akses halaman |
| Pengaturan printer | Alur Bluetooth, ukuran kertas, cetak otomatis dan cash drawer diperiksa pada source | Perlu printer fisik |
| Kustomisasi struk | Preview menggunakan komponen receipt bersama | Menunggu akses halaman |

### Batasan visual audit

LDPlayer dapat dibuka, tetapi halaman internal saat audit berhenti di login pemilik. PIN yang tersimpan tidak sama dengan PIN default. Saya tidak mereset database, tidak mengubah PIN, dan tidak mencoba menebak PIN. Karena itu, saya tidak menyatakan bahwa semua halaman sudah lolos visual QA.

Pada resolusi tablet 1600×1200, halaman login tampil dua kolom dan seluruh keypad serta tombol Masuk berada dalam batas layar. Hal ini belum membuktikan bahwa POS, laporan, pembayaran, dan pengaturan printer juga sudah fit pada tablet.

## 4. Arsitektur yang diputuskan

- Room/SQLite lokal menjadi sumber data utama.
- Google Sheets bukan database transaksi.
- Checkout tunai, stok, dan laporan lokal tidak memerlukan internet.
- PDF/CSV disimpan melalui Storage Access Framework.
- Cloud backup/sinkronisasi bersifat opsional dan belum diimplementasikan.
- WhatsApp otomatis menjadi service terpisah; tidak boleh memblokir checkout.

## 5. Validasi teknis

| Validasi | Hasil |
|---|---|
| testDebugUnitTest | BUILD SUCCESSFUL — 51 tests, 0 failures |
| assembleDebug | BUILD SUCCESSFUL — version 2.1.0, versionCode 7 |
| lintDebug | 0 errors, 39 warnings |
| assembleRelease | BUILD SUCCESSFUL — APK masih unsigned |
| LDPlayer install debug APK | Berhasil |
| Phone login screen | Terlihat dan fit pada 900×1600 |
| Tablet login screen | Terlihat dan fit pada 1600×1200 |
| PDF/CSV Save Picker | Source dan writer test; belum end-to-end di emulator |
| Printer fisik | Belum diuji |
| WhatsApp pairing | Belum ada |
| Signed release APK | Belum tersedia |

## 6. Risiko dan pekerjaan yang memblokir rilis

1. **Visual QA:** perlu emulator dibuka dengan PIN pemilik agar seluruh tab bisa diuji tanpa mereset data.
2. **SAF export:** perlu menyimpan satu PDF dan satu CSV melalui picker Android, lalu memeriksa file hasilnya.
3. **Printer:** perlu printer Bluetooth SPP fisik untuk menguji 58 mm, 80 mm, auto print, dan cash drawer.
4. **WhatsApp:** perlu menentukan VPS atau mesin lokal always-on, nomor pengirim untuk pairing, format nomor pelanggan, dan persetujuan risiko Baileys.
5. **Signing:** release signing environment variables belum tersedia; APK release tidak boleh dibagikan sebelum ditandatangani.
6. **Lint/dependency:** warning dependency dan SharedPreferences perlu ditinjau terpisah. Upgrade library tidak boleh dilakukan massal tanpa regression test.

## 7. Urutan sprint berikutnya

- **S7 — Visual QA seluruh halaman:** screenshot dan audit ponsel/tablet untuk POS, payment, QRIS, success, report, detail, item management, printer, dan receipt settings.
- **S8 — Export acceptance:** uji save PDF/CSV ke Downloads, file terbuka, ukuran file valid, dan nama file konsisten.
- **S9 — Hardware acceptance:** uji printer 58/80 mm dan cash drawer.
- **S10 — WhatsApp service:** bangun pairing UI, backend, outbox, retry, dan monitoring setelah host dipilih.
- **S11 — Release hardening:** signing, dependency review, lint cleanup, backup/restore test, dan regression suite.
