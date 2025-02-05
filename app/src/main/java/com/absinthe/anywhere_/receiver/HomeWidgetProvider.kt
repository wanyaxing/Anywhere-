package com.absinthe.anywhere_.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.absinthe.anywhere_.BuildConfig
import com.absinthe.anywhere_.R
import com.absinthe.anywhere_.constants.AnywhereType
import com.absinthe.anywhere_.constants.Const
import com.absinthe.anywhere_.model.database.AnywhereEntity
import com.absinthe.anywhere_.services.widget.AppRemoteViewsService
import com.absinthe.anywhere_.ui.shortcuts.ShortcutsActivity
import com.absinthe.anywhere_.utils.FlagDelegate

class HomeWidgetProvider : AppWidgetProvider() {
  /**
   * 每次窗口小部件被更新都调用一次该方法
   */
  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    val thisWidget = ComponentName(context, HomeWidgetProvider::class.java)
    // 点击列表触发事件
    val clickIntent = Intent(context, HomeWidgetProvider::class.java).apply {
      // 设置 Action，方便在 onReceive 中区别点击事件
      action = CLICK_ACTION
      data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
    }
    val pendingIntentTemplate = PendingIntent.getBroadcast(
      context, 0, clickIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or FlagDelegate.PENDING_INTENT_FLAG_MUTABLE
    )

    appWidgetIds.forEach { appWidgetId ->
      // 创建一个 RemoteView
      val remoteViews = RemoteViews(context.packageName, R.layout.widget_home).apply {

        // 把这个 Widget 绑定到 RemoteViewsService
        val intent = Intent(context, AppRemoteViewsService::class.java).apply {
          putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

        // 设置适配器
        setRemoteAdapter(R.id.lv_list, intent)

        // 设置当显示的 widget_list 为空显示的 View
        setEmptyView(R.id.lv_list, R.layout.widget_home)
      }

      remoteViews.setPendingIntentTemplate(R.id.lv_list, pendingIntentTemplate)

      // 更新 Widget
      appWidgetManager.apply {
        updateAppWidget(thisWidget, remoteViews)
        notifyAppWidgetViewDataChanged(appWidgetIds, R.id.lv_list)
      }
    }

    super.onUpdate(context, appWidgetManager, appWidgetIds)
  }

  /**
   * 接收窗口小部件点击时发送的广播
   */
  override fun onReceive(context: Context, intent: Intent) {
    if (CLICK_ACTION == intent.action) {
      val newIntent = Intent(context, ShortcutsActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      val ae: AnywhereEntity? = intent.getParcelableExtra(Const.INTENT_EXTRA_WIDGET_ENTITY)

      if (ae != null) {
        if (ae.type == AnywhereType.Card.IMAGE) {
          newIntent.action = ShortcutsActivity.ACTION_START_IMAGE
          newIntent.putExtra(Const.INTENT_EXTRA_SHORTCUTS_CMD, ae.param1)
        } else {
          newIntent.action = ShortcutsActivity.ACTION_START_FROM_WIDGET
          newIntent.putExtra(Const.INTENT_EXTRA_WIDGET_ENTITY, ae)
        }
        context.startActivity(newIntent)
      }
    }

    if ("miui.appwidget.action.APPWIDGET_UPDATE" == intent.action) {
      onUpdate(context, AppWidgetManager.getInstance(context), IntArray(0))
    } else {
      super.onReceive(context, intent)
    }
  }

  /**
   * 每删除一次窗口小部件就调用一次
   */
  override fun onDeleted(context: Context, appWidgetIds: IntArray) {
    super.onDeleted(context, appWidgetIds)
  }

  /**
   * 当最后一个该窗口小部件删除时调用该方法
   */
  override fun onDisabled(context: Context) {
    super.onDisabled(context)
  }

  /**
   * 当该窗口小部件第一次添加到桌面时调用该方法
   */
  override fun onEnabled(context: Context) {
    super.onEnabled(context)
  }

  /**
   * 当小部件大小改变时
   */
  override fun onAppWidgetOptionsChanged(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    newOptions: Bundle
  ) {
    super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
  }

  /**
   * 当小部件从备份恢复时调用该方法
   */
  override fun onRestored(context: Context, oldWidgetIds: IntArray, newWidgetIds: IntArray) {
    super.onRestored(context, oldWidgetIds, newWidgetIds)
  }

  companion object {
    const val CLICK_ACTION = "${BuildConfig.APPLICATION_ID}.action.CLICK"
  }
}
