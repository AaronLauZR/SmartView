package my.edu.tarc.smartview.ui.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import my.edu.tarc.smartview.R
import my.edu.tarc.smartview.databinding.FragmentSettingsBinding
import my.edu.tarc.smartview.ui.MainActivity

class SettingsFragment : Fragment() {

    //Initialize NavController
    private lateinit var navController: NavController

    //Initialize Binding
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    //Initialize SharedPreference
    val MY_PREF = "MY_PREF"

    //Preferences
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        //SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences(MY_PREF, Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nightMode = sharedPreferences.getString("night", "") //Light mode is the default mode

        if (nightMode.equals("true")) {
            binding.switchTheme.isChecked = true
        }

        binding.switchTheme.setOnClickListener {
            if (nightMode == "true") {
                // Perform time-consuming operation here
                editor.putString("night", "")
                editor.apply()
                binding.switchTheme.isEnabled = false
            } else {
                // Perform time-consuming operation here
                editor.putString("night", "true")
                editor.apply()
                binding.switchTheme.isEnabled = false
            }
            checkDarkLightTheme()
        }
    }

    private fun checkDarkLightTheme() {
        val nightMode = sharedPreferences.getString("night", "") //Light mode is the default mode

        lifecycleScope.launch {
            delay(500)

            if (nightMode.equals("true")) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }else{
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

}