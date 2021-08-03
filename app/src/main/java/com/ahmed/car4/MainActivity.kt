package com.ahmed.car4

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import android.widget.Toast.makeText
import com.ahmed.car4.Common.Common
import com.ahmed.car4.Model.RiderInfoModel
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var providers:     List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener:      FirebaseAuth.AuthStateListener
    private lateinit var database: FirebaseDatabase
    private lateinit var riderInfoRef: DatabaseReference
    private  lateinit var progress_bar:ProgressBar

    companion object {
        private const val LOGIN_REQUEST_CODE = 7171
    }
    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }
    override fun onStop() {
        firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }
    private fun delaySplashScreen() {
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread()).subscribe {
            firebaseAuth.addAuthStateListener(listener)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init() {
        database = FirebaseDatabase.getInstance()
        riderInfoRef = database.getReference(Common.RIDER_INFO_REFERENCE)

        providers = listOf(
            AuthUI.IdpConfig.PhoneBuilder().build(),

            )

        firebaseAuth= FirebaseAuth.getInstance()
        listener= FirebaseAuth.AuthStateListener {
                myFirebaseAuth-> val user = myFirebaseAuth.currentUser
            if (user!= null){
                checkUserFromFirebase()
            }
            else{
                showLoginLayout()
            }
        }
    }

    private fun checkUserFromFirebase(){
        riderInfoRef
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object: ValueEventListener{

                override fun onCancelled(p0: DatabaseError) {
                    makeText(this@MainActivity,p0.message,Toast.LENGTH_SHORT).show()
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()){

                        //makeText(this@SplashScreenActivity,"User Already Exist",Toast.LENGTH_SHORT).show()
                        val  model= dataSnapshot.getValue(RiderInfoModel::class.java)
                        gotoHomeActivity(model)
                    }
                    else{
                        showRegisterlayout()
                    }
                }

            })
    }
    private fun gotoHomeActivity(model: RiderInfoModel?) {

        Common.currentRider = model
        startActivity(Intent(this,RiderHomeActivity::class.java))
        finish()
    }

    private fun showRegisterlayout() {


        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register,null)

        val edt_first_name =  itemView.findViewById<View>(R.id.edt_first_name)as TextInputEditText
        val edt_last_name =  itemView.findViewById<View>(R.id.edt_last_name)as TextInputEditText
        val edt_phone_number =  itemView.findViewById<View>(R.id.edt_phone_number)as TextInputEditText

        val  btn_continue =  itemView.findViewById<View>(R.id.btn_register)as Button

        //Set Data
        if (FirebaseAuth.getInstance().currentUser!!.phoneNumber != null &&
            !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber))
        {
            edt_phone_number.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)
        }
        // View
        builder.setView(itemView)
        val dialog= builder.create()
        dialog.show()

        //Event
        btn_continue.setOnClickListener {
            when {
                TextUtils.isDigitsOnly(edt_first_name.text.toString()) -> {

                    Toast.makeText(this@MainActivity, "Enter First Name", Toast.LENGTH_SHORT).show()
                }
                TextUtils.isDigitsOnly(edt_last_name.text.toString()) -> {

                    Toast.makeText(this@MainActivity, "Enter last Name", Toast.LENGTH_SHORT).show()
                }
                TextUtils.isDigitsOnly(edt_last_name.text.toString()) -> {

                    Toast.makeText(this@MainActivity, "Enter Phone Number", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    val model = RiderInfoModel()
                    model.firstName= edt_first_name.text.toString()
                    model.lastName= edt_last_name.text.toString()
                    model.phoneNumber= edt_phone_number.text.toString()


                    riderInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                        .setValue(model)
                        .addOnFailureListener { e->
                            Toast.makeText(this@MainActivity, "" + e.message, Toast.LENGTH_SHORT).show()
                            dialog.dismiss()

                            progress_bar.visibility = View.GONE

                        }
                        .addOnSuccessListener {
                            Toast.makeText(this@MainActivity,
                                "Registered Successfully",
                                Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            gotoHomeActivity(model)
                            progress_bar.visibility = View.GONE


                        }
                }
            }

        }

    }
    private fun showLoginLayout() {
        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.btn_phone_sign_in)

            .build()

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build(), LOGIN_REQUEST_CODE
        )
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode== LOGIN_REQUEST_CODE){
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode== Activity.RESULT_OK){
                val user = FirebaseAuth.getInstance().currentUser
            }
            else{
                Toast.makeText(this@MainActivity,
                    "" + response!!.error!!.message,
                    Toast.LENGTH_SHORT).show()
            }
        }
    }



}