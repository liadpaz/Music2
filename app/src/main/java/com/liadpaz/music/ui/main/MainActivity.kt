package com.liadpaz.music.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.liadpaz.music.R
import com.liadpaz.music.databinding.ActivityMainBinding
import com.liadpaz.music.ui.viewmodels.MainViewModel
import com.liadpaz.music.ui.viewmodels.PlayingViewModel
import com.liadpaz.music.utils.InjectorUtils

class MainActivity : AppCompatActivity() {

    private val bottomSheetBehaviorCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            if (slideOffset >= 0) {
                binding.bottomNavView.translationY =
                    binding.bottomNavView.measuredHeightAndState * slideOffset

            }
            binding.coordinatorLayout.also {
                val layoutParams = FrameLayout.LayoutParams(it.layoutParams)
                layoutParams.bottomMargin = binding.bottomNavView.measuredHeightAndState - binding.bottomNavView.translationY.toInt()
                it.layoutParams = layoutParams
            }
//            binding.coordinatorLayout.layoutParams =
//                ConstraintLayout.LayoutParams(binding.coordinatorLayout.layoutParams).also {
//                    it.bottomMargin =
//
//                    Log.d(TAG, "onSlide: ${it.bottomMargin}")
//                }
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                when (playingViewModel.playbackState.value?.state) {
                    PlaybackStateCompat.STATE_NONE,
                    PlaybackStateCompat.STATE_STOPPED -> Unit
                    else -> viewModel.stop()
                }
            } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                binding.coordinatorLayout.also {
                    val layoutParams = FrameLayout.LayoutParams(it.layoutParams)
                    layoutParams.bottomMargin = binding.bottomNavView.measuredHeightAndState
                    it.layoutParams = layoutParams
                }
                Log.d(TAG, "onStateChanged: ${binding.coordinatorLayout.marginBottom} ${binding.coordinatorLayout.measuredHeightAndState}")
//                binding.coordinatorLayout.layoutParams =
//                    RelativeLayout.LayoutParams(binding.coordinatorLayout.layoutParams).also {
//                        it.bottomMargin = binding.bottomNavView.measuredHeightAndState
//                    }
                binding.bottomNavView.translationY = 0F
            }
        }
    }
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    private lateinit var playingViewModel: PlayingViewModel
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityMainBinding.inflate(layoutInflater).also { binding = it }.root)
        setSupportActionBar(binding.toolbarMain)

        playingViewModel =
            ViewModelProvider(viewModelStore, InjectorUtils.providePlayingViewModelFactory(application))[PlayingViewModel::class.java]
        viewModel =
            ViewModelProvider(viewModelStore, InjectorUtils.provideMainViewModelFactory(applicationContext))[MainViewModel::class.java]

        val navController = findNavController(R.id.nav_host_fragment)

        binding.bottomNavView.setupWithNavController(navController)
        setupActionBarWithNavController(navController)

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet).apply {
            addBottomSheetCallback(bottomSheetBehaviorCallback)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 123)
        } else {
            viewModel.onPermissionGranted()
        }

        binding.lifecycleOwner = this
        binding.viewModel = playingViewModel

        binding.layoutNowPlaying.root.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

        binding.layoutNowPlaying.tvSongTitle.isSelected = true

        Log.d(TAG, "onCreate: coordinatorLayout width: ${binding.coordinatorLayout.measuredWidthAndState}")

        playingViewModel.playbackState.observe(this) { playback: PlaybackStateCompat ->
            when (playback.state) {
                PlaybackStateCompat.STATE_BUFFERING,
                PlaybackStateCompat.STATE_PLAYING -> {
                    if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_HIDDEN) {
                        binding.coordinatorLayout.also {
                            val layoutParams = FrameLayout.LayoutParams(it.layoutParams)
                            layoutParams.bottomMargin = binding.bottomNavView.measuredHeightAndState
                            it.layoutParams = layoutParams
                        }
//                        binding.coordinatorLayout.layoutParams =
//                            ConstraintLayout.LayoutParams(binding.coordinatorLayout.layoutParams).also {
//                                it.bottomMargin = binding.bottomNavView.measuredHeightAndState
//                            }
                        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }
                }
                PlaybackStateCompat.STATE_NONE,
                PlaybackStateCompat.STATE_STOPPED -> {
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                }
            }
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
        if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            super.onBackPressed()
        }
    }
}

private const val TAG = "MainActivityLog"