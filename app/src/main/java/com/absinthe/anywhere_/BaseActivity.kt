package com.absinthe.anywhere_

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.absinthe.anywhere_.interfaces.OnDocumentResultListener
import com.absinthe.anywhere_.model.Const
import com.absinthe.anywhere_.model.GlobalValues
import com.absinthe.anywhere_.ui.main.MainActivity
import com.absinthe.anywhere_.utils.AppUtils
import com.absinthe.anywhere_.utils.StatusBarUtil.setDarkMode
import com.absinthe.anywhere_.utils.UiUtils
import com.absinthe.anywhere_.utils.manager.ActivityStackManager
import com.blankj.utilcode.util.BarUtils
import timber.log.Timber
import java.lang.ref.WeakReference

@SuppressLint("Registered")
abstract class BaseActivity : AppCompatActivity() {

    protected abstract val isPaddingToolbar: Boolean

    @JvmField
    protected var mToolbar: Toolbar? = null
    private var mListener: OnDocumentResultListener? = null
    private lateinit var reference: WeakReference<BaseActivity>

    protected abstract fun setViewBinding()
    protected abstract fun setToolbar()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.i("onCreate")
        reference = WeakReference(this)
        ActivityStackManager.addActivity(reference)
        setViewBinding()
        initView()
    }

    override fun onDestroy() {
        ActivityStackManager.removeActivity(reference)
        super.onDestroy()
    }

    protected open fun initView() {
        if (GlobalValues.sBackgroundUri.isEmpty() || this !is MainActivity) {
            setDarkMode(this, UiUtils.isDarkMode(this))
        }
        setToolbar()
        if (isPaddingToolbar) {
            mToolbar?.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0)
        }
        setSupportActionBar(mToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    fun setDocumentResultListener(listener: OnDocumentResultListener?) {
        mListener = listener
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Const.REQUEST_CODE_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                mListener?.onResult(it)
                AppUtils.takePersistableUriPermission(this, it, data)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun finish() {
        if (GlobalValues.sIsExcludeFromRecent) {
            finishAndRemoveTask()
        } else {
            super.finish()
        }
    }
}