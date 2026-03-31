package edu.utap.demoproject_mrl.auth

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
import edu.utap.demoproject_mrl.R

class SignInFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnSignIn = view.findViewById<Button>(R.id.btnSignIn)
        val tvCreateAccount = view.findViewById<TextView>(R.id.tvCreateAccount)

        // If already signed in, go straight to Home
        if (auth.currentUser != null) {
            findNavController().navigate(R.id.action_signIn_to_home)
        }

        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    findNavController().navigate(R.id.action_signIn_to_home)
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Sign in failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        tvCreateAccount.setOnClickListener {
            findNavController().navigate(R.id.action_signIn_to_register)
        }
    }
}