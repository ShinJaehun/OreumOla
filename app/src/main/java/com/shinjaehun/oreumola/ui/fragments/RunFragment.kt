package com.shinjaehun.oreumola.ui.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.shinjaehun.oreumola.R
import com.shinjaehun.oreumola.ui.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RunFragment: Fragment(R.layout.fragment_run) {

    private val viewModel: MainViewModel by viewModels()
}