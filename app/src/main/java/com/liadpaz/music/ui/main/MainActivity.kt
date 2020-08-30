package com.liadpaz.music.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.liadpaz.music.R
import com.liadpaz.music.databinding.ActivityMainBinding
import com.liadpaz.music.ui.adapters.ExtendedSongViewPagerAdapter
import com.liadpaz.music.ui.viewmodels.MainViewModel
import com.liadpaz.music.ui.viewmodels.PlayingViewModel
import com.liadpaz.music.utils.InjectorUtils
import com.liadpaz.music.utils.extensions.findDarkColor

class MainActivity : AppCompatActivity() {

    private var smoothScroll = false

    private lateinit var bottomSheet: BottomSheetBehavior<MotionLayout>

    private val playingViewModel by viewModels<PlayingViewModel> {
        InjectorUtils.providePlayingViewModelFactory(application)
    }
    private val viewModel by viewModels<MainViewModel> {
        InjectorUtils.provideMainViewModelFactory(applicationContext)
    }
    lateinit var binding: ActivityMainBinding

    @SuppressLint("SwitchIntDef")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityMainBinding.inflate(layoutInflater).also { binding = it }.root)
        setSupportActionBar(binding.toolbarMain)

        volumeControlStream = AudioManager.STREAM_MUSIC

        val navController = findNavController(R.id.nav_host_fragment)

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

        binding.lifecycleOwner = this
        binding.viewModel = playingViewModel

        binding.tvSongTitle.isSelected = true
        binding.tvSongArtist.isSelected = true

        playingViewModel.queue.observe(this) {
            Log.d(TAG, "onViewCreated: QUEUE CHANGED WTF??!?!")
//            smoothScroll = false
            binding.viewPager.adapter = ExtendedSongViewPagerAdapter(this)
            binding.viewPager.setCurrentItem(playingViewModel.queuePosition.value ?: 0, false)
        }
        playingViewModel.queuePosition.observe(this) {
            binding.viewPager.setCurrentItem(it, smoothScroll)
            smoothScroll = true
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
                PlaybackStateCompat.STATE_PLAYING -> {
                    if (bottomSheetState == BottomSheetBehavior.STATE_HIDDEN) {
                        bottomSheetState = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
                PlaybackStateCompat.STATE_NONE,
                PlaybackStateCompat.STATE_STOPPED -> {
                    bottomSheetState = BottomSheetBehavior.STATE_HIDDEN
                }
            }
        }

        playingViewModel.mediaMetadata.observe(this) {
            binding.bottomSheet.background =
                GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(it.color.getDominantColor(Color.GRAY), it.color.findDarkColor()))
            binding.seekBar.max = it.duration.toInt() / 1000
            binding.tvDuration.text =
                PlayingViewModel.NowPlayingMetadata.timestampToMSS(it.duration)
        }
        playingViewModel.mediaPosition.observe(this) {
            binding.seekBar.progress = (it / 1000.0).toInt()
            binding.tvElapsedTime.text = PlayingViewModel.NowPlayingMetadata.timestampToMSS(it)
        }

        bottomSheet = BottomSheetBehavior.from(binding.bottomSheet).also {
            it.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    when (newState) {
                        BottomSheetBehavior.STATE_COLLAPSED -> {
                            binding.viewPager.isUserInputEnabled = false
                            binding.bottomSheet.setOnClickListener {
                                bottomSheetState = BottomSheetBehavior.STATE_EXPANDED
                            }
                        }
                        BottomSheetBehavior.STATE_EXPANDED -> {
                            binding.viewPager.isUserInputEnabled = true
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
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 123) {
            viewModel.onPermissionGranted()
        }
    }

    override fun onSupportNavigateUp(): Boolean =
        findNavController(R.id.nav_host_fragment).navigateUp()

    override fun onBackPressed() {
        if (bottomSheetState == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetState = BottomSheetBehavior.STATE_COLLAPSED
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