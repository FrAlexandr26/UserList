package com.hugo.userlist

import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.ConnectivityManager
import android.net.Network
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var loaderView : RelativeLayout
    private lateinit var userList: RecyclerView
    private lateinit var usersAdapter : ListAdapter
    private lateinit var rep: TextView
    private lateinit var usersArr: ArrayList<User>
    private lateinit var layoutManager: RecyclerView.LayoutManager
    private lateinit var db : SQLiteDatabase
    private lateinit var query: Cursor
    private lateinit var pushBroadcastReceiver: BroadcastReceiver
    private var isDataLoaded: Boolean = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        loaderView = findViewById(R.id.loader)
        userList = findViewById(R.id.users_list)
        rep = findViewById(R.id.rep)
        usersArr = ArrayList()
        layoutManager = LinearLayoutManager(this)
        userList.layoutManager = layoutManager

        db = baseContext.openOrCreateDatabase("users.db", MODE_PRIVATE, null)
        db.execSQL("CREATE TABLE IF NOT EXISTS users (login TEXT, user_id INTEGER, repos_url TEXT, repos_list TEXT, changes_count INTEGER);")
        query = db.rawQuery("SELECT * FROM users;", null)
        if (query.moveToFirst()){
            usersArr.add(User(query.getString(0), query.getInt(1), query.getString(2)))
            var i = 1
            usersArr[0].reposList = query.getString(3)
            usersArr[0].changesCount = query.getInt(4)
            while (query.moveToNext()){
                usersArr.add(User(query.getString(0), query.getInt(1), query.getString(2)))
                usersArr[i].reposList = query.getString(3)
                usersArr[i].changesCount = query.getInt(4)
                i++
            }
            loaderView.alpha = 0F
        }
        query.close()
        db.close()

        usersAdapter = ListAdapter(usersArr, R.layout.list_item)
        userList.adapter = usersAdapter
        usersAdapter.setOnItemClickListener(object : ListAdapter.onItemClickListener{
            override fun onItemClick(position: Int) {
                rep.text = usersArr[position].reposList
            }

        })
        var netServise = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var netCallback = object: ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                if (!isDataLoaded) {
                    getUsersData()
                    isDataLoaded = true
                    loaderView.alpha = 0F
                }
            }
        }
        netServise.registerDefaultNetworkCallback(netCallback)

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@OnCompleteListener
            }

            // ?????????? ??????????
            val token = task.result

            // ???????????????? ???????????????????? ?????? ??????????
            Log.d(TAG, token)
        })
        pushBroadcastReceiver = object : BroadcastReceiver(){
            override fun onReceive(ctx: Context?, intent: Intent?) {
                var extraContains = intent?.extras
                var l = 0
                while (l < usersArr.size){
                    if (usersArr[l].id == extraContains?.getString("user_id")!!.toInt()){
                        usersArr[l].changesCount = extraContains?.getString("changes_count")!!.toInt()
                        db = baseContext.openOrCreateDatabase("users.db", MODE_PRIVATE, null)
                        db.execSQL("UPDATE users SET changes_count = ${extraContains?.getString("changes_count")!!.toInt()} WHERE user_id = ${extraContains?.getString("user_id")!!.toInt()};")
                        db.close()
                        break
                    }
                    l++
                }
            }
        }
        var iF = IntentFilter()
        iF.addAction("PUSH_EVENT")
        registerReceiver(pushBroadcastReceiver, iF)
    }

    private fun getUsersData(){
        val retrofit = Retrofit.Builder().baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create()).build().create((UsersApi::class.java))
        val data = retrofit.getUsers()

        data.enqueue(object: Callback<ArrayList<User>?> {
            override fun onResponse(
                call: Call<ArrayList<User>?>,
                response: Response<ArrayList<User>?>
            ) {
                val resBody = response.body()!!
                usersArr.clear()
                for (usersData in resBody){
                    val reposRet = Retrofit.Builder().baseUrl("https://api.github.com/users/" + usersData.login + "/")
                        .addConverterFactory(GsonConverterFactory.create()).build().create((UsersApi::class.java))
                    val reposData = reposRet.getReposList()

                    reposData.enqueue(object: Callback<ArrayList<Repos>?> {
                        override fun onResponse(
                            call: Call<ArrayList<Repos>?>,
                            response: Response<ArrayList<Repos>?>
                        ) {
                            var strBuilder = StringBuilder()
                            for (repos in response.body()!!){
                                strBuilder.append(repos.name + ", ")
                            }
                            usersData.reposList = strBuilder.toString()
                        }

                        override fun onFailure(call: Call<ArrayList<Repos>?>, t: Throwable) {
                            Toast.makeText(applicationContext, "Failure parsing users repos info", Toast.LENGTH_SHORT).show()
                        }

                    })
                    usersArr.add(usersData)
                }
                usersAdapter.notifyDataSetChanged()
                rep.text = usersArr[1].reposList
                db = baseContext.openOrCreateDatabase("users.db", MODE_PRIVATE, null)
                query = db.rawQuery("SELECT * FROM users", null)
                if (!query.moveToFirst()) {
                    var i = 0
                    while (i < usersArr.size) {
                        db.execSQL("INSERT INTO users VALUES ('${usersArr[i].login}', ${usersArr[i].id}, '${usersArr[i].repos_url}', '${usersArr[i].reposList}', ${usersArr[i].changesCount});")
                        i++
                    }
                }
                query.close()
                db.close()

            }

            override fun onFailure(call: Call<ArrayList<User>?>, t: Throwable) {
                Toast.makeText(applicationContext, "Failure parsing users info", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroy() {
        unregisterReceiver(pushBroadcastReceiver)
        super.onDestroy()
    }


}
