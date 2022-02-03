package com.github.andreypfau.intellijton

fun loadTextResource(ctx: Any, resource: String): String =
    ctx.javaClass.classLoader.getResourceAsStream(resource)!!.bufferedReader().use {
        it.readText()
    }
