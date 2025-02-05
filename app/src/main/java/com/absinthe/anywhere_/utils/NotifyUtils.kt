package com.absinthe.anywhere_.utils

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.absinthe.anywhere_.R
import com.absinthe.anywhere_.receiver.NotificationClickReceiver
import com.absinthe.anywhere_.utils.manager.LogRecorder
import com.blankj.utilcode.util.NotificationUtils
import com.blankj.utilcode.util.NotificationUtils.ChannelConfig

object NotifyUtils {

  private const val LOGCAT_CHANNEL_ID = "logcat_channel"
  private const val BACKUP_CHANNEL_ID = "backup_channel"
  private const val WORKFLOW_CHANNEL_ID = "workflow_channel"

  const val LOGCAT_NOTIFICATION_ID = 1002
  const val BACKUP_NOTIFICATION_ID = 1003
  const val WORKFLOW_NOTIFICATION_ID = 1004

  fun createLogcatNotification(context: Context) {
    val channelConfig = ChannelConfig(
      LOGCAT_CHANNEL_ID,
      context.getText(R.string.notification_channel_logcat),
      NotificationUtils.IMPORTANCE_DEFAULT
    )
    val intent = Intent(context, NotificationClickReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    NotificationUtils.notify(
      LOGCAT_NOTIFICATION_ID,
      channelConfig
    ) { param: NotificationCompat.Builder ->
      param.setContentTitle(context.getString(R.string.notification_logcat_title))
        .setContentText(context.getString(R.string.notification_logcat_content))
        .setSmallIcon(R.drawable.ic_cat)
        .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setContentIntent(pendingIntent)
        .build()
    }
    LogRecorder.getInstance().start()
  }

  fun createBackupNotification(context: Service) {
    val channelConfig = ChannelConfig(
      BACKUP_CHANNEL_ID,
      context.getText(R.string.notification_channel_backup),
      NotificationUtils.IMPORTANCE_DEFAULT
    )
    NotificationUtils.notify(
      BACKUP_NOTIFICATION_ID,
      channelConfig
    ) { param: NotificationCompat.Builder ->
      param.setContentTitle(context.getString(R.string.notification_backup_title))
        .setContentText(context.getString(R.string.notification_backup_content))
        .setSmallIcon(R.drawable.ic_logo)
        .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setProgress(0, 0, true)
        .setOngoing(true)
        .setAutoCancel(false)
      context.startForeground(BACKUP_NOTIFICATION_ID, param.build())
    }
  }

  fun createWorkflowNotification(context: Service) {
    val channelConfig = ChannelConfig(
      WORKFLOW_CHANNEL_ID,
      context.getText(R.string.notification_channel_workflow),
      NotificationUtils.IMPORTANCE_LOW
    )
    NotificationUtils.notify(
      WORKFLOW_NOTIFICATION_ID,
      channelConfig
    ) { param: NotificationCompat.Builder ->
      param.setContentTitle(context.getString(R.string.notification_workflow_title))
        .setContentText(context.getString(R.string.notification_workflow_content))
        .setSmallIcon(R.drawable.ic_card_workflow)
        .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
        .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setProgress(0, 0, true)
        .setOngoing(true)
        .setAutoCancel(false)
      context.startForeground(WORKFLOW_NOTIFICATION_ID, param.build())
    }
  }
}
