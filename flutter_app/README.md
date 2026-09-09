# CPM Core — Flutter rebuild

This branch implements a first Android client and a Firebase callable backend.
It is **not deployed to Firebase** and must not be described as a working production service.

## Implemented source
- Arabic Material 3 UI, email/password login, verified phone/password login,
  registration, password reset, and project invitation-code joining.
- Office dashboard, logo upload/display, unanswered RFI and pending-delivery counts.
- Seven project sections: overview, stages/items, RFI, NCR, files, meeting minutes, team.
- Photo preview and submission; engineering review and revision requests.
- Project-scoped roles, RFI recipient restrictions, moderated publication.
- Transactional limits: 10 stages, 5 items/stage, 1 owner, 3 contractors, 1 resident engineer.
- Server-side subscription gating; no automatic deletion of retained project data.
- Separate Flutter web entry point for subscription administration (lib/admin.dart).
- GitHub Actions analysis, tests, and debug APK build.

## Validation
The first revision passed Flutter analysis and all Flutter/backend tests on GitHub Actions.
Check the latest workflow result for subsequent revisions.
Physical-device tests, Firebase integration tests, and visual acceptance are still outstanding.

## Existing account compatibility
The Kotlin app stores accounts in Room on the device. Those accounts do not automatically
become Firebase accounts. No passwords or device data have been migrated. The user-facing
email/phone plus password flow is preserved. For phone login, the phone must first be linked
and verified by SMS in Firebase; arbitrary contact numbers cannot be used as trusted identities.

## Run Android
Install Flutter stable and Android SDK, then from repository root:
```sh
bash tools/bootstrap_android.sh
cd flutter_app
flutter pub get
flutter test
flutter run
```
The generated Android package remains `com.aistudio.cpdms.qtwxbp`.
Public Android Firebase identifiers are in `lib/firebase_options.dart`.
The APK must be signed with the existing release key to update an installed production app;
the CI artifact is debug-signed and might require a separate test device/install.

## Firebase prerequisites
1. Authenticate Firebase CLI as a project administrator of `cpm-core`.
2. Review existing Firestore and Storage rules before installing this branch's rules.
   They deny direct client access; all project operations use the authenticated backend.
   Do not overwrite rules for other applications without reviewing their compatibility.
3. Enable Email/Password and Phone providers, provision Firestore and Storage,
   confirm Cloud Functions billing, and configure Android SHA fingerprints for phone verification.
4. Set `FIREBASE_WEB_API_KEY` for the Functions codebase to the project's public Firebase API key.
5. Install Functions dependencies and deploy the `cpm-flutter` codebase, then reviewed rules.
6. Register an administrator account, provision its superAdmin claim using
   `node tools/provision-admin.mjs FIREBASE_USER_UID` from an authenticated administrative runtime,
   and sign in again. Use the separate admin entry point to activate office subscriptions.

## Separate admin web build
Use the existing Web app's Firebase App ID; the Android App ID is not interchangeable:
```sh
flutter create --platforms=web .
flutter build web -t lib/admin.dart --dart-define=FIREBASE_WEB_APP_ID=YOUR_EXISTING_WEB_APP_ID
```
Hosting is not configured or deployed in this branch.

## Known limitations before production
- Firebase deployment and account/data migration are not complete.
- Project records are currently aggregated into one Firestore document, capped at 850 KB.
  This must be split into paginated subcollections before long-running production use.
- Files are limited to JPEG/PNG/PDF and 5 MB each in this initial callable-upload implementation.
- Admin office listing currently returns the first 200 offices; pagination is required for scale.
- Pending upload cleanup, backup scheduling, App Check enforcement, richer audit/history,
  and end-to-end emulator tests remain release work.
- The PDF leaves deletion/archiving after the retention year undecided. This code does not delete
  data at that boundary.
