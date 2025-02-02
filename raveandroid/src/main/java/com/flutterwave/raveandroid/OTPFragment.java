package com.flutterwave.raveandroid;


import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 */
public class OTPFragment extends Fragment implements View.OnClickListener {

    public static final String EXTRA_OTP = "extraOTP";
    TextInputEditText otpEt;
    TextInputLayout otpTil;
    TextView chargeMessage;
    Button otpButton;

    public static final String EXTRA_CHARGE_MESSAGE = "extraChargeMessage";
    View v;
    String otp;

    public OTPFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        v = inflater.inflate(R.layout.fragment_ot, container, false);

        initializeViews();

        setChargeMessage();

        setListeners();

        return v;
    }

    private void setListeners() {
        otpButton.setOnClickListener(this);
    }

    private void initializeViews() {
        otpTil = v.findViewById(R.id.otpTil);
        otpEt = v.findViewById(R.id.otpEv);
        otpButton = v.findViewById(R.id.otpButton);
        chargeMessage = v.findViewById(R.id.otpChargeMessage);
    }

    @Override
    public void onClick(View view) {
        int i = view.getId();
        if (i == R.id.otpButton) {
            clearErrors();
            otp = otpEt.getText().toString();
            if (otp.length() < 1) {
                otpTil.setError("Enter a valid one time password");
            } else {
                goBack();
            }
        }
    }

    private void clearErrors() {
        otpTil.setErrorEnabled(false);
        otpTil.setError(null);
    }

    private void setChargeMessage() {
        if (getArguments() != null) {
            if (getArguments().containsKey(EXTRA_CHARGE_MESSAGE)) {
                chargeMessage.setText(getArguments().getString(EXTRA_CHARGE_MESSAGE));
            }
        }
    }

    public void goBack(){
        Intent intent = new Intent();
        intent.putExtra(EXTRA_OTP, otp);
        if (getActivity() != null) {
            getActivity().setResult(RavePayActivity.RESULT_SUCCESS, intent);
            getActivity().finish();
        }
    }


}
