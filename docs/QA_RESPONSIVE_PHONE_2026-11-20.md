# DaunPOS — QA Responsivitas Ponsel & Gerbang Release

Tanggal: 20 November 2026  
Perangkat fisik untuk audit responsivitas sebelumnya: Xiaomi 22111317PG, Android 14, 1080 × 2400 px, 440 dpi (sekitar 393 × 873 dp). QA splash 2.1.1 menggunakan vivo V2324A (900 × 1600 px, 320 dpi).  
Metode stress test: density override pada perangkat yang sama untuk mensimulasikan lebar 360 dp dan 320 dp; density dikembalikan ke pengaturan asli masing-masing perangkat setelah tes.

## Perubahan responsivitas sebelum release

1. **Onboarding (revisi 2.1.1)**
   - Menggunakan logo asli DaunPOS dari `drawable-nodpi/logo_daunpos.jpg`.
   - Tidak menggunakan scroll; seluruh elemen utama dirancang untuk satu layar.
   - Tagline dipersingkat, fitur ditampilkan sebagai tiga ikon, dan dua tombol utama ditampilkan langsung.
   - Ukuran logo dan tipografi menyesuaikan ruang layar; konten dibatasi maksimal 520 dp pada layar lebar.

2. **Setup profil usaha**
   - Menambahkan `imePadding()` pada root agar form tetap dapat diakses ketika keyboard muncul.
   - Pada lebar <= 360 dp, label tombol pemilih QRIS dipendekkan menjadi "Scan QRIS".
   - Form tetap dapat di-scroll; tombol simpan tersedia pada akhir formulir.

3. **POS Retail**
   - Header menggunakan logo lebih kecil pada lebar <= 360 dp.
   - Badge tujuan transaksi memendekkan "Pelanggan Umum" menjadi "Umum" dan "Online / Kurir" menjadi "Online".
   - Tombol printer dipadatkan pada lebar sempit.
   - Badge membatasi teks satu baris dengan ellipsis.

4. **Pembayaran**
   - Judul header menjadi "Bayar" pada lebar <= 360 dp.
   - Badge tujuan transaksi dipendekkan agar tidak mendorong elemen header keluar layar.
   - Root pembayaran menggunakan `imePadding()` untuk menghindari keyboard menutupi input dan aksi.

## Hasil pengujian visual

| Layar | 393 dp fisik | 360 dp simulasi | 320 dp simulasi |
|---|---|---|---|
| Onboarding | Lulus | Lulus | Lulus |
| Pilih kategori | Lulus | Belum diambil screenshot terpisah | Lulus |
| Setup profil | Lulus | Belum diambil screenshot terpisah | Lulus |
| Keyboard pada form setup | Lulus | Belum diuji | Belum diuji |
| POS Retail fisik | Belum diuji pada sesi ini | Belum diuji | Belum diuji |

Catatan:
- Retail POS, manajemen produk, dan alur transaksi telah memiliki audit LDPlayer sebelumnya pada lebar sekitar 450 dp; lihat `docs/QA_CATEGORY_AUDIT_2026-11-20.md` dan screenshot di `design/audit/screenshots/phone/`.
- Simulasi density 360/320 dp merupakan stress test layout pada satu HP fisik, bukan pengujian pada beberapa model HP fisik.
- Tes landscape, foldable, tablet fisik, keyboard pada 320 dp, printer thermal fisik, dan scanner barcode fisik belum selesai.
- Tidak ada profil toko atau PIN QA yang disimpan. Onboarding belum diselesaikan; perangkat dikembalikan ke density fisik 440 dpi.

## Build dan verifikasi

- `testDebugUnitTest`: 53 tes lulus, 0 gagal, 0 error, 0 skipped.
- `lintDebug`: 0 error, 41 warning.
- `assembleDebug`: berhasil.
- `assembleRelease`: berhasil dikompilasi, tetapi hasil saat ini **unsigned** karena kredensial signing tidak tersedia.
- `lintVitalRelease`: lulus.
- `git diff --check`: lulus.

## Versi rilis yang dituju

- applicationId: `com.daunpos.app`
- versionName: `2.1.1`
- versionCode: `8`
- Rilis stabil sebelumnya di folder `release`: `daunpos-v2.1.0-release.apk`, versionCode 7.
- APK rilis lama yang diperiksa memakai sertifikat SHA-256 yang sama.
- Skrip `scripts/build-release.ps1` meminta password keystore secara tersembunyi, membangun APK bertanda tangan, lalu menolak hasil jika fingerprint sertifikat tidak sama dengan sertifikat rilis lama.
- Jangan mendistribusikan `app-release-unsigned.apk`. APK tersebut belum dapat dipasang sebagai rilis/update.

## QA Splash Screen — versi 2.1.1

Perangkat: vivo V2324A, Android; resolusi fisik 900 × 1600 px, density asli 320 dpi (450 × 800 dp). Pengujian dilakukan pada perangkat yang sama menggunakan density override untuk mensimulasikan lebar layar 360 dp dan 320 dp. Density dikembalikan ke 320 dpi.

- Logo pada layar menggunakan resource asli `app/src/main/res/drawable-nodpi/logo_daunpos.jpg`, cocok dengan logo yang diberikan pengguna.
- Tidak ada `verticalScroll`; seluruh konten utama berada pada satu layar.
- Logo, nama aplikasi, tagline, fitur, dan kedua tombol terlihat pada 450 dp, 360 dp, dan 320 dp.
- Kedua tombol diuji: "Mulai Setup Toko" membuka pemilihan kategori; "Masuk dengan PIN" membuka layar PIN pemilik.
- QA memakai package sementara `com.daunpos.app.preview` agar aplikasi produksi tidak ditimpa. Package preview telah dihapus setelah tes; aplikasi produksi `com.daunpos.app` tetap terpasang pada versi 2.1.0.

Screenshot:
- `design/audit/splash-final-450dp.png`
- `design/audit/splash-final-360dp.png`
- `design/audit/splash-final-320dp.png`

Catatan: pengujian ini memvalidasi layout splash dan navigasi tombol pada satu perangkat fisik dengan simulasi ukuran dp, bukan tiga perangkat fisik yang berbeda.
