# Android release

## One-time setup

### 1. Generate the upload keystore

```sh
cd android
keytool -genkeypair -v \
  -keystore upload-keystore.jks \
  -alias muxy \
  -keyalg RSA -keysize 2048 -validity 10000
```

Save the keystore password and key password somewhere safe (1Password, etc.).
**Losing this keystore means you cannot ship updates** — back it up.

### 2. Create `keystore.properties`

In `android/keystore.properties` (gitignored):

```properties
storeFile=upload-keystore.jks
storePassword=<store password>
keyAlias=muxy
keyPassword=<key password>
```

### 3. Play Console

1. Create the app at https://play.google.com/console (package: `com.muxy.app`).
2. Opt into **Play App Signing** (recommended). Google manages the final
   signing key; you only keep the upload key.
3. Fill out the store listing (title, short/full description, screenshots,
   feature graphic 1024×500, app icon 512×512).
4. Privacy policy URL — host `PRIVACY.md` somewhere public (GitHub Pages
   works) and paste the URL.
5. Complete: Data Safety form, Content Rating questionnaire, Target Audience.
6. Upload the first AAB manually to **Internal testing** to verify before
   automating.

### 4. (Optional) Service account for automated uploads

Only needed if you want `scripts/release.sh --upload` or the GitHub
Actions workflow to push AABs to Play automatically. You can skip this
and upload AABs manually in the Play Console.

**a. Enable the Google Play Android Developer API**

1. Open https://console.cloud.google.com/apis/library/androidpublisher.googleapis.com
2. Pick (or create) a Google Cloud project. Click **Enable**.

**b. Create the service account**

1. Go to https://console.cloud.google.com/iam-admin/serviceaccounts
2. **Create service account**:
   - Name: `muxy-play-publisher` (anything works)
   - **Skip** the "Grant this service account access to project" step —
     it doesn't need any GCP IAM role. Click **Done**.
3. Open the new service account → **Keys** → **Add key** → **Create new
   key** → **JSON**. Save the downloaded file as
   `android/play-service-account.json` (gitignored).

**c. Grant it access in Play Console**

1. https://play.google.com/console → **Users and permissions** → **Invite
   new users**.
2. Email = the service account's email (looks like
   `muxy-play-publisher@<project>.iam.gserviceaccount.com`).
3. **App permissions** → add the Muxy app. Grant:
   - **View app information and download bulk reports** (read)
   - **Manage testing track releases** (push to internal/alpha/beta)
   - **Manage production releases** (only if you want to push to
     production from CI; otherwise leave off and promote manually)
   - **Create, edit, and delete draft apps** is NOT needed.
4. Account permissions tab: leave everything off — per-app perms above
   are enough.
5. **Invite user** → **Save changes**. The first API call may take a few
   minutes to propagate.

**d. For GitHub Actions**

Open `play-service-account.json`, copy the entire contents (raw JSON, no
base64), and paste into the `PLAY_SERVICE_ACCOUNT_JSON` repo secret.

## Local release

```sh
cd android

# Build signed AAB only:
scripts/release.sh 0.1.0 1

# Build + upload to Play internal track (as draft):
scripts/release.sh 0.1.0 1 --upload

# Build + upload to a different track:
scripts/release.sh 0.1.0 1 --upload --track production
```

The AAB is copied to `android/release-artifacts/`. Uploads go in as
**draft** — promote/publish from the Play Console.

## CI release

Run the **Release Android** workflow from GitHub Actions
(`workflow_dispatch`). Inputs:

- `version_name` (required, e.g. `1.0.0`)
- `version_code` (optional; defaults to the run number)
- `track` (default `internal`)

Required GitHub secrets:

- `ANDROID_SIGNING_KEY_BASE64` — `base64 -i upload-keystore.jks` output
- `ANDROID_KEY_STORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- `PLAY_SERVICE_ACCOUNT_JSON` — paste the raw JSON. Optional; if missing,
  the AAB is built and attached as an artifact but not uploaded.
