package com.shinjaehun.oreumola.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.shinjaehun.oreumola.repositories.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    val mainRepository: MainRepository
): ViewModel() {
}