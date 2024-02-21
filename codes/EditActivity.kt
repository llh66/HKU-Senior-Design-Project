package hk.hkuce.sdp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.widget.*
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class EditActivity : AppCompatActivity() {
    private var items: MutableList<String> = mutableListOf()
    private var keys: MutableList<String> = mutableListOf()
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
    private var delete: Button? = null
    private lateinit var detailsEditText: TextInputEditText

    private var isStartingTime = false

    private var lat: Double? = null
    private var lng: Double? = null

    private lateinit var database: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth

    private var records: MutableList<HashMap<String,Any>> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        supportActionBar!!.title = "Change stand information"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        setDate = findViewById(R.id.setDate)
        setStartingTime = findViewById(R.id.setStartingTime)
        setEndingTime = findViewById(R.id.setEndingTime)
        dateEditText = findViewById(R.id.dateEditText)
        startingTimeEditText = findViewById(R.id.startingTimeEditText)
        endingTimeEditText = findViewById(R.id.endingTimeEditText)
        cancel = findViewById(R.id.cancel)
        confirm = findViewById(R.id.confirm)
        delete = findViewById(R.id.delete)
        detailsEditText = findViewById(R.id.detailsEditText)

        var cal = Calendar.getInstance()

        firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser == null){
            Toast.makeText(
                this@EditActivity,
                "Authentication error!",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        }

        database = Firebase.database.reference
        database.child("Locations").orderByChild("username")
            .equalTo(firebaseAuth.currentUser!!.email.toString()).get().addOnSuccessListener(){
            for (record in it.children){
                var currentTime = System.currentTimeMillis()
                var endTime = SimpleDateFormat("dd/MM/yyyy-HH:mm")
                    .parse(record.child("date").value.toString()+"-"+
                            record.child("endingTime").value.toString())
                if (endTime.time < currentTime){
                    var data = HashMap<String, Any>()
                    data[record.key.toString()] = record.value as Any
                    database.child("Expired").setValue(data).addOnCompleteListener{
                        database.child("Locations").child(record.key.toString()).removeValue()
                    }
                }else{
                    var data = HashMap<String, Any>()
                    data["date"] = record.child("date").value.toString()
                    data["startingTime"] = record.child("startingTime").value.toString()
                    data["endingTime"] = record.child("endingTime").value.toString()
                    data["details"] = record.child("details").value.toString()
                    records.add(data)
                    keys.add(record.key.toString())
                    items.add(data["date"].toString()+" "+data["startingTime"].toString()
                            +"-"+data["endingTime"].toString())
                }
            }
//            Toast.makeText(this@EditActivity, records.toString(), Toast.LENGTH_SHORT).show()
        }.addOnFailureListener(){
            Toast.makeText(this@EditActivity, it.message, Toast.LENGTH_SHORT).show()
        }

        confirm!!.setOnClickListener {
            Toast.makeText(this@EditActivity, "Please choose a record first!", Toast.LENGTH_SHORT).show()
        }
        delete!!.setOnClickListener {
            Toast.makeText(this@EditActivity, "Please choose a record first!", Toast.LENGTH_SHORT).show()
        }

        val adapter = ArrayAdapter(this,R.layout.list_item,items)

        autoCompleteTextView = findViewById(R.id.autoCompleteTextView)
        autoCompleteTextView.setAdapter(adapter)
        autoCompleteTextView.onItemClickListener = AdapterView.OnItemClickListener{
                adapterView, view, i, l ->
            itemSelected = adapterView.getItemAtPosition(i).toString()
            dateEditText.setText(records[i]["date"].toString())
            startingTimeEditText.setText(records[i]["startingTime"].toString())
            endingTimeEditText.setText(records[i]["endingTime"].toString())
            detailsEditText.setText(records[i]["details"].toString())
            confirm!!.setOnClickListener {
                firebaseAuth = FirebaseAuth.getInstance()
                if (firebaseAuth.currentUser == null){
                    Toast.makeText(
                        this@EditActivity,
                        "Authentication error!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else if (itemSelected == null){
                    Toast.makeText(
                        this@EditActivity,
                        "Please choose a record!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else if (dateEditText.text.isEmpty()
                    || startingTimeEditText.text.isEmpty() || endingTimeEditText.text.isEmpty()
                    || detailsEditText.text?.isEmpty() == true){
                    Toast.makeText(
                        this@EditActivity,
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
                            this@EditActivity,
                            "Ending time should be after starting time!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else if (endTime.time < currentTime){
                        Toast.makeText(
                            this@EditActivity,
                            "Ending time should be at least one minute after current time!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else{
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Confirm")
                        builder.setMessage("Are you sure to upload?")

                        builder.setPositiveButton("Yes") { dialog, which ->
                            database.child("Locations").child(keys[i])
                                .child("date").setValue(dateEditText.text.toString())
                                .addOnFailureListener {
                                    Toast.makeText(this@EditActivity, it.message, Toast.LENGTH_SHORT).show()
                                }
                            database.child("Locations").child(keys[i])
                                .child("startingTime").setValue(startingTimeEditText.text.toString())
                                .addOnFailureListener {
                                    Toast.makeText(this@EditActivity, it.message, Toast.LENGTH_SHORT).show()
                                }
                            database.child("Locations").child(keys[i])
                                .child("endingTime").setValue(endingTimeEditText.text.toString())
                                .addOnFailureListener {
                                    Toast.makeText(this@EditActivity, it.message, Toast.LENGTH_SHORT).show()
                                }
                            database.child("Locations").child(keys[i])
                                .child("details").setValue(detailsEditText.text.toString())
                                .addOnFailureListener {
                                    Toast.makeText(this@EditActivity, it.message, Toast.LENGTH_SHORT).show()
                                }
                            finish()
                        }
                        builder.setNegativeButton("No") { dialog, _ -> dialog.cancel() }
                        builder.show()
                    }
                }
            }
            delete!!.setOnClickListener {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Confirm")
                builder.setMessage("Are you sure to upload?")
                builder.setPositiveButton("Yes"){ _, _ ->
                    database.child("Locations").child(keys[i]).removeValue().addOnCompleteListener{
                        Toast.makeText(this@EditActivity, "Deleted successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(this@EditActivity, it.message, Toast.LENGTH_SHORT).show()
                    }
                }
                builder.setNegativeButton("No") { dialog, _ -> dialog.cancel() }
                builder.show()
            }
        }

        val dateSetListener = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, monthOfYear)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            dateEditText.setText(SimpleDateFormat("dd/MM/yyyy").format(cal.time))
        }

        setDate.setOnClickListener {
            cal = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(this@EditActivity, dateSetListener,
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
            val timePickerDialog = TimePickerDialog(this@EditActivity, timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
            timePickerDialog.show()
        }
        setEndingTime.setOnClickListener {
            cal = Calendar.getInstance()
            isStartingTime = false
            val timePickerDialog = TimePickerDialog(this@EditActivity, timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
            timePickerDialog.show()
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