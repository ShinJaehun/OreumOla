package com.shinjaehun.oreumola.ui.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.shinjaehun.oreumola.R
import com.shinjaehun.oreumola.ui.viewmodels.MainViewModel
import com.shinjaehun.oreumola.ui.viewmodels.StatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StatisticsFragment: Fragment(R.layout.fragment_statistics) {

    private val viewModel: StatisticsViewModel by viewModels()

}