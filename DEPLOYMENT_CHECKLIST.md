# Parachat Deployment Checklist

## Before Running on Device

### ✅ Database Migrations (CRITICAL)

- [ ] Run `supabase/migrations/0003_contacts.sql` in Supabase SQL Editor
  - Creates `public.contacts` table
  - Needed for contact add/remove features to work

### ✅ Code Compilation

- [ ] `./gradlew :app:compileDebugKotlin` → BUILD SUCCESSFUL
- [ ] `./gradlew :app:assembleDebug` → Build APK

### ✅ Firebase Configuration

- [ ] Firebase Auth enabled (existing ✓)
- [ ] Firebase Realtime Database configured (if using Firebase path)
- [ ] FCM token generation enabled (for future notifications)

### ✅ Supabase Configuration

- [ ] Supabase project initialized
- [ ] All 3 migration scripts applied (0001, 0002, 0003)
- [ ] Storage bucket `chat-media` created (0002_storage_bucket.sql)
- [ ] RLS policies enabled on all tables
- [ ] API key in `SupabaseProvider.kt` is valid

### ✅ Android Manifest Permissions

- [x] READ_CONTACTS (for device contact import)
- [x] CAMERA (for photo capture)
- [x] RECORD_AUDIO (for voice messages)
- [x] ACCESS_FINE_LOCATION (for location sharing)
- [x] POST_NOTIFICATIONS (for push notifications)

---

## First Run on Device

### User Lifecycle

1. **Sign Up**
   - Create new user with email/password
   - Verify user appears in Supabase `users` table
   - Verify user can see all other users in "Contatos" tab

2. **Contact Management**
   - Click "Importar" button → grant READ_CONTACTS permission
   - Select contacts from device
   - Verify contacts appear with "Remover" buttons
   - Add user manually → verify "Adicionar" button
   - Remove user → verify disappears from list

3. **Encryption Test**
   - Send text message to User B
   - Open Supabase SQL: `SELECT content FROM messages LIMIT 1;`
   - Verify content is Base64-encoded (NOT plaintext)
   - On User B device: message displays as plaintext (decrypted)
   - Search in chat: search works on decrypted content

4. **Media Test**
   - Click "+" button in message bar
   - Send image (existing feature ✓)
   - Send video (new feature)
   - Send audio file (new feature)
   - Send generic file (new feature)
   - Tap each to open with system player/handler

5. **Camera Test**
   - Click camera icon
   - Grant camera permission when prompted (NEW: permission gate prevents crash)
   - Take photo
   - Verify photo sends as image message without crashing

6. **Pin Message Test**
   - Long-press any message
   - Pinned banner should appear at top immediately
   - NEW: stay in chat, send new message
   - Verify pinned banner stays visible at top (no need to reopen)

7. **Sign Out & Cleanup**
   - Tap "Sair" (Exit) button
   - Wait for "Cleared local database on sign-out" log
   - Sign back in as different user
   - Verify previous user's data NOT visible
   - NEW: Check Room database is empty
     ```bash
     adb shell "sqlite3 /data/data/com.example.parachat/databases/parachat-db 'SELECT * FROM users;'"
     # Should return empty or new user only
     ```

---

## Device Testing Commands

### Check Local Database
```bash
adb shell sqlite3 /data/data/com.example.parachat/databases/parachat-db \
  "SELECT id, email FROM users; SELECT COUNT(*) FROM messages;"
```

### Check Encryption in Logcat
```bash
adb logcat | grep -E "ChatViewModel|MessageEncryption|HomeViewModel.*Clear"
```

### Clear All App Data (Hard Reset)
```bash
adb shell pm clear com.example.parachat
```

### View SharedPreferences (if used)
```bash
adb shell cat /data/data/com.example.parachat/shared_prefs/*.xml
```

---

## Known Limitations

1. **Encryption Key Derivation**
   - Uses deterministic key from user IDs (not random key exchange)
   - Acceptable for MVP, but upgrade to X3DH for production
   - Recommendation: Use Supabase Edge Functions for key exchange

2. **Contact Sync**
   - Device contacts matched by email/username only
   - Phone numbers not normalized (would need libphonenumber)
   - No auto-sync after import

3. **Message Search on Encrypted Data**
   - Search works by decrypting all messages on-device
   - Not scalable to large conversations
   - Use searchable encryption for production

4. **Forward Secrecy**
   - Not implemented (compromising one key = all messages compromised)
   - Implement message-level key rotation for production

---

## Rollback Plan

If issues arise:

1. **Revert code**
   ```bash
   git revert HEAD~10  # Adjust to your last commit
   ```

2. **Restore databases**
   ```bash
   adb shell pm clear com.example.parachat
   # Supabase: Restore from backup or delete/recreate tables
   ```

3. **Check logs**
   ```bash
   adb logcat | grep -E "ERROR|Exception|java.lang"
   ```

---

## Success Criteria

- [x] Code compiles without errors
- [ ] Encryption works: plaintext sent, Base64 stored, plaintext received
- [ ] Users disappear on sign-out (Room + Supabase clean)
- [ ] Contacts add/remove/import works
- [ ] Media (video, file, audio) sends and opens
- [ ] Camera opens without crash (after permission granted)
- [ ] Pin message updates in real-time
- [ ] No crashes or warnings in logcat


