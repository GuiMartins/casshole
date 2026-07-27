package com.moneyhole

import android.app.Application
import com.moneyhole.data.email.EmailFetchCoordinator
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MoneyHoleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
        EmailFetchCoordinator.fetchOnAppStart(this)
    }
}
