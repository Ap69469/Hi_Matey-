Hi Matey
A couples task-sharing, fitness tracking, and daily routine Android app.
Hi Matey helps couples and accountability partners stay coordinated in their daily lives. 
Most productivity apps are either fully private or fully shared — Hi Matey bridges that gap by separating personal tasks and fitness data (private, on-device) 
from shared responsibilities (real-time synced via Firestore),
giving partners one unified space without sacrificing privacy.
Features
1. Shared Tasks — Real-Time Sync
Both partners can create, assign, complete, and delete tasks from a shared list. Every change propagates to the other device instantly via Firestore's addSnapshotListener — no polling, no manual refresh. Tasks show who they are assigned to, a completion timestamp, and strikethrough when done. Notifications fire on both task creation and completion so neither partner misses an update.
2. Daily Routine — Private Tasks + Streaks
Each user has their own private daily task list stored locally in Room Database. Tasks track streaks using a fire emoji counter — completing a task increments the streak, missing a day resets it to zero. Tasks reset at midnight via resetTasksIfNewDay(). Reminders are set per task using AlarmManager.setExactAndAllowWhileIdle(), which fires exactly at the scheduled time even in Doze mode.
3. Fitness Tracking — Timer + Photos
A private fitness tracker with a workout timer, 8 workout types (Running, Cycling, Swimming, Weight Training, Yoga, Walking, Sports, General), a weekly calendar showing workout history, and monthly stats. After a workout, users can capture a photo via the system camera. Photos are uploaded to Firebase Storage under images/{userUid}/{uuid}.jpg — strictly per-user isolated. The fullscreen photo viewer uses ViewPager2 for swipe navigation.

Tech Stack
LayerTechnologyLanguageKotlinArchitectureMVVM (Model-View-ViewModel)UIFragments, RecyclerView, ViewBinding, ViewPager2AuthFirebase AuthenticationCloud DatabaseFirebase Firestore (real-time listeners)Local DatabaseRoom 2.7.0 (SQLite, suspend DAOs, LiveData)Cloud StorageFirebase Storage — Blaze planAsyncKotlin Coroutines + LiveDataNavigationJetpack Navigation ComponentRemindersAlarmManager.setExactAndAllowWhileIdle()Image LoadingGlide v4.16.0UI ComponentsMaterial Components (buttons, theming)

Architecture
Hi Matey follows MVVM with a single-activity pattern using Jetpack Navigation:
app/
├── auth/               # Firebase Auth — Register & Sign In
├── database/           # Room DB — AppDatabase, DAOs, migrations v1→v4
├── fitness/            # Workout timer, stats, weekly calendar
├── home/               # Home dashboard — daily progress + motivational quotes
├── model/              # Data models (Task, SharedTask, WorkoutSession)
├── photos/             # Photo gallery + fullscreen ViewPager2 viewer
├── settings/           # Partner invite system + dark mode toggle
├── shared/             # Shared tasks — Firestore real-time sync
├── tasks/              # Daily routine — private tasks + streaks
└── viewmodel/          # SharedViewModel — LiveData + coroutines
LayerComponentsPatternUIFragments + XML LayoutsObserve LiveData · NavController.navigate()ViewModelSharedViewModelviewModelScope coroutines · MutableLiveDataRepositoryPhotoStorage · ReminderReceiverSuspend functions · AlarmManager callbacksDataRoom DB · Firestore · Firebase Storage · Firebase AuthDAOs · addSnapshotListener · downloadUrl

Real-Time Partner Sync — How It Works
The core technical challenge was syncing task completion between two users without FCM push notifications.
Both users write to and read from the same Firestore sharedTasks collection, filtered by their partnershipId. Firestore's addSnapshotListener pushes every change to all connected devices the moment it is confirmed on the server.
kotlinfirestore.collection("sharedTasks")
    .whereEqualTo("partnershipId", partnershipId)
    .addSnapshotListener { snapshot, error ->
        if (snapshot != null) {
            // hasPendingWrites guard — only fire notifications on
            // server-confirmed snapshots, not local optimistic updates
            val tasks = snapshot.documents.mapNotNull { it.toObject(SharedTask::class.java) }
            val serverConfirmed = snapshot.documents.none { it.metadata.hasPendingWrites() }
            if (serverConfirmed && !isFirstLoad) {
                notifyPartnerOfChanges(tasks)
            }
        }
    }
Key decisions:

hasPendingWrites() guard prevents self-notification on optimistic local writes — Firestore fires the listener twice per write; notifications only fire on server confirmation
Snapshot listener attached in onStart() and removed in onDestroyView() to prevent memory leaks
isFirstLoad flag prevents notifications firing when the fragment first subscribes


Partner Invite System
Two users must be linked through an active Firestore partnership before any shared data is accessible. The invite system uses a two-way accept/decline flow.
StateUser A (Inviter)User B (Receiver)Shared TasksNo partnershipInvite User buttonInvite User buttonHiddenPendingInvite sent — waitingYellow card: Accept/DeclineStill hiddenActiveActive partner · Remove buttonActive partner · Remove buttonVisible + syncingDeclinedFree to invite againFree to invite againHiddenRemovedFree to invite againFree to invite againHidden

Firebase Schema
CollectionKey FieldsTypesusersemail, uid, joinedAtString, String, Timestamppartnershipsmembers, memberEmails, status, createdAt[String], [String], String (pending|active), LongsharedTasksid, title, assignedTo, assignedToUid, createdBy, partnershipId, isCompleted, completedAt, completedBy, reminderTimeString×7, Boolean, Long, StringuserPhotosownerUid, uuid, byteSize, timeStampString, String, Long, @ServerTimestamp
Key Engineering Challenges
1. Kotlin Boolean + Firestore Silent Failure (2 days)
Checkboxes appeared to sync visually but isCompleted was never written to Firestore — no crash, no Logcat error. Root cause: Kotlin compiles isCompleted into getIsCompleted(), but Firestore's Java CustomClassMapper expects getCompleted(). Fixed with @get:PropertyName("isCompleted") and @set:PropertyName("isCompleted") annotations.
2. Partner Notification Firing Only Once
Firestore fires the snapshot listener twice per write (optimistic + server-confirmed). previousTasks was being updated on the first fire, so the diff showed no change by the time the server snapshot arrived. Fixed by moving previousTasks = tasks inside the hasPendingWrites() == false block.
3. WorkManager Reminder Lag (10-15 Minutes)
Task reminders were firing 10-15 minutes late on Android 12+ due to Doze mode batching. Replaced WorkManager entirely with AlarmManager.setExactAndAllowWhileIdle() which bypasses Doze and fires at exactly the scheduled time.
4. Firestore Security Rules — PERMISSION_DENIED
Partner invite silently failed with no toast or error. Firestore's allow read covers both get and list, but cannot evaluate resource.data on list queries, returning PERMISSION_DENIED silently. Fixed by splitting into separate allow get (with member check) and allow list (open to auth), and adding addOnFailureListener to every Firestore call.
5. Room Migration Crash
App crashed on launch after adding a workoutType field: A migration from 3 to 4 was required but not found. MIGRATION_3_4 was defined but never passed to addMigrations(). Fixed by adding all migrations to the builder chain.

Getting Started
Prerequisites

Android Studio Hedgehog (2023.1.1) or later
Android SDK API 34+ (minSdk 34, targetSdk 36)
Two Android emulators: Pixel 4 API 36 and Pixel 6 API 36
Built For
CS378 Android Application Development · University of Texas at Austin · Spring 2026
Solo project — all design, implementation, debugging, and documentation by Amulya Pendyala.

Author
Amulya Pendyala
MSCS Student @ UT
