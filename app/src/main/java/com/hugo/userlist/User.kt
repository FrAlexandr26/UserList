package com.hugo.userlist

data class User(var login: String, var id: Int, var repos_url: String)
{
    var reposList : String = ""
    var changesCount: Int = 0
}