# Indian Messenger — Android starter project

Indian Messenger is a Firebase-free Android Java starter project with a tricolour-inspired header, username/password account flow, exact User ID search, Supabase REST connectivity, and GitHub Actions APK build.

## Important honesty note

This ZIP is a **starter MVP, not a fully working secure messenger yet**. The sign-in and exact-ID lookup become usable after Supabase setup. The chat-send button deliberately does not send messages because the end-to-end encryption key exchange, durable private-key storage, sender/recipient key wrapping, and full messages UI must be implemented and audited before claiming secure E2E. Do not use it for sensitive messages yet.

A production messenger should use a reviewed protocol/library (for example Signal Protocol), device key verification, key rotation, encrypted local storage, abuse/report/block controls, account recovery planning, rate limits, and security review. A homemade RSA/AES design is not a safe substitute.

## 1. Create Supabase project

1. Create a project at https://supabase.com/.
2. In Project Settings → API, copy the **Project URL** and **publishable/anon key**.
3. Open `app/src/main/java/com/indianmessenger/app/MainActivity.java`.
4. Replace:
   - `https://YOUR_PROJECT.supabase.co`
   - `YOUR_SUPABASE_ANON_KEY`
5. In Supabase SQL Editor, run `supabase/schema.sql`.
6. In Authentication settings, configure email/password authentication. For this username-only prototype, the app maps a User ID to a synthetic `.invalid` email. Turn off email confirmation for testing, otherwise signup will not immediately return a session. This approach is a prototype workaround, not a verified-email account system.
7. Enable the `profiles` and `messages` tables in the API schema if your project requires it.

## 2. GitHub Actions APK

1. Upload the contents of this ZIP to a new repository named `Indian-Messenger`.
2. Ensure the workflow file path is `.github/workflows/android-build.yml`.
3. Open the repository's **Actions** tab.
4. Run **Build Indian Messenger APK** or push to `main`.
5. Download the `Indian-Messenger-debug-apk` artifact from the completed workflow.

## Current limitations

- Profile creation uses the auth user UUID returned by Supabase. If signup returns no session, check the email-confirmation setting and sign in again.
- Username exact-search is implemented against the `profiles` table.
- Gender and birth date are optional in this starter; add validation/consent and age-appropriate handling before public release.
- The full messages list, realtime updates, push notifications, attachment transfer, blocking/reporting, and E2E key exchange are not implemented.
- The displayed Indian tricolour header is a basic placeholder; add generated vector launcher resources before release.
- Free Supabase quotas and service availability may change. Free tier is not a guarantee of permanent zero cost.
- Never place a Supabase `service_role` secret in the APK. Only use the public anon/publishable key and enforce RLS.
