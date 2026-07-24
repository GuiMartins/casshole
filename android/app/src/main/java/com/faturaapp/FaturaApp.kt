package com.faturaapp

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class FaturaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}
