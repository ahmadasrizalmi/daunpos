# DaunPOS — Android POS

## Status

MVP **offline-first untuk penggunaan personal**. Build debug dapat dijalankan, tetapi aplikasi belum dinyatakan production-ready sampai pengujian visual seluruh halaman, printer fisik, notifikasi WhatsApp, dan release signing selesai.

## Target produk

DaunPOS ditujukan untuk pemilik usaha yang mengoperasikan kasir sendiri. Kategori yang didukung:

- F&B: menu, meja, takeaway.
- Retail: barang, SKU/barcode, dan stok.
- Jasa: layanan, estimasi durasi, dan catatan pesanan.

Antarmuka hanya menyediakan akun pemilik. Data historis yang berhubungan dengan staf tetap dipertahankan agar kompatibel dengan database lama, tetapi fitur pengelolaan tim tidak lagi menjadi bagian dari alur personal.

## Stack

- Kotlin 2.0.21
- Jetpack Compose + Material 3
- Navigation Compose 2.8.5
- Room 2.6.1
- AGP 8.7.3 · Gradle 8.11.1
- minSdk 24 · targetSdk 35 · compileSdk 35

## Fitur aplikasi

1. **POS personal** — katalog produk/layanan, pencarian, keranjang, catatan item, dan checkout.
2. **Pembayaran** — tunai; QRIS dan transfer dengan validasi/konfirmasi kasir. Tidak ada payment gateway yang terhubung.
3. **Inventaris** — stok dikurangi untuk F&B dan retail; stok tidak dilacak untuk kategori jasa.
4. **Struk** — preview, pengaturan header/footer, cetak ulang, dan Bluetooth ESC/POS 58/80 mm. Printer fisik masih memerlukan pengujian perangkat nyata.
5. **Laporan** — filter periode, KPI, grafik harian, riwayat transaksi, PDF, dan CSV.
6. **Ekspor** — pengguna dapat menyimpan PDF/CSV melalui Android Storage Access Framework atau membagikannya melalui Android Share Sheet.
7. **Pengaturan printer** — koneksi Bluetooth, ukuran kertas, cetak otomatis, dan opsi laci kasir.
8. **PIN pemilik** — PIN 4–6 digit; tidak ada pemilihan akun tim pada antarmuka personal.

## Arsitektur data

- **Room / SQLite lokal** adalah sumber data utama untuk produk, transaksi, pengguna, dan profil usaha.
- **SharedPreferences** menyimpan pengaturan printer/struk dan nomor invoice.
- Transaksi tunai, katalog, stok, dan laporan harus tetap dapat digunakan tanpa koneksi internet.
- Google Sheets bukan database transaksi. Spreadsheet hanya menjadi format ekspor.
- Cloud backup/sinkronisasi belum diimplementasikan dan harus bersifat opsional.

## WhatsApp

Saat ini aplikasi hanya menyediakan Android Share Intent. Pairing nomor, pengiriman otomatis, antrean pesan, retry, dan backend Baileys **belum diimplementasikan**.

Implementasi otomatis memerlukan layanan terpisah yang selalu aktif, pairing perangkat, penyimpanan sesi yang aman, antrean idempotent, serta mekanisme retry. Layanan tersebut tidak boleh menjadi prasyarat checkout. Baileys merupakan integrasi tidak resmi dan membawa risiko pemutusan sesi atau pembatasan akun.

## Build

Debug dan pengujian:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --no-daemon
```

Release:

```powershell
.\scripts\build-release.ps1
```

Skrip release:
- Meminta password keystore dan private key melalui prompt tersembunyi.
- Menggunakan alias lokal `daunpos`.
- Tidak menulis password ke file proyek atau Git.
- Memastikan fingerprint sertifikat sama dengan rilis terdahulu sebelum menyalin APK ke folder `release`.
- Menghasilkan `release/daunpos-v2.1.1-release.apk` dengan versionCode 8.

Build `assembleRelease` tanpa kredensial hanya menghasilkan `app/build/outputs/apk/release/app-release-unsigned.apk`. **Jangan mendistribusikan APK unsigned.** Keystore dan kredensial tidak boleh dimasukkan ke Git atau dokumentasi yang dibagikan.

Status build 20 November 2026:
- `testDebugUnitTest`: 53 lulus.
- `lintDebug`: 0 error, 41 warning.
- `assembleDebug`: berhasil.
- `assembleRelease`: berhasil dikompilasi, belum ditandatangani karena kredensial tidak tersedia pada sesi ini.
- `lintVitalRelease`: lulus.
- `git diff --check`: lulus.

Versi rilis berikutnya yang dituju: `2.1.1` / versionCode `8` (rilis sebelumnya `2.1.0` / versionCode 7).

Splash screen 2.1.1: logo asli DaunPOS, tanpa scroll, tiga ikon fitur, dan tombol Setup/Login dalam satu layar. QA visual: `design/audit/splash-final-450dp.png`, `splash-final-360dp.png`, dan `splash-final-320dp.png` (lihat `docs/QA_SPLASH_2026-11-20.md`).

Audit responsivitas ponsel: lihat `docs/QA_RESPONSIVE_PHONE_2026-11-20.md`.

## Gerbang sebelum rilis

- Audit visual semua halaman pada ponsel dan tablet.
- Uji transaksi tunai, QRIS, transfer, pembatalan, dan pemulihan stok.
- Uji cetak fisik 58 mm dan 80 mm serta perintah cash drawer.
- Uji simpan PDF/CSV melalui pemilih folder Android.
- Implementasikan dan deploy layanan WhatsApp otomatis bila tetap menjadi kebutuhan.
- Konfigurasi signing release dan lakukan uji instalasi APK release.
