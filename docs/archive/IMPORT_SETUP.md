# Import Setup — YouTube & Spotify

This guide explains how to use the Import feature and what manual steps are required for **Spotify import** to work.

---

## YouTube Import (No Setup Required)

1. Open **Library** tab.
2. Tap the **Import** icon (download arrow) in the header.
3. Select the **YouTube** tab.
4. Paste a YouTube playlist URL (e.g. `https://www.youtube.com/playlist?list=...`).
5. Tap **Fetch playlist**.
6. Review the playlist and tap **Import X tracks**.
7. Tracks are downloaded from YouTube and added to a new Shuckler playlist.

**Supported URL formats:**
- `https://www.youtube.com/playlist?list=PLxxx`
- `https://youtube.com/watch?v=VIDEO_ID&list=PLxxx`

---

## Spotify Import (Manual Setup Required)

Spotify import uses the Spotify Web API. You need to set up a Spotify app and add your Client ID to the project.

### Step 1: Create a Spotify App

1. Go to [Spotify Developer Dashboard](https://developer.spotify.com/dashboard).
2. Log in with your Spotify account.
3. Click **Create app**.
4. Fill in:
   - **App name:** e.g. `Shuckler` or `My Music Import`
   - **App description:** Optional (e.g. `Personal music import`)
   - **Redirect URI:** `shuckler://spotify-callback`
   - **Website:** Optional (can leave blank)
   - **Which API/SDKs are you planning to use?** Check **Web API** (and optionally **Web Playback SDK**, **Android**)
   - **Android package name** (if shown): `com.shuckler.app`
5. Accept the terms and click **Save**.

### Step 2: Get Your Client ID

1. In the app dashboard, open your app.
2. Copy the **Client ID** (not the Client Secret).

### Step 3: Add Client ID to the Project

1. Open `gradle.properties` in the project root (create it if it doesn’t exist).
2. Add:

```properties
SPOTIFY_CLIENT_ID=your_client_id_here
```

Replace `your_client_id_here` with your Client ID.

3. Sync Gradle and rebuild the app.

### Step 4: Add Redirect URI in Spotify Dashboard

Ensure the redirect URI in your Spotify app matches exactly:

```
shuckler://spotify-callback
```

### Step 5: Use Spotify Import

1. Open **Library** tab.
2. Tap the **Import** icon.
3. Select the **Spotify** tab.
4. Tap **Connect Spotify**.
5. Log in with Spotify in the browser.
6. Accept the permissions.
7. You return to Shuckler; tap **Load my playlists**.
8. Select a playlist.
9. Tap **Import X tracks from YouTube**.

**Note:** Spotify does not provide audio files. Each track is searched on YouTube and downloaded from there. The first match may be a different version (live, cover, etc.). This is a limitation of the current flow.

---

## Troubleshooting

### "Spotify Client ID not configured"
- Check that `SPOTIFY_CLIENT_ID` is in `gradle.properties`.
- Run **Sync Project with Gradle Files** and rebuild.

### "Invalid playlist URL" (YouTube)
- Ensure the URL is a playlist (contains `list=`).
- Try the full URL: `https://www.youtube.com/playlist?list=PLxxx`.

### Spotify redirect not working
- Confirm the redirect URI in the Spotify dashboard is exactly `shuckler://spotify-callback`.
- Check that the intent filter in `AndroidManifest.xml` is present for the Spotify callback.

### Import fails or times out
- Check your internet connection.
- Some YouTube videos may be unavailable or region-restricted.
- Some Spotify tracks may not match well on YouTube.
