package mega.privacy.android.app.fragments.offline

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaOffline
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.components.PositionDividerItemDecoration
import mega.privacy.android.app.components.SimpleDividerItemDecoration
import mega.privacy.android.app.databinding.FragmentOfflineBinding
import mega.privacy.android.app.fragments.homepage.EventObserver
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.fragments.homepage.main.HomepageFragmentDirections
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop
import mega.privacy.android.app.lollipop.ZipBrowserActivityLollipop
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ARRAY_OFFLINE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_INSIDE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PARENT_HANDLE
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PATH
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_PATH_NAVIGATION
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_POSITION
import mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_SCREEN_POSITION
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.DraggingThumbnailCallback
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.OfflineUtils
import mega.privacy.android.app.utils.OfflineUtils.getOfflineFile
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.scaleHeightPx
import mega.privacy.android.app.utils.autoCleared
import mega.privacy.android.app.utils.callManager
import nz.mega.sdk.MegaApiJava.INVALID_HANDLE
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import java.io.File
import java.lang.ref.WeakReference

@AndroidEntryPoint
class OfflineFragment : Fragment(), ActionMode.Callback {
    private val args: OfflineFragmentArgs by navArgs()
    private var binding by autoCleared<FragmentOfflineBinding>()
    private val viewModel: OfflineViewModel by viewModels()
    private val sortByHeaderViewModel by viewModels<SortByHeaderViewModel>()

    private var recyclerView: RecyclerView? = null
    private var adapter: OfflineAdapter? = null
    private var actionMode: ActionMode? = null

    private val receiverUpdatePosition = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) {
                return
            }

            if (intent.getIntExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, 0) == Constants.OFFLINE_ADAPTER) {
                val handle = intent.getLongExtra("handle", INVALID_HANDLE)
                when (intent.getIntExtra("actionType", -1)) {
                    Constants.SCROLL_TO_POSITION -> {
                        scrollToNode(handle)
                    }
                    Constants.UPDATE_IMAGE_DRAG -> {
                        hideDraggingThumbnail(handle)
                    }
                    else -> {
                    }
                }
            }
        }
    }
    private val receiverRefreshOffline = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshNodes()
        }
    }

    private var draggingNodeHandle = INVALID_HANDLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments == null) {
            arguments = HomepageFragmentDirections.actionHomepageToFullscreenOffline().arguments
        }
    }

    override fun onStart() {
        super.onStart()

        // TODO: workaround for navigation with ManagerActivity
        callManager {
            if (args.rootFolderOnly) {
                it.pagerOfflineFragmentOpened(this)
            } else {
                it.fullscreenOfflineFragmentOpened(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(receiverRefreshOffline, IntentFilter(REFRESH_OFFLINE_FILE_LIST))

        viewModel.loadOfflineNodes(false)
    }

    override fun onPause() {
        super.onPause()

        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(receiverRefreshOffline)
    }

    override fun onStop() {
        super.onStop()

        // TODO: workaround for navigation with ManagerActivity
        callManager {
            if (args.rootFolderOnly) {
                it.pagerOfflineFragmentClosed(this)
            } else {
                it.fullscreenOfflineFragmentClosed(this)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
            receiverUpdatePosition,
            IntentFilter(Constants.BROADCAST_ACTION_INTENT_FILTER_UPDATE_POSITION)
        )

        binding = FragmentOfflineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        setupView()
        observeLiveData()
        if (viewModel.path == "" || viewModel.path == args.path) {
            setViewModelDisplayParam(args.path)
        } else {
            setViewModelDisplayParam(viewModel.path)
        }
        setupDraggingThumbnailCallback()
    }

    private fun setViewModelDisplayParam(path: String) {
        viewModel.setDisplayParam(
            args.rootFolderOnly, isList(),
            if (isList()) 0 else binding.offlineBrowserGrid.spanCount, path,
            callManager { it.orderCloud } ?: ORDER_DEFAULT_ASC
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()

        LocalBroadcastManager.getInstance(requireContext())
            .unregisterReceiver(receiverUpdatePosition)
    }

    private fun setupView() {
        adapter =
            OfflineAdapter(isList(), sortByHeaderViewModel, object : OfflineAdapterListener {
                override fun onNodeClicked(position: Int, node: OfflineNode) {
                    var firstVisiblePosition = INVALID_POSITION
                    if (isList()) {
                        firstVisiblePosition =
                            (binding.offlineBrowserList.layoutManager as LinearLayoutManager)
                                .findFirstCompletelyVisibleItemPosition()
                    } else {
                        firstVisiblePosition =
                            binding.offlineBrowserGrid.findFirstCompletelyVisibleItemPosition()
                        if (firstVisiblePosition == INVALID_POSITION) {
                            firstVisiblePosition =
                                binding.offlineBrowserGrid.findFirstVisibleItemPosition()
                        }
                    }
                    viewModel.onNodeClicked(position, node, firstVisiblePosition)
                }

                override fun onNodeLongClicked(position: Int, node: OfflineNode) {
                    viewModel.onNodeLongClicked(position, node)
                }

                override fun onOptionsClicked(position: Int, node: OfflineNode) {
                    viewModel.onNodeOptionsClicked(position, node)
                }
            })
        adapter?.setHasStableIds(true)

        binding.offlineBrowserList.layoutManager = LinearLayoutManager(context)

        binding.offlineBrowserGrid.layoutManager?.spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter?.getItemViewType(position) == OfflineAdapter.TYPE_HEADER) {
                    binding.offlineBrowserGrid.spanCount
                } else {
                    1
                }
            }
        }

        setupRecyclerView(binding.offlineBrowserList)
        setupRecyclerView(binding.offlineBrowserGrid)

        recyclerView = if (isList()) {
            binding.offlineBrowserList.isVisible = true
            binding.offlineBrowserList
        } else {
            binding.offlineBrowserGrid.isVisible = true
            binding.offlineBrowserGrid
        }
        recyclerView?.adapter = adapter

        if (args.rootFolderOnly) {
            binding.offlineBrowserList.addItemDecoration(
                SimpleDividerItemDecoration(requireContext(), resources.displayMetrics)
            )
        } else {
            binding.offlineBrowserList.addItemDecoration(
                PositionDividerItemDecoration(requireContext(), resources.displayMetrics)
            )
        }

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.emptyHintImage.setImageResource(R.drawable.offline_empty_landscape)
        } else {
            binding.emptyHintImage.setImageResource(R.drawable.ic_empty_offline)
        }
        var textToShow = getString(R.string.context_empty_offline)
        try {
            textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>")
            textToShow = textToShow.replace("[/A]", "</font>")
            textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>")
            textToShow = textToShow.replace("[/B]", "</font>")
        } catch (e: Exception) {
            e.printStackTrace()
            logError("Exception formatting string", e)
        }
        binding.emptyHintText.text = if (VERSION.SDK_INT >= VERSION_CODES.N) {
            Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(textToShow)
        }
    }

    private fun setupRecyclerView(rv: RecyclerView) {
        rv.setPadding(0, 0, 0, scaleHeightPx(85, resources.displayMetrics))
        rv.clipToPadding = false
        rv.setHasFixedSize(true)
        rv.itemAnimator = DefaultItemAnimator()
        rv.addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                checkScroll()
            }
        })
    }

    private fun observeLiveData() {
        viewModel.nodes.observe(viewLifecycleOwner) {
            recyclerView?.isVisible = it.isNotEmpty()
            binding.emptyHint.isVisible = it.isEmpty()

            adapter?.submitList(it)

            if (!args.rootFolderOnly) {
                callManager { manager ->
                    manager.updateFullscreenOfflineFragmentOptionMenu(false)
                }
            }
        }
        viewModel.openFolderFullscreen.observe(viewLifecycleOwner, EventObserver {
            callManager { manager ->
                manager.openFullscreenOfflineFragment(it)
            }
        })
        viewModel.showOptionsPanel.observe(viewLifecycleOwner, EventObserver {
            callManager { manager ->
                manager.showOptionsPanel(it)
            }
        })
        viewModel.nodeToOpen.observe(viewLifecycleOwner, EventObserver {
            openNode(it.first, it.second)
        })
        viewModel.urlFileOpenAsUrl.observe(viewLifecycleOwner, EventObserver {
            logDebug("Is URL - launch browser intent")
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(it)
            startActivity(intent)
        })
        viewModel.urlFileOpenAsFile.observe(viewLifecycleOwner, EventObserver {
            openFile(it, MimeTypeList.typeForName(it.name))
        })

        if (args.rootFolderOnly) {
            return
        }

        viewModel.actionMode.observe(viewLifecycleOwner) { visible ->
            val actionModeVal = actionMode
            if (visible) {
                if (actionModeVal == null) {
                    actionMode = callManager { it.startSupportActionMode(this) }
                }
                actionMode?.title = viewModel.getSelectedNodesCount().toString()
                actionMode?.invalidate()
            } else {
                if (actionModeVal != null) {
                    actionModeVal.finish()
                    actionMode = null
                }
            }
        }
        viewModel.actionBarTitle.observe(viewLifecycleOwner) {
            callManager { manager ->
                if (viewModel.selecting) {
                    manager.supportActionBar?.setTitle(it)
                } else {
                    manager.setToolbarTitleFromFullscreenOfflineFragment(it, false)
                }
            }
        }
        viewModel.pathLiveData.observe(viewLifecycleOwner) {
            callManager { manager ->
                manager.pathNavigationOffline = it
            }
        }
        viewModel.submitSearchQuery.observe(viewLifecycleOwner) {
            callManager { manager ->
                manager.setTextSubmitted()
            }
        }
        viewModel.showSortedBy.observe(viewLifecycleOwner, EventObserver {
            callManager { manager ->
                manager.showNewSortByPanel()
            }
        })
        observeAnimatedItems()
        viewModel.scrollToPositionWhenNavigateOut.observe(viewLifecycleOwner) {
            val layoutManager = recyclerView?.layoutManager
            if (layoutManager is LinearLayoutManager) {
                layoutManager.scrollToPositionWithOffset(it, 0)
            }
        }

        sortByHeaderViewModel.showDialogEvent.observe(viewLifecycleOwner, EventObserver {
            callManager { manager ->
                manager.showNewSortByPanel()
            }
        })

        sortByHeaderViewModel.orderChangeEvent.observe(viewLifecycleOwner, EventObserver {
            viewModel.setOrder(it)
            adapter?.notifyItemChanged(0)
        })

        sortByHeaderViewModel.listGridChangeEvent.observe(viewLifecycleOwner, EventObserver {
            switchListGridView()
        })
    }

    private fun observeAnimatedItems() {
        var animatorSet: AnimatorSet? = null
        viewModel.nodesToAnimate.observe(viewLifecycleOwner) {
            val rvAdapter = adapter ?: return@observe

            animatorSet?.run {
                // End the started animation if any, or the view may show messy as its property
                // would be wrongly changed by multiple animations running at the same time
                // via contiguous quick clicks on the item
                if (isStarted) {
                    end()
                }
            }

            // Must create a new AnimatorSet, or it would keep all previous
            // animation and play them together
            animatorSet = AnimatorSet()
            val animatorList = mutableListOf<Animator>()

            animatorSet?.addListener(object : AnimatorListener {
                override fun onAnimationRepeat(animation: Animator?) {
                }

                override fun onAnimationEnd(animation: Animator?) {
                    viewModel.nodes.value?.let { newList ->
                        rvAdapter.submitList(ArrayList(newList))
                    }
                }

                override fun onAnimationCancel(animation: Animator?) {
                }

                override fun onAnimationStart(animation: Animator?) {
                }
            })

            it.forEach { pos ->
                recyclerView?.findViewHolderForAdapterPosition(pos)?.let { viewHolder ->
                    val itemView = viewHolder.itemView

                    val imageView: ImageView? = when (rvAdapter.getItemViewType(pos)) {
                        OfflineAdapter.TYPE_LIST -> {
                            itemView.setBackgroundColor(
                                ContextCompat.getColor(
                                    binding.root.context,
                                    R.color.new_multiselect_color
                                )
                            )
                            val thumbnail = itemView.findViewById<ImageView>(R.id.thumbnail)
                            val param = thumbnail.layoutParams as FrameLayout.LayoutParams
                            param.width = Util.px2dp(
                                OfflineListViewHolder.LARGE_IMAGE_WIDTH,
                                resources.displayMetrics
                            )
                            param.height = param.width
                            param.marginStart = Util.px2dp(
                                OfflineListViewHolder.LARGE_IMAGE_MARGIN_LEFT,
                                resources.displayMetrics
                            )
                            thumbnail.layoutParams = param
                            thumbnail
                        }
                        OfflineAdapter.TYPE_GRID_FOLDER -> {
                            itemView.background = ContextCompat.getDrawable(
                                requireContext(), R.drawable.background_item_grid_selected
                            )
                            itemView.findViewById(R.id.icon)
                        }
                        OfflineAdapter.TYPE_GRID_FILE -> {
                            itemView.background = ContextCompat.getDrawable(
                                requireContext(), R.drawable.background_item_grid_selected
                            )
                            itemView.findViewById(R.id.ic_selected)
                        }
                        else -> null
                    }

                    imageView?.run {
                        setImageResource(R.drawable.ic_select_folder)
                        visibility = View.VISIBLE

                        val animator =
                            AnimatorInflater.loadAnimator(context, R.animator.icon_select)
                        animator.setTarget(this)
                        animatorList.add(animator)
                    }
                }
            }

            animatorSet?.playTogether(animatorList)
            animatorSet?.start()
        }
    }

    private fun openNode(position: Int, node: OfflineNode) {
        val file = getOfflineFile(context, node.node)
        val mime = MimeTypeList.typeForName(file.name)
        when {
            mime.isZip -> {
                logDebug("MimeTypeList ZIP")
                val intentZip = Intent(context, ZipBrowserActivityLollipop::class.java)
                intentZip.action = ZipBrowserActivityLollipop.ACTION_OPEN_ZIP_FILE
                intentZip.putExtra(
                    ZipBrowserActivityLollipop.EXTRA_ZIP_FILE_TO_OPEN,
                    viewModel.path
                )
                intentZip.putExtra(ZipBrowserActivityLollipop.EXTRA_PATH_ZIP, file.absolutePath)
                startActivity(intentZip)
            }
            mime.isImage -> {
                val intent = Intent(context, FullScreenImageViewerLollipop::class.java)
                intent.putExtra(INTENT_EXTRA_KEY_POSITION, position)
                intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.OFFLINE_ADAPTER)
                intent.putExtra(INTENT_EXTRA_KEY_PARENT_HANDLE, INVALID_HANDLE)
                intent.putExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY, file.parent)
                val screenPosition = getThumbnailScreenPosition(position)
                if (screenPosition != null) {
                    intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, screenPosition)
                }
                intent.putExtra(
                    INTENT_EXTRA_KEY_ARRAY_OFFLINE, ArrayList(adapter!!.getOfflineNodes())
                )

                draggingNodeHandle = node.node.handle.toLong()
                setupDraggingThumbnailCallback()
                startActivity(intent)
                requireActivity().overridePendingTransition(0, 0)
            }
            mime.isVideoReproducible || mime.isAudio -> {
                logDebug("Video/Audio file")

                val mediaIntent: Intent
                val internalIntent: Boolean
                var opusFile = false
                if (mime.isVideoNotSupported || mime.isAudioNotSupported) {
                    mediaIntent = Intent(Intent.ACTION_VIEW)
                    internalIntent = false
                    val s: Array<String> = file.name.split("\\.".toRegex()).toTypedArray()
                    if (s.size > 1 && s[s.size - 1] == "opus") {
                        opusFile = true
                    }
                } else {
                    internalIntent = true
                    mediaIntent = Intent(context, AudioVideoPlayerLollipop::class.java)
                }

                mediaIntent.putExtra(INTENT_EXTRA_KEY_HANDLE, node.node.handle.toLong())
                mediaIntent.putExtra(INTENT_EXTRA_KEY_FILE_NAME, node.node.name)
                mediaIntent.putExtra(INTENT_EXTRA_KEY_PATH, file.absolutePath)
                mediaIntent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.OFFLINE_ADAPTER)
                mediaIntent.putExtra(INTENT_EXTRA_KEY_POSITION, position)
                mediaIntent.putExtra(INTENT_EXTRA_KEY_PARENT_HANDLE, INVALID_HANDLE)
                mediaIntent.putExtra(INTENT_EXTRA_KEY_OFFLINE_PATH_DIRECTORY, file.parent)
                val screenPosition = getThumbnailScreenPosition(position)
                if (screenPosition != null) {
                    mediaIntent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, screenPosition)
                }
                mediaIntent.putExtra(
                    INTENT_EXTRA_KEY_ARRAY_OFFLINE, ArrayList(adapter!!.getOfflineNodes())
                )
                mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                if (VERSION.SDK_INT >= VERSION_CODES.N) {
                    mediaIntent.setDataAndType(
                        FileProvider.getUriForFile(
                            requireContext(),
                            "mega.privacy.android.app.providers.fileprovider",
                            file
                        ), mime.type
                    )
                } else {
                    mediaIntent.setDataAndType(Uri.fromFile(file), mime.type)
                }
                if (opusFile) {
                    mediaIntent.setDataAndType(mediaIntent.data, "audio/*")
                }
                if (internalIntent) {
                    draggingNodeHandle = node.node.handle.toLong()
                    setupDraggingThumbnailCallback()
                    startActivity(mediaIntent)
                    requireActivity().overridePendingTransition(0, 0)
                } else {
                    if (MegaApiUtils.isIntentAvailable(context, mediaIntent)) {
                        startActivity(mediaIntent)
                    } else {
                        callManager {
                            it.showSnackbar(
                                Constants.SNACKBAR_TYPE,
                                getString(R.string.intent_not_available),
                                -1
                            )
                        }
                        val intentShare = Intent(Intent.ACTION_SEND)
                        if (VERSION.SDK_INT >= VERSION_CODES.N) {
                            intentShare.setDataAndType(
                                FileProvider.getUriForFile(
                                    requireContext(),
                                    "mega.privacy.android.app.providers.fileprovider",
                                    file
                                ), mime.type
                            )
                        } else {
                            intentShare.setDataAndType(Uri.fromFile(file), mime.type)
                        }
                        intentShare.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        if (MegaApiUtils.isIntentAvailable(context, intentShare)) {
                            logDebug("Call to startActivity(intentShare)")
                            startActivity(intentShare)
                        }
                    }
                }
            }
            mime.isPdf -> {
                logDebug("PDF file")

                val pdfIntent = Intent(context, PdfViewerActivityLollipop::class.java)

                pdfIntent.putExtra(INTENT_EXTRA_KEY_INSIDE, true)
                pdfIntent.putExtra(INTENT_EXTRA_KEY_HANDLE, node.node.handle.toLong())
                pdfIntent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, Constants.OFFLINE_ADAPTER)
                pdfIntent.putExtra(INTENT_EXTRA_KEY_PATH, file.absolutePath)
                pdfIntent.putExtra(INTENT_EXTRA_KEY_PATH_NAVIGATION, viewModel.path)
                val screenPosition = getThumbnailScreenPosition(position)
                if (screenPosition != null) {
                    pdfIntent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, screenPosition)
                }
                if (VERSION.SDK_INT >= VERSION_CODES.N) {
                    pdfIntent.setDataAndType(
                        FileProvider.getUriForFile(
                            requireContext(),
                            "mega.privacy.android.app.providers.fileprovider",
                            file
                        ), mime.type
                    )
                } else {
                    pdfIntent.setDataAndType(Uri.fromFile(file), mime.type)
                }
                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                draggingNodeHandle = node.node.handle.toLong()
                setupDraggingThumbnailCallback()
                startActivity(pdfIntent)
                requireActivity().overridePendingTransition(0, 0)
            }
            mime.isURL -> {
                logDebug("Is URL file")
                viewModel.processUrlFile(file)
            }
            else -> {
                openFile(file, mime)
            }
        }
    }

    private fun getThumbnailScreenPosition(position: Int): IntArray? {
        val viewHolder = recyclerView?.findViewHolderForLayoutPosition(position) ?: return null
        return adapter?.getThumbnailLocationOnScreen(viewHolder)
    }

    private fun openFile(file: File, mime: MimeTypeList) {
        logDebug("openFile")
        val viewIntent = Intent(Intent.ACTION_VIEW)
        if (VERSION.SDK_INT >= VERSION_CODES.N) {
            viewIntent.setDataAndType(
                FileProvider.getUriForFile(
                    requireContext(),
                    "mega.privacy.android.app.providers.fileprovider",
                    file
                ), mime.type
            )
        } else {
            viewIntent.setDataAndType(Uri.fromFile(file), mime.type)
        }
        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (MegaApiUtils.isIntentAvailable(context, viewIntent)) {
            startActivity(viewIntent)
        } else {
            val intentShare = Intent(Intent.ACTION_SEND)
            if (VERSION.SDK_INT >= VERSION_CODES.N) {
                intentShare.setDataAndType(
                    FileProvider.getUriForFile(
                        requireContext(),
                        "mega.privacy.android.app.providers.fileprovider",
                        file
                    ), mime.type
                )
            } else {
                intentShare.setDataAndType(
                    Uri.fromFile(file),
                    MimeTypeList.typeForName(file.name).type
                )
            }
            intentShare.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (MegaApiUtils.isIntentAvailable(context, intentShare)) {
                startActivity(intentShare)
            }
        }
    }

    private fun isList(): Boolean {
        return callManager { it.isList } ?: true || args.rootFolderOnly
    }

    fun checkScroll() {
        val rv = recyclerView
        if (rv != null) {
            callManager {
                it.changeActionBarElevation(rv.canScrollVertically(-1) || viewModel.selecting)
            }
        }
    }

    fun setSearchQuery(query: String?) {
        viewModel.setSearchQuery(query)
    }

    fun onSearchQuerySubmitted() {
        viewModel.onSearchQuerySubmitted()
    }

    fun selectAll() {
        viewModel.selectAll()
    }

    fun onBackPressed(): Int {
        return viewModel.navigateOut(args.path)
    }

    fun getItemCount(): Int {
        return viewModel.getDisplayedNodesCount()
    }

    fun scrollToNode(handle: Long) {
        logDebug("scrollToNode, handle $handle")

        val position = adapter?.getNodePosition(handle) ?: return
        logDebug("scrollToNode, handle $handle, position $position")
        if (position != INVALID_POSITION) {
            recyclerView?.scrollToPosition(position)
            notifyThumbnailLocationOnScreen()
        }
    }

    fun hideDraggingThumbnail(handle: Long) {
        logDebug("hideDraggingThumbnail: $handle")
        setDraggingThumbnailVisibility(draggingNodeHandle, View.VISIBLE)
        setDraggingThumbnailVisibility(handle, View.GONE)
        draggingNodeHandle = handle
        notifyThumbnailLocationOnScreen()
    }

    fun refreshNodes() {
        viewModel.loadOfflineNodes()
    }

    fun refreshActionBarTitle() {
        viewModel.refreshActionBarTitle()
    }

    fun saveNodeToDevice(node: MegaOffline) {
        viewModel.saveNodeToDevice(node) { intent, code ->
            startActivityForResult(intent, code)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!viewModel.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun switchListGridView() {
        recyclerView = if (isList()) {
            binding.offlineBrowserList.isVisible = true
            binding.offlineBrowserList.adapter = adapter
            binding.offlineBrowserGrid.isVisible = false
            binding.offlineBrowserGrid.adapter = null
            adapter?.isList = true

            binding.offlineBrowserList
        } else {
            binding.offlineBrowserGrid.isVisible = true
            binding.offlineBrowserGrid.adapter = adapter
            binding.offlineBrowserList.isVisible = false
            binding.offlineBrowserList.adapter = null
            adapter?.isList = false

            binding.offlineBrowserGrid
        }
        setViewModelDisplayParam(viewModel.path)
    }

    override fun onDestroy() {
        super.onDestroy()
        FullScreenImageViewerLollipop.removeDraggingThumbnailCallback(OfflineFragment::class.java)
        AudioVideoPlayerLollipop.removeDraggingThumbnailCallback(OfflineFragment::class.java)
        PdfViewerActivityLollipop.removeDraggingThumbnailCallback(OfflineFragment::class.java)
    }

    private fun setupDraggingThumbnailCallback() {
        FullScreenImageViewerLollipop.addDraggingThumbnailCallback(
            OfflineFragment::class.java, OfflineDraggingThumbnailCallback(this)
        )
        AudioVideoPlayerLollipop.addDraggingThumbnailCallback(
            OfflineFragment::class.java, OfflineDraggingThumbnailCallback(this)
        )
        PdfViewerActivityLollipop.addDraggingThumbnailCallback(
            OfflineFragment::class.java, OfflineDraggingThumbnailCallback(this)
        )
    }

    private fun setDraggingThumbnailVisibility(
        handle: Long,
        visibility: Int
    ) {
        val position = adapter?.getNodePosition(handle) ?: return
        val viewHolder: ViewHolder =
            recyclerView?.findViewHolderForLayoutPosition(position) ?: return
        adapter?.setThumbnailVisibility(viewHolder, visibility)
    }

    private fun notifyThumbnailLocationOnScreen() {
        val position = adapter?.getNodePosition(draggingNodeHandle) ?: return
        val viewHolder: ViewHolder =
            recyclerView?.findViewHolderForLayoutPosition(position) ?: return
        val res = adapter?.getThumbnailLocationOnScreen(viewHolder) ?: return
        res[0] += res[2] / 2
        res[1] += res[3] / 2
        val intent = Intent(BROADCAST_ACTION_INTENT_FILTER_UPDATE_IMAGE_DRAG)
        intent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, res)
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(intent)
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        logDebug("ActionBarCallBack::onActionItemClicked")

        when (item!!.itemId) {
            R.id.cab_menu_share_out -> {
                OfflineUtils.shareOfflineNodes(requireContext(), viewModel.getSelectedNodes())
                viewModel.clearSelection()
            }
            R.id.cab_menu_delete -> {
                callManager { it.showConfirmationRemoveSomeFromOffline(viewModel.getSelectedNodes()) }
                viewModel.clearSelection()
            }
            R.id.cab_menu_select_all -> {
                viewModel.selectAll()
            }
            R.id.cab_menu_clear_selection -> {
                viewModel.clearSelection()
            }
        }
        return false
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        logDebug("ActionBarCallBack::onCreateActionMode")
        val inflater = mode!!.menuInflater
        inflater.inflate(R.menu.offline_browser_action, menu)
        callManager {
            it.showHideBottomNavigationView(true)
            it.changeStatusBarColor(Constants.COLOR_STATUS_BAR_ACCENT)
        }
        checkScroll()
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        logDebug("ActionBarCallBack::onPrepareActionMode")

        menu!!.findItem(R.id.cab_menu_select_all).isVisible =
            (viewModel.getSelectedNodesCount()
                    < getItemCount() - viewModel.placeholderCount)
        menu.findItem(R.id.cab_menu_share_out).isVisible = !viewModel.folderSelected()

        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        logDebug("ActionBarCallBack::onDestroyActionMode")
        viewModel.clearSelection()
        callManager { it.changeStatusBarColor(Constants.COLOR_STATUS_BAR_ZERO_DELAY) }
        checkScroll()
    }

    private class OfflineDraggingThumbnailCallback constructor(fragment: OfflineFragment) :
        DraggingThumbnailCallback {
        private val fragmentRef = WeakReference(fragment)
        override fun setVisibility(visibility: Int) {
            val fragment = fragmentRef.get()
            fragment?.setDraggingThumbnailVisibility(fragment.draggingNodeHandle, visibility)
        }

        override fun getLocationOnScreen(location: IntArray) {
            val fragment = fragmentRef.get()
            if (fragment != null) {
                val position =
                    fragment.adapter?.getNodePosition(fragment.draggingNodeHandle) ?: return
                val viewHolder =
                    fragment.recyclerView?.findViewHolderForLayoutPosition(position) ?: return
                val res = fragment.adapter?.getThumbnailLocationOnScreen(viewHolder) ?: return
                System.arraycopy(res, 0, location, 0, 2)
            }
        }
    }

    companion object {
        const val REFRESH_OFFLINE_FILE_LIST = "refresh_offline_file_list"
    }
}
