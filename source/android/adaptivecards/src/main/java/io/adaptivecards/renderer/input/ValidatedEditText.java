package io.adaptivecards.renderer.input;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.text.AttributedCharacterIterator;

import io.adaptivecards.R;
import io.adaptivecards.renderer.inputhandler.TextInputHandler;

@SuppressLint("AppCompatCustomView")
public class ValidatedEditText extends EditText
{
    public ValidatedEditText(Context context)
    {
        super(context);
        setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        m_isInvalid = false;
        verifyIfUsingCustomInputs(context);
    }

    public ValidatedEditText(Context context, int errorColor)
    {
        this(context);
        m_errorColor = errorColor;
    }

    /**
     * Peeks the usingCustomInputs attribute in the style to verify if custom inputs are being used
     * @param context Context to retrieve the value from
     */
    public void verifyIfUsingCustomInputs(Context context)
    {
        Resources.Theme theme = context.getTheme();
        TypedValue isUsingCustomInputs = new TypedValue();

        if (theme.resolveAttribute(R.attr.usingCustomInputs, isUsingCustomInputs, true))
        {
            m_isUsingCustomInputs = (isUsingCustomInputs.data != 0);
        }
        else
        {
            m_isUsingCustomInputs = false;
        }
    }

    /**
     * Updates the rendered edit text to display the error visual cues
     * @param isValid Value signalizing if input has valid value
     */
    public void setValidationResult(boolean isValid)
    {
        if (m_isUsingCustomInputs)
        {
            // If using custom inputs, set the selector state for state_input_invalid as true
            setInputInvalid(!isValid);
        }
        else
        {
            // If not using custom inputs, tint the whole input with attention color
            if (isValid)
            {
                getBackground().setColorFilter(null);
            }
            else
            {
                getBackground().setColorFilter(m_errorColor, PorterDuff.Mode.SRC_ATOP);
            }
            invalidate();
        }
    }

    /**
     * Refreshes the visual selector state for valid or invalid style
     * @param isInvalid Value signalizing if the input has failed validation
     */
    public void setInputInvalid(boolean isInvalid)
    {
        m_isInvalid = isInvalid;
        refreshDrawableState();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace)
    {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (m_isInvalid)
        {
            mergeDrawableStates(drawableState, STATE_INPUT_INVALID);
        }

        return drawableState;
    }

    private static final int[] STATE_INPUT_INVALID = {R.attr.state_input_invalid};
    private boolean m_isInvalid = false;
    private boolean m_isUsingCustomInputs = false;
    private int m_errorColor = Color.TRANSPARENT;
}
