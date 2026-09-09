# CPM Core Flutter rebuild

Status: domain foundation only; not an executable application or APK yet.
Source of requirements: CPM_Core_Requirements_AR_Final_v2.pdf (8 September 2026).

Confirmed Firebase configuration from the owner's uploaded Android configuration:
- Project: `cpm-core`
- Android package: `com.aistudio.cpdms.qtwxbp`
- Android Firebase app: `1:936725772955:android:5f9dc400e992f09468e43e`
- Storage bucket: `cpm-core.firebasestorage.app`

The uploaded configuration was validated but is not published in this branch.
Configuration identifies the app; it does not grant administrative Firebase access.

## Existing authentication contract

The Kotlin source accepts email or phone plus password. Registration creates a
user, optionally an engineering office. A signed-in user joins a project using
an invitation code. Preserve this user flow in Flutter. Existing accounts are
stored in Android Room; they are not automatically Firebase accounts. A migration
and secure backend implementation remain required. Do not ship the seeded demo
credentials as production authentication or store plaintext passwords.

## Implemented

- Project-scoped role policy, including resident engineer administrative limits.
- Limits: ten stages, five items per stage, one owner, three contractors, one resident engineer.
- RFI recipient checks, approved-item progress, and non-destructive retention decisions.
- Policy tests written; not run because Flutter/Dart are unavailable in this environment.

## Remaining implementation

1. Generate Flutter Android platform files using the existing package name.
2. Implement Firebase authentication and the existing login/register/join flow.
3. Enforce project membership, permissions, atomic team/stage/item limits on the server.
4. Implement dashboard, seven project tabs, stages, photo submissions, RFI, NCR,
   moderated attachments, meeting minutes and team-code provisioning.
5. Persist subscription retention state without automatic deletion after one year.
6. Test authentication, project isolation and workflows; build and test Android APK.

Run policy tests with `flutter test` once Flutter is available.
