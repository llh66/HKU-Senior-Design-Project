package hk.hkuce.sdp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*


class ReportActivity2 : AppCompatActivity() {
    private var items: List<String> = listOf("Dessert","Snack","Beverage","Others")
    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private var itemSelected: String? = null

    private lateinit var setDate: Button
    private lateinit var setStartingTime: Button
    private lateinit var setEndingTime: Button
    private lateinit var dateEditText: EditText
    private lateinit var startingTimeEditText: EditText
    private lateinit var endingTimeEditText: EditText
    private var cancel: Button? = null
    private var confirm: Button? = null
    private lateinit var detailsEditText: TextInputEditText

    private var isStartingTime = false

    private var lat: Double? = null
    private var lng: Double? = null

    private lateinit var database: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report2)

        supportActionBar!!.title = "Other information"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        lat = intent.getDoubleExtra("Lat", 0.0)
        lng = intent.getDoubleExtra("Lng", 0.0)

        val adapter = ArrayAdapter(this,R.layout.list_item,items)

        autoCompleteTextView = findViewById(R.id.autoCompleteTextView)
        autoCompleteTextView.setAdapter(adapter)
        autoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener{
                adapterView, view, i, l ->
            itemSelected = adapterView.getItemAtPosition(i).toString()
        }

        setDate = findViewById(R.id.setDate)
        setStartingTime = findViewById(R.id.setStartingTime)
        setEndingTime = findViewById(R.id.setEndingTime)
        dateEditText = findViewById(R.id.dateEditText)
        startingTimeEditText = findViewById(R.id.startingTimeEditText)
        endingTimeEditText = findViewById(R.id.endingTimeEditText)
        cancel = findViewById(R.id.cancel)
        confirm = findViewById(R.id.confirm)
        detailsEditText = findViewById(R.id.detailsEditText)

        var cal = Calendar.getInstance()

        val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthOfYear)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            dateEditText.setText(SimpleDateFormat("dd/MM/yyyy").format(cal.time))
        }

        setDate.setOnClickListener {
            cal = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(this@ReportActivity2, dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH))
            datePickerDialog.datePicker.minDate = cal.timeInMillis
            datePickerDialog.show()
        }

        val timeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            if(isStartingTime){
                startingTimeEditText.setText(SimpleDateFormat("HH:mm").format(cal.time))
            }
            else{
                endingTimeEditText.setText(SimpleDateFormat("HH:mm").format(cal.time))
            }
        }

        setStartingTime.setOnClickListener {
            cal = Calendar.getInstance()
            isStartingTime = true
            val timePickerDialog = TimePickerDialog(this@ReportActivity2, timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
            timePickerDialog.show()
        }
        setEndingTime.setOnClickListener {
            isStartingTime = false
            val timePickerDialog = TimePickerDialog(this@ReportActivity2, timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
            timePickerDialog.show()
        }

        confirm!!.setOnClickListener {
            firebaseAuth = FirebaseAuth.getInstance()
            if (firebaseAuth.currentUser == null){
                Toast.makeText(
                    this@ReportActivity2,
                    "Authentication error!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else if (itemSelected == null || dateEditText.text.isEmpty()
                || startingTimeEditText.text.isEmpty() || endingTimeEditText.text.isEmpty()
                || detailsEditText.text?.isEmpty() == true){
                Toast.makeText(
                    this@ReportActivity2,
                    "Please fill all fields!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else {
                var currentTime = System.currentTimeMillis()
                var startTime = SimpleDateFormat("dd/MM/yyyy-HH:mm")
                    .parse(dateEditText.text.toString()+"-"+
                            startingTimeEditText.text.toString())
                var endTime = SimpleDateFormat("dd/MM/yyyy-HH:mm")
                    .parse(dateEditText.text.toString()+"-"+
                            endingTimeEditText.text.toString())
                if (endTime.before(startTime)){
                    Toast.makeText(
                        this@ReportActivity2,
                        "Ending time should be after starting time!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else if (endTime.time < currentTime){
                    Toast.makeText(
                        this@ReportActivity2,
                        "Ending time should be at least one minute after current time!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else{
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Confirm")
                    builder.setMessage("Are you sure to upload?")

                    builder.setPositiveButton("Yes") { dialog, which ->
                        val data = HashMap<String, Any>()
                        data["username"] = firebaseAuth.currentUser!!.email.toString()
                        data["Lat"] = lat!!
                        data["Lng"] = lng!!
                        data["type"] = itemSelected!!
                        data["date"] = dateEditText.text.toString()
                        data["startingTime"] = startingTimeEditText.text.toString()
                        data["endingTime"] = endingTimeEditText.text.toString()
                        data["details"] = detailsEditText.text.toString()
                        database = Firebase.database.reference
                        val input = database.child("Locations").push()
                        input.setValue(data).addOnCompleteListener {
                            Toast.makeText(
                                this@ReportActivity2,
                                "Reported successfully, thank you!",
                                Toast.LENGTH_SHORT
                            ).show()
                            val local = Intent()
                            local.action = "FINISH"
                            sendBroadcast(local)
                            finish()
                        }.addOnFailureListener {
                            Toast.makeText(this@ReportActivity2,
                                it.message,Toast.LENGTH_SHORT).show()
                        }
                    }
                    builder.setNegativeButton("No") { dialog, _ -> dialog.cancel() }
                    builder.show()
                }
            }
        }

        cancel!!.setOnClickListener {
            onBackPressed()
        }

    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}