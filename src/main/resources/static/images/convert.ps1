# Convert logo.txt to logo.png
$base64Content = Get-Content -Path "logo.txt"
$bytes = [Convert]::FromBase64String($base64Content)
[IO.File]::WriteAllBytes("logo.png", $bytes)

# Convert welcome.txt to welcome.png
$base64Content = Get-Content -Path "welcome.txt"
$bytes = [Convert]::FromBase64String($base64Content)
[IO.File]::WriteAllBytes("welcome.png", $bytes) 