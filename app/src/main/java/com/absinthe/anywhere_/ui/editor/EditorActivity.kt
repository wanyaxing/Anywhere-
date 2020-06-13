package com.absinthe.anywhere_.ui.editor

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.absinthe.anywhere_.AnywhereApplication
import com.absinthe.anywhere_.BaseActivity
import com.absinthe.anywhere_.R
import com.absinthe.anywhere_.constants.AnywhereType
import com.absinthe.anywhere_.constants.Const
import com.absinthe.anywhere_.constants.OnceTag
import com.absinthe.anywhere_.databinding.ActivityEditorBinding
import com.absinthe.anywhere_.interfaces.OnDocumentResultListener
import com.absinthe.anywhere_.model.database.AnywhereEntity
import com.absinthe.anywhere_.services.overlay.OverlayService
import com.absinthe.anywhere_.ui.editor.impl.*
import com.absinthe.anywhere_.utils.AppUtils.atLeastNMR1
import com.absinthe.anywhere_.utils.AppUtils.atLeastO
import com.absinthe.anywhere_.utils.AppUtils.atLeastR
import com.absinthe.anywhere_.utils.ShortcutsUtils
import com.absinthe.anywhere_.utils.AppTextUtils
import com.absinthe.anywhere_.utils.ToastUtil
import com.absinthe.anywhere_.utils.UiUtils
import com.absinthe.anywhere_.utils.manager.DialogManager
import com.absinthe.anywhere_.utils.manager.DialogManager.showAddShortcutDialog
import com.absinthe.anywhere_.utils.manager.DialogManager.showCannotAddShortcutDialog
import com.absinthe.anywhere_.utils.manager.DialogManager.showCreatePinnedShortcutDialog
import com.absinthe.anywhere_.utils.manager.DialogManager.showRemoveShortcutDialog
import com.absinthe.anywhere_.view.app.AnywhereDialogBuilder
import com.blankj.utilcode.util.PermissionUtils
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.google.android.material.transition.platform.MaterialContainerTransformSharedElementCallback
import jonathanfinerty.once.Once

const val EXTRA_ENTITY = "EXTRA_ENTITY"
const val EXTRA_EDIT_MODE = "EXTRA_EDIT_MODE"

class EditorActivity : BaseActivity() {

    private lateinit var binding: ActivityEditorBinding
    private lateinit var bottomDrawerBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var fragment: BaseEditorFragment

    private val entity by lazy { intent.getParcelableExtra(EXTRA_ENTITY) as AnywhereEntity? }
    private val isEditMode by lazy { intent.getBooleanExtra(EXTRA_EDIT_MODE, false) }

    override fun setViewBinding() {
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun setToolbar() {
        mToolbar = binding.bar
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (entity == null) finish()
        initTransition()
        super.onCreate(savedInstanceState)
        setUpBottomDrawer()
    }

    override fun onStart() {
        super.onStart()
        fragment = when (entity!!.anywhereType) {
            AnywhereType.URL_SCHEME -> SchemeEditorFragment(entity!!, isEditMode)
            AnywhereType.ACTIVITY -> AnywhereEditorFragment(entity!!, isEditMode)
            AnywhereType.QR_CODE -> QRCodeEditorFragment(entity!!, isEditMode)
            AnywhereType.IMAGE -> ImageEditorFragment(entity!!, isEditMode)
            AnywhereType.SHELL -> ShellEditorFragment(entity!!, isEditMode)
            else -> AnywhereEditorFragment(entity!!, isEditMode)
        }
        supportFragmentManager
                .beginTransaction()
                .replace(binding.fragmentContainerView.id, fragment)
                .commitNow()
    }

    override fun onBackPressed() {
        if (bottomDrawerBehavior.state != BottomSheetBehavior.STATE_HIDDEN) {
            bottomDrawerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.editor_bottom_bar_menu, menu)
        return true
    }

    private fun initTransition() {
        window.apply {
            requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
            sharedElementEnterTransition = MaterialContainerTransform().apply {
                addTarget(android.R.id.content)
                duration = 300L
            }
            sharedElementReturnTransition = MaterialContainerTransform().apply {
                addTarget(android.R.id.content)
                duration = 300L
            }
        }
        findViewById<View>(android.R.id.content).transitionName = getString(R.string.trans_item_container)
        setEnterSharedElementCallback(MaterialContainerTransformSharedElementCallback())
    }

    private fun setUpBottomDrawer() {
        bottomDrawerBehavior = BottomSheetBehavior.from(binding.bottomDrawer)
        bottomDrawerBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        binding.bar.apply {
            if (!isEditMode) {
                navigationIcon = ColorDrawable(Color.TRANSPARENT)
                setNavigationOnClickListener(null)
            } else {
                setNavigationOnClickListener { bottomDrawerBehavior.setState(BottomSheetBehavior.STATE_EXPANDED) }
                setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.trying_run -> {
                            fragment.tryingRun()
                        }
                        R.id.overlay -> {
                            startOverlay()
                        }
                    }
                    true
                }
            }
        }

        binding.fab.apply {
            val color = if (entity!!.color == 0) {
                ContextCompat.getColor(context, R.color.colorPrimary)
            } else {
                entity!!.color
            }
            backgroundTintList = ColorStateList.valueOf(color)

            imageTintList = if (UiUtils.isLightColor(color)) {
                ColorStateList.valueOf(Color.BLACK)
            } else {
                ColorStateList.valueOf(Color.WHITE)
            }

            setOnClickListener {
                if (fragment.doneEdit()) {
                    onBackPressed()
                }
            }
        }

        binding.navigationView.apply {
            setNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.add_shortcuts -> {
                        if (atLeastNMR1()) {
                            if (entity!!.shortcutType != AnywhereType.SHORTCUTS) {
                                addShortcut(this@EditorActivity, entity!!)
                            } else {
                                removeShortcut(this@EditorActivity, entity!!)
                            }
                        }
                    }
                    R.id.add_home_shortcuts -> {
                        if (atLeastO()) {
                            showCreatePinnedShortcutDialog(this@EditorActivity, entity!!)
                        }
                    }
                    R.id.delete -> {
                        DialogManager.showDeleteAnywhereDialog(this@EditorActivity, entity!!)
                    }
                    R.id.move_to_page -> {
                        DialogManager.showPageListDialog(this@EditorActivity, entity!!)
                    }
                    R.id.custom_color -> {
                        DialogManager.showColorPickerDialog(this@EditorActivity, entity!!)
                    }
                    R.id.share_card -> {
                        DialogManager.showCardSharingDialog(this@EditorActivity, AppTextUtils.genCardSharingUrl(entity!!))
                    }
                    R.id.custom_icon -> {
                        setDocumentResultListener(object : OnDocumentResultListener {
                            override fun onResult(uri: Uri) {
                                val ae = AnywhereEntity(entity!!).apply {
                                    iconUri = uri.toString()
                                }
                                AnywhereApplication.sRepository.update(ae)
                                onBackPressed()
                            }

                        })

                        try {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "image/*"
                            }
                            startActivityForResult(intent, Const.REQUEST_CODE_IMAGE_CAPTURE)
                        } catch (e: ActivityNotFoundException) {
                            e.printStackTrace()
                            ToastUtil.makeText(R.string.toast_no_document_app)
                        }
                    }
                    R.id.restore_icon -> {
                        val ae = AnywhereEntity(entity!!).apply {
                            iconUri = ""
                        }
                        AnywhereApplication.sRepository.update(ae)
                        onBackPressed()
                    }
                }
                bottomDrawerBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                true
            }

            menu.findItem(R.id.add_shortcuts)?.let {
                if (atLeastNMR1()) {
                    if (entity!!.shortcutType == AnywhereType.SHORTCUTS) {
                        binding.navigationView.apply {
                            menu.clear()
                            inflateMenu(R.menu.editor_added_shortcut_menu)
                        }
                    }
                } else {
                    setVisible(false)
                }
            }

            menu.findItem(R.id.add_home_shortcuts)?.isVisible = atLeastO()
            menu.findItem(R.id.restore_icon)?.isVisible = entity!!.iconUri.isNotEmpty()
            menu.findItem(R.id.share_card)?.isVisible = entity!!.anywhereType != AnywhereType.IMAGE

            invalidate()
        }
    }

    private fun startOverlay() {
        if (PermissionUtils.isGrantedDrawOverlays()) {
            startOverlayImpl()

        } else {
            if (atLeastR()) {
                ToastUtil.makeText(R.string.toast_overlay_choose_anywhere)
            }
            PermissionUtils.requestDrawOverlays(object : PermissionUtils.SimpleCallback {
                override fun onGranted() {
                    startOverlayImpl()
                }

                override fun onDenied() {}
            })
        }
    }

    private fun startOverlayImpl() {
        startService(Intent(this, OverlayService::class.java).apply {
            putExtra(OverlayService.COMMAND_STR, AppTextUtils.getItemCommand(entity!!))
            putExtra(OverlayService.PKG_NAME, entity!!.packageName)
        })
        finish()

        startActivity(Intent(Intent.ACTION_MAIN).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addCategory(Intent.CATEGORY_HOME)
        })

        if (!Once.beenDone(OnceTag.OVERLAY_TIP)) {
            ToastUtil.makeText(R.string.toast_overlay_tip)
            Once.markDone(OnceTag.OVERLAY_TIP)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private fun addShortcut(context: Context, ae: AnywhereEntity) {
        if (ShortcutsUtils.Singleton.INSTANCE.instance.dynamicShortcuts.size < 3) {
            val builder = AnywhereDialogBuilder(context)
            showAddShortcutDialog(context, builder, ae, DialogInterface.OnClickListener { _, _ ->
                ShortcutsUtils.addShortcut(ae)
                onBackPressed()
            })
        } else {
            showCannotAddShortcutDialog(context, DialogInterface.OnClickListener { _, _ ->
                ShortcutsUtils.addShortcut(ae)
                onBackPressed()
            })
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    private fun removeShortcut(context: Context, ae: AnywhereEntity) {
        showRemoveShortcutDialog(context, ae, DialogInterface.OnClickListener { _, _ ->
            ShortcutsUtils.removeShortcut(ae)
            onBackPressed()
        })
    }
}
