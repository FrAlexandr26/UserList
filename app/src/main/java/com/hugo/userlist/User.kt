package com.hugo.userlist

data class User(var login: String, var repos_url: String)
{
    var reposList : String = ""
}