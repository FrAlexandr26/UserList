package com.hugo.userlist

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var userList: RecyclerView
    private lateinit var usersAdapter : ListAdapter
    private lateinit var rep: TextView
    private lateinit var usersArr: ArrayList<User>
    private lateinit var layoutManager: RecyclerView.LayoutManager
    private lateinit var db : SQLiteDatabase
    private lateinit var query: Cursor


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)




        userList = findViewById(R.id.users_list)
        rep = findViewById(R.id.rep)
        usersArr = ArrayList()
        layoutManager = LinearLayoutManager(this)
        userList.layoutManager = layoutManager

        db = baseContext.openOrCreateDatabase("users.db", MODE_PRIVATE, null)
        db.execSQL("CREATE TABLE IF NOT EXISTS users (login TEXT, id INTEGER, node_id TEXT, avatar_url TEXT, gravatar_url TEXT, url TEXT, html_url TEXT, followers_url TEXT, following_url TEXT, starred_url TEXT, subscriptions_url TEXT, organizations_url TEXT, repos_url TEXT, events_url TEXT, received_events_url TEXT, type TEXT, site_admin INTEGER)")
        query = db.rawQuery("SELECT * FROM users;", null)
        var boolArg : Boolean = true
        if (query.moveToFirst()){
            while (query.moveToNext()){
                if (query.getInt(17) == 0){
                    boolArg = true
                } else if(query.getInt(17) == 1){
                    boolArg = false
                }
                usersArr.add(User(query.getString(0), query.getInt(1), query.getString(2), query.getString(3), query.getString(4), query.getString(5), query.getString(6), query.getString(7), query.getString(8), query.getString(9), query.getString(10), query.getString(11), query.getString(12), query.getString(13), query.getString(14), query.getString(15), boolArg))
            }
        }
        query.close()
        db.close()

        usersAdapter = ListAdapter(usersArr, R.layout.list_item)
        userList.adapter = usersAdapter
        usersAdapter.setOnItemClickListener(object : ListAdapter.onItemClickListener{
            override fun onItemClick(position: Int) {
                rep.text = usersArr[position].repos_url
            }

        })
        getUsersData()
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
                    usersArr.add(usersData)
                }
                usersAdapter.notifyDataSetChanged()

                db = baseContext.openOrCreateDatabase("users.db", MODE_PRIVATE, null)
                var boolToIntArg: Int = 0
                for(dbForm in resBody){
                    if (dbForm.site_admin == true){
                        boolToIntArg = 0
                    } else {
                        boolToIntArg = 1
                    }
                    db.execSQL("INSERT OR IGNORE INTO users VALUES (" + dbForm.login +", " + dbForm.id +", " + dbForm.node_id +", " + dbForm.avatar_url +", " + dbForm.gravatar_url +", " + dbForm.url +", " + dbForm.html_url +", " + dbForm.followers_url +", " + dbForm.following_url +", " + dbForm.starred_url +", " + dbForm.subscriptions_url +", " + dbForm.organizations_url +", " + dbForm.repos_url +", " + dbForm.events_url +", " + dbForm.received_events_url +", " + dbForm.type +", " + boolToIntArg +");")
                }
                db.close()

            }

            override fun onFailure(call: Call<ArrayList<User>?>, t: Throwable) {
                Toast.makeText(applicationContext, "Failure parsing", Toast.LENGTH_SHORT).show()
            }
        })
    }


}
