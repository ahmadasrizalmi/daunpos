# DaunPOS — Visual QA Audit

Tanggal: 20 November 2026
Perangkat: LDPlayer / ADB `emulator-5554` (Android device model V2324A)
Resolusi logis: 900 × 1600 px, density 320 dpi

## Lingkup yang sudah dilakukan

- Data lokal aplikasi `com.daunpos.app` direset dengan `pm clear` sesuai persetujuan pemilik. Source code dan file project tidak dihapus.
- APK debug terbaru berhasil dibangun dan dipasang ulang.
- Launcher icon diarahkan ke aset brand DaunPOS versi adaptive icon; fallback icon juga diperbarui.
- Tombol aksi utama menggunakan gradient tiga warna `#9BD449 → #58AE3A → #2F7D32` melalui komponen `DaunPOSButton`.
- Seluruh layar utama yang menggunakan tombol utama telah menggunakan alias komponen `DaunPOSButton`; tombol sekunder/outline tetap netral.

## Screenshot aktual

Lokasi: `design/audit/screenshots/phone/`

1. `01-onboarding.png` — halaman pengenalan.
2. `02-business-category.png` — pemilihan kategori saat pertama dibuka.
3. `02-business-category-bottom.png` — posisi setelah scroll ke bagian bawah.
4. `03-store-setup.png` — profil toko dan QRIS.
5. `03-store-setup-bottom.png` — bagian bawah setup, termasuk PIN pemilik.

Screenshot berasal dari LDPlayer melalui ADB, bukan mockup.

## Temuan awal visual

| ID | Halaman | Temuan | Prioritas | Status |
|---|---|---|---|---|
| V-01 | Kategori bisnis | Tombol lanjut berada di bawah konten panjang; pada posisi scroll awal hanya sedikit terlihat. Pengguna perlu scroll untuk mencapai CTA. Pertimbangkan sticky footer pada ponsel. | P2 | Teridentifikasi |
| V-02 | Setup profil | Form cukup panjang; bagian PIN dan tombol simpan berada di bawah QRIS sehingga perlu scroll. Pastikan CTA mudah dijangkau dan keyboard tidak menutup field. | P2 | Teridentifikasi |
| V-03 | Onboarding | Konten informatif dan terbaca; tombol utama sudah memakai gradient brand. | — | Lulus inspeksi awal |
| V-04 | Brand icon | Adaptive launcher icon dan fallback vector diarahkan ke logo kasir + daun DaunPOS. Perlu cek tampilan launcher final setelah pemasangan ulang dan launcher refresh. | P2 | Build berhasil; visual launcher belum diverifikasi |

## Status integrasi

Belum bisa dinyatakan lulus/gagal untuk alur Kasir → Pembayaran → Sukses → Riwayat → Detail, Menu → Kasir → Laporan, dan Pengaturan → Printer → Struk karena onboarding belum diselesaikan pada emulator.

Setup profil telah diisi dengan data uji non-sensitif. Pemilik perlu memasukkan PIN sendiri di LDPlayer pada layar `Akun Pemilik` lalu menekan `Simpan & Mulai Kasir`. PIN tidak dicatat di dokumen atau screenshot.

## Build

Perintah: `gradlew.bat testDebugUnitTest assembleDebug --no-daemon`

Hasil: `BUILD SUCCESSFUL` (exit code 0). APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Next steps

1. Selesaikan setup owner PIN pada emulator.
2. Ambil screenshot semua tab utama dan semua halaman sekunder.
3. Jalankan transaksi uji end-to-end dan periksa sinkronisasi state/data antarhalaman.
4. Uji layout tablet terpisah dengan ukuran landscape dan portrait.
5. Perbaiki temuan visual, rebuild, install, dan ulangi screenshot regression.


## Pembaruan logo DaunPOS — 20 November 2026

Permintaan: gunakan file sumber `design/Logo daunPOS small.jpg` untuk ikon dan logo aplikasi.

- Sumber gambar diperiksa: JPEG 500 × 500 px.
- Salinan `app/src/main/res/drawable-nodpi/logo_daunpos.jpg` dan `app/src/main/res/mipmap-nodpi/daunpos_logo.jpg` memiliki SHA-256 identik dengan sumber JPG.
- `DaunPOSLogoMark` kini menampilkan gambar sumber tersebut dan dipakai bersama oleh Onboarding, Login, header tablet, dan POS.
- Manifest Android menggunakan `@mipmap/daunpos_logo` untuk `android:icon` dan `android:roundIcon`.
- Resource ikon lama yang tidak lagi digunakan telah dibersihkan.
- Screenshot verifikasi aktual LDPlayer: `05-logo-updated-onboarding.png`, `07-app-drawer.png`, dan `08-login-logo.png`.
- APK debug dipasang ulang dengan `adb install -r`, tanpa menghapus data aplikasi.
- Verifikasi `aapt dump badging`: application icon mengarah ke `res/mipmap-nodpi-v4/daunpos_logo.jpg`.

Status: logo onboarding, login, dan ikon app drawer terlihat menggunakan artwork JPG sumber. MainScreen dan POS memakai komponen logo bersama yang sama; belum masuk ke alur transaksi karena onboarding toko belum diselesaikan.


## Status update — category audit completed

The earlier note that onboarding was waiting for an owner PIN is superseded by `QA_CATEGORY_AUDIT_2026-11-20.md`. Retail and Service flows were subsequently exercised end-to-end in LDPlayer, including local product image selection, service duration, item note, payment summary, and successful receipt. Tablet compact category layout was also checked at 1600 × 1000. After QA, app data was cleared again; LDPlayer is restored to 900 × 1600 / 320 dpi and the app is left at the initial onboarding screen. The debug APK remains installed.
