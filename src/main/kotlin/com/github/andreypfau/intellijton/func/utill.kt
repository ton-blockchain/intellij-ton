package com.github.andreypfau.intellijton.func

fun loadTextResource(ctx: Any, resource: String): String =
    ctx.javaClass.classLoader.getResourceAsStream(resource)!!.bufferedReader().use {
        it.readText()
    }
