package com.example.dailybloom;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;
    private HomeFragment homeFragment;
    private ProfileFragment profileFragment;
    private Fragment activeFragment;
    private static final String KEY_SELECTED_FRAGMENT = "selected_fragment";
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();

        if (savedInstanceState == null) {
            homeFragment = new HomeFragment();
            profileFragment = new ProfileFragment();

            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, homeFragment, "home")
                    .add(R.id.fragment_container, profileFragment, "profile")
                    .hide(profileFragment)
                    .commit();

            activeFragment = homeFragment;
        } else {
            homeFragment = (HomeFragment) fragmentManager.findFragmentByTag("home");
            profileFragment = (ProfileFragment) fragmentManager.findFragmentByTag("profile");

            String selectedTag = savedInstanceState.getString(KEY_SELECTED_FRAGMENT, "home");

            if (selectedTag.equals("profile")) {
                activeFragment = profileFragment;
                bottomNavigationView.setSelectedItemId(R.id.nav_profile);
                fragmentManager.beginTransaction()
                        .hide(homeFragment)
                        .show(profileFragment)
                        .commitNow();
            } else {
                activeFragment = homeFragment;
                bottomNavigationView.setSelectedItemId(R.id.nav_home);
                fragmentManager.beginTransaction()
                        .hide(profileFragment)
                        .show(homeFragment)
                        .commitNow();
            }
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                fragmentManager.beginTransaction()
                        .hide(activeFragment)
                        .show(homeFragment)
                        .commit();
                activeFragment = homeFragment;
                return true;
            } else if (item.getItemId() == R.id.nav_profile) {
                fragmentManager.beginTransaction()
                        .hide(activeFragment)
                        .show(profileFragment)
                        .commit();
                activeFragment = profileFragment;
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (activeFragment == profileFragment) {
            outState.putString(KEY_SELECTED_FRAGMENT, "profile");
        } else {
            outState.putString(KEY_SELECTED_FRAGMENT, "home");
        }
    }
}