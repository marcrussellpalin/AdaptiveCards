package io.adaptivecards.renderer.typeaheadsearch

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import io.adaptivecards.R
import io.adaptivecards.databinding.ActivityTypeAheadSearchBinding

class TypeAheadSearchActivity : AppCompatActivity() {
    private lateinit var viewModel: TypeAheadSearchViewModel
    private lateinit var searchTextView: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBarView: ProgressBar
    private lateinit var clearTextIconView: AppCompatImageButton
    private lateinit var descriptiveImageView: AppCompatImageView
    private lateinit var descriptiveTextView: TextView

    private lateinit var screenTitle: String

    private lateinit var crossIconParams: CrossIconParams
    private lateinit var searchIconParams: SearchIconParams
    private lateinit var backIconParams: BackIconParams
    private lateinit var tickIconParams: TickIconParams
    private lateinit var startSearchingIconParams: StartSearchingStateParams
    private lateinit var errorIconParams: ErrorStateParams
    private lateinit var noResultIconParams: NoResultStateParams


    private var activityTypeAheadSearchBinding: ActivityTypeAheadSearchBinding? = null

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_modal_screen, menu)
        menu?.let {
            val menuItem = menu.findItem(R.id.action_submit)
            val submitButton = menuItem.actionView as ImageButton
            submitButton.setImageResource(tickIconParams.drawableResourceId)
            submitButton.background = null
            submitButton.setOnClickListener {
                onOptionsItemSelected(menuItem)
            }
            // TODO: Add padding in resource file and use from there
            //val padding = resources.getDimensionPixelSize(12dp) / 2
            //submitButton.setPadding(6, 6, 6, 6)
            submitButton.contentDescription = menuItem.title
            submitButton.isEnabled = true
            submitButton.imageAlpha = 255
            submitButton.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        } else if (item.itemId == R.id.action_submit) {
            // TODO: onSubmitClicked()
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val launchParams = intent.getSerializableExtra("launchParams")
        viewModel = ViewModelProvider(this)[TypeAheadSearchViewModel::class.java]
        val choices : MutableList<String> = ArrayList()
        if (launchParams is TypeAheadSearchLaunchParams) {
            launchParams.staticChoices?.let { staticChoices -> staticChoices.forEach { choices.add(it) } }
            crossIconParams = launchParams.crossIconParams
            searchIconParams = launchParams.searchIconParams
            startSearchingIconParams = launchParams.startSearchingIconParams
            errorIconParams = launchParams.errorIconParams
            noResultIconParams = launchParams.noResultIconParams
            tickIconParams = launchParams.tickIconParams
            backIconParams = launchParams.backIconParams
            screenTitle = "Search"
            // TODO: Get screen title from launch params or from type ahead rendering interface
        }
        // pass choices data, host communication interface
        viewModel.init(choices)

        // set theme
        layoutInflater.context.setTheme(R.style.adaptiveCardStyling)

        activityTypeAheadSearchBinding = ActivityTypeAheadSearchBinding.inflate(layoutInflater)
        activityTypeAheadSearchBinding?.let { activityTypeAheadSearchBinding ->
            searchTextView = activityTypeAheadSearchBinding.typeAheadSearchQuery
            recyclerView = activityTypeAheadSearchBinding.searchResultsList
            progressBarView = activityTypeAheadSearchBinding.customOverlayView
            clearTextIconView = activityTypeAheadSearchBinding.clearTextIcon
            descriptiveImageView = activityTypeAheadSearchBinding.errorImage
            descriptiveTextView = activityTypeAheadSearchBinding.errorMsgText
            activityTypeAheadSearchBinding.viewModel = viewModel
        }

        val processClearTextIconVisibility = Observer<String?> { queryText ->
            clearTextIconView.visibility = if (queryText.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.uiState.observe(this, ::processState)
        viewModel.queryText.observe(this, processClearTextIconVisibility)

        clearTextIconView.setBackgroundResource(crossIconParams.drawableResourceId)
        clearTextIconView.setOnClickListener { viewModel.clearText().also { searchTextView.text.clear() } }
        activityTypeAheadSearchBinding?.searchIcon?.setBackgroundResource(searchIconParams.drawableResourceId)

        with(searchTextView) {
            // request focus to the SearchBarView for accessibility once the activity is open.
            isFocusable = true
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
            showKeyboard(this)
            addTextChangedListener(
                DebouncingTextWatcher(
                    lifecycleScope,
                    { s: String ->
                        viewModel.fetchDynamicOptions(s)
                    },
                    250 // TODO: pass host config / launch params value here
                )
            )
        }


        activityTypeAheadSearchBinding?.toolbar?.let { setSupportActionBar(it) }

        supportActionBar?.let { actionBar ->
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(backIconParams.drawableResourceId)
            actionBar.title = screenTitle
        }

        setContentView(activityTypeAheadSearchBinding?.root)
    }

    private fun showKeyboard(view: View): Boolean {
        view.requestFocus()
        val imm = view.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.showSoftInput(view, InputMethodManager.SHOW_FORCED)
    }

    private fun processState(dynamicTypeAheadUiState: DynamicTypeAheadUiState?) {
        when (dynamicTypeAheadUiState) {
            is DynamicTypeAheadUiState.SearchOptions -> {
                recyclerView.visibility = View.GONE
                progressBarView.visibility = View.GONE
                descriptiveImageView.setImageResource(startSearchingIconParams.drawableResourceId)
                descriptiveTextView.text = startSearchingIconParams.text
                descriptiveImageView.visibility = View.VISIBLE
                descriptiveTextView.visibility = View.VISIBLE
                showKeyboard(searchTextView)
            }
            is DynamicTypeAheadUiState.ShowingChoices -> {
                progressBarView.visibility = View.GONE
                descriptiveImageView.visibility = View.GONE
                descriptiveTextView.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
            is DynamicTypeAheadUiState.Loading -> {
                recyclerView.visibility = View.GONE
                descriptiveImageView.visibility = View.GONE
                descriptiveTextView.visibility = View.GONE
                progressBarView.visibility = View.VISIBLE
            }
            is DynamicTypeAheadUiState.NoResults -> {
                recyclerView.visibility = View.GONE
                progressBarView.visibility = View.GONE
                descriptiveImageView.setImageResource(noResultIconParams.drawableResourceId)
                descriptiveTextView.text = noResultIconParams.text
                descriptiveImageView.visibility = View.VISIBLE
                descriptiveTextView.visibility = View.VISIBLE
            }
            is DynamicTypeAheadUiState.Error -> {
                recyclerView.visibility = View.GONE
                progressBarView.visibility = View.GONE
                descriptiveImageView.setImageResource(errorIconParams.drawableResourceId)
                descriptiveTextView.text = errorIconParams.text
                descriptiveImageView.visibility = View.VISIBLE
                descriptiveTextView.visibility = View.VISIBLE
            }
        }
    }
}