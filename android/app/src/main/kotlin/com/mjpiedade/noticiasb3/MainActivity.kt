package com.mjpiedade.noticiasb3

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Ecrã único e mínimo: explica como adicionar o widget e permite:
 *  - abrir a PWA
 *  - forçar refresh dos dados
 *  - mostrar quantos widgets estão colocados
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val status = findViewById<TextView>(R.id.status)
        val mgr = AppWidgetManager.getInstance(this)
        val ids = mgr.getAppWidgetIds(ComponentName(this, NoticiasWidgetProvider::class.java))
        status.text = if (ids.isEmpty()) {
            getString(R.string.sem_widgets)
        } else {
            resources.getQuantityString(R.plurals.n_widgets, ids.size, ids.size)
        }

        findViewById<Button>(R.id.btn_abrir_pwa).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(NoticiasRepo.URL_PWA)))
        }

        findViewById<Button>(R.id.btn_refrescar).setOnClickListener {
            RefreshWorker.correrJa(this)
            it as Button
            it.text = getString(R.string.refresh_pedido)
            it.isEnabled = false
        }
    }
}
