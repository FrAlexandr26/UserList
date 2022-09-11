package com.hugo.userlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ListAdapter(var usersList: ArrayList<User>, var layoutId: Int): RecyclerView.Adapter<ListAdapter.ItemViewHolder>() {

    private lateinit var listener : onItemClickListener

    interface onItemClickListener{
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(l: onItemClickListener){ listener = l}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        var itemView = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ItemViewHolder(itemView, listener)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.loginText.text = usersList[position].login
        holder.changesCountView.text = usersList[position].changesCount.toString()
        if (usersList[position].changesCount == 0){
            holder.changesCountView.alpha = 0F
        }
    }

    override fun getItemCount(): Int {
        return usersList.size
    }

    class ItemViewHolder(itemView: View, itemListener: onItemClickListener) : RecyclerView.ViewHolder(itemView){
        var loginText: TextView = itemView.findViewById(R.id.user_name)
        var changesCountView: TextView = itemView.findViewById(R.id.changes_count)
        init{
            itemView.setOnClickListener {
                itemListener.onItemClick(adapterPosition)
            }
       }
    }
}