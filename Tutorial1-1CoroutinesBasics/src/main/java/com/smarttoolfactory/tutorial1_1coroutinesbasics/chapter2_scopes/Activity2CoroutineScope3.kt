package com.smarttoolfactory.tutorial1_1coroutinesbasics.chapter2_scopes

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.smarttoolfactory.tutorial1_1basics.R
import com.smarttoolfactory.tutorial1_1basics.databinding.Activity2Scope3Binding
import com.smarttoolfactory.tutorial1_1coroutinesbasics.guide.status
import com.smarttoolfactory.tutorial1_1coroutinesbasics.util.dataBinding
import kotlinx.coroutines.*


/**
 * ## Scopes
 * This sample displays some important topics below:
 * * When a [CoroutineScope.launch] is fired a new Job instance is created unless [Job]
 *  is passed to builder([CoroutineScope.launch]) method as parameter.
 *
 * ## Cancellations
 * * If a parent job is canceled children jobs are also canceled
 * * If a child job is canceled other children jobs continue
 *
 * ## Exceptions
 * * If a child job throws an exception parent job is canceled and other children jobs are also canceled
 * * Parent should have an exception handling [CoroutineExceptionHandler] or manual exception handling
 * for application NOT to crash.
 *
 */
class Activity2CoroutineScope3 : AppCompatActivity(R.layout.activity2_scope_3) {

    private val TAG = "Activity2CoroutineScope3"

    // Jobs

    /**
     * Job of the coroutineContext, [CoroutineScope.cancel] calls this [Job] cancel method.
     * And when this job is on completed state which does not let currentJob to be active
     */
    private var parentJob: Job = Job()

    /**
     * This is the jub that runs when button is clicked
     */
    private var currentJob: Job? = null
    private var childJob1: Job? = null
    private var childJob2: Job? = null

    // Scope
    /**
     * [Job] of [CoroutineScope] is canceled if [CoroutineScope.cancel] is called
     * and is NOT launching a coroutine using this scope
     */
    private val coroutineScope = CoroutineScope(parentJob)

    private val binding: Activity2Scope3Binding by dataBinding()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println("$TAG 🤨 onCreate() parentJob: $parentJob")

        bindViews()

    }

    private fun bindViews() {

        var isJobStarted = false

        binding.btnStartJob.setOnClickListener {

            if (!isJobStarted) {

                startJob()

                isJobStarted = true
                binding.btnStartJob.text = "Cancel"

            } else {
                cancelJob()
                isJobStarted = false
                binding.btnStartJob.text = "Start Job"
            }
        }

        binding.btnCancelChildJob1.setOnClickListener {

            childJob1?.let {
                if (childJob1!!.isActive) {
                    childJob1!!.cancel()
                    binding.btnCancelChildJob1.isEnabled = false
                    binding.tvChildJobResult1.text = "Progress canceled"
                }
            }
        }

        binding.btnCancelChildJob2.setOnClickListener {
            childJob2?.let {
                if (childJob2!!.isActive) {
                    childJob2!!.cancel()
                    binding.btnCancelChildJob2.isEnabled = false
                    binding.tvChildJobResult2.text = "Progress canceled"
                }
            }
        }

    }

    private fun startJob() {


        // Handle works in thread that exception is caught
        val handler = CoroutineExceptionHandler { coroutineContext, throwable ->
            println("$TAG 🤬 Parent Caught $throwable in thread ${Thread.currentThread().name}, and coroutineContext: $coroutineContext")
        }

        currentJob = CoroutineScope(parentJob).launch(Dispatchers.Default + handler) {

            println("$TAG 🎃 startJob() parentJob: $parentJob, currentJob: $currentJob")
            println("$TAG 😛 Inside coroutineScope.launch() scope: $this, coroutineContext job: ${this.coroutineContext[Job]}}")

            // This scope is not scope with coroutineScope used to call launch function
            startChildrenJobs(this@launch)
            displayAlphabet(binding.tvJobResult)

        }

        // Invoked when a job completes from 🔥 Thread exception caught
        currentJob?.invokeOnCompletion {

            println("$TAG invokeOnCompletion() 🤬 Exception $it, in thread: ${Thread.currentThread().name}")

            it?.let {

                runOnUiThread {
                    binding.tvJobResult.text = it.message ?: currentJob?.status()
                    binding.tvChildJobResult1.text = childJob1?.status()
                    binding.tvChildJobResult2.text = childJob2?.status()
                }

            }
        }

    }

    private fun cancelJob() {

        binding.btnCancelChildJob1.isEnabled = false
        binding.btnCancelChildJob2.isEnabled = false

        currentJob?.cancel()
        // 🔥🔥🔥 Calling this method cancels Job of this scope also and does not
        // start a coroutine using this scope next time
//        coroutineScope.cancel()
//        parentJob.cancel()

    }


    private suspend fun startChildrenJobs(myCoroutineScope: CoroutineScope) {

        childJob1 = myCoroutineScope.launch {
            displayAlphabet(binding.tvChildJobResult1)
        }

        childJob2 = myCoroutineScope.launch {

            launch {
                displayNumbers(binding.tvChildJobResult2)
            }

            delay(6000)
            // 🔥 Causes crash if parent does not catch this exception with handler
            // 🔥🔥 try catch on parent coroutine DOES NOT WORK
            throw RuntimeException("Child 2 threw RuntimeException")

        }
        childJob2?.invokeOnCompletion {
            binding.tvChildJobResult2.text = it?.message ?: childJob2?.status()
        }

        withContext(Dispatchers.Main) {
            binding.btnCancelChildJob1.isEnabled = true
            binding.btnCancelChildJob2.isEnabled = true
        }

        println(
            "$TAG 🤪 startChildrenJobs() parentJob: $parentJob, currentJob: $currentJob" +
                    ", childJob1: $childJob1, childJob2: $childJob2"
        )
    }


    private suspend fun displayAlphabet(textView: TextView) {

        for (i in 'A'..'Z') {
            withContext(Dispatchers.Main) {
                textView.text = "Progress: $i"
            }
            delay(400)


        }

        withContext(Dispatchers.Main) {
            textView.text = "Job Completed"
        }

    }

    private suspend fun displayNumbers(textView: TextView) {

        for (i in 0..100) {
            withContext(Dispatchers.Main) {
                textView.text = "Progress: $i"
            }
            delay(300)
        }

        withContext(Dispatchers.Main) {
            textView.text = "Job Completed"
        }

    }

}

