package edu.utap.demoproject_mrl.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.demoproject_mrl.R

class SettingsFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser

        // Show user email
        view.findViewById<TextView>(R.id.tvUserEmail).text =
            currentUser?.email ?: "user@example.com"

        // Check partner status
        currentUser?.uid?.let { uid ->
            db.collection("partnerships")
                .whereArrayContains("members", uid)
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener { docs ->
                    if (!docs.isEmpty) {
                        val partnerEmail = docs.documents[0].getString("partnerEmail") ?: "Partner"
                        view.findViewById<TextView>(R.id.tvPartnerStatus).text =
                            "✅ Active partner: $partnerEmail"
                    }
                }
        }

        // Theme toggle
        val switchTheme = view.findViewById<SwitchCompat>(R.id.switchTheme)
        switchTheme.isChecked =
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Invite partner
        val etPartnerEmail = view.findViewById<EditText>(R.id.etPartnerEmail)
        view.findViewById<Button>(R.id.btnInviteUser).setOnClickListener {
            val partnerEmail = etPartnerEmail.text.toString().trim()
            if (partnerEmail.isEmpty()) {
                Toast.makeText(requireContext(), "Enter partner's email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendPartnerInvite(partnerEmail, currentUser?.uid ?: "", currentUser?.email ?: "")
        }

        // Sign out
        view.findViewById<Button>(R.id.btnSignOut).setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_settings_to_signIn)
        }

        // Back
        view.findViewById<Button>(R.id.btnBackSettings).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_home)
        }
    }

    private fun sendPartnerInvite(
        partnerEmail: String,
        currentUid: String,
        currentEmail: String
    ) {
        // Find partner's UID by email
        db.collection("users")
            .whereEqualTo("email", partnerEmail)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    Toast.makeText(requireContext(),
                        "User not found. Make sure they have an account!",
                        Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val partnerUid = docs.documents[0].id

                // Create partnership document
                val partnership = hashMapOf(
                    "members" to listOf(currentUid, partnerUid),
                    "memberEmails" to listOf(currentEmail, partnerEmail),
                    "partnerEmail" to partnerEmail,
                    "status" to "active",
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("partnerships")
                    .add(partnership)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(),
                            "Partnership created with $partnerEmail! 🎉",
                            Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(),
                            "Failed: ${it.message}",
                            Toast.LENGTH_SHORT).show()
                    }
            }
    }
}