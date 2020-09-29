package com.liadpaz.music.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.motion.widget.TransitionAdapter
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.navigation.NavController
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
import com.liadpaz.music.ui.adapters.AlbumArtViewPagerAdapter
import com.liadpaz.music.ui.adapters.QueueAdapter
import com.liadpaz.music.ui.viewmodels.MainViewModel
import com.liadpaz.music.ui.viewmodels.PlayingViewModel
import com.liadpaz.music.utils.InjectorUtils

class MainActivity : AppCompatActivity() {

	private var pageSelected = false
	private var positionChanged = false

	private lateinit var bottomSheet: BottomSheetBehavior<MotionLayout>
	private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
		@SuppressLint("SwitchIntDef")
		override fun onStateChanged(bottomSheet: View, newState: Int) {
			when (newState) {
				BottomSheetBehavior.STATE_COLLAPSED -> {
					binding.bottomSheet.isClickable = true
					window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
					setButtonsState(View.INVISIBLE)
				}
				BottomSheetBehavior.STATE_SETTLING -> setButtonsState(View.VISIBLE)
				BottomSheetBehavior.STATE_EXPANDED -> {
					binding.bottomSheet.isClickable = false
					if (viewModel.displayOn.value == true) {
						window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
					}
				}
				BottomSheetBehavior.STATE_HIDDEN -> playingViewModel.stop()
			}
		}

		override fun onSlide(bottomSheet: View, slideOffset: Float) {
			// TODO: try to change ui to be youtube music-like (bottom navigation and bottom sheet above also cool animation)
			if (slideOffset > 0) {
				binding.bottomSheet.progress = slideOffset
			}
		}
	}

	private lateinit var navController: NavController

	private val playingViewModel by viewModels<PlayingViewModel> {
		InjectorUtils.providePlayingViewModelFactory(application)
	}
	private val viewModel by viewModels<MainViewModel> {
		InjectorUtils.provideMainViewModelFactory(applicationContext)
	}
	private lateinit var binding: ActivityMainBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(ActivityMainBinding.inflate(layoutInflater).also { binding = it }.root)
		setSupportActionBar(binding.toolbarMain)

		// sets the volume control on the app to 'STREAM_MUSIC'
		volumeControlStream = AudioManager.STREAM_MUSIC

		// control the title of the action bar, it depends on the destination of the navigation component
		findNavController(R.id.nav_host_fragment).also { navController = it }.addOnDestinationChangedListener { _, destination, arguments ->
			supportActionBar?.title = when (destination.id) {
				R.id.playlistFragment -> arguments?.getParcelable<MediaBrowserCompat.MediaItem>("playlist")?.description?.title
				R.id.artistFragment -> arguments?.getString("artist")
				R.id.albumFragment -> arguments?.getString("album")
				else -> getString(R.string.app_name)
			}
		}

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
			requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSION_STORAGE)
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
				playingViewModel.removeQueueItem(viewHolder.adapterPosition)
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
					if (initialIndex != viewHolder.adapterPosition) {
						playingViewModel.moveQueueItem(initialIndex, viewHolder.adapterPosition)
					}
					initialIndex = -1
				}
			}
		}).also { itemTouchHelper ->
			binding.rvQueue.adapter = QueueAdapter(playingViewModel::skipToQueueItem, itemTouchHelper::startDrag)
		}.attachToRecyclerView(binding.rvQueue)

		binding.bottomSheet.setOnClickListener { bottomSheetState = BottomSheetBehavior.STATE_EXPANDED }
		binding.ibDown.setOnClickListener { bottomSheetState = BottomSheetBehavior.STATE_COLLAPSED }
		binding.ibMore.setOnClickListener { /* TODO: set on more menu */ }

		binding.viewPager.adapter = AlbumArtViewPagerAdapter()
		binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
			override fun onPageSelected(position: Int) {
				if (!positionChanged) {
					pageSelected = true
					Log.d(TAG, "onPageSelected: ${debug()}")
					playingViewModel.skipToQueueItem(position)
				} else {
					positionChanged = false
					Log.d(TAG, "onPageSelected: ${debug()}")
				}
			}
		})

		binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
			override fun onProgressChanged(seekBar: SeekBar, position: Int, fromUser: Boolean) = Unit

			override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

			override fun onStopTrackingTouch(seekBar: SeekBar) {
				playingViewModel.seekTo(seekBar.progress * 1000L)
			}
		})

		playingViewModel.queuePosition.observe(this) {
			if (!pageSelected) {
				positionChanged = true
				Log.d(TAG, "onCreate: ${debug()}")
				binding.viewPager.currentItem = it
			} else {
				pageSelected = false
				Log.d(TAG, "onCreate: ${debug()}")
			}
		}
		playingViewModel.playbackState.observe(this) { playback: PlaybackStateCompat ->
			when (playback.state) {
				PlaybackStateCompat.STATE_NONE,
				PlaybackStateCompat.STATE_STOPPED,
				PlaybackStateCompat.STATE_ERROR,
				-> bottomSheetState = BottomSheetBehavior.STATE_HIDDEN
				else -> {
					if (bottomSheetState == BottomSheetBehavior.STATE_HIDDEN) {
						bottomSheetState = BottomSheetBehavior.STATE_COLLAPSED
					}
				}
			}
		}

		bottomSheet = BottomSheetBehavior.from(binding.bottomSheet).also { it.addBottomSheetCallback(bottomSheetCallback) }
		bottomSheet.peekHeight = resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_height", "dimen", "android")) + resources.getDimensionPixelSize(R.dimen.bottomSheetHeight)

		binding.bottomSheet.addTransitionListener(object : TransitionAdapter() {
			override fun onTransitionCompleted(motionLayout: MotionLayout, currentId: Int) {
				if (currentId == R.id.expanded) {
					motionLayout.setTransition(R.id.transition_bottomsheet)
					bottomSheet.isDraggable = true
				}
			}

			override fun onTransitionStarted(motionLayout: MotionLayout, startId: Int, endId: Int) {
				if (motionLayout.progress < 100F && startId == R.id.expanded && endId == R.id.queue_shown) {
					bottomSheet.isDraggable = false
					binding.rvQueue.stopScroll()
					(binding.rvQueue.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(playingViewModel.queuePosition.value!!, 0)
				}
			}
		})
		handleIntent(intent)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		if (requestCode == REQUEST_PERMISSION_STORAGE) {
			viewModel.onPermissionGranted()
		}
	}

	override fun onSupportNavigateUp(): Boolean = navController.navigateUp()

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
		set(value) = bottomSheet.setState(value)


	/**
	 * This function sets the three buttons (down, queue and more) visibility.
	 *
	 * @param state The visibility state to set.
	 */
	private fun setButtonsState(state: Int) {
		binding.ibDown.visibility = state
		binding.ibToggleQueue.visibility = state
		binding.ibMore.visibility = state
	}

	/**
	 * This function handles incoming [Intent]'s to check whether the bottom sheet should be extended.
	 *
	 * @param intent The [Intent] to check, may be null.
	 */
	private fun handleIntent(intent: Intent?) {
		if (intent?.extras?.getString(EXTRA_TYPE) == MusicService::class.java.canonicalName) {
			binding.root.postDelayed({
				if (bottomSheetState == BottomSheetBehavior.STATE_COLLAPSED) {
					bottomSheetState = BottomSheetBehavior.STATE_EXPANDED
				}
			}, 1)
		}
	}

	private fun debug() = "pageSelected: $pageSelected positionChanged $positionChanged"

	companion object {
		private const val REQUEST_PERMISSION_STORAGE = 123
	}
}

private const val TAG = "MainActivityLog"