package com.flutterwave.raveandroid.ach;

import android.content.Context;
import android.view.View;

import com.flutterwave.raveandroid.DeviceIdGetter;
import com.flutterwave.raveandroid.Meta;
import com.flutterwave.raveandroid.Payload;
import com.flutterwave.raveandroid.PayloadBuilder;
import com.flutterwave.raveandroid.PayloadEncryptor;
import com.flutterwave.raveandroid.PayloadToJsonConverter;
import com.flutterwave.raveandroid.RaveConstants;
import com.flutterwave.raveandroid.RavePayInitializer;
import com.flutterwave.raveandroid.TransactionStatusChecker;
import com.flutterwave.raveandroid.card.ChargeRequestBody;
import com.flutterwave.raveandroid.data.Callbacks;
import com.flutterwave.raveandroid.data.NetworkRequestImpl;
import com.flutterwave.raveandroid.data.RequeryRequestBody;
import com.flutterwave.raveandroid.data.SharedPrefsRequestImpl;
import com.flutterwave.raveandroid.di.DaggerTestAppComponent;
import com.flutterwave.raveandroid.di.TestAndroidModule;
import com.flutterwave.raveandroid.di.TestAppComponent;
import com.flutterwave.raveandroid.di.TestNetworkModule;
import com.flutterwave.raveandroid.responses.ChargeResponse;
import com.flutterwave.raveandroid.responses.RequeryResponse;
import com.flutterwave.raveandroid.responses.SubAccount;
import com.flutterwave.raveandroid.validators.AmountValidator;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AchPresenterTest {

    @Mock
    AchContract.View view;
    @Inject
    Context context;
    @Inject
    AmountValidator amountValidator;
    @Inject
    RavePayInitializer ravePayInitializer;
    @Inject
    DeviceIdGetter deviceIdGetter;
    @Inject
    PayloadToJsonConverter payloadToJsonConverter;
    @Inject
    PayloadEncryptor payloadEncryptor;
    @Inject
    NetworkRequestImpl networkRequest;
    @Inject
    SharedPrefsRequestImpl sharedPrefsRequest;
    @Inject
    TransactionStatusChecker transactionStatusChecker;
    @Mock
    AchPresenter achPresenterMock;

    @Mock
    ArrayList<Integer> orderedPaymentTypesList = new ArrayList<>();
    private AchPresenter achPresenter;

    @Before
    public void setUp() {

        MockitoAnnotations.initMocks(this);
        achPresenter = new AchPresenter(context, view);

        TestAppComponent component = DaggerTestAppComponent.builder()
                .testAndroidModule(new TestAndroidModule())
                .testNetworkModule(new TestNetworkModule())
                .build();

        component.inject(this);
        component.inject(achPresenter);

    }

    @Test
    public void init_validAmount_onAmountValidatedCalledWithCorrectParams_showRedirectMessageTrue() {

        Double amount = generateRandomDouble();
        when(amountValidator.isAmountValid(amount)).thenReturn(true);
        when(ravePayInitializer.getAmount()).thenReturn(amount);

        achPresenter.init(ravePayInitializer);

        verify(view).onAmountValidated(String.valueOf(amount), View.GONE);
        verify(view).showRedirectMessage(true);

    }

    @Test
    public void init_inValidAmount_onAmountValidatedCalledWithCorrectParams_showRedirectMessageFalse() {

        Double amount = generateRandomDouble();
        when(amountValidator.isAmountValid(amount)).thenReturn(false);
        when(ravePayInitializer.getAmount()).thenReturn(amount);

        achPresenter.init(ravePayInitializer);

        verify(view).onAmountValidated("", View.VISIBLE);
        verify(view).showRedirectMessage(false);

    }


    @Test
    public void processTransaction_chargeAccountCalledWithCorrectParams() {
        //arrange

        boolean isDisplayFee = generateRandomBoolean();
        String encryptionKey = generateRandomString();
        String amount = generateRandomDouble().toString();
        String country = generateRandomString();
        String currency = generateRandomString();
        String email = generateRandomString();
        String firstName = generateRandomString();
        String lastName = generateRandomString();
        String deviceId = generateRandomString();
        String txRef = generateRandomString();
        String meta = generateRandomString();
        String pubKey = generateRandomString();
        boolean isAch = generateRandomBoolean();
        String fingerPrint = deviceId;

        achPresenterMock.deviceIdGetter = deviceIdGetter;
        when(deviceIdGetter.getDeviceId()).thenReturn(deviceId);
        when(ravePayInitializer.getIsDisplayFee()).thenReturn(isDisplayFee);
        when(ravePayInitializer.getEncryptionKey()).thenReturn(encryptionKey);
        when(ravePayInitializer.getAmount()).thenReturn(Double.parseDouble(amount));
        when(ravePayInitializer.getCountry()).thenReturn(country);
        when(ravePayInitializer.getCurrency()).thenReturn(currency);
        when(ravePayInitializer.getEmail()).thenReturn(email);
        when(ravePayInitializer.getfName()).thenReturn(firstName);
        when(ravePayInitializer.getlName()).thenReturn(lastName);
        when(ravePayInitializer.getTxRef()).thenReturn(txRef);
        when(ravePayInitializer.getMeta()).thenReturn(meta);
        when(ravePayInitializer.getOrderedPaymentTypesList()).thenReturn(orderedPaymentTypesList);
        when(orderedPaymentTypesList.contains(RaveConstants.PAYMENT_TYPE_ACH)).thenReturn(isAch);
        when(ravePayInitializer.getPublicKey()).thenReturn(pubKey);

        //act
        doCallRealMethod().when(achPresenterMock).processTransaction(any(String.class), any(RavePayInitializer.class), anyBoolean());
        achPresenterMock.processTransaction(amount, ravePayInitializer, ravePayInitializer.getIsDisplayFee());

        ArgumentCaptor<String> captorEncryptionKey = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> captorIsDisplayFee = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Payload> payloadCaptor = ArgumentCaptor.forClass(Payload.class);

        verify(achPresenterMock).chargeAccount(payloadCaptor.capture(),
                captorEncryptionKey.capture(),
                captorIsDisplayFee.capture());

        //assert
        assertEquals(encryptionKey, captorEncryptionKey.getValue());
        assertEquals(isDisplayFee, captorIsDisplayFee.getValue());

        Payload capturedPayload = payloadCaptor.getValue();
        assertEquals(deviceId, capturedPayload.getDevice_fingerprint());
        assertEquals(amount, capturedPayload.getAmount());
        assertEquals(country, capturedPayload.getCountry());
        assertEquals(currency, capturedPayload.getCurrency());
        assertEquals(email, capturedPayload.getEmail());
        assertEquals(firstName, capturedPayload.getFirstname());
        assertEquals(lastName, capturedPayload.getLastname());
        assertEquals(fingerPrint, capturedPayload.getIP());
        assertEquals(txRef, capturedPayload.getTxRef());
        assertEquals(pubKey, capturedPayload.getPBFPubKey());
        assertEquals(isAch, capturedPayload.isIs_us_bank_charge());
        assertEquals(deviceId, capturedPayload.getDevice_fingerprint());

    }


    @Test
    public void processTransaction_setAmountCalledOnRavePayInitializerWithCorrectParam() {
        //arrange
        String amount = generateRandomDouble().toString();
        when(deviceIdGetter.getDeviceId()).thenReturn(generateRandomString());

        //act
        achPresenter.processTransaction(amount, ravePayInitializer, ravePayInitializer.getIsDisplayFee());

        //assert
        verify(ravePayInitializer).setAmount(Double.parseDouble(amount));

    }


    @Test
    public void chargeAccount_noDisplayFee_onSuccess_validResponseReturned_showWebViewCalled() {

        Payload payload = generatePayload();

        payload.setPBFPubKey(generateRandomString());

        when(payloadToJsonConverter.convertChargeRequestPayloadToJson(any(Payload.class))).thenReturn(generateRandomString());
        when(payloadEncryptor.getEncryptedData(any(String.class), any(String.class))).thenReturn(generateRandomString());

        achPresenter.chargeAccount(payload, generateRandomString(), false);
        verify(view).showProgressIndicator(true);

        ArgumentCaptor<Callbacks.OnChargeRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnChargeRequestComplete.class);

        verify(networkRequest).chargeCard(any(ChargeRequestBody.class), captor.capture());
        captor.getAllValues().get(0).onSuccess(generateValidChargeResponse(), generateRandomString());

        verify(sharedPrefsRequest).saveFlwRef(any(String.class));
        verify(view).showProgressIndicator(false);
        verify(view, never()).showFee(any(String.class), any(String.class), any(String.class), any(String.class));
        verify(view).showWebView(any(String.class), any(String.class));

    }

    @Test
    public void chargeAccount_displayFee_chargeCard_onSuccess_validResponseReturned_showFeeCalled() {

        Payload payload = generatePayload();
        payload.setPBFPubKey(generateRandomString());

        when(payloadToJsonConverter.convertChargeRequestPayloadToJson(any(Payload.class))).thenReturn(generateRandomString());
        when(payloadEncryptor.getEncryptedData(any(String.class), any(String.class))).thenReturn(generateRandomString());

        achPresenter.chargeAccount(payload, generateRandomString(), true);

        verify(view).showProgressIndicator(true);

        ArgumentCaptor<Callbacks.OnChargeRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnChargeRequestComplete.class);

        verify(networkRequest).chargeCard(any(ChargeRequestBody.class), captor.capture());
        captor.getAllValues().get(0).onSuccess(generateValidChargeResponse(), generateRandomString());

        verify(sharedPrefsRequest).saveFlwRef(any(String.class));
        verify(view).showProgressIndicator(false);

        verify(view, never()).showWebView(any(String.class), any(String.class));
        verify(view).showFee(any(String.class), any(String.class), any(String.class), any(String.class));

    }


    @Test
    public void chargeAccount_onSuccess_nullChargeResponseReturned_onPaymentErrorCalled_noResponseMessage() {

        Payload payload = generatePayload();
        payload.setPBFPubKey(generateRandomString());

        when(payloadToJsonConverter.convertChargeRequestPayloadToJson(any(Payload.class))).thenReturn(generateRandomString());
        when(payloadEncryptor.getEncryptedData(any(String.class), any(String.class))).thenReturn(generateRandomString());

        achPresenter.chargeAccount(payload, generateRandomString(), true);

        verify(view).showProgressIndicator(true);
        ArgumentCaptor<Callbacks.OnChargeRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnChargeRequestComplete.class);

        verify(networkRequest).chargeCard(any(ChargeRequestBody.class), captor.capture());

        captor.getAllValues().get(0).onSuccess(generateNullChargeResponse(), any(String.class));
        verify(view).showProgressIndicator(false);

        verify(view).onPaymentError(RaveConstants.noResponse);

    }

    @Test
    public void chargeAccount_onSuccess_inValidResponseReturned_onPaymentErrorCalled_noAuthUrlMessage() {

        Payload payload = generatePayload();
        payload.setPBFPubKey(generateRandomString());

        when(payloadToJsonConverter.convertChargeRequestPayloadToJson(any(Payload.class))).thenReturn(generateRandomString());
        when(payloadEncryptor.getEncryptedData(any(String.class), any(String.class))).thenReturn(generateRandomString());

        achPresenter.chargeAccount(payload, generateRandomString(), true);

        verify(view).showProgressIndicator(true);

        ArgumentCaptor<Callbacks.OnChargeRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnChargeRequestComplete.class);

        verify(networkRequest).chargeCard(any(ChargeRequestBody.class), captor.capture());

        captor.getAllValues().get(0).onSuccess(generateRandomChargeResponse(), generateRandomString());
        verify(view).showProgressIndicator(false);

        verify(view).onPaymentError(RaveConstants.no_authurl_was_returnedmsg);

    }

    @Test
    public void chargeAccount_onError_onPaymentErrorCalled_messageReturned() {

        Payload payload = generatePayload();
        payload.setPBFPubKey(generateRandomString());

        String message = generateRandomString();

        when(payloadToJsonConverter.convertChargeRequestPayloadToJson(any(Payload.class))).thenReturn(generateRandomString());
        when(payloadEncryptor.getEncryptedData(any(String.class), any(String.class))).thenReturn(generateRandomString());

        achPresenter.chargeAccount(payload, generateRandomString(), true);
        verify(view).showProgressIndicator(true);
        ArgumentCaptor<Callbacks.OnChargeRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnChargeRequestComplete.class);

        verify(networkRequest).chargeCard(any(ChargeRequestBody.class), captor.capture());

        captor.getAllValues().get(0).onError(message, generateRandomString());

        verify(view).showProgressIndicator(false);
        verify(view).onPaymentError(message);

    }

    @Test
    public void onFeeConfirmed_showWebViewCalledWithCorrectParams() {
        String authUrl = generateRandomString();
        String flwRef = generateRandomString();
        achPresenter.onFeeConfirmed(authUrl, flwRef);
        verify(view).showWebView(authUrl, flwRef);
    }

    @Test
    public void requeryTx_onSuccess_onRequerySuccessfulCalledWithCorrectParams() {
        String flwRef = generateRandomString();
        RequeryResponse requeryResponse = generateRequerySuccessful();
        String jsonResponse = generateRandomString();

        when(sharedPrefsRequest.fetchFlwRef()).thenReturn(flwRef);
        achPresenter.requeryTx(generateRandomString());

        verify(view).showProgressIndicator(true);
        ArgumentCaptor<Callbacks.OnRequeryRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnRequeryRequestComplete.class);
        verify(networkRequest).requeryTx(any(RequeryRequestBody.class), captor.capture());
        captor.getAllValues().get(0).onSuccess(requeryResponse, jsonResponse);

        verify(view).showProgressIndicator(false);
        verify(view).onRequerySuccessful(requeryResponse, jsonResponse, flwRef);

    }


    @Test
    public void requeryTx_onError_onPaymentFailedCalledWithCorrectParams() {

        String message = generateRandomString();
        String jsonResponse = generateRandomString();

        achPresenter.requeryTx(generateRandomString());
        verify(view).showProgressIndicator(true);
        ArgumentCaptor<Callbacks.OnRequeryRequestComplete> captor = ArgumentCaptor.forClass(Callbacks.OnRequeryRequestComplete.class);
        verify(networkRequest).requeryTx(any(RequeryRequestBody.class), captor.capture());
        captor.getAllValues().get(0).onError(message, jsonResponse);
        verify(view).onPaymentFailed(message, jsonResponse);

    }

    @Test
    public void verifyRequeryResponseStatus_transactionUnsuccessful_onPaymentFailedCalled() {
        when(transactionStatusChecker.getTransactionStatus(anyString(), anyString(), anyString())).thenReturn(false);
        achPresenter.verifyRequeryResponse(generateRequerySuccessful(), generateRandomString(), ravePayInitializer, generateRandomString());
        verify(view).onPaymentFailed(String.valueOf(anyObject()), anyString());
    }

    @Test
    public void verifyRequeryResponseStatus_transactionSuccessful_onPaymentSuccessfulCalled() {

        when(transactionStatusChecker.getTransactionStatus(any(String.class), any(String.class), any(String.class)))
                .thenReturn(true);

        when(ravePayInitializer.getAmount()).thenReturn(generateRandomDouble());
        when(ravePayInitializer.getCurrency()).thenReturn(generateRandomString());

        achPresenter.verifyRequeryResponse(generateRequerySuccessful(), generateRandomString(), ravePayInitializer, generateRandomString());
        verify(view).onPaymentSuccessful(String.valueOf(anyObject()), anyString(), anyString());
    }

    @Test
    public void onDataCollected_validAmount_showAmountError_onValidationSuccessfulCalledWithCorrectParams() {

        String amount = generateRandomDouble().toString();

        when(amountValidator.isAmountValid(amount)).thenReturn(true);
        achPresenter.onDataCollected(ravePayInitializer, amount);
        verify(view).showAmountError(null);
        verify(view).onValidationSuccessful(amount);
    }

    @Test
    public void onDataCollected_inValidAmount_showAmountErrorWithCorrectParams() {
        when(amountValidator.isAmountValid(ravePayInitializer.getAmount())).thenReturn(false);
        achPresenter.onDataCollected(ravePayInitializer, anyString());
        verify(view).showAmountError(null);
        verify(view).showAmountError(RaveConstants.validAmountPrompt);
    }

    private Double generateRandomDouble() {
        return new Random().nextDouble();
    }

    private boolean generateRandomBoolean() {
        return new Random().nextBoolean();
    }

    private String generateRandomString() {
        return UUID.randomUUID().toString();
    }

    private RequeryResponse generateRequerySuccessful() {
        return new RequeryResponse();
    }

    private Payload generatePayload() {
        List<Meta> metas = new ArrayList<>();
        List<SubAccount> subAccounts = new ArrayList<>();
        return new Payload(generateRandomString(), metas, subAccounts, generateRandomString(), generateRandomString(), generateRandomString(), generateRandomString(), generateRandomString(), generateRandomString(), generateRandomString(), generateRandomString(), generateRandomString(), generateRandomString(), generateRandomString());
    }

    private ChargeResponse generateNullChargeResponse() {
        ChargeResponse chargeResponse = new ChargeResponse();
        chargeResponse.setData(null);

        return chargeResponse;
    }

    private PayloadBuilder generatePayloadBuilder() {
        PayloadBuilder payloadBuilder = new PayloadBuilder();
        payloadBuilder.setPaymentPlan(generateRandomString());
        return payloadBuilder;
    }

    private ChargeResponse generateRandomChargeResponse() {
        ChargeResponse chargeResponse = new ChargeResponse();
        chargeResponse.setData(new ChargeResponse.Data());
        chargeResponse.getData().setChargeResponseCode("00");
        chargeResponse.getData().setAuthurl(null);
        return chargeResponse;
    }

    private ChargeResponse generateValidChargeResponse() {
        ChargeResponse chargeResponse = new ChargeResponse();
        chargeResponse.setData(new ChargeResponse.Data());
        chargeResponse.getData().setChargeResponseCode("00");
        chargeResponse.getData().setCurrency(generateRandomString());
        chargeResponse.getData().setChargedAmount(generateRandomString());
        chargeResponse.getData().setAuthurl(generateRandomString());
        chargeResponse.getData().setFlwRef(generateRandomString());
        return chargeResponse;
    }
}