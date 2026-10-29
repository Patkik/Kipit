package com.kipit.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import com.kipit.app.presentation.AppViewModel
import com.kipit.app.presentation.KipitApp
import com.kipit.app.presentation.theme.KipitTheme
@AndroidEntryPoint class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);enableEdgeToEdge();val vm=ViewModelProvider(this)[AppViewModel::class.java];setContent{KipitTheme{KipitApp(vm)}}}}
