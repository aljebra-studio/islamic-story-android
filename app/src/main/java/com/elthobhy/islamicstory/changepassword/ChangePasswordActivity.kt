package com.elthobhy.islamicstory.changepassword

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.elthobhy.islamicstory.core.utils.dialogError
import com.elthobhy.islamicstory.core.utils.dialogLoading
import com.elthobhy.islamicstory.core.utils.dialogSuccess
import com.elthobhy.islamicstory.core.utils.vo.Status
import com.elthobhy.islamicstory.databinding.ActivityChangePasswordBinding
import com.elthobhy.islamicstory.user.UserActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.*
import org.koin.android.ext.android.inject

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private var currentUser: FirebaseUser? = null
    private val changePasswordViewModel by inject<ChangePasswordViewModel>()
    private lateinit var dialogLoading: AlertDialog
    private lateinit var mAdView: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dialogLoading = dialogLoading(this)
        currentUser = FirebaseAuth.getInstance().currentUser
        onClick()

        MobileAds.initialize(this) {}

        mAdView = binding.adView
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)
    }

    private fun onClick() {
        binding.apply {
            btnChangePassword.setOnClickListener {
                val oldPass: String = etOldPassword.text.toString().trim()
                val newPass = etNewPassword.text.toString().trim()
                val confirmNewPass = etConfirmNewPassword.text.toString().trim()
                if(validationCheck(newPass, confirmNewPass)){
                    val user = GoogleSignIn.getLastSignedInAccount(this@ChangePasswordActivity)
                    if(oldPass.isEmpty() && user?.idToken?.isNotEmpty() == true){
                        val credential = GoogleAuthProvider.getCredential(user.idToken, null)
                        changePass(newPass, credential)
                    }else{
                        val credential = EmailAuthProvider.getCredential(currentUser?.email.toString(), oldPass)
                        changePass(newPass, credential)
                    }
                }
            }
            btnCloseChangePassword.setOnClickListener {
                finish()
            }
        }
    }

    private fun changePass(newPass: String, credential: AuthCredential) {
            changePasswordViewModel.changePassword(newPass, credential).observe(this){
                when (it.status) {
                    Status.LOADING -> {
                        dialogLoading.show()
                    }
                    Status.SUCCESS -> {
                        dialogLoading.dismiss()
                        dialogSuccess(this).show()
                        startActivity(
                            Intent(
                                this@ChangePasswordActivity,
                                UserActivity::class.java
                            )
                        )
                        finishAffinity()
                    }
                    Status.ERROR -> {
                        dialogLoading.dismiss()
                        dialogError(it.message,this).show()
                    }
                }
        }
    }

    private fun validationCheck(newPass: String, confirmNewPass: String): Boolean {
        binding.apply {
            when{
                newPass.isEmpty()->{
                    etNewPassword.error = "Please Field Your New Password"
                    etNewPassword.requestFocus()
                }
                confirmNewPass.isEmpty()->{
                    etConfirmNewPassword.error = "Please Field Confirm New Password"
                    etConfirmNewPassword.requestFocus()
                }
                newPass.length < 8 ->{
                    etNewPassword.error = "Minimum password is 8 character"
                    etNewPassword.requestFocus()
                }
                confirmNewPass.length < 8 ->{
                    etConfirmNewPassword.error = "Minimum password is 8 character"
                    etConfirmNewPassword.requestFocus()
                }
                newPass != confirmNewPass ->{
                    etNewPassword.error = "confirm password didn't match"
                    etNewPassword.requestFocus()
                    etConfirmNewPassword.error = "confirm password didn't match"
                    etConfirmNewPassword.requestFocus()
                }
                else->{
                    return true
                }
            }
        }
        return false
    }
}