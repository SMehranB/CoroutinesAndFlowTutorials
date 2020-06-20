package com.smarttoolfactory.tutorial1_1coroutinesbasics.chapter2_scopes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.smarttoolfactory.tutorial1_1basics.R
import com.smarttoolfactory.tutorial1_1basics.databinding.Activity2Scope2Binding
import com.smarttoolfactory.tutorial1_1coroutinesbasics.util.dataBinding
import kotlinx.coroutines.*

/**
 * Many coroutines can run in a single thread.
 * This example illustrates the usage of withTimeoutOrNull() function with a simple job
 * */

class Activity2CoroutineScope2 : AppCompatActivity(R.layout.activity2_scope_2) {

    private val binding: Activity2Scope2Binding by dataBinding()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.button4.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                fakeApiRequest()
            }
        }
    }

    private suspend fun fakeApiRequest() {
        val job = withTimeoutOrNull(1900) {
            val result1 =
                getResult1FromApi()    // the following line is not executed until the suspending function getResult1FromApi() returns
            setTextOnMainThread(result1)                 // the following line is not executed until the suspending function setTextOnMainThread() returns

            val result2 =
                getResult2FromApi()   // the following line is not executed until the suspending function getResult2FromApi() returns
            setTextOnMainThread(result2)                // the following line is not executed until the suspending function setTextOnMainThread() returns
        }

        if (job == null) {
            val cancelMessage = "\nCancelling job ...  Job took longer than 1900 ms"
            println("test123 : $cancelMessage")
            setTextOnMainThread(cancelMessage)
        }
    }

    /** Simulates network request
     * delay(2000) simulates the waiting for response from server
     * */
    private suspend fun getResult1FromApi(): String {
        println("test123 - getResult1FromApi : ${Thread.currentThread().name}")
        delay(2000)
        return "\nResult 1"
    }

    /**
     * delay(2000) simulates the waiting for response from server
     * Simulates network request */
    private suspend fun getResult2FromApi(): String {
        println("test123 - getResult2FromApi : ${Thread.currentThread().name}")
        delay(2000)
        return "\nResult 2"
    }

    private fun setNewText(newString: String) {
        val newText = binding.textView.text.toString() + newString
        binding.textView.text = newText
    }

    private suspend fun setTextOnMainThread(newString: String) {
        withContext(Dispatchers.Main) {
            println("test123 - setTextOnMainThread : ${Thread.currentThread().name}")
            delay(2000)
            setNewText(newString)
        }
    }

}

