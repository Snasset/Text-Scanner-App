package com.dheril.textscanner

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Layout
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.AlignmentSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doBeforeTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.dheril.textscanner.data.ItemRepository
import com.dheril.textscanner.data.entity.ItemEntity


import com.dheril.textscanner.databinding.ActivityResultBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private lateinit var title: String
    private var isEdit = false
    private var currentImage: Uri? = null

    private val viewModel by viewModels<ItemViewModel> {
        ViewModelFactory.getInstance(application)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val resultText = intent.getStringExtra(EXTRA_RESULT_TEXT) ?: ""
        val imageUri: Uri? = intent.getStringExtra(EXTRA_IMAGE_URI)?.let { uriString ->
            Uri.parse(uriString)
        }
        currentImage = imageUri
        binding.edDesc.setText(resultText)
        binding.previewImage.setImageURI(imageUri)

        setupView()

        val itemId = intent.getIntExtra(EXTRA_ID, 0)
        if (itemId != 0) {
            isEdit = true
            viewModel.getItem(itemId).observe(this){item ->
                setItem(item)
            }
        }
        setupAction(itemId)

    }

    private fun setItem(item: ItemEntity){
        with(binding){
            edTitle.setText(item.title)
            edDesc.setText(item.descResult)
            currentImage = Uri.parse(item.imageResult)
            Glide.with(this@ResultActivity)
                .load(currentImage)
                .into(previewImage)
        }
    }


    private fun setupView() {
        binding.edTitle.error = "Judul tidak boleh kosong"
        binding.edTitle.doOnTextChanged { text, start, before, count ->
            if (text.isNullOrEmpty()) {
                binding.edTitle.error = "Judul tidak boleh kosong"
                binding.btnSave.isEnabled = false
            } else {
                binding.edTitle.error = null
                binding.btnSave.isEnabled = true
            }
        }

        binding.edDesc.doOnTextChanged { text, start, before, count ->
            if (text.isNullOrEmpty()) {
                binding.edTitle.error = "Deskripsi tidak boleh kosong"
                binding.btnSave.isEnabled = false
            } else {
                binding.edTitle.error = null
                binding.btnSave.isEnabled = true
            }
        }
    }


    private fun setupAction(itemId: Int){
        with(binding){
            btnSave.setOnClickListener {
                title = edTitle.text.toString()
                val date = getCurrentDate()

                if (isEdit){
                    val item = ItemEntity(itemId, title ,currentImage.toString(), edDesc.text.toString(), date)
                    viewModel.update(item)
                } else {
                    val item = ItemEntity(0, title ,currentImage.toString(), edDesc.text.toString(), date)
                    viewModel.insert(item)
                }
                showToast("Berhasil disimpan")
                finish()
            }
        }
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = Date()
        return dateFormat.format(date)
    }

    private fun showToast(message: String) {
        Toast.makeText( this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val EXTRA_RESULT_TEXT = "extra_result_text"
        const val EXTRA_ID = "extra_id"
    }
}