package com.liadpaz.music.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.TransitionAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.liadpaz.music.R
import com.liadpaz.music.databinding.ActivityMainBinding
import com.liadpaz.music.ui.adapters.ExtendedSongViewPagerAdapter
import com.liadpaz.music.ui.adapters.QueueAdapter
import com.liadpaz.music.ui.viewmodels.MainViewModel
import com.liadpaz.music.ui.viewmodels.PlayingViewModel
import com.liadpaz.music.utils.InjectorUtils
import kotlin.time.ExperimentalTime

class MainActivity : AppCompatActivity() {

    private var smoothScroll = false

    private lateinit var bottomSheet: BottomSheetBehavior<MotionLayout>

    private val playingViewModel by viewModels<PlayingViewModel> {
        InjectorUtils.providePlayingViewModelFactory(application)
    }
    private val viewModel by viewModels<MainViewModel> {
        InjectorUtils.provideMainViewModelFactory(applicationContext)
    }
    private lateinit var binding: ActivityMainBinding

    @ExperimentalTime
    @SuppressLint("SwitchIntDef")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityMainBinding.inflate(layoutInflater).also { binding = it }.root)
        setSupportActionBar(binding.toolbarMain)

        smoothScroll = false

        // sets the volume control on the app to 'STREAM_MUSIC'
        volumeControlStream = AudioManager.STREAM_MUSIC

        val navController = findNavController(R.id.nav_host_fragment)

        // control the title of the action bar, it depends on the destination of the navigation component
        navController.addOnDestinationChangedListener { _, destination, arguments ->
            supportActionBar?.title = when (destination.id) {
                R.id.artistFragment -> arguments?.getParcelable<MediaBrowserCompat.MediaItem>("artist")?.description?.subtitle
                R.id.albumFragment -> arguments?.getParcelable<MediaBrowserCompat.MediaItem>("album")?.description?.description
                else -> getString(R.string.app_name)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 123)
        } else {
            viewModel.onPermissionGranted()
        }

        // bind the lifecycle owner of the layout & the view model
        binding.lifecycleOwner = this
        binding.viewModel = playingViewModel

        // select the song title & song artist in order for the marquee to work
        binding.tvSongTitle.isSelected = true
        binding.tvSongArtist.isSelected = true

        // set the bottom guideline on the top of the navigation bar
        binding.guidelineBottomScreen.setGuidelineEnd(resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_height", "dimen", "android")))
        binding.guidelineTopScreen.setGuidelineBegin(resources.getDimensionPixelSize(resources.getIdentifier("status_bar_height", "dimen", "android")))
        binding.rvQueue.updatePadding(bottom = resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_height", "dimen", "android")))

        binding.rvQueue.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
                makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, if (viewHolder.adapterPosition == playingViewModel.queuePosition.value) 0 else ItemTouchHelper.START or ItemTouchHelper.END)

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                playingViewModel.moveQueueItem(viewHolder.adapterPosition, target.adapterPosition)
                (recyclerView.adapter as QueueAdapter).onMove(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                playingViewModel.removeQueueItem(viewHolder.adapterPosition)
                (binding.rvQueue.adapter as QueueAdapter).onSwipe(viewHolder.adapterPosition)
            }
        }).also { itemTouchHelper ->
            binding.rvQueue.adapter = QueueAdapter({
                playingViewModel.skipToQueueItem(it)
            }) {
                itemTouchHelper.startDrag(it)
            }
        }.attachToRecyclerView(binding.rvQueue)

        playingViewModel.queue.observe(this) {
            binding.viewPager.adapter = ExtendedSongViewPagerAdapter(this)
            binding.viewPager.setCurrentItem(playingViewModel.queuePosition.value ?: 0, false)
        }
        playingViewModel.queuePosition.observe(this) {
            if (lifecycle.currentState < Lifecycle.State.RESUMED) {
                smoothScroll = false
            }
            binding.viewPager.setCurrentItem(it, smoothScroll)
        }

        binding.ibDown.setOnClickListener {
            bottomSheetState = BottomSheetBehavior.STATE_COLLAPSED
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (smoothScroll) {
                    playingViewModel.skipToQueueItem(position)
                } else {
                    smoothScroll = true
                }
            }
        })

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, position: Int, fromUser: Boolean) {
                if (fromUser) {
                    playingViewModel.seekTo(seekBar.progress * 1000L)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })

        playingViewModel.playbackState.observe(this) { playback: PlaybackStateCompat ->
            when (playback.state) {
                PlaybackStateCompat.STATE_BUFFERING,
                PlaybackStateCompat.STATE_PLAYING,
                -> {
                    if (bottomSheetState == BottomSheetBehavior.STATE_HIDDEN) {
                        bottomSheetState = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
                PlaybackStateCompat.STATE_NONE,
                PlaybackStateCompat.STATE_STOPPED,
                -> {
                    bottomSheetState = BottomSheetBehavior.STATE_HIDDEN
                }
            }
        }

        bottomSheet = BottomSheetBehavior.from(binding.bottomSheet).also {
            it.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            binding.bottomSheet.setOnClickListener {
                                bottomSheetState = BottomSheetBehavior.STATE_EXPANDED
                            }
                        }
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            binding.bottomSheet.setOnClickListener { }
                        }
                        BottomSheetBehavior.STATE_HIDDEN -> {
                            playingViewModel.stop()
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    if (slideOffset > 0) {
                        binding.bottomSheet.progress = slideOffset
                    }
                }
            })
        }

        binding.bottomSheet.addTransitionListener(object : TransitionAdapter() {
            override fun onTransitionCompleted(motionLayout: MotionLayout, currentId: Int) {
                when (currentId) {
                    R.id.expanded -> {
                        motionLayout.setTransition(R.id.transition_bottomsheet)
                        binding.bottomSheet.isNestedScrollingEnabled = false
                    }
                    R.id.queue_shown -> binding.bottomSheet.isNestedScrollingEnabled = true
                }
            }

            override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {
                if (startId == R.id.expanded && endId == R.id.queue_shown) {
                    (binding.rvQueue.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(playingViewModel.queuePosition.value
                        ?: 0, 0)
                }
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 123) {
            viewModel.onPermissionGranted()
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        findNavController(R.id.nav_host_fragment).navigateUp()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (bottomSheetState == BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetState = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    override fun onBackPressed() {
        if (bottomSheetState == BottomSheetBehavior.STATE_EXPANDED) {
            if (binding.bottomSheet.currentState == R.id.queue_shown) {
                binding.bottomSheet.transitionToStart()
            } else {
                bottomSheetState = BottomSheetBehavior.STATE_COLLAPSED
            }
        } else {
            super.onBackPressed()
        }
    }

    @BottomSheetBehavior.State
    private var bottomSheetState: Int
        get() = bottomSheet.state
        set(value) {
            bottomSheet.state = value
        }
}

private const val TAG = "MainActivityLog"