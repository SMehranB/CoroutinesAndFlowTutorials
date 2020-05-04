package com.smarttoolfactory.tutorial1_1coroutinesbasics.chapter5_viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import java.util.*
import kotlin.collections.ArrayList

class CoroutinesViewModel : ViewModel() {

    val result = MutableLiveData<String>()
    val resultWithRetry = MutableLiveData<String>()
    val enableRetryButton = MutableLiveData<Boolean>().apply {
        value = true
    }

    val resultWithTimeout = MutableLiveData<String>()
    val enableWithTimeoutButton = MutableLiveData<Boolean>().apply {
        value = true
    }

    val resultMultiple = MutableLiveData<String>()
    val enableMultipleButton = MutableLiveData<Boolean>().apply {
        value = true
    }

    fun getMockResult() {

        result.value = "Loading..."

        viewModelScope.launch {

            println("ViewModel scope: $this, thread: ${Thread.currentThread().name}")

            result.value = generateMockNetworkResponse()
        }

    }


    fun getMockResultWithTimeout() {

        val timeout = 4000L

        resultWithTimeout.value = "Loading..."

        viewModelScope.launch {

            // Countdown timer
            launch {
                withTimeout(timeout) {
                    // 10 is random number to show that it cancels after timeout
                    val times = 10
                    repeat(times) {
                        resultWithTimeout.value = "${times - it}"
                        delay(1000)
                    }
                }
            }

            // Get mock response with a timeout
            withTimeout(timeout) {
                try {
                    resultWithTimeout.value = generateMockNetworkResponse(timeout)
                } catch (exception: TimeoutCancellationException) {
                    resultWithTimeout.value = exception.message
                } catch (exception: Exception) {
                    resultWithTimeout.value = exception.message
                }

            }

        }
    }

    fun getMockResultWithRetry() {

        enableRetryButton.value = false

        viewModelScope.launch {

            resultWithRetry.value = "Loading..."

            try {
                retry(3, 100, 2000) {
                    resultWithRetry.value = "Retry count: $it"
                    resultWithRetry.value = generateMockNetworkResponse(2100)
                    enableRetryButton.value = true
                }
            } catch (exception: Exception) {
                resultWithRetry.value = "Exception ${exception.message}"
                enableRetryButton.value = true
            }
        }
    }


    // retry with exponential backoff
    // inspired by https://stackoverflow.com/questions/46872242/how-to-exponential-backoff-retry-on-kotlin-coroutines

    @Throws(Exception::class)
    private suspend fun <T> retry(
        times: Int,
        initialDelayMillis: Long = 100,
        maxDelayMillis: Long = 1000,
        factor: Double = 2.0,
        block: suspend (Int) -> T?
    ): T? {
        var currentDelay = initialDelayMillis

        repeat(times) {

            try {
                return block(it + 1)
            } catch (exception: Exception) {
                exception.printStackTrace()
//               if (it == times -1) throw RuntimeException("Failed after $times tries")
            }

            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMillis)

        }

          throw RuntimeException("Failed after $times tries")
    }


    fun getMultipleResponsesConcurrently() {

        val listOfResponses = ArrayList<Deferred<String>>()


        resultMultiple.value = "Loading..."
        enableMultipleButton.value = false

        viewModelScope.launch {

            val startTime = System.currentTimeMillis()

            val city = async {
                getRandomCity()
            }


            val emoji = async {
                getRandomEmoji()
            }

            val response = async {
                generateMockNetworkResponse()
            }


            // 🔥 Alternative 1: Get results concurrently with deferred await functions
//            resultMultiple.value =
//                "${city.await()} - ${emoji.await()} - ${response.await()}," +
//                        " time: ${System.currentTimeMillis() - startTime}ms"

            // 🔥 Alternative 2: Use a list
            listOfResponses.add(city)
            listOfResponses.add(emoji)
            listOfResponses.add(response)

            val results = listOfResponses.awaitAll()
            resultMultiple.value =
                "${results[0]} - ${results[1]} - ${results[2]}," +
                        " time: ${System.currentTimeMillis() - startTime}ms"

            enableMultipleButton.value = true

        }

    }

    /*
        Mock Response Functions
     */

    private suspend fun generateMockNetworkResponse(delay: Long = 2000): String {
        delay(delay)

        if (delay > 2000) throw RuntimeException()

        return "Hello World"
    }

    private suspend fun getRandomCity(): String {
        val cityList = listOf("Berlin", "New York", "London", "Paris", "Istanbul")

        val random = Random()

        delay(3000)
        return cityList[random.nextInt(cityList.size)]
    }

    private suspend fun getRandomEmoji(): String {

        val emojiList = listOf("🤪", "🙄", "😛", "😭", "🤬", "🥶", "😱")
        delay(2500)

        val random = Random()
        return emojiList[random.nextInt(emojiList.size)]

    }

}