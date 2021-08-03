package com.ahmed.car4


import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.ahmed.car4.Common.Common

import com.ahmed.car4.databinding.ActivityRiderHomeBinding
import com.ahmed.wts.Utils.UserUtils
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class RiderHomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityRiderHomeBinding

    private lateinit var navView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var  navController: NavController
    private lateinit var img_avatar: ImageView
    private lateinit var waitingDialog: AlertDialog
    private lateinit var storageReference: StorageReference
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRiderHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarRiderHome.toolbar)


        drawerLayout= binding.drawerLayout
        navView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_rider_home)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        init()
    }
    private fun init(){
        storageReference = FirebaseStorage.getInstance().getReference()
        waitingDialog = AlertDialog.Builder(this)
            .setMessage("Waiting...")
            .setCancelable(false).create()

        navView.setNavigationItemSelectedListener {
            if (it.itemId == R.id.nav_sign_out)
            {
                val  builder = android.app.AlertDialog.Builder(this@RiderHomeActivity)
                builder.setTitle("Sign Out")
                    .setMessage("Do you really want to Sign Out?")
                    .setNegativeButton("Cancel",
                        {dialogInterface,_ -> dialogInterface.dismiss()})

                    .setPositiveButton("'Sign Out") { dialogInterface, _ ->
                        FirebaseAuth.getInstance().signOut()


                        val intent =
                            Intent(this@RiderHomeActivity, MainActivity::class.java)
                        intent.setFlags((Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                        startActivity(intent)
                        finish()

                    }.setCancelable(false)
                val dialog = builder.create()
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(ContextCompat.getColor(this@RiderHomeActivity,android.R.color.holo_red_dark))
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(ContextCompat.getColor(this@RiderHomeActivity,android.R.color.black))
                }
                dialog.show()

            }

            true
        }
        val headerView= navView.getHeaderView(0)
        val txt_name = headerView.findViewById<View>(R.id.txt_name) as TextView
        val txt_phone = headerView.findViewById<View>(R.id.txt_phone) as TextView

        img_avatar= headerView.findViewById<View>(R.id.image_avatar) as ImageView
        txt_name.setText(Common.BuildWelcomeMessage())
        txt_phone.setText(Common.currentRider!!.phoneNumber)


        if (Common.currentRider !=null
            && Common.currentRider !!. avatar != null
            && !TextUtils.isEmpty(Common.currentRider!!.avatar))
        {
            Glide.with(this)
                .load(Common.currentRider!!.avatar)
                .into(img_avatar)

        }
        img_avatar.setOnClickListener {
            val intent= Intent()
            intent.setType("image/*")
            intent.setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(intent,"Select Picture"),PICK_IMAGE_REQUEST)
        }



    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode== PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK){

            if (data != null && data.data != null)
            {
                imageUri = data.data
                img_avatar.setImageURI(imageUri)
            }

            showDialogUpload()
        }
    }
    private fun showDialogUpload() {
        val  builder = android.app.AlertDialog.Builder(this@RiderHomeActivity)
        builder.setTitle("Change Avatar")
            .setMessage("Do you really want to Change Image?")
            .setNegativeButton("Cancel",
                {dialogInterface,_ ->dialogInterface.dismiss()})

            .setPositiveButton("Change") { dialogInterface, _ ->
                if (imageUri != null){
                    waitingDialog.show()
                    val avatarFolder = storageReference.child("avatars/"+FirebaseAuth.getInstance().currentUser!!.uid)

                    avatarFolder.putFile(imageUri!!)
                        .addOnFailureListener{e ->
                            Snackbar.make(drawerLayout, e.message!!, Snackbar.LENGTH_LONG).show()
                            waitingDialog.dismiss()
                        }.addOnCompleteListener {  task ->
                            if (task.isSuccessful){
                                avatarFolder.downloadUrl.addOnSuccessListener { uri ->
                                    val update_data= HashMap<String,Any>()
                                    update_data.put("avatar",uri.toString())
                                    UserUtils.UpdateUser(drawerLayout,update_data)

                                }
                            }
                            waitingDialog.dismiss()
                        }.addOnProgressListener { taskSnapshot->
                            val progress = (100.0*taskSnapshot.bytesTransferred/taskSnapshot.totalByteCount)
                            waitingDialog.setMessage(StringBuilder("Uploading: ") .append(progress).append("%"))

                        }

                }


            }.setCancelable(false)
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this@RiderHomeActivity,android.R.color.holo_red_dark))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this@RiderHomeActivity,android.R.color.black))
        }
        dialog.show()
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.rider_home, menu)
        return true
    }
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_rider_home)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    companion object{
        val PICK_IMAGE_REQUEST= 7272
    }
}