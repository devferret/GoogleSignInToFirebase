package com.example.mygooglesignintofirebase

import android.app.ProgressDialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.view.ViewCompat.animate
import android.util.Log.e
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Status
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import org.jetbrains.anko.find
import org.jetbrains.anko.sdk25.coroutines.onClick

class SignInActivity : AppCompatActivity() {

    private lateinit var mGoogleApiClient: GoogleApiClient

    private val RC_SIGN_IN = 1231
    private val TAG = "CHECK"

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mAuthListener: FirebaseAuth.AuthStateListener

    private lateinit var txtCenter: TextView
    private lateinit var btnGoogleSignIn: SignInButton
    private lateinit var btnGoogleSignOut: Button

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        bindView()
        googleConfig()
        firebaseConfig()
    }

    private fun bindView() {
        txtCenter = find(R.id.txtCenter)
        btnGoogleSignIn = find(R.id.btnGoogleSignIn)
        btnGoogleSignOut = find(R.id.btnGoogleSignout)

        btnGoogleSignIn.visibility = View.VISIBLE
        btnGoogleSignOut.visibility = View.GONE

        btnGoogleSignIn.onClick { signIn() }
        btnGoogleSignOut.onClick { signOut() }
    }

    private fun signIn() {
        var intent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(intent, RC_SIGN_IN)
    }

    private fun signOut() {
        FirebaseAuth.getInstance().signOut()
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback { status: Status ->
            btnGoogleSignIn.visibility = View.VISIBLE
            btnGoogleSignOut.visibility = View.GONE
            txtCenter.text = "Welcome!"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            var result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            e(TAG, "result: " + result.isSuccess)
            if (result.isSuccess) {
                // Sign In Successful
                var account = result.signInAccount
                firebaseAuthWithGoogle(account!!)
            }
        }
    }

    private fun googleConfig() {
        // Configure Google Sign In
        var gso: GoogleSignInOptions = GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, { connectionResult: ConnectionResult ->
                    //onFailed
                    e(TAG, "googleApiClient:build_failed: " + connectionResult.toString())
                }).addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()
    }

    private fun firebaseConfig() {
        // Config Fire base
        mAuth = FirebaseAuth.getInstance()
        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth: FirebaseAuth ->
            var user = firebaseAuth.currentUser

            if (user != null) {
                e(TAG, "onAuthStateChanged:sign_in: " + user.uid)

                btnGoogleSignIn.visibility = View.GONE
                btnGoogleSignOut.visibility = View.VISIBLE

                if (user.displayName != null) {
                    txtCenter.text = String.format(resources.getString(R.string.str_txtCenter_signedIn), user.displayName)
                }
            } else {
                e(TAG, "onAuthStateChanged:sign_out: " + user?.uid)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        mAuth.addAuthStateListener(mAuthListener)
    }

    override fun onStop() {
        super.onStop()
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener)
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        e(TAG, "firebaseAuthWithGoogle:account_id: " + account.id)

        progressDialog = ProgressDialog.show(this@SignInActivity, "", "Loading...", true)

        var credential = GoogleAuthProvider.getCredential(account.idToken, null)
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener { task: Task<AuthResult> ->
                    e(TAG, "signInWithCredential:on_complete: " + task.isSuccessful)
                    progressDialog.dismiss()
                    if (!task.isSuccessful) {
                        e(TAG, "signInWithCredential", task.exception)
                        Toast.makeText(this@SignInActivity, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                    }
                }
    }
}
