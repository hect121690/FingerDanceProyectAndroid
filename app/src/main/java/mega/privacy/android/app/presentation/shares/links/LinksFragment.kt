package mega.privacy.android.app.presentation.shares.links

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.main.adapters.MegaNodeAdapter
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.manager.model.Tab
import mega.privacy.android.app.presentation.shares.MegaNodeBaseFragment
import mega.privacy.android.app.utils.CloudStorageOptionControlUtil
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.ColorUtils.setImageViewAlphaIfDark
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.ORDER_CLOUD
import mega.privacy.android.app.utils.MegaNodeUtil.areAllFileNodesAndNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.areAllNotTakenDown
import mega.privacy.android.app.utils.MegaNodeUtil.canMoveToRubbish
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber

/**
 * Links shares page
 */
@AndroidEntryPoint
class LinksFragment : MegaNodeBaseFragment() {

    private val viewModel: LinksViewModel by activityViewModels()

    private fun state() = viewModel.state.value

    companion object {
        @JvmStatic
        fun getLinksOrderCloud(orderCloud: Int, isFirstNavigationLevel: Boolean): Int {
            return if (!isFirstNavigationLevel) {
                orderCloud
            } else when (orderCloud) {
                MegaApiJava.ORDER_MODIFICATION_ASC -> MegaApiJava.ORDER_LINK_CREATION_ASC
                MegaApiJava.ORDER_MODIFICATION_DESC -> MegaApiJava.ORDER_LINK_CREATION_DESC
                else -> orderCloud
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        if (megaApi.rootNode == null) {
            return null
        }

        val view = getListView(inflater, container)

        initAdapter()
        observe()

        return view
    }

    override fun activateActionMode() {
        if (adapter.isMultipleSelect) return

        super.activateActionMode()
        actionMode =
            (requireActivity() as AppCompatActivity).startSupportActionMode(
                ActionBarCallBack(SharesTab.LINKS_TAB)
            )
    }

    override fun refresh() {}

    override fun itemClick(position: Int) {
        val actualPosition = position - adapter.placeholderCount

        when {
            // select mode
            adapter.isMultipleSelect -> {
                adapter.toggleSelection(position)
                val selectedNodes = adapter.selectedNodes
                if (selectedNodes.size > 0)
                    updateActionModeTitle()
            }

            // click on a folder
            state().nodes[actualPosition].isFolder ->
                navigateToFolder(state().nodes[actualPosition])

            // click on a file
            else ->
                openFile(state().nodes[actualPosition], Constants.LINKS_ADAPTER, actualPosition)
        }
    }

    override fun navigateToFolder(node: MegaNode) {
        Timber.d("Is folder deep: %s", state().linksTreeDepth)

        viewModel.pushToLastPositionState(mLayoutManager.findFirstCompletelyVisibleItemPosition())
        viewModel.increaseLinksTreeDepth(node.handle)
        recyclerView.scrollToPosition(0)
        checkScroll()
    }

    override fun onBackPressed(): Int {
        Timber.d("deepBrowserTree:%s", state().linksTreeDepth)

        if (adapter == null)
            return 0

        return when {
            state().linksTreeDepth == 1 -> {
                Timber.d("deepBrowserTree==1")
                viewModel.resetLinksTreeDepth()

                val lastVisiblePosition = viewModel.popLastPositionStack()

                lastVisiblePosition.takeIf { it > 0 }?.let {
                    mLayoutManager.scrollToPositionWithOffset(it, 0)
                }

                recyclerView.visibility = View.VISIBLE
                emptyImageView.visibility = View.GONE
                emptyLinearLayout.visibility = View.GONE

                3
            }

            state().linksTreeDepth > 1 -> {
                Timber.d("deepTree>1")

                viewModel.getParentNodeHandle()?.let { parentHandle ->
                    recyclerView.visibility = View.VISIBLE
                    emptyImageView.visibility = View.GONE
                    emptyLinearLayout.visibility = View.GONE
                    viewModel.decreaseLinksTreeDepth(parentHandle)

                    val lastVisiblePosition = viewModel.popLastPositionStack()

                    lastVisiblePosition.takeIf { it > 0 }?.let {
                        mLayoutManager.scrollToPositionWithOffset(it, 0)
                    }
                }

                2
            }
            else -> {
                Timber.d("ELSE deepTree")
                viewModel.resetLinksTreeDepth()
                0
            }
            
        }

    }

    override fun updateContact(contactHandle: Long) {}

    override fun showSortByPanel() {
        managerActivity.showNewSortByPanel(ORDER_CLOUD)
    }

    override fun viewerFrom(): Int = Constants.VIEWER_FROM_LINKS

    override fun getParentHandle(): Long = state().linksParentHandle

    /**
     * Observe viewModel
     */
    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state.collect {
                    Timber.d("Collect ui state")

                    // If the nodes are loading, don't display the UI
                    if (it.isLoading) {
                        recyclerView.visibility = View.GONE
                        hideTabs(true)
                        return@collect
                    }

                    updateNodes(it.nodes)
                    hideTabs(!it.isFirstNavigationLevel())

                    managerActivity.showFabButton()
                    managerActivity.invalidateOptionsMenu()
                    managerActivity.setToolbarTitle()

                    visibilityFastScroller()
                    hideActionMode()
                    setEmptyView(it.isInvalidParentHandle)

                }
            }
        }
    }

    /**
     * Initialize the adapter
     */
    private fun initAdapter() {
        if (adapter == null) {
            adapter = MegaNodeAdapter(requireActivity(),
                this,
                state().nodes,
                state().linksParentHandle,
                recyclerView,
                Constants.LINKS_ADAPTER,
                MegaNodeAdapter.ITEM_VIEW_TYPE_LIST,
                sortByHeaderViewModel)
        } else {
            adapter.parentHandle = state().linksParentHandle
            adapter.setListFragment(recyclerView)
        }

        adapter.isMultipleSelect = false
        recyclerView.adapter = adapter
    }

    /**
     * Update displayed nodes
     *
     * @param nodes the list of nodes to display
     */
    private fun updateNodes(nodes: List<MegaNode>) {
        val mutableListNodes = ArrayList(nodes)
        adapter.setNodes(mutableListNodes)
    }

    /**
     * Hide/Show shares tab
     *
     * @param hide true if needs to hide shares tabs
     */
    private fun hideTabs(hide: Boolean) {
        managerActivity.hideTabs(hide, SharesTab.LINKS_TAB)
    }

    /**
     * Set the empty view and message depending if the parent handle is valid or not
     *
     * @param isInvalidParentHandle true if the parent handle is invalid
     */
    private fun setEmptyView(isInvalidParentHandle: Boolean) {
        var textToShow: String? = null
        if (isInvalidParentHandle) {
            setImageViewAlphaIfDark(requireContext(), emptyImageView, ColorUtils.DARK_IMAGE_ALPHA)
            emptyImageView.setImageResource(R.drawable.ic_zero_data_public_links)
            textToShow = requireContext().getString(R.string.context_empty_links)
        }
        setFinalEmptyView(textToShow)
    }

    private inner class ActionBarCallBack(currentTab: Tab?) : BaseActionBarCallBack(currentTab) {
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            super.onPrepareActionMode(mode, menu)
            val control = CloudStorageOptionControlUtil.Control()
            val areAllNotTakenDown = selected.areAllNotTakenDown()
            if (areAllNotTakenDown) {
                if (selected.size == 1) {
                    control.manageLink().setVisible(true).showAsAction =
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                    control.removeLink().isVisible = true
                } else {
                    control.removeLink().setVisible(true).showAsAction =
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                }
                control.shareOut().setVisible(true).showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                if (areAllFileNodesAndNotTakenDown(selected)) {
                    control.sendToChat().setVisible(true).showAsAction =
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                }
                control.copy().isVisible = true
                if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
                    control.copy().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                } else {
                    control.copy().showAsAction = MenuItem.SHOW_AS_ACTION_NEVER
                }
            } else {
                control.saveToDevice().isVisible = false
            }
            if (selected.size == 1
                && megaApi.checkAccessErrorExtended(selected[0],
                    MegaShare.ACCESS_FULL).errorCode == MegaError.API_OK
            ) {
                control.rename().isVisible = true
                if (control.alwaysActionCount() < CloudStorageOptionControlUtil.MAX_ACTION_COUNT) {
                    control.rename().showAsAction = MenuItem.SHOW_AS_ACTION_ALWAYS
                } else {
                    control.rename().showAsAction = MenuItem.SHOW_AS_ACTION_NEVER
                }
            }
            control.selectAll().isVisible = notAllNodesSelected()
            control.trash().isVisible = canMoveToRubbish(selected)
            CloudStorageOptionControlUtil.applyControl(menu, control)
            return true
        }
    }
}