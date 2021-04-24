package com.utsman.geolibsample.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.utsman.geolibsample.MainActivity
import com.utsman.geolibsample.R

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnGettingStarted = view.findViewById<Button>(R.id.btn_getting_started)
        val btnGithub = view.findViewById<CardView>(R.id.btn_github)
        val btnDocs = view.findViewById<CardView>(R.id.btn_docs)

        btnGettingStarted.setOnClickListener {
            val mainActivity = (activity as MainActivity)
            mainActivity.openDrawer()
        }

        btnGithub.setOnClickListener {
            openUrl("https://github.com/utsmannn/geolib")
        }

        btnDocs.setOnClickListener {
            openUrl("https://utsmannn.github.io/geolib/docs/")
        }
    }

    private fun openUrl(url: String) {
        Intent(Intent.ACTION_VIEW).run {
            data = Uri.parse(url)
            startActivity(this)
        }
    }
}