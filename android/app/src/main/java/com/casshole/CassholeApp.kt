package com.casshole

import android.app.Application
import com.casshole.data.email.EmailFetchCoordinator
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class CassholeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        EmailFetchCoordinator.fetchOnAppStart(this)
    }
}
