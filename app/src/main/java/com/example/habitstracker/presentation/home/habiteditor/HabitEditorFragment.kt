package com.example.habitstracker.presentation.home.habiteditor

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.domain.utils.HabitFrequency
import com.example.domain.utils.HabitPriority
import com.example.domain.utils.HabitType
import com.example.habitstracker.App
import com.example.habitstracker.R
import com.example.habitstracker.databinding.FragmentHabitEditorBinding
import com.example.habitstracker.presentation.home.Mapper
import com.example.habitstracker.presentation.main.MainActivity
import com.example.habitstracker.utils.ColorPicker
import com.google.android.material.textfield.TextInputLayout
import javax.inject.Inject


class HabitEditorFragment : Fragment(), ColorPicker.OnColorSquareItemListener {

    private var _binding: FragmentHabitEditorBinding? = null
    private val binding get() = _binding!!

    private val args: HabitEditorFragmentArgs by navArgs()

    private lateinit var prioritiesItems: List<String>
    private lateinit var intervalItems: List<String>

    private lateinit var colorPicker: ColorPicker

    @Inject
    lateinit var factory: HabitEditorViewModelFactory

    lateinit var viewModel: HabitEditorViewModel

    @Inject
    lateinit var mapper: Mapper

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_habit_editor, container, false)

        (requireActivity().application as App).applicationComponent.habitSubcomponent().build().inject(this)
        viewModel = ViewModelProvider(requireParentFragment(), factory).get(HabitEditorViewModel::class.java)

        fixEditTextName()

        initViewModel()
        setAdapters()

        colorPicker = ColorPicker(this, binding.habitsColorLinearLayout)

        fillForm()

        binding.confirmHabitButton.setOnClickListener(this::onSaveHabitButtonClicked)
        binding.radioGroupHabitType.setOnCheckedChangeListener(this::onHabitTypeCheckedChange)

        binding.viewModel = viewModel

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    private fun fixEditTextName() {
        // Make first letter uppercase
        binding.editTextName.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
    }

    private fun initViewModel() {
        val habit = args.habit
        viewModel.setData(habit)
    }

    private fun setAdapters() {
        prioritiesItems = HabitPriority.values().map { mapper.mapToString(it) }
        val priorityAdapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, prioritiesItems)
        binding.editTextPriority.setAdapter(priorityAdapter)
        binding.editTextPriority.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                viewModel.habitBuilder.priority = mapper.mapToHabitPriority(prioritiesItems[position])
            }

        intervalItems = HabitFrequency.values().map { mapper.mapToString(it) }
        val intervalAdapter = ArrayAdapter(requireContext(), R.layout.support_simple_spinner_dropdown_item, intervalItems)
        binding.editTextInterval.setAdapter(intervalAdapter)
        binding.editTextInterval.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                viewModel.habitBuilder.frequency = mapper.mapToHabitFrequency(intervalItems[position])
            }
    }

    private fun fillForm() {
        val supportActionBarHelper = activity as SupportActionBarHelper
        if (viewModel.habitBuilder.id != null) {
            supportActionBarHelper.changeSupportActionBarTitle(resources.getString(R.string.header_edit_habit))
            binding.confirmHabitButton.text = resources.getString(R.string.habit_button_save)
            binding.confirmHabitButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_edit)
            setCheckedHabitTypeRadioButton()
            binding.editTextPriority.setText(viewModel.habitBuilder.priority?.toString(), false)
            binding.editTextInterval.setText(viewModel.habitBuilder.frequency?.toString(), false)
        }
        else {
            supportActionBarHelper.changeSupportActionBarTitle(resources.getString(R.string.header_new_habit))
            binding.confirmHabitButton.text = resources.getString(R.string.habit_button_add)
            binding.confirmHabitButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_add)
        }
        setSelectedColorToView()
    }

    private fun setCheckedHabitTypeRadioButton() {
        when(viewModel.habitBuilder.type) {
            HabitType.Good -> binding.radioGroupHabitType.check(R.id.radioButtonGoodHabit)
            HabitType.Bad -> binding.radioGroupHabitType.check(R.id.radioButtonBadHabit)
        }
    }

    private fun onHabitTypeCheckedChange(group: RadioGroup, id: Int) {
        when (id) {
            R.id.radioButtonGoodHabit -> viewModel.habitBuilder.type = HabitType.Good
            R.id.radioButtonBadHabit -> viewModel.habitBuilder.type = HabitType.Bad
        }
    }

    override fun onColorSquareItemClick(view: View) {
        viewModel.habitBuilder.color = view.tag.toString().toInt()
        setSelectedColorToView()
    }

    private fun setSelectedColorToView() {
        if (viewModel.habitBuilder.color != null) {
            binding.selectedHabitColor.setColorFilter(viewModel.habitBuilder.color!!)
            binding.selectedHabitColorRGB.text = getSelectedColorRGB()
            val hsv = getSelectedColorHSV()
            binding.selectedHabitColorHSV.text = "${hsv[0]}°, ${hsv[1]}, ${hsv[2]}"
        }
        else
            binding.selectedHabitColor.setColorFilter(
                    ContextCompat.getColor(
                            requireContext(),
                            R.color.gray
                    )
            )
    }

    private fun getSelectedColorRGB(): String {
        return String.format("#%06X", (0xFFFFFF and viewModel.habitBuilder.color!!))
    }

    private fun getSelectedColorHSV(): FloatArray {
        val hsv = FloatArray(3)
        Color.colorToHSV(viewModel.habitBuilder.color!!, hsv)
        return hsv
    }

    private fun onSaveHabitButtonClicked(view: View) {
        if (isAllFieldsFilled()) {
            viewModel.saveHabit()
            val navController = findNavController()
            navController.popBackStack()
        }
    }

    private fun isAllFieldsFilled(): Boolean {
        clearFocus()

        val inputViews = listOf(
                InputView(binding.editTextName, binding.inputLayoutName, R.string.error_fill_this_field),
                InputView(binding.editTextDescription, binding.inputLayoutDescription, R.string.error_fill_this_field),
                InputView(binding.editTextPriority, binding.inputLayoutPriority, R.string.error_make_a_choice),
                InputView(binding.editTextTimesCount, binding.inputLayoutTimesCount, R.string.error_fill_this_field),
                InputView(binding.editTextInterval, binding.inputLayoutInterval, R.string.error_make_a_choice)
        )

        return isNoErrorInFields(inputViews).and(isTypeSelected().and(isColorSelected()))
    }

    private fun clearFocus() {
        val viewInFocus = activity?.currentFocus
        val inputMethodManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        // Hide keyboard
        inputMethodManager?.hideSoftInputFromWindow(viewInFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        // Clear focus of last input field
        viewInFocus?.clearFocus()
    }

    private fun isTypeSelected(): Boolean {
        var result = true
        if (binding.radioGroupHabitType.checkedRadioButtonId == View.NO_ID) {
            result = false
            binding.radioGroupError.setText(R.string.error_make_a_choice)
            binding.radioGroupError.visibility = View.VISIBLE
        } else
            binding.radioGroupError.visibility = View.GONE

        return result
    }

    private fun isColorSelected(): Boolean {
        val result = viewModel.habitBuilder.color != null
        if (!result) {
            binding.selectedHabitColorError.setText(R.string.error_make_a_choice)
            binding.selectedHabitColorError.visibility = View.VISIBLE
        } else {
            binding.selectedHabitColorError.visibility = View.GONE
        }
        return result
    }

    private fun isNoErrorInFields(inputViews: List<InputView>): Boolean {
        var result = true

        for (view in inputViews) {
            if (view.inputView.text.toString().isEmpty()) {
                result = false
                view.errorLayout.error = getString(view.errorStringResource)
            } else
                view.errorLayout.isErrorEnabled = false
        }

        return result
    }

    private data class InputView(
            val inputView: EditText,
            val errorLayout: TextInputLayout,
            val errorStringResource: Int
    )

    interface SupportActionBarHelper {
        fun changeSupportActionBarTitle(title: String)
    }

    companion object {

        private const val TAG = "FragmentHabitEditor"

    }

}