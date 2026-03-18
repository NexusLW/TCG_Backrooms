package com.example.tcgbackrooms

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    //user id and username passed from LoginActivity
    //id y nombre de usuario pasados desde LoginActivity
    var userId: Int = -1
    var username: String = ""

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var welcomeText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //grab the user data from the intent
        //obtener los datos del usuario desde el intent
        userId = intent.getIntExtra("userId", -1)
        username = intent.getStringExtra("username") ?: ""

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        welcomeText = findViewById(R.id.welcomeText)

        welcomeText.text = "welcome, $username"

        //set up the viewpager with our three fragments
        //configurar el viewpager con nuestros tres fragmentos
        viewPager.adapter = MainPagerAdapter(this)

        //connect the tab layout to the viewpager so they stay in sync
        //conectar el tab layout al viewpager para que se mantengan sincronizados
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Packs"
                1 -> "Collection"
                2 -> "My Cards"
                else -> ""
            }
        }.attach()
    }

    //-------Pager adapter / Adaptador del pager-------

    //simple adapter that holds the three fragments
    //adaptador simple que contiene los tres fragmentos
    private inner class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment()
                1 -> CollectionFragment()
                2 -> CustomCollectionFragment()
                else -> HomeFragment()
            }
        }
    }
}