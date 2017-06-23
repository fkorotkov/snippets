package com.fkorotkov.github.apps.signing

fun main(vararg args: String) {
  val secrets = GithubSecrets.initialize()
  val signed = secrets.sign("239")
  println("Signed: $signed")
}