<#
.SYNOPSIS
    Generates a secure release signing keystore for Kipit and outputs GitHub Actions secrets configuration.

.DESCRIPTION
    Creates an Android release signing keystore (.jks) using Java keytool.
    Outputs the Base64 representation needed for the RELEASE_KEYSTORE_BASE64 secret in GitHub Actions.
#>

[CmdletBinding()]
param (
    [string]$KeystorePath = "$PSScriptRoot/../app/release.jks",
    [string]$Alias = "kipit-release",
    [int]$ValidityDays = 10000,
    [string]$CommonName = "Kipit Android Application",
    [string]$Organization = "Kipit"
)

$ErrorActionPreference = "Stop"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "  Kipit Release Keystore Generator        " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# Locate keytool
$keytoolPath = "keytool"
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME/bin/keytool.exe")) {
    $keytoolPath = "$env:JAVA_HOME/bin/keytool.exe"
} elseif (Test-Path "C:\Program Files\Java\jdk-21.0.11\bin\keytool.exe") {
    $keytoolPath = "C:\Program Files\Java\jdk-21.0.11\bin\keytool.exe"
}

$resolvedKeystore = [System.IO.Path]::GetFullPath($KeystorePath)

if (Test-Path $resolvedKeystore) {
    Write-Warning "Keystore file already exists at: $resolvedKeystore"
    $overwrite = Read-Host "Do you want to overwrite it? (y/N)"
    if ($overwrite -ne "y" -and $overwrite -ne "Y") {
        Write-Host "Aborted. Existing keystore preserved." -ForegroundColor Yellow
        exit 0
    }
    Remove-Item $resolvedKeystore -Force
}

$password = Read-Host -Prompt "Enter keystore & key password (min 6 characters)" -AsSecureString
$plainPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
)

if ([string]::IsNullOrWhiteSpace($plainPassword) -or $plainPassword.Length -lt 6) {
    Write-Error "Password must be at least 6 characters long."
}

$dname = "CN=$CommonName, O=$Organization, C=US"

Write-Host "`nGenerating keystore at: $resolvedKeystore..." -ForegroundColor Green

& $keytoolPath -genkeypair `
    -v `
    -keystore $resolvedKeystore `
    -alias $Alias `
    -keyalg RSA `
    -keysize 2048 `
    -validity $ValidityDays `
    -storepass $plainPassword `
    -keypass $plainPassword `
    -dname $dname

if (-not (Test-Path $resolvedKeystore)) {
    Write-Error "Failed to generate keystore file."
}

Write-Host "Keystore generated successfully!`n" -ForegroundColor Green

# Generate Base64 for GitHub Actions
$bytes = [System.IO.File]::ReadAllBytes($resolvedKeystore)
$base64 = [System.Convert]::ToBase64String($bytes)

Write-Host "--------------------------------------------------------" -ForegroundColor Yellow
Write-Host "GitHub Actions Secrets Setup Instructions:" -ForegroundColor Yellow
Write-Host "Go to your repository on GitHub: Settings -> Secrets and variables -> Actions" -ForegroundColor Yellow
Write-Host "Add the following Repository Secrets:" -ForegroundColor Yellow
Write-Host "1. RELEASE_KEYSTORE_BASE64 : [Base64 string below]" -ForegroundColor White
Write-Host "2. KEYSTORE_PASSWORD       : $plainPassword" -ForegroundColor White
Write-Host "3. KEY_ALIAS               : $Alias" -ForegroundColor White
Write-Host "4. KEY_PASSWORD            : $plainPassword" -ForegroundColor White
Write-Host "--------------------------------------------------------" -ForegroundColor Yellow
Write-Host "`nBase64 Keystore String (copy all):" -ForegroundColor Cyan
Write-Host $base64 -ForegroundColor White
Write-Host "`nNote: app/release.jks is added to .gitignore. Keep your keystore safe!" -ForegroundColor Magenta
