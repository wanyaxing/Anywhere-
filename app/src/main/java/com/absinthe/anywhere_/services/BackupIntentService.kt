package com.absinthe.anywhere_.services

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.JobIntentService
import com.absinthe.anywhere_.BuildConfig
import com.absinthe.anywhere_.constants.GlobalValues
import com.absinthe.anywhere_.utils.*
import com.absinthe.anywhere_.utils.manager.URLManager
import com.blankj.utilcode.util.NotificationUtils
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BackupIntentService : JobIntentService() {

  override fun onStart(intent: Intent?, startId: Int) {
    super.onStart(intent, startId)
    NotifyUtils.createBackupNotification(this)
  }

  override fun onHandleWork(intent: Intent) {
    NotifyUtils.createBackupNotification(this)

    if (GlobalValues.webdavHost.isEmpty() ||
      GlobalValues.webdavUsername.isEmpty() ||
      GlobalValues.webdavPassword.isEmpty()
    ) {
      stopForeground(true)
      return
    }

    val sardine = OkHttpSardine()
    sardine.setCredentials(GlobalValues.webdavUsername, GlobalValues.webdavPassword)

    try {
      val hostDir = GlobalValues.webdavHost + URLManager.BACKUP_DIR

      if (!sardine.exists(hostDir)) {
        sardine.createDirectory("Anywhere-")
        sardine.createDirectory("Backup")
      }

      val backupName =
        "Anywhere-Backups-${AppTextUtils.webDavFormatDate}-${BuildConfig.VERSION_NAME}.awbackups"

      StorageUtils.exportAnywhereEntityJsonString()?.let { content ->
        CipherUtils.encrypt(content)?.let { encrypted ->
          if (!sardine.exists(hostDir + backupName)) {
            sardine.put(hostDir + backupName, encrypted.toByteArray())

            val list = sardine.list(hostDir).filter { !it.isDirectory }.toMutableList()
            if (list.size > 25) {
              list.sortBy { it.displayName }
              while (list.size > 25) {
                sardine.delete(hostDir + list.removeAt(0).displayName)
              }
            }

            NotificationUtils.cancel(NotifyUtils.BACKUP_NOTIFICATION_ID)
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
      GlobalScope.launch(Dispatchers.Main) {
        Toast.makeText(this@BackupIntentService, "Backup failed: $e", Toast.LENGTH_LONG).show()
      }
      stopForeground(true)
    } finally {
      stopForeground(true)
    }
  }

  companion object {

    private const val JOB_ID = 1

    fun enqueueWork(context: Context, work: Intent) {
      enqueueWork(context, BackupIntentService::class.java, JOB_ID, work)
    }
  }
}
