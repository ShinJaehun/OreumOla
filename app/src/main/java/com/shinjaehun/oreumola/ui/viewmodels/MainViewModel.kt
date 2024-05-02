package com.shinjaehun.oreumola.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.shinjaehun.oreumola.repositories.MainRepository
import javax.inject.Inject

class MainViewModel @Inject constructor(
    val mainRepository: MainRepository
): ViewModel() {
}