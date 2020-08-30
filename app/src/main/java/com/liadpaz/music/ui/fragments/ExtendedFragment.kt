package com.liadpaz.music.ui.fragments
//
//import android.os.Bundle
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import androidx.viewpager2.widget.ViewPager2
//import com.google.android.material.bottomsheet.BottomSheetBehavior
//import com.liadpaz.music.databinding.FragmentExtendedBinding
//import com.liadpaz.music.ui.adapters.ExtendedSongViewPagerAdapter
//import com.liadpaz.music.ui.viewmodels.MainViewModel
//import com.liadpaz.music.ui.viewmodels.PlayingViewModel
//import com.liadpaz.music.utils.InjectorUtils
//
//class ExtendedFragment : Fragment() {
//
//    private var smoothScroll = false
//
//    private lateinit var mainViewModel: MainViewModel
//    private lateinit var viewModel: PlayingViewModel
//    private lateinit var binding: FragmentExtendedBinding
//
//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
//        FragmentExtendedBinding.inflate(inflater, container, false).also { binding = it }.root
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        viewModel =
//            ViewModelProvider(requireActivity().viewModelStore, InjectorUtils.providePlayingViewModelFactory(requireActivity().application))[PlayingViewModel::class.java]
//
//        viewModel.queue.observe(viewLifecycleOwner, {
//            Log.d(TAG, "onViewCreated: QUEUE CHANGED WTF??!?!")
//            smoothScroll = false
//            binding.viewPager.adapter = ExtendedSongViewPagerAdapter(this)
//            binding.viewPager.setCurrentItem(viewModel.queuePosition.value ?: 0, false)
//        })
//        viewModel.queuePosition.observe(viewLifecycleOwner, {
//            binding.viewPager.setCurrentItem(it, smoothScroll)
//            smoothScroll = true
//        })
//
//        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
//            override fun onPageSelected(position: Int) {
//                if (smoothScroll) {
//                    viewModel.skipToQueueItem(position)
//                } else {
//                    smoothScroll = true
//                }
//            }
//        })
//    }
//}
//
//private const val TAG = "ExtendedFragment"