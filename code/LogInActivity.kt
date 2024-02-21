package hk.hkuce.sdp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LogInActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var signIn: Button
    private lateinit var cancel: Button
    private lateinit var signUp: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        supportActionBar!!.title = "Log In"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        signIn = findViewById(R.id.signIn)
        cancel = findViewById(R.id.cancel)
        signUp = findViewById(R.id.signUp)
        firebaseAuth = FirebaseAuth.getInstance()

        signUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
        signIn.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty()){
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener{
                    if(it.isSuccessful){
                        Toast.makeText(this, "Log-in succeeded!", Toast.LENGTH_SHORT).show()
                        onBackPressed()
                    }
                    else{
                        Toast.makeText(this, it.exception!!.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else{
                Toast.makeText(this, "Please enter email and password!", Toast.LENGTH_SHORT).show()
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