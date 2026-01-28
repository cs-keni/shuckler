# Sync to Main Branch Instructions

Your local repository is on `master` but GitHub has `main`. Follow these steps to consolidate to a single `main` branch.

## Quick Method (PowerShell Script)

1. Open PowerShell in this directory
2. Run: `.\sync_to_main.ps1`

## Manual Method (Step by Step)

Run these commands in your terminal (PowerShell, Command Prompt, or Git Bash):

### Step 1: Fetch from GitHub
```bash
git fetch origin
```
This downloads information about branches on GitHub without changing your local files.

### Step 2: Checkout the main branch from GitHub
```bash
git checkout -b main origin/main
```
This creates a local `main` branch based on GitHub's `main` branch (which has LICENSE).

### Step 3: Pull the LICENSE file
```bash
git pull origin main
```
This ensures you have the latest version from GitHub.

### Step 4: Add your local files
```bash
git add .
```
This stages your local files (PHASES.md, project.txt, .gitignore, etc.)

### Step 5: Commit your files
```bash
git commit -m "Add project planning documents and setup files"
```

### Step 6: Push to main
```bash
git push -u origin main
```
This pushes your local files to GitHub's main branch and sets it as upstream.

### Step 7: Delete the master branch (optional)
```bash
git branch -d master
```
This removes the local `master` branch since you're now using `main`.

## Verify

After completing the steps:
- ✅ You should see `LICENSE` file in your directory
- ✅ `git branch` should show `* main` (asterisk means current branch)
- ✅ Your files (PHASES.md, project.txt, etc.) should be on GitHub

## Troubleshooting

**If Step 2 fails with "fatal: A branch named 'main' already exists":**
```bash
git checkout main
git pull origin main
```

**If you get merge conflicts:**
- Your local files and GitHub's LICENSE should merge fine
- If prompted, just complete the merge

**If authentication fails:**
- Use a Personal Access Token (not password) when prompted
- Or set up SSH authentication
