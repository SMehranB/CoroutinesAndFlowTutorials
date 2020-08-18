package com.smarttoolfactory.tutorial2_1flowbasics.chapter4_single_source_of_truth.domain

import com.smarttoolfactory.tutorial2_1flowbasics.chapter4_single_source_of_truth.DispatcherProvider
import com.smarttoolfactory.tutorial2_1flowbasics.chapter4_single_source_of_truth.data.repository.PostRepository
import com.smarttoolfactory.tutorial2_1flowbasics.data.mapper.EntityToPostMapper
import com.smarttoolfactory.tutorial2_1flowbasics.data.model.Post
import com.smarttoolfactory.tutorial2_1flowbasics.data.model.PostEntity
import com.smarttoolfactory.tutorial2_1flowbasics.data.model.Status
import com.smarttoolfactory.tutorial2_1flowbasics.data.model.ViewState
import com.smarttoolfactory.tutorial2_1flowbasics.util.EmptyDataException
import kotlinx.coroutines.flow.*

class GetPostsUseCaseFlow(
    private val repository: PostRepository,
    private val entityToPostMapper: EntityToPostMapper,
    private val dispatcherProvider: DispatcherProvider
) {


    /**
     * Method for getting data and saving data to db to use db as Single Source of Truth.
     * This method is useful for using in splash screen to fetch data, and data is required
     * to be used in multiple screens
     */
    suspend fun fetchDataFromRemoteAndSaveToDB() {
        val posts = repository.fetchEntitiesFromRemote()
        repository.deletePostEntities()
        repository.savePostEntity(posts)
    }


    /**
     * This function always looks for new data from remote source first
     *
     * * if data is fetched from remote source: deletes old data, saves new data and returns new data
     * * if error occurred while fetching data from remote: it tries to fetch data from database
     * * if both network and db didn't have any data throws empty set exception error
     */
    fun getPostFlowOfflineLast(): Flow<ViewState<List<Post>>> {

        // *** START This section was in repo before moving to here ***
        return flow { emit(repository.fetchEntitiesFromRemote()) }
            .map {
                println("🍏 getPostFlowOfflineLast() First map in thread: ${Thread.currentThread().name}")

                if (it.isNullOrEmpty()) {
                    throw EmptyDataException("Data is available in neither in remote nor local source!")
                } else {
                    repository.deletePostEntities()
                    repository.savePostEntity(it)
                    repository.getPostEntitiesFromLocal()
                }

            }
            // *** END This section was in repo before moving to here ***
            .flowOn(dispatcherProvider.ioDispatcher)
            .catch { cause ->
                println("❌ getPostFlowOfflineLast() FIRST catch with error: $cause, in thread: ${Thread.currentThread().name}")
                emitAll(flowOf(repository.getPostEntitiesFromLocal()))
            }
            .map {
                if (!it.isNullOrEmpty()) {
                    entityToPostMapper.map(it)
                } else {
                    throw EmptyDataException("No data is available in both remote and local source!")
                }
            }
            .map { postList ->
                println("🎃 getPostFlowOfflineLast() Third map in thread: ${Thread.currentThread().name}")
                ViewState(status = Status.SUCCESS, data = postList)
            }
            .catch { cause: Throwable ->
                println("❌ getPostFlowOfflineLast() SECOND catch with error: $cause, in thread: ${Thread.currentThread().name}")
                emitAll(flowOf(ViewState(Status.ERROR, error = cause)))
            }
            .flowOn(dispatcherProvider.defaultDispatcher)
    }

    /**
     * Flow to get data from cache if it's not expired, if it's expired check remote data source.
     *
     * This function is used with a offline-first approach that cache can expire after a fixed amount of time
     */
    private fun getCachedDataFlow(): Flow<List<PostEntity>> {
        return flow {
            emit(
                if (repository.isCacheExpired()) {
                    repository.fetchEntitiesFromRemote()
                } else {
                    repository.getPostEntitiesFromLocal()
                }
            )
        }
    }

    fun getPostFlowOfflineFirst(): Flow<ViewState<List<Post>>> {

        return flow { emit(repository.getPostEntitiesFromLocal()) }
            .map {

                println("🍏 getPostFlowOfflineFirst() First map in thread: ${Thread.currentThread().name}")
                if (it.isEmpty()) {
                    repository.run {
                        repository.deletePostEntities()
                        repository.savePostEntity(fetchEntitiesFromRemote())
                        repository.getPostEntitiesFromLocal()
                    }
                } else {
                    it
                }
            }
            .flowOn(dispatcherProvider.ioDispatcher)
            .map {
                println("🎃 getPostFlowOfflineFirst() Second map in thread: ${Thread.currentThread().name}")

                // Map Entity to UI item
                if (!it.isNullOrEmpty()) {
                    entityToPostMapper.map(it)
                } else {
                    throw EmptyDataException("Data is available in neither in remote nor local source!")
                }
            }
            .map { postList ->

                println("🍎 getPostFlowOfflineFirst() Third map in thread: ${Thread.currentThread().name}")

                ViewState(status = Status.SUCCESS, data = postList)
            }
            .catch { cause: Throwable ->
                println("❌ getPostFlowOfflineFirst() SECOND catch with error: $cause, in thread: ${Thread.currentThread().name}")
                emitAll(flowOf(ViewState(Status.ERROR, error = cause)))
            }
            .flowOn(dispatcherProvider.defaultDispatcher)
    }
}