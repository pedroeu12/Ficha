package com.pedroeu.ficha.data.model

data class Feat(
    val id: String,
    val name: String,
    val description: String,
    /** The book this comes from; the character's chosen books decide whether it is offered. */
    override val book: Sourcebook = Sourcebook.PHB,
) : FromSourcebook
