package dev.olog.presentation.library

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import dev.olog.analytics.TrackerFacade
import dev.olog.core.MediaIdCategory
import dev.olog.presentation.FloatingWindowHelper
import dev.olog.presentation.R
import dev.olog.presentation.base.BaseFragment
import dev.olog.presentation.interfaces.HasBottomNavigation
import dev.olog.presentation.model.BottomNavigationPage
import dev.olog.presentation.model.LibraryPage
import dev.olog.presentation.navigator.Navigator
import dev.olog.presentation.tutorial.TutorialTapTarget
import dev.olog.shared.android.extensions.*
import dev.olog.shared.lazyFast
import kotlinx.android.synthetic.main.fragment_library.*
import kotlinx.coroutines.delay
import javax.inject.Inject

class LibraryFragment : BaseFragment() {

    companion object {
        @JvmStatic
        val TAG_TRACK = LibraryFragment::class.java.name
        @JvmStatic
        val TAG_PODCAST = LibraryFragment::class.java.name + ".podcast"
        const val IS_PODCAST = "is_podcast"

        @JvmStatic
        fun newInstance(isPodcast: Boolean): LibraryFragment {
            return LibraryFragment().withArguments(
                IS_PODCAST to isPodcast
            )
        }
    }

    @Inject
    internal lateinit var presenter: LibraryFragmentPresenter
    @Inject
    lateinit var navigator: Navigator
    @Inject
    lateinit var trackerFacade: TrackerFacade

    private val isPodcast by lazyFast {
        getArgument<Boolean>(IS_PODCAST)
    }

    private val pagerAdapter by lazyFast {
        LibraryFragmentAdapter(
            act.applicationContext, childFragmentManager, presenter.getCategories(isPodcast)
        )
    }

    fun isCurrentFragmentFolderTree(): Boolean {
        return pagerAdapter.getCategoryAtPosition(viewPager.currentItem) == MediaIdCategory.FOLDERS &&
                pagerAdapter.showFolderAsHierarchy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager.adapter = pagerAdapter
        tabLayout.setupWithViewPager(viewPager)
        viewPager.currentItem = presenter.getViewPagerLastPage(pagerAdapter.count, isPodcast)

        pagerEmptyState.toggleVisibility(pagerAdapter.isEmpty(), true)

        val selectedView: TextView = if (!isPodcast) tracks else podcasts
        val unselectedView: TextView = if (!isPodcast) podcasts else tracks
        selectedView.setTextColor(requireContext().textColorPrimary())
        unselectedView.setTextColor(requireContext().textColorSecondary())

        if (!presenter.canShowPodcasts()){
            podcasts.isVisible = false
        }

        if (presenter.showFloatingWindowTutorialIfNeverShown()) {
            viewLifecycleOwner.lifecycleScope
                .launchWhenResumed {
                    delay(500) // TODO try
                    TutorialTapTarget.floatingWindow(floatingWindow)
                }
        }
    }

    override fun onResume() {
        super.onResume()
        viewPager.addOnPageChangeListener(onPageChangeListener)
        more.setOnClickListener { navigator.toMainPopup(it, createMediaId()) }
        floatingWindow.setOnClickListener { startServiceOrRequestOverlayPermission() }

        tracks.setOnClickListener { changeLibraryPage(LibraryPage.TRACKS) }
        podcasts.setOnClickListener { changeLibraryPage(LibraryPage.PODCASTS) }
    }

    override fun onPause() {
        super.onPause()
        viewPager.removeOnPageChangeListener(onPageChangeListener)
        more.setOnClickListener(null)
        floatingWindow.setOnClickListener(null)
        tracks.setOnClickListener(null)
        podcasts.setOnClickListener(null)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewPager.adapter = null
    }

    private fun changeLibraryPage(page: LibraryPage) {
        presenter.setLibraryPage(page)
        (requireActivity() as HasBottomNavigation).navigate(BottomNavigationPage.LIBRARY)
    }

    private fun createMediaId(): MediaIdCategory? {
        return pagerAdapter.getCategoryAtPosition(viewPager.currentItem)
    }

    private fun startServiceOrRequestOverlayPermission() {
        FloatingWindowHelper.startServiceOrRequestOverlayPermission(activity!!)
    }

    private val onPageChangeListener =
        object : androidx.viewpager.widget.ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                presenter.setViewPagerLastPage(position, isPodcast)
                pagerAdapter.getCategoryAtPosition(position)?.let {
                    trackerFacade.trackScreen(it.toString(), null)
                }

            }
        }

    override fun provideLayoutId(): Int = R.layout.fragment_library
}