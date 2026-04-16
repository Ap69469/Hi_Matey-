package edu.utap.demoproject_mrl.tasks

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import edu.utap.demoproject_mrl.MainActivity
import edu.utap.demoproject_mrl.R

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val taskTitle = inputData.getString("task_title") ?: "Task Reminder"
        showNotification(taskTitle)
        return Result.success()
    }

    private fun showNotification(taskTitle: String) {
        val appContext = applicationContext
        val channelId = "himatey_reminders"

        // ✅ Permission guard for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    appContext, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) return
        }

        // ✅ Create notification channel
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId,
            "Hi Matey Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Task reminder notifications"
        }
        notificationManager.createNotificationChannel(channel)

        // ✅ PendingIntent to open app on tap
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // ✅ Build notification with applicationContext
        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Hi Matey Reminder 🔔")
            .setContentText("Time for: $taskTitle")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Time for: $taskTitle"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // ✅ Opens app on tap
            .build()

        // ✅ Use NotificationManagerCompat + stable ID per task
        NotificationManagerCompat.from(appContext)
            .notify(taskTitle.hashCode(), notification)
    }
}