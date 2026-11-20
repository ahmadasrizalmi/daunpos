# Build and verify a signed DaunPOS release without placing passwords in the repository.
param(
    [string]$StorePassword = $env:DAUNPOS_STORE_PASSWORD,
    [string]$KeyPassword = $env:DAUNPOS_KEY_PASSWORD
)
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$keystorePath = Join-Path $projectRoot "keystore\daunpos-release.jks"
$builtApk = Join-Path $projectRoot "app\build\outputs\apk\release\app-release.apk"
$targetApk = Join-Path $projectRoot "release\daunpos-v2.1.1-release.apk"
$apksigner = "C:\Android\Sdk\build-tools\36.0.0\apksigner.bat"
$aapt = "C:\Android\Sdk\build-tools\36.0.0\aapt.exe"
$expectedCertificate = "fdf6d9296840427110f459ba2dd03622468efdabff0e61842750eb82bab7f6e8"

function Convert-SecureStringToPlainText([Security.SecureString]$Value) {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

if (-not (Test-Path $keystorePath)) { throw "Keystore tidak ditemukan: $keystorePath" }
if (-not (Test-Path $apksigner)) { throw "Android apksigner tidak ditemukan: $apksigner" }
if (Test-Path $targetApk) { throw "File rilis sudah ada. Amankan/ubah nama file lama sebelum membuat rilis baru: $targetApk" }

Write-Host "DaunPOS release 2.1.1 (versionCode 8)"
$storePasswordSecure = $null
$keyPasswordSecure = $null

if ([string]::IsNullOrWhiteSpace($StorePassword)) {
    Write-Host "Password hanya diminta secara tersembunyi dan tidak disimpan ke file."
    $storePasswordSecure = Read-Host "Masukkan keystore password" -AsSecureString
    $StorePassword = Convert-SecureStringToPlainText $storePasswordSecure
}
if ([string]::IsNullOrWhiteSpace($KeyPassword)) {
    $keyPasswordSecure = Read-Host "Masukkan private-key password" -AsSecureString
    $KeyPassword = Convert-SecureStringToPlainText $keyPasswordSecure
}

Push-Location $projectRoot
try {
    $env:DAUNPOS_STORE_PASSWORD = $StorePassword
    $env:DAUNPOS_KEY_ALIAS = "daunpos"
    $env:DAUNPOS_KEY_PASSWORD = $KeyPassword

    & .\gradlew.bat assembleRelease --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "Gradle assembleRelease gagal (exit $LASTEXITCODE)." }
    if (-not (Test-Path $builtApk)) {
        throw "APK bertanda tangan tidak ditemukan. Periksa kredensial signing dan konfigurasi Gradle."
    }

    $verifyOutput = cmd.exe /c "`"$apksigner`" verify --print-certs `"$builtApk`" 2>&1"
    if ($LASTEXITCODE -ne 0) { throw "APK gagal diverifikasi oleh apksigner: $verifyOutput" }

    $certificateLine = ($verifyOutput | Select-String "Signer #1 certificate SHA-256 digest").Line
    if (-not $certificateLine) { throw "Fingerprint sertifikat penandatangan tidak ditemukan." }
    $actualCertificate = ($certificateLine -split ":")[-1].Trim().ToLowerInvariant()
    if ($actualCertificate -ne $expectedCertificate) {
        throw "Sertifikat tidak cocok dengan rilis lama. APK tidak disalin ke folder release. Fingerprint: $actualCertificate"
    }

    $badging = cmd.exe /c "`"$aapt`" dump badging `"$builtApk`""
    $packageLine = ($badging | Select-String "^package:").Line
    if ($packageLine -notmatch "name='com.daunpos.app'.*versionCode='8'.*versionName='2.1.1'") {
        throw "Identitas APK tidak sesuai target rilis: $packageLine"
    }

    Copy-Item $builtApk $targetApk
    $hash = (Get-FileHash $targetApk -Algorithm SHA256).Hash
    $sizeMb = [Math]::Round((Get-Item $targetApk).Length / 1MB, 2)

    Write-Host ""
    Write-Host "SIGNED RELEASE BERHASIL"
    Write-Host "File       : $targetApk"
    Write-Host "Versi      : 2.1.1 (8)"
    Write-Host "Ukuran     : $sizeMb MB"
    Write-Host "SHA-256    : $hash"
    Write-Host "Sertifikat : cocok dengan rilis DaunPOS sebelumnya."
}
finally {
    Remove-Item Env:DAUNPOS_STORE_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:DAUNPOS_KEY_ALIAS -ErrorAction SilentlyContinue
    Remove-Item Env:DAUNPOS_KEY_PASSWORD -ErrorAction SilentlyContinue
    if ($storePasswordSecure) { $storePasswordSecure.Dispose() }
    if ($keyPasswordSecure) { $keyPasswordSecure.Dispose() }
    Pop-Location
}
