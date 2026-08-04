$rsa = [System.Security.Cryptography.RSA]::Create(2048)
$privateKeyBytes = $rsa.ExportPkcs8PrivateKey()
$publicKeyBytes = $rsa.ExportSubjectPublicKeyInfo()
$privateKeyPem = [System.Convert]::ToBase64String($privateKeyBytes, [System.Base64FormattingOptions]::InsertLineBreaks)
$publicKeyPem = [System.Convert]::ToBase64String($publicKeyBytes, [System.Base64FormattingOptions]::InsertLineBreaks)

$privatePemStr = "-----BEGIN PRIVATE KEY-----`n" + $privateKeyPem + "`n-----END PRIVATE KEY-----"
$publicPemStr = "-----BEGIN PUBLIC KEY-----`n" + $publicKeyPem + "`n-----END PUBLIC KEY-----"

Set-Content -Path "cloudfront_private_key.pem" -Value $privatePemStr
Set-Content -Path "cloudfront_public_key.pem" -Value $publicPemStr
