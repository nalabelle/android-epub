package com.example.androidepub

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.androidepub.databinding.ActivitySettingsBinding
import com.example.androidepub.utils.PreferenceManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.settingsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settingsContainer, SettingsFragment())
                .commit()
        }

        binding.settingsToolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val storageLocationPref = findPreference<Preference>("storage_location")
            storageLocationPref?.summary = PreferenceManager.getEpubStorageLocation(requireContext())

            storageLocationPref?.setOnPreferenceClickListener {
                // Open folder picker
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                startActivityForResult(intent, FOLDER_PICKER_REQUEST_CODE)
                true
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == FOLDER_PICKER_REQUEST_CODE && resultCode == RESULT_OK) {
                data?.data?.let { uri ->
                    // Persist permission to access the folder
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)

                    // Save the URI
                    PreferenceManager.setEpubStorageLocationUri(requireContext(), uri.toString())
                    
                    // Update the summary
                    val storageLocationPref = findPreference<Preference>("storage_location")
                    storageLocationPref?.summary = PreferenceManager.getEpubStorageLocation(requireContext())
                }
            }
        }

        companion object {
            private const val FOLDER_PICKER_REQUEST_CODE = 2001
        }
    }
}
