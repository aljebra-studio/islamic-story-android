package com.elthobhy.islamicstory.core.data.remote

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import com.elthobhy.islamicstory.core.data.remote.response.ListResponseItem
import com.elthobhy.islamicstory.core.data.remote.vo.ApiResponse
import com.elthobhy.islamicstory.core.domain.model.ListDomain
import com.elthobhy.islamicstory.core.domain.model.User
import com.elthobhy.islamicstory.core.utils.vo.Resource
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RemoteDataSource(
    private val firebaseAuth: FirebaseAuth,
    private val userDatabase: DatabaseReference,
    private val nabiDatabase: DatabaseReference,
    private val storageReference: StorageReference
) {
    fun register(name: String, email: String, password: String): LiveData<Resource<AuthResult>> {
        val auth = MutableLiveData<Resource<AuthResult>>()
        auth.postValue(Resource.loading())
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = firebaseAuth.uid
                    val imageUser = "https://ui-avatars.com/api/?background=218B5E&color=fff&size=100&rounded=true&name=$name"
                    val user = User(
                        nameUser = name,
                        avatarUser = imageUser,
                        emailUser = email,
                        uidUser = uid
                    )
                    userDatabase.child(uid.toString())
                        .setValue(user)
                        .addOnSuccessListener {
                            auth.postValue(Resource.success(task.result))
                        }
                        .addOnFailureListener {
                            auth.postValue(Resource.error(task.exception?.message))
                        }
                }
            }
            .addOnFailureListener { error ->
                auth.postValue(Resource.error(error.message.toString()))
            }
        return auth
    }
    fun login(email: String, password: String): LiveData<Resource<AuthResult>>{
        val credential = EmailAuthProvider.getCredential(email, password)
        val auth = MutableLiveData<Resource<AuthResult>>()
        auth.postValue(Resource.loading())
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    auth.postValue(Resource.success(task.result))
                }else{
                    auth.postValue(Resource.error(task.exception?.message))
                }
            }
            .addOnFailureListener {
                auth.postValue(Resource.error(it.message.toString()))
            }
        return auth
    }
    fun loginWithGoogle(name: String, email: String, credential: AuthCredential): LiveData<Resource<AuthResult>>{
        val auth = MutableLiveData<Resource<AuthResult>>()
        auth.postValue(Resource.loading())
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    auth.postValue(Resource.success(task.result))
                    val uid = firebaseAuth.uid
                    val imageUser = "https://ui-avatars.com/api/?background=218B5E&color=fff&size=100&rounded=true&name=$name"
                    val user = User(
                        nameUser = name,
                        avatarUser = imageUser,
                        emailUser = email,
                        uidUser = uid
                    )
                    userDatabase.child(uid.toString())
                        .setValue(user)
                        .addOnSuccessListener {
                            auth.postValue(Resource.success(task.result))
                        }
                        .addOnFailureListener {
                            auth.postValue(Resource.error(task.exception?.message))
                        }
                }else{
                    auth.postValue(Resource.error(task.exception?.message))
                }
            }
            .addOnFailureListener {
                auth.postValue(Resource.error(it.message.toString()))
            }
        return auth
    }
    fun getDataUser(uid: String): LiveData<Resource<User>>{
        val dataUser = MutableLiveData<Resource<User>>()
        dataUser.postValue(Resource.loading())
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                dataUser.postValue(Resource.success(user))
            }

            override fun onCancelled(error: DatabaseError) {
                dataUser.postValue(Resource.error(error.message))
            }
        }
        userDatabase.child(uid)
                .addValueEventListener(listener)


        return dataUser
    }
    fun changePassword(newPass: String, credential: AuthCredential): LiveData<Resource<Void>>{
        val auth = MutableLiveData<Resource<Void>>()
        auth.postValue(Resource.loading())
        val currentUser = firebaseAuth.currentUser
        currentUser?.reauthenticate(credential)
            ?.addOnCompleteListener { task->
                if(task.isSuccessful){
                    currentUser.updatePassword(newPass)
                    auth.postValue(Resource.success(task.result))
                }
            }
            ?.addOnFailureListener {
                auth.postValue(Resource.error(it.message))
            }
        return auth
    }
    fun forgotPassword(email: String): LiveData<Resource<Void>>{
        val auth = MutableLiveData<Resource<Void>>()
        auth.postValue(Resource.loading())
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener {
                if(it.isSuccessful){
                    auth.postValue(Resource.success(it.result))
                }
            }
            .addOnFailureListener {
                auth.postValue(Resource.error(it.message))
            }
        return auth
    }

    fun getList(): Flow<ApiResponse<List<ListResponseItem>>> {
        val data = MutableLiveData<ApiResponse<List<ListResponseItem>>>()
        val listener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                    val dataList = snapshot.children.toList()
                    val dataNabi = dataList.sortedWith(compareBy{
                        it.getValue(ListResponseItem::class.java)?.keyId
                    })
                    val newData = ArrayList<ListResponseItem>()
                    for(i in dataNabi.indices){
                        val dataKu = dataNabi[i].getValue(ListResponseItem::class.java)
                        if (dataKu != null) {
                            newData.add(dataKu)
                        }
                    }
                    data.postValue(ApiResponse.success(newData))
            }

            override fun onCancelled(error: DatabaseError) {
                data.postValue(ApiResponse.error(error.message))
            }


        }
        nabiDatabase.addValueEventListener(listener)
        return data.asFlow()
    }
    fun postDataNabi(
        nama: String? = null,
        umur: String? = null,
        tempatDiutus: String? = null,
        kisah: String? = null,
        keyId: String,
        profile: File? = null,
        display: File? = null,
        recentActivity: Boolean,
        tag: String
    ): LiveData<Resource<ListDomain>>{
        val dataNabi = MutableLiveData<Resource<ListDomain>>()
        val profilePath = Uri.fromFile(File(profile.toString()))
        val displayPath = Uri.fromFile(File(display.toString()))
        val formatter = SimpleDateFormat("yyyy_MM_dd_HH_mm_SS", Locale.getDefault())
        val date = Date()
        val fileName = formatter.format(date)
        getStorage(
            name = nama,
            umur = umur,
            umat = tempatDiutus,
            detail = kisah,
            keyId = keyId,
            dataNabi = dataNabi,
            profilePath = profilePath,
            displayPath = displayPath,
            fileName = fileName,
            recentActivity = recentActivity,
            tag = tag
        )
        return dataNabi
    }

    private fun getStorage(
        name: String? = null,
        umur: String? = null,
        umat: String? = null,
        detail: String? = null,
        keyId: String,
        dataNabi: MutableLiveData<Resource<ListDomain>>,
        profilePath: Uri? = null,
        displayPath: Uri? = null,
        fileName: String,
        recentActivity: Boolean,
        tag: String
    ) {
        if (profilePath != Uri.parse("file:///null") && displayPath != Uri.parse("file:///null") && profilePath != null && displayPath != null) {
            Log.e("tesIt", "getStorage: $profilePath $profilePath" )
            storageReference.child(fileName).putFile(profilePath)
            storageReference.child(fileName+"_display").putFile(displayPath)
                .addOnSuccessListener {
                    storageReference.child(fileName+"_display").downloadUrl.addOnSuccessListener { display ->
                        storageReference.child(fileName).downloadUrl.addOnSuccessListener { profile ->
                            getDatabase(
                                name = name,
                                umur = umur,
                                umat = umat,
                                detail = detail,
                                keyId = keyId,
                                profile = profile.toString(),
                                display = display.toString(),
                                dataNabi = dataNabi,
                                recentActivity = recentActivity,
                                tag = tag
                            )
                        }.addOnFailureListener {
                            dataNabi.postValue(Resource.error(it.message))
                            Log.e("error", "getStorage: ${it.message}" )
                        }
                    }.addOnFailureListener {
                        dataNabi.postValue(Resource.error(it.message))
                        Log.e("error", "getStorage: ${it.message}" )
                    }
                }
        }else{
            getDatabase(
                name = name,
                umur = umur,
                umat = umat,
                detail = detail,
                keyId = keyId,
                dataNabi = dataNabi,
                recentActivity = recentActivity,
                tag = tag
            )
        }
    }

    private fun getDatabase(
        name: String? = null,
        umur: String? = null,
        umat: String? = null,
        detail: String? = null,
        keyId: String,
        profile: String? = null,
        display: String? = null,
        dataNabi: MutableLiveData<Resource<ListDomain>>,
        recentActivity: Boolean,
        tag: String
    ) {
        val data = ListDomain(
            name = name,
            umur = umur,
            umat = umat,
            detail = detail,
            keyId = keyId,
            profile = profile,
            display = display,
            recentActivity = recentActivity,
            tag = tag
        )
        dataNabi.postValue(Resource.loading())
        nabiDatabase.child(keyId)
            .setValue(data)
            .addOnSuccessListener {
                dataNabi.postValue(Resource.success(data))
            }
            .addOnFailureListener {
                dataNabi.postValue(Resource.error(it.message))
            }
    }

    fun removeData(keyId: String): LiveData<Resource<String>> {
        val message = MutableLiveData<Resource<String>>()
        nabiDatabase.child(keyId).removeValue()
            .addOnSuccessListener {
                message.postValue(Resource.success("deleted"))
            }.addOnFailureListener {
                message.postValue(Resource.error(it.message))
            }
        return message
    }
    fun setRecentActivity(state: Boolean, keyId: String){
        nabiDatabase.child(keyId)
            .child("recentActivity")
            .setValue(state)
    }
    fun clearRecentActivity(state: Boolean, keyId: String){
        nabiDatabase.child(keyId)
            .child("recentActivity")
            .setValue(state)
    }
}