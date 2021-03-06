package com.jkmvc.example.controller

import com.jkmvc.http.Controller

/**
 * 主页
 */
class WelcomeController: Controller() {

    /**
     * 主页
     */
    public fun actionIndex() {
        res.render("hello world");
    }

    /**
     * 显示jsp视图
     * render jsp view
     */
    public fun actionJsp(){
        res.render(view("index" /* view file */, mutableMapOf("name" to "shijianhang") /* view data */))
    }

}