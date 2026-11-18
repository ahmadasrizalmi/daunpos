# DaunPOS — Android App

Aplikasi Android kasir kafe **DaunPOS** dibangun dari PRD `prd_aplikasi_kasir_android_daunpos.md`
dan design kit `design/` (termasuk `design/DaunPOS.pen`).

## Status

MVP produksi (v2): data persisten, login + role, printer Bluetooth ESC/POS, ekspor PDF/CSV.

## Stack

- Kotlin 2.0.21
- Jetpack Compose (Material 3, BOM 2024.10.01)
- Navigation Compose 2.8.5
- Room 2.6.1 (KSP)
- AGP 8.7.3 · Gradle 8.11.1
- minSdk 24 · targetSdk 35 · compileSdk 35

## Fitur

1. **Login + role** — PIN di-hash PBKDF2, tersimpan di Room. Kasir & pemilik.
   - PIN kasir demo: `1234`
   - PIN pemilik demo: `0000`
2. **POS** — kategori, grid menu adaptif, stok habis, keranjang + stepper, total otomatis.
3. **Pembayaran** — Tunai (kembalian real-time), QRIS, Transfer Bank.
4. **Struk thermal** — pratinjau + cetak fisik via Bluetooth SPP (ESC/POS) 58/80mm,
   cetak otomatis setelah bayar, test print, cetak ulang.
5. **Laporan** — filter tanggal, KPI, grafik, riwayat + badge status, ekspor **PDF** & **CSV (Excel)**.
6. **Manajemen menu** — search, filter kategori, toggle stok, tambah/edit/hapus.
7. **Pengaturan printer** — scan Bluetooth nyata (bonded + discovery), hubungkan/putuskan,
   ukuran kertas, toggle cetak otomatis & laci kasir.
8. **Kustomisasi struk** — live preview, header/footer, toggle logo & QR.

## Persistensi

- Produk, transaksi, user → **Room** (`daunpos.db`)
- Pengaturan struk/printer & nomor invoice → **SharedPreferences**
- Seed otomatis saat database pertama kali dibuat.

## Catatan

- QRIS/Transfer/WhatsApp masih simulasi (belum terhubung payment gateway / WhatsApp API).
- Printer fisik harus mendukung Bluetooth SPP (mayoritas printer thermal 58mm seperti RPP02N).
- Keystore demo: `keystore/daunpos-release.jks` (alias `daunpos`, pass `daunpos123`).

## Build release APK

```bash
gradle assembleRelease
# atau
./gradlew assembleRelease
```

Hasil: `app/build/outputs/apk/release/app-release.apk` (sudah ditandatangani).

## Instalasi

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```
