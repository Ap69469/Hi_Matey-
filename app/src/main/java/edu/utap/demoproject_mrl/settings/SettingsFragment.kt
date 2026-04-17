package edu.utap.demoproject_mrl.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var pendingPartnershipId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db   = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        view.findViewById<TextView>(R.id.tvUserEmail).text =
            currentUser?.email ?: "Not logged in"

        // ── Dark mode ──────────────────────────────────────────────────────────
        val switchTheme = view.findViewById<SwitchCompat>(R.id.switchTheme)
        val prefs = requireContext().getSharedPreferences("himatey_prefs", Context.MODE_PRIVATE)
        switchTheme.isChecked = prefs.getBoolean("dark_mode", false)

        switchTheme.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                prefs.edit().putBoolean("dark_mode", true).apply()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                prefs.edit().putBoolean("dark_mode", false).apply()
            }
        }

        // ── Pending invite card ────────────────────────────────────────────────
        val layoutPending   = view.findViewById<LinearLayout>(R.id.layoutPendingInvite)
        val tvPendingText   = view.findViewById<TextView>(R.id.tvPendingInviteText)
        val btnAccept       = view.findViewById<Button>(R.id.btnAcceptInvite)
        val btnDecline      = view.findViewById<Button>(R.id.btnDeclineInvite)
        val tvPartnerStatus = view.findViewById<TextView>(R.id.tvPartnerStatus)
        val btnRemove       = view.findViewById<Button>(R.id.btnRemovePartner)

        checkForPendingInvite(layoutPending, tvPendingText, tvPartnerStatus, btnRemove)

        btnAccept.setOnClickListener {
            acceptInvite(layoutPending, tvPartnerStatus)
        }

        btnDecline.setOnClickListener {
            declineInvite(layoutPending, tvPartnerStatus)
        }

        // ── Send invite ────────────────────────────────────────────────────────
        val etPartnerEmail = view.findViewById<EditText>(R.id.etPartnerEmail)

        view.findViewById<Button>(R.id.btnInviteUser).setOnClickListener {
            val partnerEmail = etPartnerEmail.text.toString().trim()
            if (partnerEmail.isEmpty()) {
                Toast.makeText(requireContext(),
                    "Enter partner email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendPartnerInvite(partnerEmail, etPartnerEmail)
        }

        // ── Sign out / back ────────────────────────────────────────────────────
        view.findViewById<Button>(R.id.btnSignOut).setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_settings_to_signIn)
        }

        view.findViewById<Button>(R.id.btnBackSettings).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_home)
        }
    }

    // ── Check for pending invite ───────────────────────────────────────────────
    private fun checkForPendingInvite(
        layoutPending: LinearLayout,
        tvPendingText: TextView,
        tvPartnerStatus: TextView,
        btnRemove: Button
    ) {
        val currentEmail = auth.currentUser?.email ?: return

        db.collection("partnerships")
            .whereArrayContains("memberEmails", currentEmail)
            .whereEqualTo("status", "active")
            .get()
            .addOnSuccessListener { activeDocs ->
                if (!activeDocs.isEmpty) {
                    val doc          = activeDocs.documents[0]
                   // val partnerEmail = doc.get("partnerEmail") as? String ?: "partner"
                    val memberEmails = doc.get("memberEmails") as? List<*>
                    val partnerEmail = memberEmails
                        ?.filterIsInstance<String>()
                        ?.firstOrNull { it != currentEmail }
                        ?: "partner"
                    val activeId     = doc.id

                    tvPartnerStatus.text = "✅ Active partner: $partnerEmail"
                    tvPartnerStatus.setTextColor(
                        resources.getColor(android.R.color.holo_green_dark, null))

                    // Show Remove Partner button
                    btnRemove.visibility = View.VISIBLE
                    btnRemove.setOnClickListener {
                        db.collection("partnerships").document(activeId)
                            .delete()
                            .addOnSuccessListener {
                                btnRemove.visibility = View.GONE
                                tvPartnerStatus.text = "No active partner"
                                tvPartnerStatus.setTextColor(
                                    resources.getColor(android.R.color.darker_gray, null))
                                Toast.makeText(requireContext(),
                                    "Partner removed.", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(),
                                    "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    return@addOnSuccessListener
                }

                // No active — check for pending
                db.collection("partnerships")
                    .whereArrayContains("memberEmails", currentEmail)
                    .whereEqualTo("status", "pending")
                    .get()
                    .addOnSuccessListener { pendingDocs ->
                        if (pendingDocs.isEmpty) {
                            tvPartnerStatus.text = "No active partner"
                            return@addOnSuccessListener
                        }

                        val doc          = pendingDocs.documents[0]
                        pendingPartnershipId = doc.id
                        val memberEmails = doc.get("memberEmails") as? List<*>
                        val senderEmail  = memberEmails?.firstOrNull() as? String

                        if (senderEmail == currentEmail) {
                            tvPartnerStatus.text = "⏳ Invite sent — waiting for partner to accept"
                            tvPartnerStatus.setTextColor(
                                resources.getColor(android.R.color.holo_orange_dark, null))
                        } else {
                            tvPendingText.text = "📩 Partner invite from: $senderEmail"
                            layoutPending.visibility = View.VISIBLE
                            tvPartnerStatus.text = "📩 You have a pending invite"
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(),
                            "Error checking invites: ${e.message}",
                            Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(),
                    "Error loading partnership: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }


    private fun acceptInvite(layoutPending: LinearLayout, tvPartnerStatus: TextView) {
        val id = pendingPartnershipId ?: return

        db.collection("partnerships").document(id)
            .update("status", "active")
            .addOnSuccessListener {
                layoutPending.visibility = View.GONE
                tvPartnerStatus.text = "✅ Partnership accepted!"
                tvPartnerStatus.setTextColor(
                    resources.getColor(android.R.color.holo_green_dark, null))
                Toast.makeText(requireContext(),
                    "Partnership accepted! ✅", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(),
                    "Accept failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun declineInvite(layoutPending: LinearLayout, tvPartnerStatus: TextView) {
        val id = pendingPartnershipId ?: return

        db.collection("partnerships").document(id)
            .delete()
            .addOnSuccessListener {
                layoutPending.visibility = View.GONE
                pendingPartnershipId = null
                tvPartnerStatus.text = "No active partner"
                tvPartnerStatus.setTextColor(
                    resources.getColor(android.R.color.darker_gray, null))
                Toast.makeText(requireContext(),
                    "Invite declined.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(),
                    "Decline failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun sendPartnerInvite(partnerEmail: String, etPartnerEmail: EditText) {
        val currentUser  = auth.currentUser ?: return
        val currentEmail = currentUser.email ?: return

        db.collection("partnerships")
            .whereArrayContains("memberEmails", currentEmail)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val status = docs.documents[0].getString("status")
                    if (status == "active") {
                        Toast.makeText(requireContext(),
                            "You already have an active partner!",
                            Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(),
                            "You already sent an invite — waiting for acceptance.",
                            Toast.LENGTH_SHORT).show()
                    }
                    return@addOnSuccessListener
                }

                db.collection("users")
                    .whereEqualTo("email", partnerEmail)
                    .get()
                    .addOnSuccessListener { userDocs ->
                        if (userDocs.isEmpty) {
                            Toast.makeText(requireContext(),
                                "No account found for $partnerEmail. " +
                                        "Make sure they have registered first.",
                                Toast.LENGTH_LONG).show()
                            return@addOnSuccessListener
                        }

                        val partnerUid = userDocs.documents[0].id

                        val partnership = hashMapOf(
                            "memberEmails" to listOf(currentEmail, partnerEmail),
                            "members"      to listOf(currentUser.uid, partnerUid),
                            "partnerEmail" to partnerEmail,
                            "status"       to "pending",
                            "createdAt"    to System.currentTimeMillis()
                        )

                        db.collection("partnerships")
                            .add(partnership)
                            .addOnSuccessListener {
                                etPartnerEmail.text.clear()
                                Toast.makeText(requireContext(),
                                    "Invite sent! ✅ Waiting for $partnerEmail to accept.",
                                    Toast.LENGTH_LONG).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(),
                                    "Failed to send invite: ${e.message}",
                                    Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(),
                            "User lookup failed: ${e.message}",
                            Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(),
                    "Invite failed: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }
}