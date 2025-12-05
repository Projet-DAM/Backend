# Backend Image/File Serving Debug Guide

## ✅ What I Fixed

### 1. **Static File Serving Configuration**
- Moved static file serving **before** the global JWT guard
- Added request logging to see all incoming requests
- Consolidated duplicate static serving code
- Added detailed console logs showing the upload directory path

### 2. **Directory Structure**
- Auto-creates all required subdirectories on startup:
  - `./uploads/messages/images/`
  - `./uploads/messages/audio/`
  - `./uploads/tournois/`

### 3. **CORS Configuration**
- Added `allowedHeaders` to ensure proper cross-origin requests
- Allows all methods needed for file access

### 4. **Diagnostic Endpoints** (NEW)
Added helper endpoints to debug file serving issues:

#### Check Uploads Directory Structure
```
GET http://localhost:3000/diagnostics/uploads/check
```
Returns complete directory tree with all files and their public URLs.

#### Test Specific File
```
GET http://localhost:3000/diagnostics/uploads/test/messages/images/filename.jpg
```
Checks if a specific file exists and shows the expected URL.

#### Get Network Info
```
GET http://localhost:3000/diagnostics/network/info
```
Shows all local IP addresses you can use in your Android app.

---

## 🔍 Step-by-Step Debugging

### Step 1: Start Your Backend
```powershell
npm run start:dev
```

Look for these console logs:
```
Created directory: ./uploads/messages/images
Created directory: ./uploads/messages/audio
Serving static files from: C:\Users\...\Backend\uploads
Application is running on: http://localhost:3000
```

### Step 2: Check Your Network IP
Visit: `http://localhost:3000/diagnostics/network/info`

You'll see something like:
```json
{
  "addresses": [
    {
      "interface": "Wi-Fi",
      "address": "192.168.1.15",
      "testUrl": "http://192.168.1.15:3000/uploads/"
    }
  ]
}
```

**Update your Android `ApiConfig.BASE_URL_PHYSICAL`** with this IP.

### Step 3: Test File Upload
1. Upload a test image:
   ```bash
   POST http://localhost:3000/uploads/messages/image
   Content-Type: multipart/form-data
   
   file: [select an image]
   ```

2. Response will show the file path:
   ```json
   {
     "filePath": "/uploads/messages/images/abc-123.jpg"
   }
   ```

### Step 4: Verify File Is Accessible

#### Option A: In Browser (PC)
Visit: `http://localhost:3000/uploads/messages/images/abc-123.jpg`

If you see the image ✅ Backend is working correctly.
If you get 404 ❌ Check console logs.

#### Option B: From Your Phone's Browser
Visit: `http://192.168.1.15:3000/uploads/messages/images/abc-123.jpg` (use your actual IP)

If it works ✅ Your network is fine.
If timeout ❌ Check firewall (see Step 6).

### Step 5: Check Upload Directory
Visit: `http://localhost:3000/diagnostics/uploads/check`

This shows all files in your uploads folder with their URLs:
```json
{
  "structure": {
    "files": [],
    "directories": [
      {
        "name": "messages",
        "contents": {
          "directories": [
            {
              "name": "images",
              "contents": {
                "files": [
                  {
                    "name": "abc-123.jpg",
                    "url": "/uploads/messages/images/abc-123.jpg"
                  }
                ]
              }
            }
          ]
        }
      }
    ]
  }
}
```

### Step 6: Windows Firewall (If Phone Can't Connect)

#### Quick Test - Temporarily Disable Firewall
1. Open Windows Security
2. Firewall & network protection
3. Turn off for Private network (temporarily)
4. Test from phone
5. **Turn back ON**

#### Proper Fix - Add Firewall Rule
```powershell
# Run as Administrator
New-NetFirewallRule -DisplayName "Node.js Dev Server" -Direction Inbound -Action Allow -Protocol TCP -LocalPort 3000
```

### Step 7: Check Database Paths

Your upload controller returns paths correctly:
```typescript
filePath: `/uploads/messages/images/${file.filename}`
```

When saving messages, ensure you store **exactly** this path in `mediaUrl` field.

❌ **Wrong**: `C:\Users\...\uploads\messages\images\file.jpg` (absolute path)
❌ **Wrong**: `uploads\messages\images\file.jpg` (backslashes)
✅ **Correct**: `/uploads/messages/images/file.jpg` (relative with forward slashes)

---

## 📱 Android Integration

Your Android code should construct URLs like:
```kotlin
val imageUrl = "${ApiConfig.BASE_URL_PHYSICAL}${message.mediaUrl}"
// Example: http://192.168.1.15:3000/uploads/messages/images/abc-123.jpg
```

Where:
- `ApiConfig.BASE_URL_PHYSICAL` = `"http://192.168.1.15:3000"` (from diagnostics/network/info)
- `message.mediaUrl` = `"/uploads/messages/images/abc-123.jpg"` (from database)

---

## 🔍 Monitor Requests in Real-Time

The backend now logs every request:
```
[2025-12-05T12:00:00.000Z] GET /uploads/messages/images/abc-123.jpg
```

Watch the console when you open chat in Android:
- If you see the GET request → Backend received it
- If you don't see it → Network issue or wrong URL construction

---

## 🧪 Quick Test Checklist

- [ ] Backend starts without errors
- [ ] Console shows "Serving static files from: ..."
- [ ] `GET /diagnostics/network/info` returns your local IP
- [ ] Upload test image via Swagger/Postman
- [ ] Access image in PC browser at `http://localhost:3000/uploads/...`
- [ ] Access image from phone browser at `http://192.168.1.X:3000/uploads/...`
- [ ] Check firewall if phone can't connect
- [ ] Verify database stores relative paths (not absolute)
- [ ] Android app uses correct BASE_URL with local IP

---

## 🐛 Common Issues & Solutions

### Issue: 404 Not Found in Browser (PC)
**Cause**: Static serving not configured correctly
**Solution**: Already fixed - restart backend

### Issue: Works on PC, fails on Phone
**Cause**: Windows Firewall blocking port 3000
**Solution**: Add firewall rule (Step 6)

### Issue: Wrong IP Address
**Cause**: Using wrong network interface
**Solution**: Check `/diagnostics/network/info` for correct IP

### Issue: Database has absolute paths
**Cause**: Upload controller modified or wrong field used
**Solution**: Use the `filePath` returned by upload endpoint exactly as-is

### Issue: Backslashes in paths
**Cause**: Windows path separator used
**Solution**: Upload controller already uses forward slashes - check if anything modifies it

---

## 📝 Summary of Changes

1. **`src/main.ts`**: 
   - Request logging middleware
   - Fixed static file serving order (before guards)
   - Auto-create all upload subdirectories

2. **`src/diagnostics/`** (NEW):
   - Upload directory scanner
   - File existence checker  
   - Network IP helper

3. **`src/app.module.ts`**:
   - Registered DiagnosticsModule

All uploads are now publicly accessible at `/uploads/**` without authentication.
All diagnostic endpoints are available to help debug.
