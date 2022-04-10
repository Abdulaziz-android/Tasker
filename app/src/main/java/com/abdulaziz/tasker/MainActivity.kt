package com.abdulaziz.tasker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.abdulaziz.tasker.databinding.ActivityMainBinding
import com.abdulaziz.tasker.fragments.HomeFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding:ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.getStringExtra("noti") != null) {
            val bundle = bundleOf(Pair("noti", intent.getStringExtra("noti")))
            supportFragmentManager.beginTransaction().replace(
                R.id.fragment_container,
                HomeFragment::class.java,
                bundle
            ).commit()
        } else {
            supportFragmentManager.beginTransaction().replace(
                R.id.fragment_container,
                HomeFragment()
            ).commit()
        }
    }
}