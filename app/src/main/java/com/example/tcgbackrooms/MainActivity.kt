package com.example.tcgbackrooms

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    var userId: Int = -1
    var username: String = ""

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userId = intent.getIntExtra("userId", -1)
        username = intent.getStringExtra("username") ?: ""

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        viewPager.adapter = MainPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Packs"
                1 -> "Collection"
                2 -> "My Cards"
                else -> ""
            }
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        //set the username as the title of the user icon so it shows on long press
        //poner el username como titulo del icono de usuario para que aparezca al mantener pulsado
        menu.findItem(R.id.action_user)?.title = username

        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_user -> {
                val anchor = findViewById<View>(R.id.action_user) ?: return true
                val popupView = layoutInflater.inflate(R.layout.popup_user, null)
                popupView.findViewById<android.widget.TextView>(R.id.popupUsername).text = "User: $username"

                val popup = android.widget.PopupWindow(
                    popupView,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    true // focusable so it dismisses on outside tap
                )
                popup.elevation = 8f
                popup.showAsDropDown(anchor)
                true
            }
            R.id.action_info -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private inner class MainPagerAdapter(activity: AppCompatActivity) :
        FragmentStateAdapter(activity) {

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