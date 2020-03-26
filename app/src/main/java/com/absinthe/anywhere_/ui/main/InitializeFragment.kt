package com.absinthe.anywhere_.ui.main

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.absinthe.anywhere_.R
import com.absinthe.anywhere_.databinding.*
import com.absinthe.anywhere_.model.Const
import com.absinthe.anywhere_.model.GlobalValues
import com.absinthe.anywhere_.utils.ToastUtil
import com.absinthe.anywhere_.utils.manager.DialogManager
import com.absinthe.anywhere_.utils.manager.ShizukuHelper
import com.blankj.utilcode.util.PermissionUtils
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.button.MaterialButtonToggleGroup.OnButtonCheckedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import moe.shizuku.api.ShizukuApiConstants
import timber.log.Timber

class InitializeFragment : Fragment(), OnButtonCheckedListener {

    private lateinit var mBinding: FragmentInitializeBinding
    private lateinit var rootBinding: CardAcquireRootPermissionBinding
    private lateinit var shizukuBinding: CardAcquireShizukuPermissionBinding
    private lateinit var overlayBinding: CardAcquireOverlayPermissionBinding
    private lateinit var popupBinding: CardAcquirePopupPermissionBinding

    private var isRoot: MutableLiveData<Boolean> = MutableLiveData()
    private var isOverlay: MutableLiveData<Boolean> = MutableLiveData()
    private var isPopup: MutableLiveData<Boolean> = MutableLiveData()
    private var isShizukuCheck: MutableLiveData<Boolean> = MutableLiveData()
    private var isShizuku: MutableLiveData<Boolean> = MutableLiveData()
    private var allPerm: MutableLiveData<Int> = MutableLiveData()

    private var bRoot = false
    private var bShizuku = false
    private var bOverlay = false
    private var bPopup = false
    private var mWorkingMode: String = Const.WORKING_MODE_URL_SCHEME

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = FragmentInitializeBinding.inflate(inflater, container, false)
        rootBinding = CardAcquireRootPermissionBinding.inflate(inflater, container, false)
        shizukuBinding = CardAcquireShizukuPermissionBinding.inflate(inflater, container, false)
        overlayBinding = CardAcquireOverlayPermissionBinding.inflate(inflater, container, false)
        popupBinding = CardAcquirePopupPermissionBinding.inflate(inflater, container, false)
        initView()

        return mBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initObserver()
    }

    private fun initView() {
        setHasOptionsMenu(true)
        mBinding.selectWorkingMode.toggleGroup.addOnButtonCheckedListener(this)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            allPerm.value?.or(OVERLAY_PERM)
        }
    }

    override fun onButtonChecked(group: MaterialButtonToggleGroup, checkedId: Int, isChecked: Boolean) {
        Timber.d("onButtonChecked")
        when (checkedId) {
            R.id.btn_url_scheme -> if (isChecked) {
                actCards(CARD_ROOT, false)
                actCards(CARD_SHIZUKU, false)
                actCards(CARD_OVERLAY, false)
                actCards(CARD_POPUP, false)
                mWorkingMode = Const.WORKING_MODE_URL_SCHEME
            }
            R.id.btn_root -> if (isChecked) {
                actCards(CARD_SHIZUKU, false)
                actCards(CARD_ROOT, true)
                actCards(CARD_OVERLAY, true)
                actCards(CARD_POPUP, true)
                mWorkingMode = Const.WORKING_MODE_ROOT
            }
            R.id.btn_shizuku -> if (isChecked) {
                actCards(CARD_ROOT, false)
                actCards(CARD_SHIZUKU, true)
                actCards(CARD_OVERLAY, true)
                actCards(CARD_POPUP, true)
                mWorkingMode = Const.WORKING_MODE_SHIZUKU
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.initialize_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.toolbar_initialize_done) {
            Timber.d("enterMain")
            GlobalValues.setsWorkingMode(mWorkingMode)
            var flag = false
            val allPerm = allPerm.value ?: 0
            when (mWorkingMode) {
                Const.WORKING_MODE_URL_SCHEME -> flag = true
                Const.WORKING_MODE_ROOT -> if (allPerm == ROOT_PERM or OVERLAY_PERM) {
                    flag = true
                }
                Const.WORKING_MODE_SHIZUKU -> if (allPerm == SHIZUKU_GROUP_PERM or OVERLAY_PERM) {
                    flag = true
                }
            }
            if (flag) {
                enterMainFragment()
            } else {
                DialogManager.showHasNotGrantPermYetDialog(context, DialogInterface.OnClickListener { _, _ ->
                    enterMainFragment()
                })
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun enterMainFragment() {
        val fragment = MainFragment.newInstance(GlobalValues.sCategory)
        MainActivity.getInstance().viewModel.fragment.value = fragment
        MainActivity.getInstance().initFab()
        MainActivity.getInstance().initObserver()
    }

    private fun initObserver() {
        isRoot.observe(viewLifecycleOwner, Observer { aBoolean: Boolean ->
            if (aBoolean) {
                rootBinding.btnAcquireRootPermission.setText(R.string.btn_acquired)
                rootBinding.btnAcquireRootPermission.isEnabled = false
                rootBinding.done.visibility = View.VISIBLE
                allPerm.value?.or(ROOT_PERM)
                Timber.d("allPerm = %s", allPerm.value)
            } else {
                Timber.d("ROOT permission denied.")
                ToastUtil.makeText(R.string.toast_root_permission_denied)
            }
        })
        isShizukuCheck.observe(viewLifecycleOwner, Observer { aBoolean: Boolean ->
            if (aBoolean) {
                shizukuBinding.btnCheckShizukuState.setText(R.string.btn_checked)
                shizukuBinding.btnCheckShizukuState.isEnabled = false
                allPerm.value?.or(SHIZUKU_CHECK_PERM)
                shizukuBinding.btnAcquirePermission.isEnabled = true
            }
            if (allPerm.value!! and SHIZUKU_GROUP_PERM == SHIZUKU_GROUP_PERM) {
                shizukuBinding.done.visibility = View.VISIBLE
            }
        })
        isShizuku.observe(viewLifecycleOwner, Observer { aBoolean: Boolean ->
            if (aBoolean) {
                shizukuBinding.btnAcquirePermission.setText(R.string.btn_acquired)
                shizukuBinding.btnAcquirePermission.isEnabled = false
                allPerm.value?.or(SHIZUKU_PERM)
            }
            if (allPerm.value!! and SHIZUKU_GROUP_PERM == SHIZUKU_GROUP_PERM) {
                shizukuBinding.done.visibility = View.VISIBLE
            }
        })
        isOverlay.observe(viewLifecycleOwner, Observer { aBoolean: Boolean ->
            if (aBoolean) {
                overlayBinding.btnAcquireOverlayPermission.setText(R.string.btn_acquired)
                overlayBinding.btnAcquireOverlayPermission.isEnabled = false
                overlayBinding.done.visibility = View.VISIBLE
                allPerm.value?.or(OVERLAY_PERM)
                Timber.d("allPerm = %s", allPerm.value)
            }
        })
    }

    private fun actCards(card: Int, isAdd: Boolean) {
        when (card) {
            CARD_ROOT -> {
                rootBinding.btnAcquireRootPermission.setOnClickListener {
                    val result = com.absinthe.anywhere_.utils.PermissionUtils.upgradeRootPermission(context?.packageCodePath)
                    isRoot.setValue(result)
                }
                if (isAdd) {
                    if (!bRoot) {
                        mBinding.container.addView(rootBinding.root, 1)
                        bRoot = true
                    }
                } else {
                    mBinding.container.removeView(rootBinding.root)
                    bRoot = false
                }
            }
            CARD_SHIZUKU -> {
                shizukuBinding.btnAcquirePermission.isEnabled = false
                shizukuBinding.btnCheckShizukuState.setOnClickListener {
                    val result = ShizukuHelper.checkShizukuOnWorking(context)
                    isShizukuCheck.setValue(result)
                }
                shizukuBinding.btnAcquirePermission.setOnClickListener {
                    val result = ShizukuHelper.isGrantShizukuPermission()
                    isShizuku.value = result
                    if (!result) {
                        ShizukuHelper.requestShizukuPermission()
                    }
                }
                if (isAdd) {
                    if (!bShizuku) {
                        mBinding.container.addView(shizukuBinding.root, 1)
                        bShizuku = true
                    }
                } else {
                    mBinding.container.removeView(shizukuBinding.root)
                    bShizuku = false
                }
            }
            CARD_OVERLAY -> {
                overlayBinding.btnAcquireOverlayPermission.setOnClickListener {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        isOverlay.setValue(true)
                    } else {
                        val isGrant = PermissionUtils.isGrantedDrawOverlays()
                        isOverlay.value = isGrant
                        if (!isGrant) {
                            if (Build.VERSION.SDK_INT >= 30) {
                                ToastUtil.makeText(R.string.toast_overlay_choose_anywhere)
                            }
                            PermissionUtils.requestDrawOverlays(object : PermissionUtils.SimpleCallback {
                                override fun onGranted() {
                                    isOverlay.value = true
                                }

                                override fun onDenied() {
                                    isOverlay.value = false
                                }
                            })
                        }
                    }
                }
                if (isAdd) {
                    if (!bOverlay) {
                        mBinding.container.addView(overlayBinding.root, -1)
                        bOverlay = true
                    }
                } else {
                    mBinding.container.removeView(overlayBinding.root)
                    bOverlay = false
                }
            }
            CARD_POPUP -> {
                popupBinding.btnAcquirePopupPermission.setOnClickListener {
                    if (com.absinthe.anywhere_.utils.PermissionUtils.isMIUI()) {
                        com.absinthe.anywhere_.utils.PermissionUtils.goToMIUIPermissionManager(context)
                    }
                }
                if (isAdd) {
                    if (!bPopup) {
                        mBinding.container.addView(popupBinding.root, -1)
                        bPopup = true
                    }
                } else {
                    mBinding.container.removeView(popupBinding.root)
                    bPopup = false
                }
            }
            else -> {
                popupBinding.btnAcquirePopupPermission.setOnClickListener {
                    if (com.absinthe.anywhere_.utils.PermissionUtils.isMIUI()) {
                        com.absinthe.anywhere_.utils.PermissionUtils.goToMIUIPermissionManager(context)
                    }
                }
                if (isAdd) {
                    if (!bPopup) {
                        mBinding.container.addView(popupBinding.root, -1)
                        bPopup = true
                    }
                } else {
                    mBinding.container.removeView(popupBinding.root)
                    bPopup = false
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Const.REQUEST_CODE_ACTION_MANAGE_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(context)) {
                    isOverlay.value = java.lang.Boolean.TRUE
                }
            }
        } else if (requestCode == Const.REQUEST_CODE_SHIZUKU_PERMISSION) {
            GlobalScope.launch(Dispatchers.Main) {
                delay(3000)
                if (ActivityCompat.checkSelfPermission(requireContext(), ShizukuApiConstants.PERMISSION)
                        == PackageManager.PERMISSION_GRANTED) {
                    isShizuku.value = java.lang.Boolean.TRUE
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == Const.REQUEST_CODE_SHIZUKU_PERMISSION) {
            if (ActivityCompat.checkSelfPermission(requireContext(), ShizukuApiConstants.PERMISSION) == PackageManager.PERMISSION_GRANTED) {
                isShizuku.value = java.lang.Boolean.TRUE
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val CARD_ROOT = 1
        private const val CARD_SHIZUKU = 2
        private const val CARD_OVERLAY = 3
        private const val CARD_POPUP = 4

        const val ROOT_PERM = 1
        const val SHIZUKU_CHECK_PERM = 2
        const val SHIZUKU_PERM = 4
        const val OVERLAY_PERM = 8
        const val SHIZUKU_GROUP_PERM = 6

        @JvmStatic
        fun newInstance(): InitializeFragment {
            return InitializeFragment()
        }
    }
}