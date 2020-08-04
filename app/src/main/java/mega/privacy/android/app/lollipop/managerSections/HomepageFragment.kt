package mega.privacy.android.app.lollipop.managerSections

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_homepage.view.*
import kotlinx.android.synthetic.main.homepage_fabs.view.*
import mega.privacy.android.app.HomepageBottomSheetBehavior
import mega.privacy.android.app.R
import mega.privacy.android.app.components.BottomSheetPagerAdapter
import mega.privacy.android.app.components.search.FloatingSearchView
import mega.privacy.android.app.databinding.FabMaskLayoutBinding
import mega.privacy.android.app.databinding.FragmentHomepageBinding
import mega.privacy.android.app.fragments.managerFragments.homepage.HomePageViewModel
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import javax.inject.Inject

@AndroidEntryPoint
class HomepageFragment : Fragment() {

    @Inject lateinit var megaApi: MegaApiAndroid
    @Inject lateinit var megaChatApi: MegaChatApiAndroid

    private val viewModel: HomePageViewModel by viewModels()

    private lateinit var viewDataBinding : FragmentHomepageBinding
    private lateinit var rootView : View
    private lateinit var bottomSheetBehavior: HomepageBottomSheetBehavior<View>
    private lateinit var searchInputView: FloatingSearchView
    private lateinit var fabMain: FloatingActionButton
    private lateinit var fabMaskMain: FloatingActionButton
    private lateinit var fabMaskLayout: View
    private var window: ViewGroup? = null

    private var isFabExpanded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        viewDataBinding = FragmentHomepageBinding.inflate(inflater, container, false)
        rootView = viewDataBinding.root
        window = activity?.window?.findViewById(Window.ID_ANDROID_CONTENT)
        fabMaskLayout = FabMaskLayoutBinding.inflate(inflater, window, false).root
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearchView()
        setupBottomSheetUI()
        setupBottomSheetBehavior()
        setupFabs()
    }

    private fun setupSearchView() {
        searchInputView = viewDataBinding.searchView
        searchInputView.attachNavigationDrawerToMenuButton(
            (activity as ManagerActivityLollipop).drawerLayout!!)

        viewModel.notification.observe(viewLifecycleOwner) {
            searchInputView.setShowLeftDot(it)
        }
        viewModel.avatar.observe(viewLifecycleOwner) {
            searchInputView.setAvatar(it)
        }
        viewModel.chatStatus.observe(viewLifecycleOwner) {
            searchInputView.setChatStatus(it != 0, it)
        }

        searchInputView.setAvatarClickListener(
            OnClickListener { (activity as ManagerActivityLollipop).showMyAccount() })
    }

    private fun setupBottomSheetUI() {
        val viewPager = rootView.findViewById<ViewPager2>(R.id.view_pager)
        viewPager.adapter = BottomSheetPagerAdapter(this)

        // Attach the view pager to the tab layout
        val tabs = rootView.findViewById<TabLayout>(R.id.tabs)
        val mediator = TabLayoutMediator(tabs, viewPager) { tab, position ->
            tab.text = getTabTitle(position)
        }
        mediator.attach()

        // Pass selected page view to HomepageBottomSheetBehavior which would seek for
        // the nested scrolling child views and deal with the logic of nested scrolling
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bottomSheetBehavior.invalidateScrollingChild(
                        (viewPager.adapter as BottomSheetPagerAdapter).getViewAt(position)
                )
            }
        })
    }

    private fun getTabTitle(position: Int): String? {
        val resources = activity?.resources

        when (position) {
            BottomSheetPagerAdapter.RECENT_INDEX -> return resources?.getString(R.string.tab_recents)
            BottomSheetPagerAdapter.FAVOURITES_INDEX -> return resources?.getString(R.string.tab_favourites)
            BottomSheetPagerAdapter.OFFLINE_INDEX -> return resources?.getString(R.string.tab_offline)
        }

        return ""
    }

    private fun setupBottomSheetBehavior() {
        bottomSheetBehavior = HomepageBottomSheetBehavior.from(viewDataBinding.homepageBottomSheet)
        setBottomSheetPeekHeight()
        setBottomSheetExpandedTop()
    }

    private fun setBottomSheetPeekHeight() {
        rootView.viewTreeObserver?.addOnPreDrawListener {
            bottomSheetBehavior.peekHeight = rootView.height - viewDataBinding.banner.bottom
            true
        }
    }

    private fun setBottomSheetExpandedTop() {
        bottomSheetBehavior.addBottomSheetCallback(object :
                HomepageBottomSheetBehavior.BottomSheetCallback() {

            val backgroundMask = rootView.findViewById<View>(R.id.background_mask)
            val dividend = 1.0f - SLIDE_OFFSET_CHANGE_BACKGROUND
            val bottomSheet = viewDataBinding.homepageBottomSheet
            val maxElevation = bottomSheet.elevation

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                val layoutParams = bottomSheet.layoutParams
                val maxHeight = rootView.height - searchInputView.bottom

                if (bottomSheet.height > maxHeight) {
                    layoutParams.height = maxHeight
                    bottomSheet.layoutParams = layoutParams
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // A background color and BottomSheet elevation transition anim effect
                // as dragging the BottomSheet close to/ far away from the top
                val diff = slideOffset - SLIDE_OFFSET_CHANGE_BACKGROUND

                if (diff <= 0) {
                    // The calculation for "alpha" may get a very small Float instead of 0.0f
                    // Reset it to 0f here
                    if (backgroundMask.alpha > 0f) backgroundMask.alpha = 0f
                    // So is the elevation
                    if (bottomSheet.elevation < maxElevation) bottomSheet.elevation = maxElevation
                    return
                }

                val res = diff / dividend
                backgroundMask.alpha = res
                bottomSheet.elevation = maxElevation - res * maxElevation
            }
        })
    }

    private fun setupFabs() {
        fabMain = rootView.fab_home_main
        fabMaskMain = fabMaskLayout.fab_main
        val fabChat = fabMaskLayout.fab_chat
        val fabUpload = fabMaskLayout.fab_upload
        val textChat = fabMaskLayout.text_chat
        val textUpload = fabMaskLayout.text_upload

        fabMain.setOnClickListener {
            (this::fabMainClickCallback)(fabChat, fabUpload, textChat, textUpload)
        }

        fabMaskMain.setOnClickListener {
            (this::fabMainClickCallback)(fabChat, fabUpload, textChat, textUpload)
        }

        fabChat.setOnClickListener {
        }

        fabUpload.setOnClickListener {
        }
    }

    private fun fabMainClickCallback(fabChat: View, fabUpload: View, textChat: View, textUpload: View) {
        if (isFabExpanded) {
            rotateFab()
            showOut(fabChat, fabUpload, textChat, textUpload)
            runDelay(200) {
                removeMask()
                fabMain.visibility = View.VISIBLE
                isFabExpanded = !isFabExpanded
            }
        } else {
            fabMain.visibility = View.GONE
            addMask()
            // Have to wait for 1ms after mask added, otherwise, fabMaskMain.background is null.
            runDelay(1) {
                rotateFab()
                showIn(fabChat, fabUpload, textChat, textUpload)
                isFabExpanded = !isFabExpanded
            }
        }
    }

    private fun runDelay(delayMs: Long, task: () -> Unit) {
        Handler().postDelayed(task, delayMs)
    }

    private fun showIn(vararg fabs: View) {
        for (fab in fabs) {
            showIn(fab)
        }
    }

    private fun showOut(vararg fabs: View) {
        for (fab in fabs) {
            showOut(fab)
        }
    }

    private fun addMask() {
        window?.addView(fabMaskLayout)
    }

    private fun removeMask() {
        window?.removeView(fabMaskLayout)
    }

    private fun rotateFab() {
        val rotateAnim = ObjectAnimator.ofFloat(
            fabMaskMain, "rotation",
            if (isFabExpanded) FAB_DEFAULT_ANGEL else FAB_ROTATE_ANGEL
        )

        // The tint of the icon in the middle of the FAB
        val tintAnim = ObjectAnimator.ofArgb(
            fabMaskMain.drawable.mutate(), "tint",
            if (isFabExpanded) Color.WHITE else Color.BLACK
        )

        // The background tint of the FAB
        val backgroundTintAnim = ObjectAnimator.ofArgb(
            fabMaskMain.background.mutate(), "tint",
            if (isFabExpanded) resources.getColor(R.color.accentColor) else Color.WHITE
        )

        AnimatorSet().apply {
            duration = FAB_ANIM_DURATION
            playTogether(rotateAnim, backgroundTintAnim, tintAnim)
            start()
        }
    }

    private fun showIn(view: View) {
        view.visibility = View.VISIBLE
        view.alpha = ALPHA_TRANSPARENT
        view.translationY = view.height.toFloat()

        view.animate()
                .setDuration(FAB_ANIM_DURATION)
                .translationY(0f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                    }
                })
                .alpha(ALPHA_OPAQUE)
                .start()
    }

    private fun showOut(view: View) {
        view.animate()
                .setDuration(FAB_ANIM_DURATION)
                .translationY(view.height.toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        view.visibility = View.GONE
                        super.onAnimationEnd(animation)
                    }
                }).alpha(ALPHA_TRANSPARENT)
                .start()
    }

    companion object {
        private const val FAB_ANIM_DURATION = 200L
        private const val ALPHA_TRANSPARENT = 0f
        private const val ALPHA_OPAQUE = 1f
        private const val FAB_DEFAULT_ANGEL = 0f
        private const val FAB_ROTATE_ANGEL = 135f
        private const val SLIDE_OFFSET_CHANGE_BACKGROUND = 0.8f
    }
}