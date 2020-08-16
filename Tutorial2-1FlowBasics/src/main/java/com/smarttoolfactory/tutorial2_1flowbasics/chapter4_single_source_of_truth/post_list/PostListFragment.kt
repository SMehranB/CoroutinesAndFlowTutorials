package com.smarttoolfactory.tutorial2_1flowbasics.chapter4_single_source_of_truth.post_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.smarttoolfactory.tutorial2_1flowbasics.R
import com.smarttoolfactory.tutorial2_1flowbasics.adapter.PostListAdapter
import com.smarttoolfactory.tutorial2_1flowbasics.databinding.FragmentPostListBinding

class PostListFragment : Fragment() {

    lateinit var dataBinding: FragmentPostListBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        dataBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_post_list, container, false)
        bindViews()
        return dataBinding.root
    }

    private val viewModel by activityViewModels<PostViewModel> {
        PostViewModelFactory(application = requireActivity().application)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getPosts()
    }

    private fun bindViews() {

        val binding = dataBinding!!

        with(binding) {
            // 🔥 Set lifecycle for data binding
            lifecycleOwner = viewLifecycleOwner

            viewModel = viewModel

            recyclerView.apply {

                // Set Layout manager
                this.layoutManager =
                    LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)

                // Set RecyclerViewAdapter
                this.adapter =
                    PostListAdapter(
                        R.layout.row_post
                    )
            }

        }

        subscribeGoToDetailScreen()

    }

    private fun subscribeGoToDetailScreen() {

        viewModel.goToDetailScreen.observe(viewLifecycleOwner, Observer {

            it.getContentIfNotHandled()?.let { post ->
                val bundle = bundleOf("post" to post)
//                findNavController().navigate(
//                    R.id.action_verticalPostFragment_to_postDetailFragment,
//                    bundle
//                )
            }
        })
    }
}