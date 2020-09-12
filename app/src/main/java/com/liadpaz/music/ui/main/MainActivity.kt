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
import android.view.WindowManager
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.TransitionAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.navigation.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.liadpaz.music.R
import com.liadpaz.music.databinding.ActivityMainBinding
import com.liadpaz.music.service.MusicService
import com.liadpaz.music.service.MusicService.Companion.EXTRA_TYPE
import com.liadpaz.music.ui.adapters.ExtendedSongViewPagerAdapter
import com.liadpaz.music.ui.adapters.QueueAdapter
import com.liadpaz.music.ui.utils.ProgressSeekBar
import com.liadpaz.music.ui.viewmodels.MainViewModel
import com.liadpaz.music.ui.viewmodels.PlayingViewModel
import com.liadpaz.music.utils.InjectorUtils

class MainActivity : AppCompatActivity() {

    private var smoothScroll = false
    private var isQueueChanging = false

    private lateinit var bottomSheet: BottomSheetBehavior<MotionLayout>

    private val playingViewModel by viewModels<PlayingViewModel> {
        InjectorUtils.providePlayingViewModelFactory(application)
    }
    private val viewModel by viewModels<MainViewModel> {
        InjectorUtils.provideMainViewModelFactory(applicationContext)
    }
    private lateinit var binding: ActivityMainBinding

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
                R.id.playlistFragment -> arguments?.getParcelable<MediaBrowserCompat.MediaItem>("playlist")?.description?.title
                R.id.artistFragment -> arguments?.getString("artist")
                R.id.albumFragment -> arguments?.getString("album")
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
        binding.tvSongTitleSmall.isSelected = true
        binding.tvSongArtistSmall.isSelected = true
        binding.tvSongTitleLarge.isSelected = true
        binding.tvSongArtistLarge.isSelected = true

        // set the bottom guideline on the top of the navigation bar
        binding.guidelineBottomScreen.setGuidelineEnd(resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_height", "dimen", "android")))
        binding.guidelineTopScreen.setGuidelineBegin(resources.getDimensionPixelSize(resources.getIdentifier("status_bar_height", "dimen", "android")))
        binding.rvQueue.updatePadding(bottom = resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_height", "dimen", "android")))

        binding.rvQueue.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        ItemTouchHelper(object : ItemTouchHelper.Callback() {
            private var initialIndex = -1

            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
                makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, if (viewHolder.adapterPosition == playingViewModel.queuePosition.value) 0 else ItemTouchHelper.START or ItemTouchHelper.END)

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                (recyclerView.adapter as QueueAdapter).onMove(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                isQueueChanging = true
                val position = viewHolder.adapterPosition
                (binding.rvQueue.adapter as QueueAdapter).onSwipe(position)
                playingViewModel.removeQueueItem(position)
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder!!.itemView.elevation = 16F
                    initialIndex = viewHolder.adapterPosition
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                viewHolder.itemView.elevation = 0F
                if (initialIndex != -1) {
                    isQueueChanging = initialIndex != viewHolder.adapterPosition
                    if (isQueueChanging) {
                        playingViewModel.moveQueueItem(initialIndex, viewHolder.adapterPosition)
                    }
                    initialIndex = -1
                }
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
            isQueueChanging = false
        }
        playingViewModel.queuePosition.observe(this) {
            if (!isQueueChanging) {
                binding.viewPager.setCurrentItem(it, smoothScroll)
                smoothScroll = true
            } else {
                isQueueChanging = false
            }
        }

        binding.ibDown.setOnClickListener {
            bottomSheetState = BottomSheetBehavior.STATE_COLLAPSED
        }
        binding.ibMore.setOnClickListener {
            // TODO: set on more menu
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (smoothScroll && !isQueueChanging) {
                    playingViewModel.skipToQueueItem(position)
                } else {
                    smoothScroll = true
                }
            }
        })
        binding.viewPager.requestDisallowInterceptTouchEvent(true)

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, position: Int, fromUser: Boolean) = Unit

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                (seekBar as ProgressSeekBar).isUser = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                playingViewModel.seekTo(seekBar.progress * 1000L)
                (seekBar as ProgressSeekBar).isUser = false
            }
        })

        playingViewModel.playbackState.observe(this) { playback: PlaybackStateCompat ->
            when (playback.state) {
                PlaybackStateCompat.STATE_NONE,
                PlaybackStateCompat.STATE_STOPPED,
                PlaybackStateCompat.STATE_ERROR,
                -> {
                    bottomSheetState = BottomSheetBehavior.STATE_HIDDEN
                }
                else -> {
                    if (bottomSheetState == BottomSheetBehavior.STATE_HIDDEN) {
                        bottomSheetState = BottomSheetBehavior.STATE_COLLAPSED
                    }
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
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                            setButtonsState(View.INVISIBLE)
                        }
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            binding.bottomSheet.setOnClickListener { }
                            // TODO: check if screen should stay on
                            setButtonsState(View.VISIBLE)
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
        bottomSheet.peekHeight = resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_height", "dimen", "android")) + resources.getDimensionPixelSize(R.dimen.bottomSheetHeight)

        binding.bottomSheet.addTransitionListener(object : TransitionAdapter() {
            override fun onTransitionCompleted(motionLayout: MotionLayout, currentId: Int) {
                if (currentId == R.id.expanded) {
                    motionLayout.setTransition(R.id.transition_bottomsheet)
                    binding.bottomSheet.isNestedScrollingEnabled = false
                }
            }

            override fun onTransitionStarted(motionLayout: MotionLayout, startId: Int, endId: Int) {
                if (motionLayout.progress < 100F && startId == R.id.expanded && endId == R.id.queue_shown) {
                    binding.bottomSheet.isNestedScrollingEnabled = true
                    binding.rvQueue.stopScroll()
                    (binding.rvQueue.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(playingViewModel.queuePosition.value!!, 0)
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        handleIntent(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 123) {
            viewModel.onPermissionGranted()
        }
    }

    override fun onSupportNavigateUp(): Boolean = findNavController(R.id.nav_host_fragment).navigateUp()

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onBackPressed() {
        if (bottomSheetState == BottomSheetBehavior.STATE_EXPANDED) {
            if (binding.bottomSheet.currentState == R.id.queue_shown) {
                binding.bottomSheet.transitionToStart()
            } else {
                if (binding.bottomSheet.progress != 0F) {
                    binding.bottomSheet.progress = 0F
                }
                if (binding.bottomSheet.currentState != R.id.expanded) {
                    binding.bottomSheet.setTransition(R.id.transition_bottomsheet)
                }
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

    private fun setButtonsState(state: Int) {
        binding.ibDown.visibility = state
        binding.ibToggleQueue.visibility = state
        binding.ibMore.visibility = state
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.extras?.getString(EXTRA_TYPE) == MusicService::class.java.canonicalName) {
            if (bottomSheetState == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetState = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }
}

private const val TAG = "MainActivityLog"