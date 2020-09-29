package com.liadpaz.music.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.navigation.ActivityNavigator
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.liadpaz.music.R
import com.liadpaz.music.databinding.ActivitySettingsBinding
import com.liadpaz.music.ui.viewmodels.SettingsViewModel
import com.liadpaz.music.utils.InjectorUtils

class SettingsActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		ActivitySettingsBinding.inflate(layoutInflater).also {
			setContentView(it.root)
			setSupportActionBar(it.toolbarSettings)
			supportActionBar?.setDisplayHomeAsUpEnabled(true)
			supportFragmentManager.beginTransaction().replace(R.id.settings, SettingsFragment()).commit()
		}
	}

	override fun onSupportNavigateUp(): Boolean = ActivityNavigator(this).popBackStack()

	class SettingsFragment : PreferenceFragmentCompat() {
		private val viewModel by viewModels<SettingsViewModel> {
			InjectorUtils.provideSettingsViewModelFactory(requireContext())
		}
		private lateinit var folderPreference: Preference

		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) = setPreferencesFromResource(R.xml.root_preferences, rootKey)

		override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
			super.onViewCreated(view, savedInstanceState)

			findPreference<SwitchPreferenceCompat>(KEY_STOP_TASK)!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
				viewModel.setStopTask(newValue as Boolean)
				true
			}
			findPreference<Preference>(KEY_FOLDER_RESET)!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
				viewModel.resetFolder()
				true
			}
			findPreference<Preference>(KEY_FOLDER)!!.also { folderPreference = it }.onPreferenceClickListener = Preference.OnPreferenceClickListener {
				startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), REQUEST_OPEN_TREE)
				true
			}
			findPreference<SwitchPreferenceCompat>(KEY_DISPLAY)!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
				viewModel.setDisplayOn(newValue as Boolean)
				true
			}

			viewModel.folder.observe(viewLifecycleOwner) {
				folderPreference.summary = if (it.isEmpty()) getString(R.string.folder_all) else it
			}
		}

		override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
			if (requestCode == REQUEST_OPEN_TREE) {
				if (resultCode == RESULT_OK) {
					val path = data!!.data!!.path!!.substringAfter(':')
					preferenceManager.sharedPreferences.edit().putString(KEY_FOLDER, path).apply()
					viewModel.setFolder(path)
				}
			} else {
				super.onActivityResult(requestCode, resultCode, data)
			}
		}

		companion object {
			private const val REQUEST_OPEN_TREE = 321
		}
	}

	companion object {
		private const val KEY_STOP_TASK = "key_stop_task"
		private const val KEY_FOLDER_RESET = "key_reset_folder"
		private const val KEY_FOLDER = "key_folder"
		private const val KEY_DISPLAY = "key_display"
	}
}