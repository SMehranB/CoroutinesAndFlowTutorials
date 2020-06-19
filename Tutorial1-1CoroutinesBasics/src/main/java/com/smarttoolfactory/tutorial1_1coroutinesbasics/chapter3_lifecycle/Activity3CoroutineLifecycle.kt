package com.smarttoolfactory.tutorial1_1coroutinesbasics.chapter3_lifecycle

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.smarttoolfactory.tutorial1_1basics.databinding.Activity3CoroutineLifecycleBinding
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class Activity3CoroutineLifecycle : AppCompatActivity(), CoroutineScope {


    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = job +
                Dispatchers.Main +
                CoroutineName("🙄 Activity Scope") +
                CoroutineExceptionHandler { coroutineContext, throwable ->
                    println("🤬 Exception $throwable in context:$coroutineContext")
                }


    private val binding by lazy {
        Activity3CoroutineLifecycleBinding.inflate(layoutInflater)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        job = Job()

        binding.button.setOnClickListener {

            // 🔥⚠️ This scope lives as long as Application is alive
            GlobalScope.launch {
                for (i in 0..300) {
                    println("$TAG 🤪 Global Progress: $i in thread: ${Thread.currentThread().name}, scope: $this")
                    delay(300)
                }
            }

            // This scope is canceled whenever this Activity's onDestroy method is called
            launch {
                for (i in 0..300) {
                    println("$TAG 😍 Activity Scope Progress: $i in thread: ${Thread.currentThread().name}, scope: $this")
                    withContext(Dispatchers.Main) {
                        binding.tvResult.text =
                            "Activity Scope Progress: $i in thread: ${Thread.currentThread().name}, scope: $this"
                    }
                    delay(300)
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        // cancel()             // this is the cancel method of coroutineScope. You can call coroutineScope.cancel() or parentJob.cancel() in order to cancel the parent job.
        job.cancel()
    }

    companion object {
        const val TAG = "Test-Activity3CoroutineLifecycle"
    }
}
