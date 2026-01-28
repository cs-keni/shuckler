# PowerShell script to sync with GitHub main branch
# Run this in PowerShell: .\sync_to_main.ps1

Write-Host "Fetching from GitHub..." -ForegroundColor Cyan
git fetch origin

Write-Host "`nChecking out main branch from origin..." -ForegroundColor Cyan
git checkout -b main origin/main

Write-Host "`nPulling latest changes..." -ForegroundColor Cyan
git pull origin main

Write-Host "`nStaging your local files..." -ForegroundColor Cyan
git add .

Write-Host "`nCommitting your files..." -ForegroundColor Cyan
git commit -m "Add project planning documents and setup files"

Write-Host "`nPushing to main branch..." -ForegroundColor Cyan
git push -u origin main

Write-Host "`nDeleting local master branch..." -ForegroundColor Cyan
git branch -d master

Write-Host "`n✅ Done! You're now on the main branch with LICENSE file." -ForegroundColor Green
Write-Host "Current branch: $(git branch --show-current)" -ForegroundColor Green
