package com.dheril.textscanner

import android.app.Application
import androidx.lifecycle.ViewModel
import com.dheril.textscanner.data.ItemRepository
import com.dheril.textscanner.data.entity.ItemEntity

class ItemViewModel(application: Application) : ViewModel() {
    private val mItemRepository: ItemRepository = ItemRepository(application)


    fun insert(item: ItemEntity) = mItemRepository.insert(item)

    fun delete(item: ItemEntity) = mItemRepository.delete(item)

    fun update(item: ItemEntity) = mItemRepository.update(item)

    fun getAllItem() = mItemRepository.getAllItem()

    fun getItem(id: Int) = mItemRepository.getItem(id)
}