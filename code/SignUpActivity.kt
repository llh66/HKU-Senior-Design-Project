package hk.hkuce.sdp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var passwordEditText2: TextInputEditText
    private lateinit var cancel: Button
    private lateinit var signUp: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        supportActionBar!!.title = "Sign up"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        passwordEditText2 = findViewById(R.id.passwordEditText2)
        cancel = findViewById(R.id.cancel)
        signUp = findViewById(R.id.signUp)
        firebaseAuth = FirebaseAuth.getInstance()

        signUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
        signUp.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val password2 = passwordEditText2.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty() && password2.isNotEmpty()){
                if (password == password2){
                    firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(this, "Sign-up succeeded!", Toast.LENGTH_SHORT).show()
                            onBackPressed()
                        }
                        else{
                            Toast.makeText(this, it.exception!!.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                else{
                    Toast.makeText(this, "The passwords are different!", Toast.LENGTH_SHORT).show()
                }
            }
            else{
                Toast.makeText(this, "Please fill-in all fields!", Toast.LENGTH_SHORT).show()
            }
        }
        cancel.setOnClickListener {
            onBackPressed()
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}