package com.dheril.textscanner

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dheril.textscanner.ResultActivity.Companion.EXTRA_IMAGE_URI
import com.dheril.textscanner.ResultActivity.Companion.EXTRA_RESULT_TEXT
import com.dheril.textscanner.data.entity.ItemEntity
import com.dheril.textscanner.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentImageUri: Uri? = null
    private val viewModel by viewModels<ItemViewModel>() {
        ViewModelFactory.getInstance(application)
    }



    private val timeStamp: String = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(
        Date()
    )

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                showToast("Izin diberikan")
            } else {
                showToast("Izin ditolak")
            }
        }

    private lateinit var adapter: ItemAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!cameraPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION_CAMERA)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val layoutManager = LinearLayoutManager(this)
        binding.rvItem.layoutManager = layoutManager

        viewModel.getAllItem().observe(this){item ->
            setItems(item)
            if (item.isEmpty()){
                binding.tvEmpty.visibility = View.VISIBLE
            } else {
                binding.tvEmpty.visibility = View.GONE
                setItems(item)
            }
        }

        binding.addButton.setOnClickListener{
            showBottomSheet()
        }

    }


    private fun setItems(item: List<ItemEntity>){
        val itemReversed = item.reversed()
        adapter = ItemAdapter(viewModel, this@MainActivity)
        adapter.submitList(itemReversed)
        binding.rvItem.adapter = adapter
    }
    private fun showBottomSheet(){
        val dialogView = layoutInflater.inflate(R.layout.bottomsheet,null)
        val dialog = BottomSheetDialog(this)
        val galleryGroup = dialogView.findViewById<View>(R.id.galleryGroup)
        val cameraGroup =  dialogView.findViewById<View>(R.id.cameraGroup)
        galleryGroup.setOnClickListener{
            startGallery()
            dialog.dismiss()
        }
        cameraGroup.setOnClickListener{
            startCamera()
            dialog.dismiss()
        }
        dialog.setContentView(dialogView)
        dialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            currentImageUri = data?.let { UCrop.getOutput(it) }
            currentImageUri?.let { analyzeImage(it) }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            showToast(cropError.toString())
        }
    }

    private fun openCropActivity(inputUri: Uri?) {
        val outputUri = getImageUri(this)
        val options = UCrop.Options().apply {
            setFreeStyleCropEnabled(true)
        }

        UCrop.of(inputUri!!, outputUri)
            .withMaxResultSize(1920, 1080)
            .withAspectRatio(1f, 1F)
            .withOptions(options)
            .start(this)
    }

    private fun cameraPermissionsGranted() =
        ContextCompat.checkSelfPermission(this, REQUIRED_PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            openCropActivity(currentImageUri)
        } else {
            showToast("Tidak ada gambar yang dipilih")
        }
    }

    private fun startCamera() {
        currentImageUri = getImageUri(this)
        currentImageUri?.let { launcherIntentCamera.launch(it) }
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            openCropActivity(currentImageUri)
        }
    }


    private fun analyzeImage(uri: Uri) {
        binding.progressIndicator.visibility = View.VISIBLE

        val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val inputImage: InputImage = InputImage.fromFilePath(this, uri)
        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText: Text ->
                val detectedText: String = visionText.text
                if (detectedText.isNotBlank()) {
                    binding.progressIndicator.visibility = View.GONE
                    val intent = Intent(this, ResultActivity::class.java)
                    intent.putExtra(EXTRA_IMAGE_URI, currentImageUri.toString())
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    intent.putExtra(EXTRA_RESULT_TEXT, detectedText)
                    startActivity(intent)
                } else {
                    binding.progressIndicator.visibility = View.GONE
                    showToast("Tidak ada teks terbaca")
                }
            }
            .addOnFailureListener {
                binding.progressIndicator.visibility = View.GONE
                showToast(it.message.toString())
            }
    }

    private fun getImageUri(context: Context): Uri {
        var uri: Uri? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val timeStamp: String = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$timeStamp.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/MyCamera/")
            }
            uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
        }
        return uri ?: getImageUriForPreQ(context)
    }

    private fun getImageUriForPreQ(context: Context): Uri {
        val timeStamp: String = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(Date())
        val filesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File(filesDir, "/MyCamera/$timeStamp.jpg")
        if (imageFile.parentFile?.exists() == false) imageFile.parentFile?.mkdir()
        return FileProvider.getUriForFile(
            context,
            BuildConfig.APPLICATION_ID + ".provider",
            imageFile
        )
    }

    private fun showToast(message: String) {
        Toast.makeText( this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
        private const val REQUIRED_PERMISSION_CAMERA = Manifest.permission.CAMERA
    }


}