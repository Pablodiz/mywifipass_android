# User Guide

## Adding a Wi-Fi Pass

### Via QR Code

1. On the main screen, tap **Add Wifi Pass**
2. Point the camera at the Wi-Fi pass QR code (received via email or from the event organizer)
3. The pass is downloaded and added to your network list

### Via Deep Link

If you received a Wi-Fi pass URL (e.g. in an email), tap the link on your Android device and select **MyWifiPass** to open it. The pass is added to your list.

---

## Getting Authorized

Before a certificate can be signed, it may be needed authorization for you to be able to access the network. There are two ways this happens depending on how the network is configured.

### Physical Validator

If a person at the event needs to check your identity:

1. Tap the network card on the main screen
2. The detail screen shows your Wi-Fi pass QR code — show it to the event validator
3. The validator scans it with the MyWifiPass admin app, checks your ID document, and taps **OK**
4. The app gets your certificate signed and configures Wi-Fi

### FIDO2 / Passkey

If the network supports self-validation via passkey:

1. Tap the network card on the main screen
2. Your device shows a biometric prompt — authenticate with fingerprint, face, or screen lock
3. The app gets your certificate signed and configures Wi-Fi

> Passkey registration is done once via the server's web UI at `/fido2/register/`.

---

## Connecting to Wi-Fi

After the certificate is installed, the Wi-Fi connection is configured:

- **Android 11+:** a system dialog asks you to add the network; confirm it
- **Android 10:** the connection is configured directly

---

## Deleting a Wi-Fi Pass

Long-press a network card → confirm deletion. This removes the pass and its certificate data from the app.

On Android 11+ the Wi-Fi network suggestion cannot be removed by the app - go to the device **Wi-Fi settings → Saved networks** and remove it manually.

---

## Admin / Validator Panel

1. Tap the **three-dot menu (⋮)** in the top-right corner
2. Log in with your credentials or by scanning the validator QR token (generated from the server admin panel)
3. Tap **Open QR Scanner** and scan the user's Wi-Fi pass QR code
4. A dialog shows a ✓ (valid) or ✗ (invalid) indicator with the user's identity document
5. Tap **OK** to authorize, or dismiss to cancel
