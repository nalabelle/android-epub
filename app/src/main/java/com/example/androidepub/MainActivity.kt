package com.example.androidepub

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.androidepub.databinding.ActivityMainBinding
import com.example.androidepub.viewmodels.MainViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    
    companion object {
        private const val PICK_OUTPUT_DIRECTORY = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        
        // Set initial visibility
        binding.mainContent.visibility = View.GONE
        binding.noFolderSelected.root.visibility = View.VISIBLE

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        
        // Setup observers
        setupObservers()
        
        // Setup click listeners
        setupClickListeners()
        
        // Handle intent (for when app receives a shared URL)
        handleIntent(intent)
        
        // Update UI based on folder selection state
        updateFolderSelectionState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        // Check if this is a URL shared from another app
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrEmpty()) {
                binding.urlEditText.setText(sharedText)
                // Automatically process the URL
                if (hasOutputDirectory()) {
                    // If we have a directory selected, create the EPUB automatically
                    createEpubFromUrl(sharedText)
                } else {
                    // If no directory is selected, show the no folder screen and prompt user to select one
                    showNoFolderSelectedScreen()
                    Toast.makeText(this, R.string.select_output_folder_prompt, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupObservers() {
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.createEpubButton.isEnabled = !isLoading
        }
        
        // Observe status message
        viewModel.statusMessage.observe(this) { message ->
            if (message.isNotEmpty()) {
                binding.statusTextView.text = message
                binding.statusTextView.visibility = View.VISIBLE
            } else {
                binding.statusTextView.visibility = View.GONE
            }
        }
        
        // Observe error messages
        viewModel.errorMessage.observe(this) { errorMessage ->
            if (errorMessage.isNotEmpty()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                binding.statusTextView.text = errorMessage
                binding.statusTextView.visibility = View.VISIBLE
                viewModel.clearErrorMessage()
            }
        }
        
        // Observe success messages
        viewModel.successMessage.observe(this) { successMessage ->
            if (successMessage.isNotEmpty()) {
                Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show()
                binding.statusTextView.text = successMessage
                binding.statusTextView.visibility = View.VISIBLE
                viewModel.clearSuccessMessage()
            }
        }
    }

    private fun setupClickListeners() {
        binding.createEpubButton.setOnClickListener {
            val url = binding.urlEditText.text.toString().trim()
            if (url.isNotEmpty()) {
                createEpubFromUrl(url)
            } else {
                Toast.makeText(this, R.string.error_invalid_url, Toast.LENGTH_SHORT).show()
            }
        }

        binding.selectFolderButton.setOnClickListener {
            selectOutputDirectory()
        }

        binding.noFolderSelected.selectFolderButton.setOnClickListener {
            selectOutputDirectory()
        }
    }

    private fun createEpubFromUrl(url: String) {
        val outputUri = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getString("output_directory", null)?.let { Uri.parse(it) }

        if (outputUri == null) {
            showNoFolderSelectedScreen()
            return
        }

        viewModel.createEpubFromUrl(this, url, outputUri)
    }

    private fun selectOutputDirectory() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        startActivityForResult(intent, PICK_OUTPUT_DIRECTORY)
    }

    private fun hasOutputDirectory(): Boolean {
        val uri = getSharedPreferences("app_prefs", MODE_PRIVATE)
            .getString("output_directory", null)
        return uri != null
    }

    private fun updateFolderSelectionState() {
        if (hasOutputDirectory()) {
            binding.mainContent.visibility = View.VISIBLE
            binding.noFolderSelected.root.visibility = View.GONE
        } else {
            binding.mainContent.visibility = View.GONE
            binding.noFolderSelected.root.visibility = View.VISIBLE
        }
    }

    private fun showNoFolderSelectedScreen() {
        binding.mainContent.visibility = View.GONE
        binding.noFolderSelected.root.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_OUTPUT_DIRECTORY && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                // Persist permission to write to this directory
                contentResolver.takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                
                // Save the selected directory URI
                getSharedPreferences("app_prefs", MODE_PRIVATE).edit()
                    .putString("output_directory", uri.toString())
                    .apply()

                // Show success message
                Toast.makeText(this, R.string.success_folder_selected, Toast.LENGTH_SHORT).show()

                // Update UI
                updateFolderSelectionState()

                // If there's a pending URL, process it
                val url = binding.urlEditText.text.toString().trim()
                if (url.isNotEmpty()) {
                    createEpubFromUrl(url)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
