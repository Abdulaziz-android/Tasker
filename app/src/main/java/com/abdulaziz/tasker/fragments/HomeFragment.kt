package com.abdulaziz.tasker.fragments

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.abdulaziz.tasker.R
import com.abdulaziz.tasker.adapters.ActualTaskAdapter
import com.abdulaziz.tasker.adapters.TaskTypeAdapter
import com.abdulaziz.tasker.databinding.FragmentHomeBinding
import com.abdulaziz.tasker.databinding.ItemBottomsheetBinding
import com.abdulaziz.tasker.utils.TaskTypes
import com.abdulaziz.tasker.utils.Types
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.*
import android.view.WindowManager
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import com.abdulaziz.tasker.database.AppDatabase
import com.abdulaziz.tasker.database.entity.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.Exception

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var actualAdapter: ActualTaskAdapter
    private lateinit var taskAdapter: TaskTypeAdapter
    private lateinit var database: AppDatabase
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var actualTime = "Today"
    private lateinit var sPref: SharedPreferences
    private var animated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sPref = requireActivity().getSharedPreferences("shared", MODE_PRIVATE)
    }

    private fun setActualTasks(string: String) {
        val amount = when (string) {
            "Yesterday" -> -1
            "Today" -> 0
            "Tomorrow" -> 1
            else -> 0
        }
        binding.todayTv.text = string
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, amount)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val actualDay = dateFormat.format(calendar.time)
        var list: List<Task>
        lifecycleScope.launch(Dispatchers.Main) {
            database.taskDao().getTasksByDate(actualDay).collect {
                list = it
                actualAdapter.disableAnimation(animated)
                actualAdapter.setTasks(list)
                if (list.isEmpty()) {
                    binding.nothingTv.visibility = View.VISIBLE
                } else binding.nothingTv.visibility = View.GONE
            }
        }
    }


    private fun initAdapters() {
        val string = arguments?.getString("noti", "")
        actualAdapter =
            ActualTaskAdapter(string.toString(), isByType = false)

        taskAdapter = TaskTypeAdapter(
            TaskTypes.getTypes(requireContext()),
            object : TaskTypeAdapter.OnTypeClickListener {
                @SuppressLint("SetTextI18n")
                override fun onTypeClicked(type: Types) {
                    openBottomSheet(type)
                }
            },
            false
        )

        binding.actualTasksRv.adapter = actualAdapter
        binding.tasksListRv.adapter = taskAdapter
        lifecycleScope.launch(Dispatchers.Main) {
            database.taskDao().getTasks().collect {
                taskAdapter.setTasks(it)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(layoutInflater, container, false)

        database = AppDatabase.getDatabase(binding.root.context)

        onClickListeners()

        initAdapters()
        actualTime = sPref.getString("actual", "Today")!!
        setActualTasks(actualTime)

        return binding.root
    }

    private fun onClickListeners() {
        binding.apply {

            fam.setOnMenuToggleListener {
                if (it) {
                    fam.setMenuButtonColorNormalResId(R.color.blue)
                    fam.menuIconView.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_combined_shape_white
                        )
                    )
                    showPopup(fam)
                } else {
                    fam.setMenuButtonColorNormalResId(R.color.white)
                    fam.menuIconView.setImageDrawable(
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_combined_shape
                        )
                    )
                }
            }

            // Menu Click
            moreIv.setOnClickListener {
                val popupMenu = PopupMenu(requireContext(), moreIv)
                popupMenu.inflate(R.menu.more_menu)
                popupMenu.setOnMenuItemClickListener {
                    actualTime = it.title.toString()
                    sPref.edit().putString("actual", actualTime).apply()
                    setActualTasks(actualTime)
                    true
                }
                popupMenu.show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun openBottomSheet(type: Types) {
        bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.SheetDialog)
        val view = ItemBottomsheetBinding.inflate(layoutInflater)
        view.titleTv.text = type.type_name

        view.fam.setOnMenuToggleListener {
            if (it) {
                view.fam.setMenuButtonColorNormalResId(R.color.blue)
                view.fam.menuIconView.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_combined_shape_white
                    )
                )
                showPopup(view.fam)
            } else {
                view.fam.setMenuButtonColorNormalResId(R.color.white)
                view.fam.menuIconView.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.ic_combined_shape
                    )
                )
            }
        }

        view.linearLayout.background.setTint(type.card_color)
        view.titleTv.setTextColor(type.name_color)
        view.titleTv.compoundDrawablesRelative.forEach {
            it?.setTint(type.name_color)
        }
        view.taskCountTv.setTextColor(type.count_color)
        val adapter = ActualTaskAdapter("", isByType = true)

        lifecycleScope.launch(Dispatchers.IO) {
            val list = database.taskDao().getTasksByType(type.type_name)
            adapter.setTasks(list)
            view.taskCountTv.text = list.size.toString() + if (list.size > 1) " tasks" else " task"
        }

        view.rv.adapter = adapter

        bottomSheetDialog?.setContentView(view.root)
        bottomSheetDialog?.setOnShowListener {
            val dialog = it as BottomSheetDialog
            val parentLayout =
                dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let { v ->
                val behaviour = BottomSheetBehavior.from(v)
                setupFullHeight(v)
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        bottomSheetDialog?.show()

    }


    private fun setupFullHeight(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }

    override fun onPause() {
        super.onPause()
        animated = true
    }


    @SuppressLint("DiscouragedPrivateApi")
    private fun showPopup(view: View) {
        val popupMenu = PopupMenu(requireContext(), view, Gravity.END)

        popupMenu.setOnDismissListener {
            binding.fam.close(true)
        }

        popupMenu.setOnMenuItemClickListener { item: MenuItem? ->
            bottomSheetDialog?.dismiss()
            when (item!!.itemId) {
                R.id.task -> {
                    parentFragmentManager.beginTransaction().replace(
                        R.id.fragment_container,
                        AddTaskFragment()
                    ).addToBackStack("add").commit()
                }
                R.id.list -> {
                    Toast.makeText(requireContext(), "coming soon", Toast.LENGTH_SHORT).show()
                }
            }
            true
        }
        popupMenu.inflate(R.menu.popup_menu)
        try {
            val fieldMPopup = PopupMenu::class.java.getDeclaredField("mPopup")
            fieldMPopup.isAccessible = true
            val mPopup = fieldMPopup.get(popupMenu)
            mPopup.javaClass
                .getDeclaredMethod("setForceShowIcon", Boolean::class.java)
                .invoke(mPopup, true)
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error showing menu icons, ${e.message}")
        } finally {
            popupMenu.show()
        }
    }

}
