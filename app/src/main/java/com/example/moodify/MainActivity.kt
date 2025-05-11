package com.example.moodify // Replace with your actual package name

// Core Android/AppCompat imports
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment

// Material Design Components imports
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView // Import BottomNav
import com.google.android.material.navigation.NavigationView

import android.content.Intent // Make sure Intent is imported
// Make sure Auth is imported
import com.google.firebase.auth.FirebaseAuth // Core Auth class
import com.google.firebase.auth.ktx.auth    // KTX Extension Property
import com.google.firebase.ktx.Firebase
import android.util.Log


// Import your Fragment classes
// import com.example.moodify.fragments.CheckinFragment // Secondary/Drawer section

// ... other fragments for drawer items


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView // Drawer
    private lateinit var bottomNavigationView: BottomNavigationView // Bottom Nav
    private lateinit var toolbar: MaterialToolbar
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Layout with DrawerLayout as root

        // Initialize Views
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)
        bottomNavigationView = findViewById(R.id.bottom_navigation) // Find BottomNav
        toolbar = findViewById(R.id.topAppBar)

        // Setup Toolbar
        setSupportActionBar(toolbar)

        // Setup Drawer Toggle
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        toggle.syncState()

        // --- Set Listeners ---
        navigationView.setNavigationItemSelectedListener(this) // For Drawer items
        setupBottomNavigationListener() // For BottomNav items

        // Load Initial Fragment (linked to BottomNav default)
        if (savedInstanceState == null) {
            // *** SELECT DEFAULT BOTTOM NAV ITEM ***
            bottomNavigationView.selectedItemId = R.id.navigation_log // e.g., Log is default
            // Manually load the initial fragment associated with the default bottom nav item
            loadFragment(LogFragment(), "Log Mood")
        }
    }

    // Listener Setup for Bottom Navigation
    private fun setupBottomNavigationListener() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            var title = getString(R.string.app_name)

            when (item.itemId) {
                // Use IDs from bottom_nav_menu.xml
                R.id.navigation_log -> {
                    selectedFragment = LogFragment()
                    title = "Log Mood"
                }
                R.id.navigation_history -> {
                    selectedFragment = InsightsFragment()
                    title = "Insights"
                }
                R.id.navigation_dashboard -> {
                    selectedFragment = DashboardFragment()
                    title = "Dashboard"
                }
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment, title)
                // Optional: If a drawer item corresponds to this, uncheck drawer items
                // navigationView.setCheckedItem(View.NO_ID) // Or specific ID if needed
                true // Indicate selection handled
            } else {
                false // Indicate selection not handled (shouldn't happen here)
            }
        }
    }


    // Listener Implementation for Drawer Navigation
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        var selectedFragment: Fragment? = null
        var title = getString(R.string.app_name)
        var shouldLoadFragment = true // Flag to decide if fragment needs loading

        when (item.itemId) {
            // Use IDs from drawer_menu.xml
            R.id.nav_checkin -> {
                // selectedFragment = CheckinFragment() // TODO: Create CheckinFragment
                title = "Check-in"
                selectedFragment = PlaceholderFragment.newInstance(title) // Use placeholder
            }

            R.id.nav_journal -> {
                // Example: Journal might be both in drawer and bottom nav
                // If clicked here, ensure bottom nav selects corresponding item
                title = "Journal"
                selectedFragment = JournalFragment()
                // Make sure R.id.bottom_nav_journal exists if linking
                // bottomNavigationView.selectedItemId = R.id.bottom_nav_journal // Link selection
            }

            R.id.nav_membership -> {
                // selectedFragment = MembershipFragment() // TODO: Create MembershipFragment
                title = "Membership"
                selectedFragment = PlaceholderFragment.newInstance(title) // Use placeholder
            }

            R.id.nav_notifications -> {
                // selectedFragment = NotificationsFragment() // TODO: Create NotificationsFragment
                title = "Notifications"
                selectedFragment = PlaceholderFragment.newInstance(title) // Use placeholder
            }

            R.id.nav_friends -> {
                // selectedFragment = FriendsFragment() // TODO: Create FriendsFragment
                title = "Friends"
                selectedFragment = PlaceholderFragment.newInstance(title) // Use placeholder
            }
            R.id.nav_logout -> {
                shouldLoadFragment = false // We are not loading a fragment, we are logging out
                Log.d("MainActivity", "Logout item clicked.")
                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut()
                // Navigate back to LoginActivity
                val intent = Intent(this, LoginActivity::class.java)
                // Clear activity stack so user cannot press back to get into MainActivity
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish() // Close MainActivity
            }
            else -> shouldLoadFragment = false

        }

        // Load the fragment only if needed and selected
        if (shouldLoadFragment && selectedFragment != null) {
            loadFragment(selectedFragment, title)
            // Optional: If you want drawer selection to clear bottom nav selection visually
            // bottomNavigationView.menu.findItem(bottomNavigationView.selectedItemId)?.isChecked = false
        }

        // Close the drawer
        drawerLayout.closeDrawer(GravityCompat.START)
        return true // Signify handled
    }

    // Helper Function to Load Fragments (remains the same)
    private fun loadFragment(fragment: Fragment, title: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
        supportActionBar?.title = title // Update toolbar title
    }

    // Handle Back Button (remains the same)
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // Sync Toggle State (remains the same)
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    // Placeholder Fragment (remains the same)
    class PlaceholderFragment : Fragment() {
        companion object {
            private const val ARG_TITLE = "fragment_title"
            fun newInstance(title: String): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putString(ARG_TITLE, title)
                fragment.arguments = args
                return fragment
            }
        }
        override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): android.view.View? {
            val view = TextView(activity).apply {
                gravity = android.view.Gravity.CENTER
                textSize = 24f
                text = arguments?.getString(ARG_TITLE) ?: "Placeholder Screen"
            }
            return view
        }
    }
    // --- END OF PLACEHOLDER ---

    // TODO: Create separate files for your actual Fragments
}