package edu.utap.demoproject_mrl.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.utap.demoproject_mrl.R

class SettingsFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        view.findViewById<TextView>(R.id.tvUserEmail).text =
            currentUser?.email ?: "Not logged in"

        // ✅ Correct IDs matching XML
        val etPartnerEmail = view.findViewById<EditText>(R.id.etPartnerEmail)

        view.findViewById<Button>(R.id.btnInviteUser).setOnClickListener {
            val partnerEmail = etPartnerEmail.text.toString().trim()
            if (partnerEmail.isEmpty()) {
                Toast.makeText(requireContext(),
                    "Enter partner email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendPartnerInvite(partnerEmail)
        }

        view.findViewById<Button>(R.id.btnSignOut).setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_settings_to_signIn)
        }

        view.findViewById<Button>(R.id.btnBackSettings).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_home)
        }
    }

    private fun sendPartnerInvite(partnerEmail: String) {
        val currentUser = auth.currentUser ?: return
        val currentEmail = currentUser.email ?: return

        db.collection("partnerships")
            .whereArrayContains("memberEmails", currentEmail)
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    Toast.makeText(requireContext(),
                        "You already have an active partner!",
                        Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                db.collection("users")
                    .whereEqualTo("email", partnerEmail)
                    .get()
                    .addOnSuccessListener { userDocs ->
                        if (userDocs.isEmpty) {
                            Toast.makeText(requireContext(),
                                "User not found!", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        val partnerUid = userDocs.documents[0].id

                        val partnership = hashMapOf(
                            "memberEmails" to listOf(currentEmail, partnerEmail),
                            "members" to listOf(currentUser.uid, partnerUid),
                            "partnerEmail" to partnerEmail,
                            "status" to "active",
                            "createdAt" to System.currentTimeMillis()
                        )

                        db.collection("partnerships")
                            .add(partnership)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(),
                                    "Partner invited! ✅",
                                    Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(),
                                    "Failed: ${e.message}",
                                    Toast.LENGTH_SHORT).show()
                            }
                    }
            }
    }
}