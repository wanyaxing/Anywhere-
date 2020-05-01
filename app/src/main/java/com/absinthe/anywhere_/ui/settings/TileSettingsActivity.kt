package com.absinthe.anywhere_.ui.settings

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.absinthe.anywhere_.AnywhereApplication
import com.absinthe.anywhere_.BaseActivity
import com.absinthe.anywhere_.R
import com.absinthe.anywhere_.adapter.applist.AppListAdapter
import com.absinthe.anywhere_.adapter.tile.TileCardAdapter
import com.absinthe.anywhere_.constants.Const
import com.absinthe.anywhere_.databinding.ActivityTileSettingsBinding
import com.absinthe.anywhere_.model.AnywhereEntity
import com.absinthe.anywhere_.model.AppListBean
import com.absinthe.anywhere_.utils.SPUtils.getString
import com.absinthe.anywhere_.utils.SPUtils.putString
import com.absinthe.anywhere_.utils.TextUtils
import com.absinthe.anywhere_.utils.UiUtils
import com.absinthe.anywhere_.utils.manager.DialogManager.showCardListDialog
import com.chad.library.adapter.base.BaseQuickAdapter

@RequiresApi(api = Build.VERSION_CODES.N)
open class TileSettingsActivity : BaseActivity() {

    private lateinit var mBinding: ActivityTileSettingsBinding
    private var mAdapter: TileCardAdapter = TileCardAdapter()
    private var mList: List<AnywhereEntity> = ArrayList()

    init {
        isPaddingToolbar = true
    }

    override fun setViewBinding() {
        mBinding = ActivityTileSettingsBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
    }

    override fun setToolbar() {
        mToolbar = mBinding.toolbar
    }

    override fun initView() {
        super.initView()
        mBinding.apply {
            rvTiles.apply {
                layoutManager = LinearLayoutManager(this@TileSettingsActivity)
                adapter = mAdapter
            }
        }

        mAdapter.setOnItemChildClickListener { _: BaseQuickAdapter<*, *>?, view: View, position: Int ->
            if (view.id == R.id.btn_select) {
                showCardListDialog(this).apply {
                    setOnItemClickListener(object : AppListAdapter.OnItemClickListener {
                        override fun onClick(bean: AppListBean, which: Int) {
                            mAdapter.setData(position, bean)
                            var tile = ""
                            var tileLabel = ""
                            var tileCmd = ""

                            when (position) {
                                0 -> {
                                    tile = Const.PREF_TILE_ONE
                                    tileLabel = Const.PREF_TILE_ONE_LABEL
                                    tileCmd = Const.PREF_TILE_ONE_CMD
                                }
                                1 -> {
                                    tile = Const.PREF_TILE_TWO
                                    tileLabel = Const.PREF_TILE_TWO_LABEL
                                    tileCmd = Const.PREF_TILE_TWO_CMD
                                }
                                2 -> {
                                    tile = Const.PREF_TILE_THREE
                                    tileLabel = Const.PREF_TILE_THREE_LABEL
                                    tileCmd = Const.PREF_TILE_THREE_CMD
                                }
                            }
                            putString(this@TileSettingsActivity, tile, mList[which].id)
                            putString(this@TileSettingsActivity, tileLabel, mList[which].appName)
                            putString(this@TileSettingsActivity, tileCmd, TextUtils.getItemCommand(mList[which]))
                            dismiss()
                        }
                    })
                }
            }
        }
        AnywhereApplication.sRepository.allAnywhereEntities.value?.let {
            mList = it
            load()
        }
    }

    private fun initCard(): AppListBean {
        return AppListBean().apply {
            this.appName = getString(R.string.app_name)
            this.packageName = getPackageName()
            this.className = localClassName
            this.icon = getDrawable(R.mipmap.ic_launcher) ?: ColorDrawable(Color.TRANSPARENT)
        }
    }

    private fun initCard(item: AnywhereEntity): AppListBean {
        return AppListBean().apply {
            appName = item.appName
            packageName = item.param1
            className = item.param2
            icon = UiUtils.getAppIconByPackageName(this@TileSettingsActivity, item)
        }
    }

    private fun load() {
        loadImpl(Const.PREF_TILE_ONE)
        loadImpl(Const.PREF_TILE_TWO)
        loadImpl(Const.PREF_TILE_THREE)
    }

    private fun loadImpl(flag: String) {
        if (getString(this, flag).isEmpty()) {
            mAdapter.addData(initCard())
        } else {
            val id = getString(this, flag)
            mList.find { it.id == id }?.let {
                mAdapter.addData(initCard(it))
            }
        }
    }
}