package com.liadpaz.music.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.observe
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.liadpaz.music.R
import com.liadpaz.music.databinding.ActivityMainBinding
import com.liadpaz.music.ui.viewmodels.MainViewModel
import com.liadpaz.music.utils.InjectorUtils
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    private val viewModel by viewModels<MainViewModel> {
        InjectorUtils.provideMainViewModelFactory(applicationContext)
    }
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityMainBinding.inflate(layoutInflater).also { binding = it }.root)
        setSupportActionBar(binding.toolbarMain)

        val navController = findNavController(R.id.nav_host_fragment)

        binding.bottomNavView.setupWithNavController(navController)
        setupActionBarWithNavController(navController)

        viewModel.setBottomSheetBehavior(BottomSheetBehavior.from(binding.bottomSheet))

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 123)
        } else {
            viewModel.onPermissionGranted()
        }

        viewModel.slideOffset.observe(this) { slideOffset ->
            val bottomNavHeight = binding.bottomNavView.measuredHeightAndState
            binding.mainContent.updatePadding(bottom = max(viewModel.getPeekHeight() * (1 + slideOffset), bottomNavHeight.toFloat()).toInt())
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
        if (viewModel.bottomSheetState.value == BottomSheetBehavior.STATE_EXPANDED) {
            viewModel.setState(BottomSheetBehavior.STATE_COLLAPSED)
        } else {
            super.onBackPressed()
        }
    }
}